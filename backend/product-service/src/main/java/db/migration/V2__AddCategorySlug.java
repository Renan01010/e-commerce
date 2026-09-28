package db.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstore.product.domain.CategorySlug;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V2__AddCategorySlug extends BaseJavaMigration {
    private static final String OVERRIDES_PLACEHOLDER = "categorySlugOverrides";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        List<LegacyCategory> categories = loadCategories(connection);
        Map<UUID, String> overrides = parseOverrides(context.getConfiguration().getPlaceholders()
                .get(OVERRIDES_PLACEHOLDER));
        Map<UUID, String> finalSlugs = resolveAndValidateSlugs(categories, overrides);

        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE categories ADD COLUMN slug VARCHAR(" + CategorySlug.MAX_LENGTH + ")");
        }

        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE categories SET slug = ? WHERE id = ?")) {
            for (Map.Entry<UUID, String> entry : finalSlugs.entrySet()) {
                update.setString(1, entry.getValue());
                update.setObject(2, entry.getKey());
                if (update.executeUpdate() != 1) {
                    throw new FlywayException("Category disappeared during slug migration: " + entry.getKey());
                }
            }
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE categories ALTER COLUMN slug SET NOT NULL");
            statement.execute("ALTER TABLE categories ADD CONSTRAINT uk_categories_slug UNIQUE (slug)");
            statement.execute("ALTER TABLE categories ADD CONSTRAINT chk_categories_slug_canonical "
                    + "CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$')");
        }
    }

    private List<LegacyCategory> loadCategories(Connection connection) throws SQLException {
        List<LegacyCategory> categories = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery("SELECT id, name FROM categories ORDER BY id")) {
            while (results.next()) {
                categories.add(new LegacyCategory(results.getObject("id", UUID.class), results.getString("name")));
            }
        }
        return categories;
    }

    private Map<UUID, String> parseOverrides(String json) {
        if (json == null || json.isBlank()) return Map.of();

        final JsonNode rawOverrides;
        try {
            rawOverrides = objectMapper.readTree(json);
        } catch (Exception exception) {
            throw new FlywayException("CATEGORY_SLUG_OVERRIDES_JSON must be a JSON object mapping categoryId to slug", exception);
        }
        if (rawOverrides == null || !rawOverrides.isObject()) {
            throw new FlywayException("CATEGORY_SLUG_OVERRIDES_JSON must be a JSON object mapping categoryId to slug");
        }

        Map<UUID, String> overrides = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> entries = rawOverrides.fields();
        while (entries.hasNext()) {
            Map.Entry<String, JsonNode> entry = entries.next();
            final UUID categoryId;
            try {
                categoryId = UUID.fromString(entry.getKey());
            } catch (IllegalArgumentException exception) {
                throw new FlywayException("Invalid categoryId in CATEGORY_SLUG_OVERRIDES_JSON: " + entry.getKey(), exception);
            }
            if (!entry.getValue().isTextual()) {
                throw new FlywayException("Slug override must be a JSON string for categoryId " + categoryId);
            }
            final String canonicalSlug;
            try {
                canonicalSlug = CategorySlug.normalize(entry.getValue().textValue());
            } catch (IllegalArgumentException exception) {
                throw new FlywayException("Invalid slug override for categoryId " + categoryId + ": " + exception.getMessage(), exception);
            }
            if (!canonicalSlug.equals(entry.getValue().textValue())) {
                throw new FlywayException("Slug override must already be canonical for categoryId " + categoryId
                        + ": " + entry.getValue().textValue());
            }
            overrides.put(categoryId, canonicalSlug);
        }
        return overrides;
    }

    private Map<UUID, String> resolveAndValidateSlugs(List<LegacyCategory> categories, Map<UUID, String> overrides) {
        Map<UUID, String> candidates = new LinkedHashMap<>();
        Map<UUID, String> derivationErrors = new LinkedHashMap<>();
        for (LegacyCategory category : categories) {
            try {
                candidates.put(category.id(), CategorySlug.fromName(category.name()));
            } catch (IllegalArgumentException exception) {
                derivationErrors.put(category.id(), exception.getMessage());
            }
        }

        Set<UUID> knownIds = categories.stream().map(LegacyCategory::id).collect(Collectors.toSet());
        List<UUID> unknownOverrides = overrides.keySet().stream()
                .filter(categoryId -> !knownIds.contains(categoryId))
                .sorted()
                .toList();
        if (!unknownOverrides.isEmpty()) {
            throw new FlywayException("CATEGORY_SLUG_OVERRIDES_JSON contains unknown categoryIds: " + unknownOverrides);
        }

        Map<String, List<LegacyCategory>> categoriesByCandidate = categories.stream()
                .filter(category -> candidates.containsKey(category.id()))
                .collect(Collectors.groupingBy(category -> candidates.get(category.id()), TreeMap::new, Collectors.toList()));

        Map<UUID, String> requiredOverrideReasons = new LinkedHashMap<>();
        for (Map.Entry<String, List<LegacyCategory>> group : categoriesByCandidate.entrySet()) {
            if (group.getValue().size() > 1) {
                for (LegacyCategory category : group.getValue()) {
                    requiredOverrideReasons.put(category.id(), group.getKey());
                }
            }
        }
        derivationErrors.forEach((categoryId, reason) -> requiredOverrideReasons.put(categoryId, "<no-candidate: " + reason + ">"));

        List<UUID> missingOverrides = requiredOverrideReasons.keySet().stream()
                .filter(categoryId -> !overrides.containsKey(categoryId))
                .sorted()
                .toList();
        if (!missingOverrides.isEmpty()) {
            throw collisionException(categories, candidates, requiredOverrideReasons, missingOverrides,
                    "Missing explicit categoryId-to-slug overrides");
        }

        List<UUID> unnecessaryOverrides = overrides.keySet().stream()
                .filter(categoryId -> !requiredOverrideReasons.containsKey(categoryId))
                .sorted()
                .toList();
        if (!unnecessaryOverrides.isEmpty()) {
            throw new FlywayException("CATEGORY_SLUG_OVERRIDES_JSON contains categoryIds that do not require collision resolution: "
                    + unnecessaryOverrides);
        }

        Map<UUID, String> resolvedSlugs = new LinkedHashMap<>(candidates);
        resolvedSlugs.putAll(overrides);
        Map<String, List<UUID>> idsByFinalSlug = new HashMap<>();
        resolvedSlugs.forEach((categoryId, slug) -> idsByFinalSlug.computeIfAbsent(slug, ignored -> new ArrayList<>()).add(categoryId));
        Map<UUID, String> remainingCollisions = new LinkedHashMap<>();
        idsByFinalSlug.forEach((slug, ids) -> {
            if (ids.size() > 1) ids.forEach(categoryId -> remainingCollisions.put(categoryId, slug));
        });
        if (!remainingCollisions.isEmpty()) {
            throw collisionException(categories, resolvedSlugs, remainingCollisions,
                    remainingCollisions.keySet().stream().sorted().toList(),
                    "Slug overrides still collide after normalization");
        }

        return resolvedSlugs.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (first, second) -> first, LinkedHashMap::new));
    }

    private FlywayException collisionException(List<LegacyCategory> categories,
                                               Map<UUID, String> candidates,
                                               Map<UUID, String> collisionGroups,
                                               List<UUID> affectedIds,
                                               String heading) {
        Map<UUID, LegacyCategory> categoryById = categories.stream()
                .collect(Collectors.toMap(LegacyCategory::id, category -> category));
        String details = affectedIds.stream()
                .sorted(Comparator.naturalOrder())
                .map(categoryId -> {
                    LegacyCategory category = categoryById.get(categoryId);
                    String candidate = collisionGroups.getOrDefault(categoryId, candidates.get(categoryId));
                    return "categoryId=" + categoryId + ", name=" + category.name() + ", slugCandidate=" + candidate;
                })
                .collect(Collectors.joining("; "));
        return new FlywayException(heading + ": " + details);
    }

    private record LegacyCategory(UUID id, String name) {}
}
