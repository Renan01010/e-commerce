package com.techstore.gateway.routes;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.stereotype.Component;

@Component
public class GatewayRouteStartupLogger implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(GatewayRouteStartupLogger.class);
    private final RouteDefinitionLocator routeDefinitionLocator;
    private final RouteLocator routeLocator;
    private volatile Map<String, RouteDefinition> definitionsById = Map.of();

    public GatewayRouteStartupLogger(RouteDefinitionLocator routeDefinitionLocator, RouteLocator routeLocator) {
        this.routeDefinitionLocator = routeDefinitionLocator;
        this.routeLocator = routeLocator;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<RouteDefinition> definitions = routeDefinitionLocator.getRouteDefinitions()
                .collectList().block(Duration.ofSeconds(10));
        List<Route> loadedRoutes = routeLocator.getRoutes().collectList().block(Duration.ofSeconds(10));
        if (definitions == null) definitions = List.of();
        if (loadedRoutes == null) loadedRoutes = List.of();

        definitionsById = definitions.stream().collect(Collectors.toUnmodifiableMap(
                RouteDefinition::getId, Function.identity(), (first, ignored) -> first));
        Map<String, Route> loadedById = loadedRoutes.stream().collect(Collectors.toMap(
                Route::getId, Function.identity(), (first, ignored) -> first));

        log.info("========== LOADED GATEWAY ROUTES ==========");
        loadedRoutes.stream().sorted(Comparator.comparing(Route::getId)).forEach(route -> {
            RouteDefinition definition = definitionsById.get(route.getId());
            String predicates = definition == null ? sanitize(route.getPredicate().toString())
                    : predicatesFor(definition);
            String filters = definition == null
                    ? sanitize(route.getFilters().toString()) : filtersFor(definition);
            log.info("routeId={} uri={} predicates={} filters={}", route.getId(),
                    safeUri(route.getUri()), predicates, filters);
        });
        definitions.stream().filter(definition -> !loadedById.containsKey(definition.getId()))
                .sorted(Comparator.comparing(RouteDefinition::getId))
                .forEach(definition -> log.warn("routeId={} uri={} predicates={} filters={} loaded=false",
                        definition.getId(), safeUri(definition.getUri()), predicatesFor(definition),
                        filtersFor(definition)));
        log.info("============================================");
    }

    public String predicatesFor(String routeId) {
        RouteDefinition definition = definitionsById.get(routeId);
        return definition == null ? "unavailable" : predicatesFor(definition);
    }

    public String filtersFor(String routeId) {
        RouteDefinition definition = definitionsById.get(routeId);
        return definition == null ? "unavailable" : filtersFor(definition);
    }

    private static String predicatesFor(RouteDefinition definition) {
        return definition.getPredicates().stream()
                .map(GatewayRouteStartupLogger::formatPredicate)
                .map(GatewayRouteStartupLogger::sanitize)
                .collect(Collectors.joining(";"));
    }

    private static String filtersFor(RouteDefinition definition) {
        return definition.getFilters().stream()
                .map(GatewayRouteStartupLogger::formatFilter)
                .map(GatewayRouteStartupLogger::sanitize)
                .collect(Collectors.joining(";"));
    }

    private static String formatPredicate(PredicateDefinition definition) {
        return definition.getName() + "=" + String.join(",", definition.getArgs().values());
    }

    private static String formatFilter(FilterDefinition definition) {
        return definition.getName() + "=" + String.join(",", definition.getArgs().values());
    }

    public static String safeUri(URI uri) {
        if (uri == null) return "unknown";
        try {
            return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), uri.getPath(), null, null).toString();
        } catch (URISyntaxException exception) {
            return sanitize(uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort());
        }
    }

    private static String sanitize(String value) {
        return value
                .replaceAll("(?i)Bearer\\s+[^\\s,;]+", "Bearer [REDACTED]")
                .replaceAll("(?i)(password|secret|token|authorization|cookie|api[-_]?key)=([^&\\s,;]+)", "$1=[REDACTED]")
                .replaceAll("(?i)(https?://)[^/@\\s]+:[^/@\\s]+@", "$1[REDACTED]@");
    }
}