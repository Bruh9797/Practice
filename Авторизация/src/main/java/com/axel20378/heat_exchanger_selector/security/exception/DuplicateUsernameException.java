package com.axel20378.heat_exchanger_selector.security.exception;

public class DuplicateUsernameException extends RuntimeException {
    public DuplicateUsernameException(String username) {
        super("Пользователь с логином '" + username + "' уже существует");
    }
}
