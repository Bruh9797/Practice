package com.axel20378.heat_exchanger_selector.security.exception;

public class DuplicateUserException extends RuntimeException {

    public DuplicateUserException(String message) {
        super(message);
    }

    public static DuplicateUserException username(String username) {
        return new DuplicateUserException("Пользователь с логином '" + username + "' уже существует");
    }

    public static DuplicateUserException email(String email) {
        return new DuplicateUserException("Пользователь с email '" + email + "' уже существует");
    }
}
