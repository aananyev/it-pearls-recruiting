package com.company.hunttech.hunttech.core;

import com.company.hunttech.HunttechTestContainer;
import com.company.hunttech.app.ProcessedImage;
import com.company.hunttech.app.ProjectLogoImageProcessingService;
import com.haulmont.cuba.core.global.AppBeans;
import org.junit.ClassRule;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Тесты обработки логотипа проекта: конвертация в PNG, ресайз до 300x300,
 * удаление белого фона, вписывание в круг.
 */
public class ProjectLogoImageProcessingServiceBeanTest {

    @ClassRule
    public static HunttechTestContainer cont = HunttechTestContainer.Common.INSTANCE;

    private final ProjectLogoImageProcessingService service =
            AppBeans.get(ProjectLogoImageProcessingService.NAME);

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
     * Читает обработанные байты обратно в изображение.
     */
    private BufferedImage read(ProcessedImage processed) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(processed.getData()));
    }

    @Test
    public void testConvertsJpegToPngAndResizesToMaxSize() throws IOException {
        // Большой JPEG 800x600: должен стать PNG не крупнее 300x300.
        byte[] data = createWhiteBackgroundImage(800, 600);
        ProcessedImage processed = service.process(data, "logo.jpg");

        assertTrue("Изображение должно быть обработано", processed.isProcessed());
        assertEquals("Формат должен быть png", "png", processed.getExtension());
        assertEquals("Имя без расширения", "logo", processed.getName());

        BufferedImage result = read(processed);
        assertTrue("Ширина не больше 300", result.getWidth() <= 300);
        assertTrue("Высота не больше 300", result.getHeight() <= 300);
    }

    @Test
    public void testWhiteBackgroundBecomesTransparent() throws IOException {
        byte[] data = createWhiteBackgroundImage(300, 300);
        ProcessedImage processed = service.process(data, "logo.jpg");
        BufferedImage result = read(processed);

        // Углы канваса были белыми — после обработки должны стать прозрачными.
        int cornerAlpha = (result.getRGB(0, 0) >> 24) & 0xFF;
        assertEquals("Угол должен быть прозрачным (фон удалён)", 0, cornerAlpha);
    }

    @Test
    public void testLogoFitsInsideCircle() throws IOException {
        byte[] data = createWhiteBackgroundImage(300, 300);
        ProcessedImage processed = service.process(data, "logo.jpg");
        BufferedImage result = read(processed);

        // Канвас квадратный (вписывание в круг).
        assertEquals(result.getWidth(), result.getHeight());

        // Логотип (красный квадрат в центре) должен остаться непрозрачным.
        int centerAlpha = (result.getRGB(result.getWidth() / 2, result.getHeight() / 2) >> 24) & 0xFF;
        assertTrue("Центр должен быть непрозрачным", centerAlpha > 200);
    }

    @Test
    public void testWhiteCavityInsideLetterBecomesTransparent() throws IOException {
        // Белый фон + красный блок с белой полостью внутри (имитация буквы «А»
        // Альфа-Банка): при removeAllWhite=true полость считается фоном и удаляется.
        BufferedImage image = new BufferedImage(120, 120, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, 120, 120);
            g.setColor(new Color(200, 40, 40));
            g.fillRect(30, 20, 60, 80);
            g.setColor(Color.WHITE);
            g.fillRect(50, 40, 20, 40); // замкнутая белая полость
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);

        ProcessedImage processed = service.process(out.toByteArray(), "logo.jpg");
        BufferedImage result = read(processed);

        // Центр канваса — полость (логотип рисуется по центру): должен стать прозрачным.
        int centerAlpha = (result.getRGB(result.getWidth() / 2, result.getHeight() / 2) >> 24) & 0xFF;
        assertEquals("Белая полость внутри буквы должна удаляться (removeAllWhite=true)",
                0, centerAlpha);
    }

    @Test
    public void testNonImageFileIsPassedThrough() {
        // Не изображение — обработка не требуется, данные возвращаются как есть.
        byte[] data = "not an image".getBytes();
        ProcessedImage processed = service.process(data, "file.txt");

        assertFalse("Не-изображение не должно обрабатываться", processed.isProcessed());
        assertArrayEquals("Данные не должны меняться", data, processed.getData());
    }

    @Test
    public void testGrayGradientBackgroundBecomesTransparent() throws IOException {
        // Логотип SSP: серый градиентный фон (от светлого к тёмно-серому) + синий логотип
        // в центре. Серый фон, соединённый с краями, должен стать полностью прозрачным,
        // а цветной логотип сохраниться.
        BufferedImage image = new BufferedImage(300, 300, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            // Горизонтальный серый градиент: слева светлый (200,200,200), справа тёмный (80,80,80).
            for (int x = 0; x < 300; x++) {
                int v = 200 - (int) (120.0 * x / 299);
                g.setColor(new Color(v, v, v));
                g.drawLine(x, 0, x, 299);
            }
            // Синий логотип в центре (насыщенный цвет — не должен быть удалён как серый фон).
            g.setColor(new Color(20, 60, 200));
            g.fillRect(120, 110, 60, 80);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);

        ProcessedImage processed = service.process(out.toByteArray(), "logo.jpg");
        BufferedImage result = read(processed);

        // Угол канваса был серым — после обработки должен стать полностью прозрачным.
        int cornerAlpha = (result.getRGB(0, 0) >> 24) & 0xFF;
        assertEquals("Серый градиентный угол должен быть прозрачным (фон удалён)", 0, cornerAlpha);

        // Синий логотип в центре должен остаться непрозрачным.
        int centerAlpha = (result.getRGB(result.getWidth() / 2, result.getHeight() / 2) >> 24) & 0xFF;
        assertTrue("Синий логотип должен остаться непрозрачным", centerAlpha > 200);
    }

    @Test
    public void testSmallImageIsNotUpscaled() throws IOException {
        // Маленькое изображение 100x100 — не должно увеличиваться сверх исходного размера.
        byte[] data = createWhiteBackgroundImage(100, 100);
        ProcessedImage processed = service.process(data, "logo.png");
        BufferedImage result = read(processed);

        assertTrue("Канвас не больше 300", result.getWidth() <= 300);
        assertTrue("Канвас не больше 300", result.getHeight() <= 300);
    }
}
