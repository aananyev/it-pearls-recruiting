package com.company.hunttech.app;

/** Внутренняя ошибка декодирования/кодирования image-файла. */
public class ImageProcessingException extends RuntimeException {
    public ImageProcessingException(String message) {
        super(message);
    }

    public ImageProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
