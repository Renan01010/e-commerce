package com.techstore.user.application.port.out;

import java.util.function.Supplier;

public interface TransactionRunner {
    <T> T runInTransaction(Supplier<T> work);
}