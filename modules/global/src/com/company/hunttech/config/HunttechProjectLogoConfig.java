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
     * Максимальная насыщенность (разница max-min каналов) пикселя, который считается
     * "серым фоном": серые/бесцветные пиксели (например, фон-градиент от белого к тёмно-серому),
     * соединённые с краем изображения, становятся прозрачными вместе с белым фоном.
     * Больше — агрессивнее удаляет светло-серые градиенты, но может зацепить серые
     * элементы дизайна, соединённые с краем.
     */
    @Property("hunttech.projectLogo.graySaturationThreshold")
    @DefaultInt(30)
    int getGraySaturationThreshold();

    /**
     * Минимальная яркость (minChannel, 0-255) пикселя, который считается "серым фоном".
     * Пиксели темнее порога (например, тёмно-серый текст на логотипе) не удаляются.
     */
    @Property("hunttech.projectLogo.grayMinChannel")
    @DefaultInt(40)
    int getGrayMinChannel();

    /**
     * Доля стороны канваса, в которую вписывается логотип (0.0-1.0).
     * 0.7071 = сторона квадрата, вписанного в круг канваса — логотип не выходит за круг.
     */
    @Property("hunttech.projectLogo.circleInscribeRatio")
    @DefaultInt(71)
    int getCircleInscribeRatioPercent();

    /**
     * Удалять ВСЕ пиксели, удовлетворяющие порогу белизны, включая замкнутые полости
     * внутри букв и фигур (например, белый просвет внутри буквы «А» у логотипа
     * Альфа-Банка) — они считаются фоном. При {@code false} удаляется только фон,
     * соединённый с краями изображения (flood-fill), а белые элементы дизайна внутри
     * логотипа сохраняются.
     */
    @Property("hunttech.projectLogo.removeAllWhite")
    @DefaultBoolean(true)
    boolean getRemoveAllWhite();

    /**
     * Включает обработку логотипа проекта (JPEG/PNG -> PNG, ресайз, удаление белого фона, вписывание в круг).
     */
    @Property("hunttech.projectLogo.enabled")
    @DefaultBoolean(true)
    boolean getEnabled();

    /**
     * Включает AI-первый этап обработки логотипа (capability IMAGE_GENERATION, функция
     * PROJECT_LOGO_IMAGE_GENERATE): нейросеть удаляет фон, затем классический конвейер
     * выполняет ресайз и вписывание в круг. При недоступности AI — автоматический
     * классический конвейер (flood-fill).
     */
    @Property("hunttech.projectLogo.ai.enabled")
    @DefaultBoolean(true)
    boolean getAiProcessingEnabled();
}
