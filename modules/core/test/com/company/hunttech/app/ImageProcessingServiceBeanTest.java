package com.company.hunttech.app;

import com.company.hunttech.config.HunttechImageConfig;
import com.haulmont.cuba.core.config.Config;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.DevelopmentException;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ImageProcessingServiceBeanTest {

    private static final int DEFAULT_TARGET_IMAGE_SIZE = 100;
    private static final String DEFAULT_TARGET_IMAGE_FORMAT = "png";

    @Test
    public void smallImageWithinLimits_returnsOriginalUnchanged() throws Exception {
        ImageProcessingServiceBean service = createService(DEFAULT_TARGET_IMAGE_SIZE, DEFAULT_TARGET_IMAGE_FORMAT);
        byte[] original = createImageBytes(50, 50, "png");

        ProcessedImage result = service.process(original, "avatar.png");

        assertFalse(result.isProcessed());
        assertArrayEquals(original, result.getData());
        assertEquals("avatar", result.getName());
        assertEquals("png", result.getExtension());
    }

    @Test
    public void largeImageByDimensions_isCompressedAndWithinTargetSize() throws Exception {
        ImageProcessingServiceBean service = createService(DEFAULT_TARGET_IMAGE_SIZE, DEFAULT_TARGET_IMAGE_FORMAT);
        byte[] original = createImageBytes(300, 200, "png");

        ProcessedImage result = service.process(original, "photo.png");

        assertTrue(result.isProcessed());
        BufferedImage output = ImageIO.read(new java.io.ByteArrayInputStream(result.getData()));
        assertNotNull(output);
        assertTrue(output.getWidth() <= DEFAULT_TARGET_IMAGE_SIZE);
        assertTrue(output.getHeight() <= DEFAULT_TARGET_IMAGE_SIZE);
    }

    @Test
    public void processedImage_updatesExtensionToTargetFormat() throws Exception {
        ImageProcessingServiceBean service = createService(DEFAULT_TARGET_IMAGE_SIZE, "jpeg");
        byte[] original = createImageBytes(250, 250, "png");

        ProcessedImage result = service.process(original, "profile.png");

        assertTrue(result.isProcessed());
        assertEquals("profile", result.getName());
        assertEquals("jpg", result.getExtension());
    }

    @Test
    public void processedImage_keepsPngExtension() throws Exception {
        ImageProcessingServiceBean service = createService(DEFAULT_TARGET_IMAGE_SIZE, DEFAULT_TARGET_IMAGE_FORMAT);
        byte[] original = createImageBytes(250, 250, "jpeg");

        ProcessedImage result = service.process(original, "profile.jpg");

        assertTrue(result.isProcessed());
        assertEquals("profile", result.getName());
        assertEquals("png", result.getExtension());
    }

    @Test
    public void emptyData_throwsDevelopmentException() throws Exception {
        ImageProcessingServiceBean service = createService(DEFAULT_TARGET_IMAGE_SIZE, DEFAULT_TARGET_IMAGE_FORMAT);

        try {
            service.process(new byte[0], "empty.png");
            fail("Expected DevelopmentException for empty byte array");
        } catch (DevelopmentException e) {
            assertTrue(e.getMessage().contains("Empty image data"));
        }

        try {
            service.process(null, "empty.png");
            fail("Expected DevelopmentException for null data");
        } catch (DevelopmentException e) {
            assertTrue(e.getMessage().contains("Empty image data"));
        }
    }

    @Test
    public void nonImageBytes_returnsOriginalUnchanged() throws Exception {
        ImageProcessingServiceBean service = createService(DEFAULT_TARGET_IMAGE_SIZE, DEFAULT_TARGET_IMAGE_FORMAT);
        byte[] notAnImage = new byte[]{0x00, 0x01, 0x02, 0x03, (byte) 0xFF};

        ProcessedImage result = service.process(notAnImage, "data.bin");

        assertFalse(result.isProcessed());
        assertArrayEquals(notAnImage, result.getData());
        assertEquals("data", result.getName());
        assertEquals("bin", result.getExtension());
    }

    private static ImageProcessingServiceBean createService(int targetImageSize, String targetImageFormat)
            throws Exception {
        ImageProcessingServiceBean service = new ImageProcessingServiceBean();
        HunttechImageConfig config = new StubHunttechImageConfig(targetImageSize, targetImageFormat);
        Configuration configuration = new Configuration() {
            @Override
            @SuppressWarnings("unchecked")
            public <T extends Config> T getConfig(Class<T> aClass) {
                if (aClass == HunttechImageConfig.class) {
                    return (T) config;
                }
                throw new IllegalArgumentException("Unknown config: " + aClass.getName());
            }
        };
        Field configurationField = ImageProcessingServiceBean.class.getDeclaredField("configuration");
        configurationField.setAccessible(true);
        configurationField.set(service, configuration);
        return service;
    }

    private static byte[] createImageBytes(int width, int height, String format) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        return writeImage(image, format);
    }

    private static byte[] writeImage(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return output.toByteArray();
    }

    private static final class StubHunttechImageConfig implements HunttechImageConfig {
        private final int targetImageSize;
        private final String targetImageFormat;

        private StubHunttechImageConfig(int targetImageSize, String targetImageFormat) {
            this.targetImageSize = targetImageSize;
            this.targetImageFormat = targetImageFormat;
        }

        @Override
        public int getTargetImageSize() {
            return targetImageSize;
        }

        @Override
        public String getTargetImageFormat() {
            return targetImageFormat;
        }

        @Override
        public String getDefaultFallbackImagePath() {
            return "images/hunttech-placeholder.svg";
        }
    }
}
