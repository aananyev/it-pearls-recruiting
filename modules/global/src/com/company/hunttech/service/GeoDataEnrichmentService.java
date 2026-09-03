package com.company.hunttech.service;

import com.company.hunttech.entity.*;
import com.haulmont.cuba.core.entity.FileDescriptor;
import java.util.List;

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
     * Скачивает изображение по URL и возвращает байты для сохранения в БД (BLOB).
     */
    byte[] downloadImageAsBytes(String imageUrl);

    /**
     * Сохраняет флаг страны в БД (byte[])
     */
    void saveCountryFlag(Country country, String flagUrl);

    /**
     * Сохраняет герб региона в БД (byte[])
     */
    void saveRegionEmblem(Region region, String emblemUrl);

    /**
     * Сохраняет герб города в БД (byte[])
     */
    void saveCityEmblem(City city, String emblemUrl);

    /**
     * @deprecated Используйте downloadImageAsBytes + saveCountryFlag/saveRegionEmblem/saveCityEmblem
     * Скачивание изображения (флага, герба) по URL и сохранение как FileDescriptor.
     */
    @Deprecated
    FileDescriptor downloadAndSaveImage(String imageUrl, String fileName);

    /**
     * Пакетное сохранение флагов/гербов для списка сущностей (одна транзакция).
     * Используйте для bulk-обогащения справочников.
     */
    void saveImagesBatch(List<Country> countries, List<Region> regions, List<City> cities);

    /**
     * Проверка доступности подключения к Geo API (например DaData).
     */
    boolean testGeoApiConnection(String apiKey, String secretKey, String apiUrl);
}
