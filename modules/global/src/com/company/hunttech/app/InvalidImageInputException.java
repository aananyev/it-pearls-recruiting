package com.company.hunttech.app;

import com.haulmont.cuba.core.global.DevelopmentException;

/** Ошибка пользовательского image-файла, которую можно безопасно показать как validation failure. */
public class InvalidImageInputException extends DevelopmentException {
    public InvalidImageInputException(String message) {
        super(message);
    }

    public InvalidImageInputException(String message, Throwable cause) {
        super(message, cause);
    }
}
