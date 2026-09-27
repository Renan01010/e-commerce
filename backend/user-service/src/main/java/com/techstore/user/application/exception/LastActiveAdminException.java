package com.techstore.user.application.exception;

public class LastActiveAdminException extends RuntimeException {
    public LastActiveAdminException() {
        super("The last active ADMIN cannot be demoted or deactivated");
    }
}