package com.company.hunttech.service;

import com.company.hunttech.entity.*;
import com.haulmont.cuba.core.entity.FileDescriptor;

public interface GeoDataEnrichmentService {
    String NAME = "hunttech_GeoDataEnrichmentService";

    /**
     * Поиск и обогащение реквизитов страны по названию или коду (ISO, столица, валюта, телефонный код, флаг).
     */
    GeoCountryData enrichCountry(String countryNameOrCode);

    /**
     * Поиск и обогащение реквизитов региона по названию и стране.
     */
    GeoRegionData enrichRegion(String regionName, Country country);

    /**
     * Поиск и обогащение реквизитов города по названию, региону и стране.
     */
    GeoCityData enrichCity(String cityName, Region region, Country country);

    /**
     * Скачивание изображения (флага, герба) по URL и сохранение как FileDescriptor.
     */
    FileDescriptor downloadAndSaveImage(String imageUrl, String fileName);

    /**
     * Проверка доступности подключения к Geo API (например DaData).
     */
    boolean testGeoApiConnection(String apiKey, String secretKey, String apiUrl);
}
