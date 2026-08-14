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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
 *     <li>локальный rembg-этап (если включён {@code hunttech.projectLogo.rembg.enabled}):
 *         бесплатная нейросеть u2net на сервере приложения (POST /api/remove) удаляет фон
 *         без внешних API; при недоступности — платный AI-этап, затем классика;</li>
 *     <li>платный AI-этап (если включён {@code hunttech.projectLogo.ai.enabled}): функция
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

            // 0. Локальный rembg-этап (бесплатная нейросеть u2net на сервере приложения):
            // удаляет фон без внешних API и ключей; результат проходит тот же
            // детерминированный финал (ресайз, обрезка, круг). При недоступности —
            // платный AI-этап, затем классический конвейер.
            byte[] rembgResult = tryRembgBackgroundRemoval(data, fileName, config);
            if (rembgResult != null) {
                BufferedImage rembgImage = ImageIO.read(new ByteArrayInputStream(rembgResult));
                if (rembgImage != null) {
                    source = rembgImage;
                } else {
                    log.warn("rembg вернул не-растровое изображение для логотипа {}, используется следующий этап",
                            fileName);
                }
            } else {
                // 0.1. Платный AI-этап (если включён hunttech.projectLogo.ai.enabled):
                // нейросеть удаляет фон; при недоступности — классический конвейер.
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
            }

            // 1. Единое представление с альфа-каналом (ARGB) — основа для PNG и прозрачности.
            BufferedImage argb = toArgb(source);

            // 2. Пропорциональное уменьшение, если сторона превышает лимит.
            argb = downscaleIfNeeded(argb, config.getMaxSize());

            // 3. Удаление белого/серого фона, соединённого с краями изображения.
            argb = removeWhiteBackground(argb, config.getWhiteThreshold(),
                    config.getRemoveAllWhite(),
                    config.getGraySaturationThreshold(), config.getGrayMinChannel());

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
     * Пробует удалить фон локальным rembg-сервером — первым шагом AI-конвейера.
     *
     * <p>Бесплатная нейросеть u2net развёрнута на сервере приложения (systemd
     * {@code rembg.service}, эндпоинт {@code POST {url}/api/remove}, multipart
     * form-data, поле {@code file}). Этап не требует API-ключей и не передаёт
     * изображение во внешние сервисы. Любая недоступность (сервис выключен,
     * таймаут, HTTP-ошибка, пустой или не-растровый ответ) возвращает
     * {@code null} — вызывающий код переходит к платному AI-этапу, затем
     * к классическому конвейеру; загрузка никогда не прерывается.</p>
     *
     * @return PNG с прозрачным фоном или {@code null} при недоступности rembg
     */
    private byte[] tryRembgBackgroundRemoval(byte[] data, String fileName, HunttechProjectLogoConfig config) {
        if (!config.getRembgEnabled()) {
            log.debug("rembg-этап отключён конфигом hunttech.projectLogo.rembg.enabled=false");
            return null;
        }
        String endpoint = StringUtils.removeEnd(config.getRembgUrl(), "/") + "/api/remove";
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(endpoint).openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(config.getRembgTimeoutMs());
            connection.setReadTimeout(config.getRembgTimeoutMs());

            String boundary = "----hrmRembg" + Long.toHexString(System.nanoTime());
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream output = connection.getOutputStream()) {
                output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
                output.write(("Content-Disposition: form-data; name=\"file\"; filename=\""
                        + safeFileName(fileName) + "\"\r\n").getBytes(StandardCharsets.UTF_8));
                output.write(("Content-Type: " + detectMimeType(fileName) + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                output.write(data);
                output.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                log.warn("rembg вернул HTTP {} для логотипа {}, используется следующий этап",
                        responseCode, fileName);
                return null;
            }
            try (InputStream input = connection.getInputStream()) {
                ByteArrayOutputStream result = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    result.write(buffer, 0, read);
                }
                if (result.size() == 0) {
                    log.warn("rembg вернул пустой ответ для логотипа {}, используется следующий этап", fileName);
                    return null;
                }
                log.info("Логотип {} обработан локальным rembg ({} байт)", fileName, result.size());
                return result.toByteArray();
            }
        } catch (Exception e) {
            log.warn("rembg недоступен для логотипа {}, используется следующий этап. Причина: {}",
                    fileName, e.toString());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Обеззараживает имя файла для заголовка {@code Content-Disposition}: убирает
     * кавычки и переводы строк — защита от инъекции в multipart-заголовок.
     */
    private String safeFileName(String fileName) {
        return safeValue(fileName).replace("\"", "").replace("\r", "").replace("\n", "");
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
     * Удаляет белый и серый фон, соединённый с краями изображения.
     *
     * <p>Фоном считаются пиксели двух типов:</p>
     * <ul>
     *     <li><b>белый</b> — минимум RGB-каналов не ниже {@code whiteThreshold}
     *     (порог конфигурации, по умолчанию 235);</li>
     *     <li><b>серый</b> — низкая насыщенность (разница max-min каналов не выше
     *     {@code graySaturationThreshold}, по умолчанию 30) при яркости не ниже
     *     {@code grayMinChannel} (по умолчанию 40) — покрывает фон-градиенты
     *     от белого к тёмно-серому (логотип SSP).</li>
     * </ul>
     *
     * <p>Серый фон удаляется только если соединён с краем непрерывной областью
     * (flood-fill от границ) — серые элементы дизайна внутри логотипа (текст, значки),
     * окружённые цветными пикселями, сохраняются. Белый фон при
     * {@code removeAllWhite = true} удаляется по всему полотну, включая замкнутые
     * полости внутри букв и фигур (например, белый просвет внутри буквы «А»).</p>
     *
     * <p>На границе белого фона применяется плавный переход прозрачности
     * ({@link #EDGE_SOFTNESS}); серый фон становится полностью прозрачным.</p>
     */
    private BufferedImage removeWhiteBackground(BufferedImage image, int whiteThreshold,
                                                boolean removeAllWhite,
                                                int graySaturationThreshold, int grayMinChannel) {
        int width = image.getWidth();
        int height = image.getHeight();

        boolean[][] isBackground = new boolean[height][width];

        if (removeAllWhite) {
            // Маска по всему полотну: белые пиксели (минимум RGB-каналов >= порога)
            // считаются фоном (включая замкнутые полости внутри букв).
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
        }

        // Flood-fill от краёв: и белый, и серый фон, соединённый с границей.
        // Серые пиксели помечаются только здесь (не по всему полотну) — так серые
        // элементы дизайна внутри логотипа, не соединённые с краем, сохраняются.
        Deque<int[]> queue = new ArrayDeque<>();

        // Затравка: все пиксели на границе, удовлетворяющие белому или серому порогу.
        for (int x = 0; x < width; x++) {
            enqueueIfWhite(image, isBackground, queue, x, 0, whiteThreshold, graySaturationThreshold, grayMinChannel);
            enqueueIfWhite(image, isBackground, queue, x, height - 1, whiteThreshold, graySaturationThreshold, grayMinChannel);
        }
        for (int y = 0; y < height; y++) {
            enqueueIfWhite(image, isBackground, queue, 0, y, whiteThreshold, graySaturationThreshold, grayMinChannel);
            enqueueIfWhite(image, isBackground, queue, width - 1, y, whiteThreshold, graySaturationThreshold, grayMinChannel);
        }

        // BFS по 4-связным белым/серым пикселям.
        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};
        while (!queue.isEmpty()) {
            int[] p = queue.poll();
            for (int i = 0; i < 4; i++) {
                int nx = p[0] + dx[i];
                int ny = p[1] + dy[i];
                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    enqueueIfWhite(image, isBackground, queue, nx, ny, whiteThreshold,
                            graySaturationThreshold, grayMinChannel);
                }
            }
        }

        // Применение прозрачности: белый фон — с плавным переходом, серый — полностью прозрачный.
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
                    if (minChannel >= whiteThreshold) {
                        // Белый фон: прозрачность зависит от "белизны" — плавный край.
                        // Чем белее пиксель (ближе к 255), тем прозрачнее; у нижней границы
                        // зоны (threshold - EDGE_SOFTNESS) пиксель остаётся непрозрачным.
                        int alpha = 255 - clamp255((minChannel - (whiteThreshold - EDGE_SOFTNESS))
                                * 255 / EDGE_SOFTNESS);
                        result.setRGB(x, y, (Math.min(a, alpha) << 24) | (r << 16) | (g << 8) | b);
                    } else {
                        // Серый фон: полностью прозрачный (градиент удаляется целиком).
                        result.setRGB(x, y, 0);
                    }
                } else if (hasBackgroundNeighbor(isBackground, x, y, width, height)
                        && minChannel >= whiteThreshold - EDGE_SOFTNESS) {
                    // Пиксель на границе белого фона: частичная прозрачность для сглаживания контура.
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
     * Добавляет пиксель в очередь flood-fill, если он ещё не помечен и является
     * белым (минимум RGB-каналов >= {@code whiteThreshold}) или серым фоном
     * (насыщенность max-min <= {@code graySaturationThreshold} при яркости
     * >= {@code grayMinChannel}).
     */
    private void enqueueIfWhite(BufferedImage image, boolean[][] isBackground, Deque<int[]> queue,
                                int x, int y, int whiteThreshold,
                                int graySaturationThreshold, int grayMinChannel) {
        if (isBackground[y][x]) {
            return;
        }
        int rgb = image.getRGB(x, y);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int minChannel = Math.min(r, Math.min(g, b));
        int maxChannel = Math.max(r, Math.max(g, b));
        boolean isWhite = minChannel >= whiteThreshold;
        boolean isGray = !isWhite
                && minChannel >= grayMinChannel
                && (maxChannel - minChannel) <= graySaturationThreshold;
        if (isWhite || isGray) {
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
