package com.company.hunttech.web.screens.mainscreen;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

/**
 * Проверяет и нормализует пользовательское изображение фона до безопасного JPEG.
 * Метаданные исходного файла не переносятся, а размеры проверяются до декодирования.
 */
@Component(MainScreenBackgroundImageProcessor.NAME)
public class MainScreenBackgroundImageProcessor {

    public static final String NAME = "hunttech_MainScreenBackgroundImageProcessor";
    public static final long MAX_INPUT_BYTES = 15L * 1024L * 1024L;
    public static final long MAX_OUTPUT_BYTES = 4L * 1024L * 1024L;
    public static final int MAX_SOURCE_DIMENSION = 12_000;
    public static final long MAX_SOURCE_PIXELS = 40_000_000L;
    public static final int TARGET_WIDTH = 2560;
    public static final int TARGET_HEIGHT = 1440;

    private static final float[] JPEG_QUALITIES = {0.88f, 0.78f, 0.68f};
    private static final int MIN_OUTPUT_DIMENSION = 640;

    public ProcessedImage process(byte[] sourceBytes, String originalFileName) {
        if (sourceBytes == null || sourceBytes.length == 0) {
            throw new ImageValidationException("Файл изображения пуст.");
        }
        if (sourceBytes.length > MAX_INPUT_BYTES) {
            throw new ImageValidationException("Размер файла превышает 15 МБ.");
        }

        ImageFormat format = detectFormat(sourceBytes);
        validateDeclaredExtension(originalFileName, format);

        try (ImageInputStream input = ImageIO.createImageInputStream(
                new ByteArrayInputStream(sourceBytes))) {
            if (input == null) {
                throw new ImageValidationException("Не удалось прочитать изображение.");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new ImageValidationException(
                        "Для формата изображения не найден безопасный декодер.");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int sourceWidth = reader.getWidth(0);
                int sourceHeight = reader.getHeight(0);
                validateDimensions(sourceWidth, sourceHeight);

                ImageReadParam readParam = reader.getDefaultReadParam();
                BufferedImage decoded = reader.read(0, readParam);
                if (decoded == null) {
                    throw new ImageValidationException("Изображение не декодировано.");
                }

                BufferedImage normalized = resizeToTarget(decoded);
                EncodedJpeg encoded = encodeWithinLimit(normalized);
                return new ProcessedImage(
                        encoded.bytes, "jpg", "image/jpeg",
                        encoded.image.getWidth(), encoded.image.getHeight(),
                        sourceWidth, sourceHeight);
            } finally {
                reader.dispose();
            }
        } catch (ImageValidationException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new ImageValidationException(
                    "Файл повреждён либо содержит неподдерживаемые данные изображения.", e);
        }
    }

    private ImageFormat detectFormat(byte[] bytes) {
        if (bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A) {
            return ImageFormat.PNG;
        }
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF) {
            return ImageFormat.JPEG;
        }
        if (bytes.length >= 12
                && bytes[0] == 'R'
                && bytes[1] == 'I'
                && bytes[2] == 'F'
                && bytes[3] == 'F'
                && bytes[8] == 'W'
                && bytes[9] == 'E'
                && bytes[10] == 'B'
                && bytes[11] == 'P') {
            return ImageFormat.WEBP;
        }
        throw new ImageValidationException("Допустимы только PNG, JPG, JPEG и WEBP.");
    }

    private void validateDeclaredExtension(String originalFileName, ImageFormat format) {
        String extension = extensionOf(originalFileName);
        if (!format.matches(extension)) {
            throw new ImageValidationException(
                    "Расширение файла не соответствует фактическому формату изображения.");
        }
    }

    private String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private void validateDimensions(int width, int height) {
        if (width <= 0 || height <= 0
                || width > MAX_SOURCE_DIMENSION
                || height > MAX_SOURCE_DIMENSION
                || (long) width * height > MAX_SOURCE_PIXELS) {
            throw new ImageValidationException(
                    "Разрешение изображения превышает безопасный предел.");
        }
    }

    private BufferedImage resizeToTarget(BufferedImage source) throws IOException {
        if (source.getWidth() <= TARGET_WIDTH && source.getHeight() <= TARGET_HEIGHT) {
            return flattenToRgb(source);
        }
        BufferedImage resized = Thumbnails.of(source)
                .size(TARGET_WIDTH, TARGET_HEIGHT)
                .keepAspectRatio(true)
                .asBufferedImage();
        return flattenToRgb(resized);
    }

    private BufferedImage flattenToRgb(BufferedImage source) {
        BufferedImage rgb = new BufferedImage(
                source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgb.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return rgb;
    }

    private EncodedJpeg encodeWithinLimit(BufferedImage initial) throws IOException {
        BufferedImage candidate = initial;
        while (true) {
            for (float quality : JPEG_QUALITIES) {
                byte[] encoded = writeJpeg(candidate, quality);
                if (encoded.length <= MAX_OUTPUT_BYTES) {
                    return new EncodedJpeg(candidate, encoded);
                }
            }

            if (Math.max(candidate.getWidth(), candidate.getHeight()) <= MIN_OUTPUT_DIMENSION) {
                throw new ImageValidationException(
                        "Не удалось уменьшить изображение до безопасного размера.");
            }
            int nextWidth = Math.max(1,
                    (int) Math.round(candidate.getWidth() * 0.85));
            int nextHeight = Math.max(1,
                    (int) Math.round(candidate.getHeight() * 0.85));
            candidate = flattenToRgb(Thumbnails.of(candidate)
                    .size(nextWidth, nextHeight)
                    .keepAspectRatio(true)
                    .asBufferedImage());
        }
    }

    private byte[] writeJpeg(BufferedImage image, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new ImageValidationException("JPEG-кодировщик недоступен.");
        }

        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(imageOutput);
            ImageWriteParam parameters = writer.getDefaultWriteParam();
            if (parameters.canWriteCompressed()) {
                parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                parameters.setCompressionQuality(quality);
            }
            // Новый IIOImage не получает metadata исходника, поэтому EXIF и иные служебные блоки удаляются.
            writer.write(null, new IIOImage(image, null, null), parameters);
            imageOutput.flush();
            return output.toByteArray();
        } finally {
            writer.dispose();
        }
    }

    private enum ImageFormat {
        PNG("png"),
        JPEG("jpg", "jpeg"),
        WEBP("webp");

        private final String[] extensions;

        ImageFormat(String... extensions) {
            this.extensions = extensions;
        }

        private boolean matches(String extension) {
            for (String candidate : extensions) {
                if (candidate.equals(extension)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static final class ProcessedImage {
        private final byte[] bytes;
        private final String extension;
        private final String mimeType;
        private final int width;
        private final int height;
        private final int sourceWidth;
        private final int sourceHeight;

        private ProcessedImage(byte[] bytes,
                               String extension,
                               String mimeType,
                               int width,
                               int height,
                               int sourceWidth,
                               int sourceHeight) {
            this.bytes = bytes;
            this.extension = extension;
            this.mimeType = mimeType;
            this.width = width;
            this.height = height;
            this.sourceWidth = sourceWidth;
            this.sourceHeight = sourceHeight;
        }

        public byte[] getBytes() {
            return bytes;
        }

        public String getExtension() {
            return extension;
        }

        public String getMimeType() {
            return mimeType;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public int getSourceWidth() {
            return sourceWidth;
        }

        public int getSourceHeight() {
            return sourceHeight;
        }
    }

    public static class ImageValidationException extends RuntimeException {
        public ImageValidationException(String message) {
            super(message);
        }

        public ImageValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static final class EncodedJpeg {
        private final BufferedImage image;
        private final byte[] bytes;

        private EncodedJpeg(BufferedImage image, byte[] bytes) {
            this.image = image;
            this.bytes = bytes;
        }
    }
}
