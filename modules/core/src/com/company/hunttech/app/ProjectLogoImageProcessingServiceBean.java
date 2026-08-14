package com.company.hunttech.app;

import com.company.hunttech.config.HunttechProjectLogoConfig;
import com.company.hunttech.service.AiExecutionService;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.DevelopmentException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Реализация обработки логотипа проекта.
 *
 * <p>Конвейер преобразований (все шаги выполняются в памяти до записи в файловое хранилище):</p>
 * <ol>
 *     <li>чтение растрового изображения любого поддерживаемого формата (ImageIO);</li>
 *     <li>AI-первый этап (если включён {@code hunttech.projectLogo.ai.enabled}): функция
 *         {@code PROJECT_LOGO_IMAGE_GENERATE} (capability IMAGE_GENERATION) удаляет фон
 *         нейросетью; при недоступности AI (функция не активна, нет credentials, ошибка
 *         провайдера) — бесшовный переход к классическому конвейеру;</li>
 *     <li>перевод в ARGB и конвертация в PNG (нужна прозрачность после удаления фона);</li>
 *     <li>если сторона больше {@code maxSize} — пропорциональное уменьшение до {@code maxSize};</li>
 *     <li>удаление белого фона: flood-fill от краёв изображения — белые пиксели, достижимые
 *         от границы, становятся прозрачными (белые элементы внутри логотипа сохраняются);</li>
 *     <li>вписывание в круг: квадратный канвас со стороной, равной диагонали логотипа
 *         (с запасом 5%), логотип центрируется — при отображении в круглом аватаре
 *         {@code ovaFallbackImage} углы логотипа не обрезаются.</li>
 * </ol>
 */
@Service(ProjectLogoImageProcessingService.NAME)
public class ProjectLogoImageProcessingServiceBean implements ProjectLogoImageProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ProjectLogoImageProcessingServiceBean.class);

    /**
     * Стабильный код AI-функции удаления фона логотипа (см. AiFunctionConfiguration,
     * capability IMAGE_GENERATION). Управляется администратором в «Управление AI» без кода.
     */
    public static final String FUNCTION_PROJECT_LOGO_IMAGE_GENERATE = "PROJECT_LOGO_IMAGE_GENERATE";

    /**
     * Запас канваса относительно диагонали логотипа (5%) — логотип не касается границы круга.
     */
    private static final double CANVAS_MARGIN = 0.95;

    /**
     * Ширина зоны плавного перехода на границе белого фона (в уровнях яркости).
     */
    private static final int EDGE_SOFTNESS = 24;

    @Inject
    private Configuration configuration;

    @Inject
    private AiExecutionService aiExecutionService;

    @Override
    public ProcessedImage process(byte[] data, String fileName) {
        if (data == null || data.length == 0) {
            throw new DevelopmentException("Empty image data");
        }

        HunttechProjectLogoConfig config = configuration.getConfig(HunttechProjectLogoConfig.class);
        if (!config.getEnabled()) {
            log.debug("Обработка логотипа отключена конфигом hunttech.projectLogo.enabled=false");
            return new ProcessedImage(data, extractName(fileName), extractExtension(fileName), false);
        }

        String name = extractName(fileName);
        String extension = extractExtension(fileName);

        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(data));
            if (source == null) {
                log.debug("Файл {} не является растровым изображением, обработка пропущена", fileName);
                return new ProcessedImage(data, name, extension, false);
            }

            // 0. AI-первый этап: нейросеть удаляет фон; результат проходит тот же
            // детерминированный финал (ресайз, обрезка, круг). При сбое — классический конвейер.
            byte[] aiResult = tryAiBackgroundRemoval(data, fileName, config);
            if (aiResult != null) {
                BufferedImage aiImage = ImageIO.read(new ByteArrayInputStream(aiResult));
                if (aiImage != null) {
                    source = aiImage;
                } else {
                    log.warn("AI-результат логотипа {} не является растровым изображением, используется оригинал",
                            fileName);
                }
            }

            // 1. Единое представление с альфа-каналом (ARGB) — основа для PNG и прозрачности.
            BufferedImage argb = toArgb(source);

            // 2. Пропорциональное уменьшение, если сторона превышает лимит.
            argb = downscaleIfNeeded(argb, config.getMaxSize());

            // 3. Удаление белого фона, соединённого с краями изображения.
            argb = removeWhiteBackground(argb, config.getWhiteThreshold());

            // 4. Обрезка по фактическому содержимому (прозрачные поля после удаления фона).
            Rectangle bounds = getOpaqueBounds(argb);
            if (bounds.isEmpty()) {
                log.debug("После удаления белого фона содержимое отсутствует, возвращён исходный файл");
                return new ProcessedImage(data, name, extension, false);
            }
            argb = argb.getSubimage(bounds.x, bounds.y, bounds.width, bounds.height);

            // 5. Вписывание в круг: канвас-квадрат со стороной, равной диагонали содержимого.
            BufferedImage circle = fitIntoCircle(argb, config.getMaxSize());

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(circle, config.getFormat(), output);

            log.info("Логотип {} обработан: {}x{} -> {}x{}, формат {}",
                    fileName, source.getWidth(), source.getHeight(),
                    circle.getWidth(), circle.getHeight(), config.getFormat());

            return new ProcessedImage(output.toByteArray(), name, config.getFormat(), true);
        } catch (IOException e) {
            log.error("Ошибка обработки логотипа {}: {}", fileName, e.toString(), e);
            throw new DevelopmentException("Failed to process project logo: " + e.getMessage(), e);
        }
    }

    /**
     * Пробует AI-удаление фона логотипа через функцию {@link #FUNCTION_PROJECT_LOGO_IMAGE_GENERATE}.
     *
     * <p>Любая недоступность AI (функция не активна, не настроено подключение, ошибка
     * провайдера, сетевой таймаут) не прерывает загрузку: возвращается {@code null},
     * и вызывающий код продолжает классическим конвейером.</p>
     *
     * @return обработанное изображение или {@code null} при недоступности AI
     */
    private byte[] tryAiBackgroundRemoval(byte[] data, String fileName, HunttechProjectLogoConfig config) {
        if (!config.getAiProcessingEnabled()) {
            log.debug("AI-обработка логотипа отключена конфигом hunttech.projectLogo.ai.enabled=false");
            return null;
        }
        try {
            Map<String, Object> context = new LinkedHashMap<>();
            context.put("sourceFileName", safeValue(fileName));
            byte[] result = aiExecutionService.executeImage(
                    FUNCTION_PROJECT_LOGO_IMAGE_GENERATE, context, data, detectMimeType(fileName));
            if (result == null || result.length == 0) {
                log.warn("AI вернул пустой результат для логотипа {}, используется классический конвейер", fileName);
                return null;
            }
            log.info("Логотип {} обработан AI-функцией {}", fileName, FUNCTION_PROJECT_LOGO_IMAGE_GENERATE);
            return result;
        } catch (Exception e) {
            log.warn("AI-обработка логотипа {} недоступна, используется классический конвейер. Причина: {}",
                    fileName, e.toString());
            return null;
        }
    }

    private String detectMimeType(String fileName) {
        String extension = extractExtension(fileName);
        if (extension == null) {
            return "image/png";
        }
        switch (extension.toLowerCase()) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "webp":
                return "image/webp";
            default:
                return "image/png";
        }
    }

    private String safeValue(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * Переводит изображение в ARGB-представление.
     * Если исходное изображение уже имеет альфа-канал — используется как есть.
     */
    private BufferedImage toArgb(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_ARGB) {
            return source;
        }
        BufferedImage argb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = argb.createGraphics();
        try {
            g.drawImage(source, 0, 0, null);
        } finally {
            g.dispose();
        }
        return argb;
    }

    /**
     * Пропорционально уменьшает изображение, если большая сторона превышает {@code maxSize}.
     */
    private BufferedImage downscaleIfNeeded(BufferedImage image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        if (width <= maxSize && height <= maxSize) {
            return image;
        }

        double scale = (double) maxSize / Math.max(width, height);
        int newWidth = Math.max(1, (int) Math.round(width * scale));
        int newHeight = Math.max(1, (int) Math.round(height * scale));

        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resized.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(image, 0, 0, newWidth, newHeight, null);
        } finally {
            g.dispose();
        }
        return resized;
    }

    /**
     * Удаляет белый фон, соединённый с краями изображения.
     *
     * <p>Flood-fill от всех граничных пикселей: если пиксель "белый" (минимум RGB-каналов
     * не меньше порога) — он помечается как фон. Все достижимые от края белые области
     * становятся прозрачными. Белые пиксели ВНУТРИ логотипа не затрагиваются, так как
     * не соединены с границей непрерывной белой областью.</p>
     *
     * <p>На границе фона применяется плавный переход прозрачности ({@link #EDGE_SOFTNESS}),</p>
     * чтобы избежать грубой ступеньки по контуру логотипа.
     */
    private BufferedImage removeWhiteBackground(BufferedImage image, int whiteThreshold) {
        boolean removeAllWhite =
                configuration.getConfig(HunttechProjectLogoConfig.class).getRemoveAllWhite();
        return removeWhiteBackground(image, whiteThreshold, removeAllWhite);
    }

    /**
     * Удаляет белый фон изображения.
     *
     * <p>При {@code removeAllWhite = true} прозрачными становятся ВСЕ пиксели,
     * удовлетворяющие порогу белизны, — включая замкнутые полости внутри букв и фигур
     * (например, белый просвет внутри буквы «А» у логотипа Альфа-Банка): такие области
     * считаются фоном. Это согласуется с требованием AI-промпта функции
     * {@code PROJECT_LOGO_IMAGE_GENERATE}.</p>
     *
     * <p>При {@code removeAllWhite = false} используется flood-fill от границ: белыми
     * становятся только области, соединённые с краем непрерывной белой зоной, а белые
     * элементы дизайна внутри логотипа (текст, просветы букв) сохраняются.</p>
     *
     * <p>На границе фона применяется плавный переход прозрачности ({@link #EDGE_SOFTNESS}),
     * чтобы избежать грубой ступеньки по контуру логотипа.</p>
     */
    private BufferedImage removeWhiteBackground(BufferedImage image, int whiteThreshold,
                                                boolean removeAllWhite) {
        int width = image.getWidth();
        int height = image.getHeight();

        boolean[][] isBackground = new boolean[height][width];

        if (removeAllWhite) {
            // Маска по всему полотну: любой пиксель с минимумом RGB-каналов >= порога
            // считается фоном (включая замкнутые полости внутри букв).
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = image.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    int minChannel = Math.min(r, Math.min(g, b));
                    if (minChannel >= whiteThreshold) {
                        isBackground[y][x] = true;
                    }
                }
            }
        } else {
            Deque<int[]> queue = new ArrayDeque<>();

            // Затравка: все пиксели на границе, удовлетворяющие порогу белизны.
            for (int x = 0; x < width; x++) {
                enqueueIfWhite(image, isBackground, queue, x, 0, whiteThreshold);
                enqueueIfWhite(image, isBackground, queue, x, height - 1, whiteThreshold);
            }
            for (int y = 0; y < height; y++) {
                enqueueIfWhite(image, isBackground, queue, 0, y, whiteThreshold);
                enqueueIfWhite(image, isBackground, queue, width - 1, y, whiteThreshold);
            }

            // BFS по 4-связным белым пикселям.
            int[] dx = {1, -1, 0, 0};
            int[] dy = {0, 0, 1, -1};
            while (!queue.isEmpty()) {
                int[] p = queue.poll();
                for (int i = 0; i < 4; i++) {
                    int nx = p[0] + dx[i];
                    int ny = p[1] + dy[i];
                    if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                        enqueueIfWhite(image, isBackground, queue, nx, ny, whiteThreshold);
                    }
                }
            }
        }

        // Применение прозрачности: фон полностью прозрачный, зона у порога — с плавным переходом.
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int a = (rgb >> 24) & 0xFF;
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int minChannel = Math.min(r, Math.min(g, b));

                if (isBackground[y][x]) {
                    // Пиксель фона: прозрачность зависит от "белизны" — плавный край.
                    // Чем белее пиксель (ближе к 255), тем прозрачнее; у нижней границы
                    // зоны (threshold - EDGE_SOFTNESS) пиксель остаётся непрозрачным.
                    int alpha = 255 - clamp255((minChannel - (whiteThreshold - EDGE_SOFTNESS))
                            * 255 / EDGE_SOFTNESS);
                    result.setRGB(x, y, (Math.min(a, alpha) << 24) | (r << 16) | (g << 8) | b);
                } else if (hasBackgroundNeighbor(isBackground, x, y, width, height)
                        && minChannel >= whiteThreshold - EDGE_SOFTNESS) {
                    // Пиксель на границе фона: частичная прозрачность для сглаживания контура.
                    int alpha = 255 - clamp255((minChannel - (whiteThreshold - EDGE_SOFTNESS))
                            * 255 / EDGE_SOFTNESS);
                    result.setRGB(x, y, (Math.min(a, alpha) << 24) | (r << 16) | (g << 8) | b);
                } else {
                    result.setRGB(x, y, rgb);
                }
            }
        }
        return result;
    }

    /**
     * Добавляет пиксель в очередь flood-fill, если он ещё не помечен и является "белым".
     */
    private void enqueueIfWhite(BufferedImage image, boolean[][] isBackground, Deque<int[]> queue,
                                int x, int y, int whiteThreshold) {
        if (isBackground[y][x]) {
            return;
        }
        int rgb = image.getRGB(x, y);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        if (Math.min(r, Math.min(g, b)) >= whiteThreshold) {
            isBackground[y][x] = true;
            queue.add(new int[]{x, y});
        }
    }

    /**
     * Проверяет, есть ли среди 4-соседей пиксель фона (для плавного края).
     */
    private boolean hasBackgroundNeighbor(boolean[][] isBackground, int x, int y, int width, int height) {
        if (x > 0 && isBackground[y][x - 1]) return true;
        if (x < width - 1 && isBackground[y][x + 1]) return true;
        if (y > 0 && isBackground[y - 1][x]) return true;
        return y < height - 1 && isBackground[y + 1][x];
    }

    /**
     * Находит bounding box непрозрачных пикселей (фактическое содержимое логотипа).
     */
    private Rectangle getOpaqueBounds(BufferedImage image) {
        int minX = image.getWidth(), minY = image.getHeight();
        int maxX = -1, maxY = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int a = (image.getRGB(x, y) >> 24) & 0xFF;
                if (a > 8) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }
        if (maxX < minX || maxY < minY) {
            return new Rectangle();
        }
        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    /**
     * Вписывает логотип в круг: создаёт квадратный канвас со стороной, равной диагонали
     * содержимого (с запасом {@link #CANVAS_MARGIN}), и центрирует логотип на нём.
     *
     * <p>Круглый аватар {@code ovaFallbackImage} обрезает канвас по окружности диаметром,
     * равным стороне канваса. Так как сторона канваса не меньше диагонали логотипа,
     * логотип целиком попадает внутрь круга — обрезки по углам не происходит.</p>
     */
    private BufferedImage fitIntoCircle(BufferedImage logo, int maxSize) {
        int width = logo.getWidth();
        int height = logo.getHeight();

        // Диагональ содержимого — минимальный диаметр круга, вмещающего логотип целиком.
        double diagonal = Math.sqrt((double) width * width + (double) height * height);
        // Сторона канваса: диагональ с запасом, но не больше лимита.
        int canvasSize = (int) Math.ceil(Math.min(diagonal / CANVAS_MARGIN, maxSize));

        BufferedImage canvas = new BufferedImage(canvasSize, canvasSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int x = (canvasSize - width) / 2;
            int y = (canvasSize - height) / 2;
            g.drawImage(logo, x, y, null);
        } finally {
            g.dispose();
        }
        return canvas;
    }

    private static int clamp255(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static String extractName(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return "project-logo";
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            return fileName.substring(0, dotIndex);
        }
        return fileName;
    }

    private static String extractExtension(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return null;
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1);
        }
        return null;
    }
}
