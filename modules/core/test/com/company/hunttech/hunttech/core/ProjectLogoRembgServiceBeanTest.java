package com.company.hunttech.hunttech.core;

import com.company.hunttech.HunttechTestContainer;
import com.company.hunttech.app.ProcessedImage;
import com.company.hunttech.app.ProjectLogoImageProcessingService;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.sys.AppContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Тесты локального rembg-этапа обработки логотипа проекта (первый шаг AI-конвейера):
 * rembg-сервер доступен → его результат используется; недоступен/отключён →
 * бесшовный классический конвейер, загрузка не прерывается.
 *
 * <p>В тесте поднимается встроенный {@link HttpServer} на случайном порту, который
 * отвечает PNG целиком синего цвета. Маркер «rembg сработал» — углы результата
 * остаются непрозрачными (синий не удаляется классическим flood-fill), тогда как
 * классический конвейер на белом фоне делает углы прозрачными.</p>
 *
 * <p>Конфиг переопределяется через {@link AppContext#setProperty} — для source
 * DATABASE значения AppContext имеют приоритет над SYS_CONFIG и не пишут в БД;
 * в {@link #tearDown()} значения возвращаются к дефолтам.</p>
 */
public class ProjectLogoRembgServiceBeanTest {

    @ClassRule
    public static HunttechTestContainer cont = HunttechTestContainer.Common.INSTANCE;

    private final ProjectLogoImageProcessingService service =
            AppBeans.get(ProjectLogoImageProcessingService.NAME);

    private HttpServer server;
    private AtomicInteger rembgCalls = new AtomicInteger();

    @Before
    public void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/remove", this::handleRemove);
        server.start();
        String url = "http://127.0.0.1:" + server.getAddress().getPort();
        AppContext.setProperty("hunttech.projectLogo.rembg.url", url);
        AppContext.setProperty("hunttech.projectLogo.rembg.enabled", "true");
        AppContext.setProperty("hunttech.projectLogo.rembg.timeoutMs", "5000");
        rembgCalls.set(0);
    }

    @After
    public void tearDown() {
        server.stop(0);
        // Возврат к дефолтам, чтобы не влиять на соседние тесты (AppContext приоритетнее SYS_CONFIG).
        AppContext.setProperty("hunttech.projectLogo.rembg.url", "http://127.0.0.1:7000");
        AppContext.setProperty("hunttech.projectLogo.rembg.enabled", "true");
        AppContext.setProperty("hunttech.projectLogo.rembg.timeoutMs", "15000");
    }

    /**
     * Создаёт тестовое изображение: белый фон + цветной прямоугольник в центре.
     */
    private byte[] createWhiteBackgroundImage(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            g.setColor(new Color(200, 40, 40));
            g.fillRect(width / 4, height / 4, width / 2, height / 2);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return out.toByteArray();
    }

    /**
     * Обработчик rembg: читает multipart-body и возвращает целиком синий PNG.
     * Синий цвет не удаляется классическим flood-fill — это маркер использования
     * результата rembg.
     */
    private void handleRemove(HttpExchange exchange) throws IOException {
        try (InputStream input = exchange.getRequestBody()) {
            byte[] buffer = new byte[8192];
            while (input.read(buffer) != -1) {
                // consume multipart body
            }
        }
        rembgCalls.incrementAndGet();

        BufferedImage image = new BufferedImage(300, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(new Color(0, 60, 200, 255));
            g.fillRect(0, 0, 300, 300);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        byte[] png = out.toByteArray();

        exchange.getResponseHeaders().set("Content-Type", "image/png");
        exchange.sendResponseHeaders(200, png.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(png);
        }
    }

    @Test
    public void testRembgResultUsedWhenAvailable() throws IOException {
        byte[] data = createWhiteBackgroundImage(300, 300);
        ProcessedImage processed = service.process(data, "logo.jpg");

        assertTrue("Изображение должно быть обработано", processed.isProcessed());
        assertEquals("rembg должен был получить ровно один запрос", 1, rembgCalls.get());
        assertTrue("Фон удалён нейросетью rembg — изображение должно помечаться как AI-обработанное",
                processed.isAiProcessed());

        BufferedImage result = ImageIO.read(new ByteArrayInputStream(processed.getData()));
        int cornerAlpha = (result.getRGB(0, 0) >> 24) & 0xFF;
        assertEquals("Угол должен остаться непрозрачным (использован результат rembg, а не классика)",
                255, cornerAlpha);
    }

    @Test
    public void testClassicFallbackWhenRembgUnavailable() throws IOException {
        // Сервер остановлен — connection refused на 127.0.0.1.
        server.stop(0);

        byte[] data = createWhiteBackgroundImage(300, 300);
        ProcessedImage processed = service.process(data, "logo.jpg");

        assertTrue("Изображение должно быть обработано классическим конвейером", processed.isProcessed());
        assertEquals("rembg не должен был получить запрос", 0, rembgCalls.get());
        assertFalse("Классический flood-fill — не AI-обработка",
                processed.isAiProcessed());

        BufferedImage result = ImageIO.read(new ByteArrayInputStream(processed.getData()));
        int cornerAlpha = (result.getRGB(0, 0) >> 24) & 0xFF;
        assertEquals("Угол должен быть прозрачным (классический конвейер удалил белый фон)", 0, cornerAlpha);
    }

    @Test
    public void testClassicPipelineWhenRembgDisabled() throws IOException {
        AppContext.setProperty("hunttech.projectLogo.rembg.enabled", "false");

        byte[] data = createWhiteBackgroundImage(300, 300);
        ProcessedImage processed = service.process(data, "logo.jpg");

        assertTrue("Изображение должно быть обработано классическим конвейером", processed.isProcessed());
        assertEquals("rembg не должен вызываться при отключённом этапе", 0, rembgCalls.get());
        assertFalse("Классический flood-fill — не AI-обработка",
                processed.isAiProcessed());

        BufferedImage result = ImageIO.read(new ByteArrayInputStream(processed.getData()));
        int cornerAlpha = (result.getRGB(0, 0) >> 24) & 0xFF;
        assertEquals("Угол должен быть прозрачным (классический конвейер удалил белый фон)", 0, cornerAlpha);
    }

    @Test
    public void testCandidatePhotoRembgResultMarkedAiProcessed() throws IOException {
        // Фото кандидата + доступный rembg: фон удалён нейросетью — результат должен
        // помечаться как AI-обработанный (в форме показывается нотификация).
        byte[] data = createWhiteBackgroundImage(300, 400);
        ProcessedImage processed = service.process(data, "photo.jpg", true);

        assertTrue("Фото должно быть обработано", processed.isProcessed());
        assertEquals("rembg должен был получить ровно один запрос", 1, rembgCalls.get());
        assertTrue("Фон фото удалён нейросетью — AI-обработка",
                processed.isAiProcessed());
    }

    @Test
    public void testCandidatePhotoRembgUnavailableIsNotMarkedAiProcessed() throws IOException {
        // rembg недоступен (сервер остановлен): фото кандидата проходит только
        // конвертацию в PNG + ресайз (processed=true), но НЕ помечается как
        // AI-обработанное — нотификация «обработано с помощью AI» показываться не должна.
        server.stop(0);

        byte[] data = createWhiteBackgroundImage(300, 400);
        ProcessedImage processed = service.process(data, "photo.jpg", true);

        assertTrue("Фото должно быть конвертировано (PNG + ресайз)", processed.isProcessed());
        assertFalse("Без rembg фон не удалён — фото не должно помечаться как AI-обработанное",
                processed.isAiProcessed());
    }
}
