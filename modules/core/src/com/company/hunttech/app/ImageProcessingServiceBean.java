package com.company.hunttech.app;

import com.company.hunttech.config.HunttechImageConfig;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.DevelopmentException;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service(ImageProcessingService.NAME)
public class ImageProcessingServiceBean implements ImageProcessingService {

    @Inject
    private Configuration configuration;

    @Override
    public ProcessedImage process(byte[] data, String fileName) {
        if (data == null || data.length == 0) {
            throw new DevelopmentException("Empty image data");
        }

        HunttechImageConfig config = configuration.getConfig(HunttechImageConfig.class);
        int targetImageSize = config.getTargetImageSize();
        String targetImageFormat = config.getTargetImageFormat();

        String name = extractName(fileName);
        String extension = extractExtension(fileName);

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
            if (image == null) {
                return new ProcessedImage(data, name, extension, false);
            }

            int width = image.getWidth();
            int height = image.getHeight();

            if (width <= targetImageSize && height <= targetImageSize) {
                return new ProcessedImage(data, name, extension, false);
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Thumbnails.of(new ByteArrayInputStream(data))
                    .size(targetImageSize, targetImageSize)
                    .outputFormat(targetImageFormat)
                    .toOutputStream(output);

            byte[] processedData = output.toByteArray();
            String newExtension = normalizeFormatExtension(targetImageFormat);
            return new ProcessedImage(processedData, name, newExtension, true);
        } catch (IOException e) {
            throw new DevelopmentException("Failed to process image: " + e.getMessage(), e);
        }
    }

    private static String extractName(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return "image";
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

    private static String normalizeFormatExtension(String targetFormat) {
        if (StringUtils.equalsIgnoreCase(targetFormat, "jpeg")) {
            return "jpg";
        }
        return targetFormat.toLowerCase();
    }
}
