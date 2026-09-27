package com.techstore.user.adapter.persistence;

import com.techstore.user.domain.Role;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmail(String normalizedEmail);

    long countByRoleAndActiveTrue(Role role);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from UserEntity user where user.role = :role and user.active = true order by user.id")
    List<UserEntity> findActiveByRoleForUpdate(@Param("role") Role role);
}