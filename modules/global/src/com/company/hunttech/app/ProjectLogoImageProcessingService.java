package com.company.hunttech.app;

/**
 * Сервис обработки логотипа (проекта {@code Project.projectLogo} и компании
 * {@code Company.fileCompanyLogo}) перед сохранением в файловое хранилище.
 *
 * Преобразует загруженное изображение любого формата (JPEG, PNG, GIF, BMP, WebP и т.п.) в PNG,
 * уменьшает до заданного максимального размера (по умолчанию 300x300) с сохранением пропорций,
 * удаляет белый фон (делает его прозрачным) и вписывает логотип в круг так, чтобы при
 * отображении в круглом аватаре {@code ovaFallbackImage} не происходила обрезка по углам.
 */
public interface ProjectLogoImageProcessingService {

    String NAME = "hunttech_ProjectLogoImageProcessingService";

    /**
     * Обрабатывает изображение логотипа.
     *
     * @param data     байты исходного изображения (любой растровый формат)
     * @param fileName исходное имя файла (используется для определения расширения)
     * @return результат обработки; если изображение не является растровым или обработка
     *         отключена конфигом — возвращает исходные данные с {@code processed = false}
     */
    ProcessedImage process(byte[] data, String fileName);
}
