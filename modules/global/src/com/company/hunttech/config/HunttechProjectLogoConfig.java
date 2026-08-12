package com.company.hunttech.config;

import com.haulmont.cuba.core.config.Config;
import com.haulmont.cuba.core.config.Property;
import com.haulmont.cuba.core.config.Source;
import com.haulmont.cuba.core.config.SourceType;
import com.haulmont.cuba.core.config.defaults.DefaultBoolean;
import com.haulmont.cuba.core.config.defaults.DefaultInt;
import com.haulmont.cuba.core.config.defaults.DefaultString;

/**
 * Конфигурация обработки логотипа проекта при загрузке в {@code ProjectEdit}.
 *
 * Логотип приводится к единому виду: PNG, максимум 300x300, белый фон удаляется,
 * изображение вписывается в круг (чтобы при отображении в круглом аватаре
 * {@code ovaFallbackImage} не было обрезки по углам).
 */
@Source(type = SourceType.DATABASE)
public interface HunttechProjectLogoConfig extends Config {

    /**
     * Максимальная сторона логотипа в пикселях (большие изображения уменьшаются с сохранением пропорций).
     */
    @Property("hunttech.projectLogo.maxSize")
    @DefaultInt(300)
    int getMaxSize();

    /**
     * Целевой формат файла логотипа после обработки (PNG — обязателен для прозрачности после удаления белого фона).
     */
    @Property("hunttech.projectLogo.format")
    @DefaultString("png")
    String getFormat();

    /**
     * Порог "белизны" (0-255): пиксели светлее порога, соединённые с краем изображения,
     * считаются фоном и становятся прозрачными. Меньше — агрессивнее удаляет фон.
     */
    @Property("hunttech.projectLogo.whiteThreshold")
    @DefaultInt(235)
    int getWhiteThreshold();

    /**
     * Доля стороны канваса, в которую вписывается логотип (0.0-1.0).
     * 0.7071 = сторона квадрата, вписанного в круг канваса — логотип не выходит за круг.
     */
    @Property("hunttech.projectLogo.circleInscribeRatio")
    @DefaultInt(71)
    int getCircleInscribeRatioPercent();

    /**
     * Включает обработку логотипа проекта (JPEG/PNG -> PNG, ресайз, удаление белого фона, вписывание в круг).
     */
    @Property("hunttech.projectLogo.enabled")
    @DefaultBoolean(true)
    boolean getEnabled();
}
