package com.techstore.user.application.port.in;

import com.techstore.user.domain.Role;
import com.techstore.user.domain.User;
import java.util.UUID;

public interface ManageUsersUseCase {
    User create(String email, String rawPassword, Role role);

    User update(UUID userId, UUID actorId, Role role, Boolean active);
}