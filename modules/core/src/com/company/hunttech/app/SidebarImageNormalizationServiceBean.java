package com.company.hunttech.app;

import net.coobird.thumbnailator.util.exif.ExifFilterUtils;
import net.coobird.thumbnailator.util.exif.ExifUtils;
import net.coobird.thumbnailator.util.exif.Orientation;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Arrays;

/**
 * ImageIO-based implementation of the shared sidebar image contract.
 */
@Service(SidebarImageNormalizationService.NAME)
public class SidebarImageNormalizationServiceBean implements SidebarImageNormalizationService {

    static final int MAX_SIDE = 512;

    /**
     * 25 megapixels intentionally includes a 5000x5000 source from the acceptance
     * scenario, while bounding a decoded upload well below the previous 40 MP limit.
     * Large rasters are drawn directly into the <=512 ARGB target, so no second
     * full-size ARGB copy is allocated.
     */
    static final long MAX_PIXELS = SidebarImageNormalizationService.MAX_PIXELS;

    @Override
    public ProcessedImage normalize(byte[] data, String fileName) {
        validateInput(data);
        String normalizedName = normalizedBaseName(fileName);

        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            if (input == null) {
                throw invalidImage();
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw invalidImage();
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, false);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                validateDimensions(width, height);
                Orientation orientation = readExifOrientation(reader, data);

                // Index 0 is intentional: animated GIFs are normalized to their first
                // valid frame because sidebar components are not animation viewers.
                BufferedImage decoded = reader.read(0);
                if (decoded == null) {
                    throw invalidImage();
                }
                // Resize before EXIF rotation: max dimension is invariant under the
                // rotate/flip operation, while the filter then works only on <=512 px.
                BufferedImage resized = downscale(decoded);
                BufferedImage oriented = ExifFilterUtils.getFilterForOrientation(orientation)
                        .apply(resized);
                BufferedImage normalized = toArgb(oriented);
                return new ProcessedImage(writePng(normalized), normalizedName, "png", true);
            } finally {
                reader.dispose();
            }
        } catch (InvalidImageInputException e) {
            throw e;
        } catch (ImageProcessingException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new ImageProcessingException("Не удалось безопасно декодировать изображение.", e);
        }
    }

    private void validateInput(byte[] data) {
        if (data == null || data.length == 0) {
            throw new InvalidImageInputException("Файл изображения пуст.");
        }
        if (data.length > MAX_INPUT_BYTES) {
            throw new InvalidImageInputException("Размер изображения превышает "
                    + (MAX_INPUT_BYTES / (1024 * 1024)) + " МБ.");
        }
    }

    private void validateDimensions(int width, int height) {
        if (width <= 0 || height <= 0 || (long) width * height > MAX_PIXELS) {
            throw new InvalidImageInputException("Разрешение изображения превышает безопасный предел.");
        }
    }

    private Orientation readExifOrientation(ImageReader reader, byte[] sourceData) {
        try {
            Orientation orientation = ExifUtils.getExifOrientation(reader, 0);
            if (orientation != null) {
                return orientation;
            }
        } catch (IOException | RuntimeException ignored) {
            // Some ImageIO readers do not expose APP1 as metadata. The safe raw APP1
            // fallback below still delegates TIFF/IFD parsing to Thumbnailator.
        }
        byte[] exif = findJpegExifSegment(sourceData);
        try {
            Orientation orientation = exif == null ? null : ExifUtils.getOrientationFromExif(exif);
            return orientation == null ? Orientation.TOP_LEFT : orientation;
        } catch (RuntimeException ignored) {
            // Corrupt optional EXIF metadata must not reject an otherwise valid raster.
            return Orientation.TOP_LEFT;
        }
    }

    private byte[] findJpegExifSegment(byte[] data) {
        if (data.length < 4 || (data[0] & 0xff) != 0xff || (data[1] & 0xff) != 0xd8) {
            return null;
        }
        int cursor = 2;
        while (cursor + 1 < data.length) {
            if ((data[cursor] & 0xff) != 0xff) {
                cursor++;
                continue;
            }
            // Пропускаем дополнительные 0xFF перед маркером JPEG, чтобы корректно читать EXIF.
            while (cursor + 1 < data.length
                    && (data[cursor] & 0xff) == 0xff
                    && (data[cursor + 1] & 0xff) == 0xff) {
                cursor++;
            }
            if (cursor + 1 >= data.length) {
                return null;
            }
            int marker = data[cursor + 1] & 0xff;
            cursor += 2;
            if (marker == 0xd9 || marker == 0xda) {
                return null;
            }
            if (marker == 0x01 || marker >= 0xd0 && marker <= 0xd7) {
                continue;
            }
            if (cursor + 2 > data.length) {
                return null;
            }
            int length = (data[cursor] & 0xff) << 8 | data[cursor + 1] & 0xff;
            if (length < 2 || cursor + length > data.length) {
                return null;
            }
            int payloadStart = cursor + 2;
            int payloadEnd = cursor + length;
            if (marker == 0xe1 && payloadEnd - payloadStart >= 6
                    && data[payloadStart] == 'E' && data[payloadStart + 1] == 'x'
                    && data[payloadStart + 2] == 'i' && data[payloadStart + 3] == 'f') {
                return Arrays.copyOfRange(data, payloadStart, payloadEnd);
            }
            cursor += length;
        }
        return null;
    }

    private BufferedImage toArgb(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_ARGB) {
            return source;
        }
        BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Src);
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return result;
    }

    private BufferedImage downscale(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        if (width <= MAX_SIDE && height <= MAX_SIDE) {
            return source;
        }
        double scale = (double) MAX_SIDE / Math.max(width, height);
        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));
        BufferedImage target = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Src);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    private byte[] writePng(BufferedImage image) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("png");
        if (!writers.hasNext()) {
            throw new ImageProcessingException("PNG encoder недоступен в runtime.");
        }
        ImageWriter writer = writers.next();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(bytes)) {
            writer.setOutput(output);
            ImageWriteParam parameters = writer.getDefaultWriteParam();
            if (parameters.canWriteCompressed()) {
                parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                // Средний lossless-профиль: быстрый UI request важнее нескольких КБ.
                parameters.setCompressionQuality(0.6f);
            }
            writer.write(null, new IIOImage(image, null, null), parameters);
        } finally {
            writer.dispose();
        }
        return bytes.toByteArray();
    }

    private String normalizedBaseName(String fileName) {
        String safe = fileName == null ? "sidebar-image" : fileName.trim();
        if (safe.isEmpty()) {
            return "sidebar-image";
        }
        int slash = Math.max(safe.lastIndexOf('/'), safe.lastIndexOf('\\'));
        if (slash >= 0) {
            safe = safe.substring(slash + 1);
        }
        int dot = safe.lastIndexOf('.');
        return dot > 0 ? safe.substring(0, dot) : safe;
    }

    private InvalidImageInputException invalidImage() {
        return new InvalidImageInputException("Файл не является поддерживаемым растровым изображением.");
    }
}
