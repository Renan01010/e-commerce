package com.techstore.user.application.port.out;

import com.techstore.user.domain.Role;
import com.techstore.user.domain.User;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface UserRepository {
    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String normalizedEmail);

    long countActiveByRole(Role role);

    List<User> findActiveByRoleForUpdate(Role role);

    User save(User user);
}