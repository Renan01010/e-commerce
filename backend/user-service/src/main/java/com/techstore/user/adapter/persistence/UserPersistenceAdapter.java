package com.techstore.user.adapter.persistence;

import com.techstore.user.application.port.out.UserRepository;
import com.techstore.user.domain.Role;
import com.techstore.user.domain.User;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class UserPersistenceAdapter implements UserRepository {
    private final UserJpaRepository repository;

    public UserPersistenceAdapter(UserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<User> findById(UUID id) {
        return repository.findById(id).map(UserEntity::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String normalizedEmail) {
        return repository.findByEmail(normalizedEmail).map(UserEntity::toDomain);
    }

    @Override
    public long countActiveByRole(Role role) {
        return repository.countByRoleAndActiveTrue(role);
    }

    @Override
    public List<User> findActiveByRoleForUpdate(Role role) {
        return repository.findActiveByRoleForUpdate(role).stream().map(UserEntity::toDomain).toList();
    }

    @Override
    public User save(User user) {
        return repository.save(UserEntity.fromDomain(user)).toDomain();
    }
}