package com.company.hunttech.app;

import com.haulmont.cuba.core.global.DevelopmentException;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.imageio.IIOImage;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Iterator;
import java.util.zip.CRC32;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SidebarImageNormalizationServiceBeanTest {

    private final SidebarImageNormalizationService service = new SidebarImageNormalizationServiceBean();

    @Test
    public void wideJpegIsNormalizedToPngWithinBoundingBox() throws Exception {
        ProcessedImage result = service.normalize(image("jpg", 2000, 1000, false), "logo.jpg");

        assertPng(result, 512, 256);
    }

    @Test
    public void tallBmpKeepsAspectRatio() throws Exception {
        ProcessedImage result = service.normalize(image("bmp", 1000, 2000, false), "logo.bmp");

        assertPng(result, 256, 512);
    }

    @Test
    public void smallImageIsNotUpscaled() throws Exception {
        ProcessedImage result = service.normalize(image("png", 200, 100, false), "logo.png");

        assertPng(result, 200, 100);
    }

    @Test
    public void transparentPixelsRemainTransparent() throws Exception {
        BufferedImage source = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        source.setRGB(0, 0, new Color(10, 20, 30, 0).getRGB());

        ProcessedImage result = service.normalize(write(source, "png"), "alpha.png");
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result.getData()));

        assertEquals(0, (decoded.getRGB(0, 0) >>> 24) & 0xff);
    }

    @Test
    public void animatedGifUsesFirstFrameAndProducesPng() throws Exception {
        ProcessedImage result = service.normalize(animatedGif(), "animated.gif");

        assertPng(result, 40, 30);
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result.getData()));
        assertEquals(Color.RED.getRGB(), decoded.getRGB(10, 10));
    }

    @Test
    public void wbmpContentIsDecodedAndNormalizedToPng() throws Exception {
        BufferedImage source = new BufferedImage(37, 19, BufferedImage.TYPE_BYTE_BINARY);

        ProcessedImage result = service.normalize(write(source, "wbmp"), "mobile.wbmp");

        assertPng(result, 37, 19);
    }

    @Test
    public void webpContentIsDecodedAndNormalizedToPng() throws Exception {
        byte[] webp = Base64.getDecoder().decode(
                "UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEADsD+JaQAA3AAAAAA");

        ProcessedImage result = service.normalize(webp, "tiny.webp");

        assertPng(result, 1, 1);
    }

    @Test
    public void tiffContentIsDecodedAndNormalizedToPng() throws Exception {
        ProcessedImage result = service.normalize(image("tiff", 37, 19, false), "scan.tiff");

        assertPng(result, 37, 19);
    }

    @Test
    public void jpegExifOrientationSixSwapsOutputDimensions() throws Exception {
        byte[] jpeg = image("jpg", 120, 60, false);

        ProcessedImage result = service.normalize(withExifOrientation(jpeg, 6), "phone.jpg");

        assertPng(result, 60, 120);
    }

    @Test
    public void jpegExifScannerSkipsFillBytesBeforeApp1Marker() throws Exception {
        byte[] jpeg = withExifOrientation(image("jpg", 120, 60, false), 6);
        Method scanner = SidebarImageNormalizationServiceBean.class
                .getDeclaredMethod("findJpegExifSegment", byte[].class);
        scanner.setAccessible(true);

        byte[] exifSegment = (byte[]) scanner.invoke(new SidebarImageNormalizationServiceBean(), jpeg);

        assertNotNull(exifSegment);
        assertArrayEquals("Exif\0\0".getBytes(StandardCharsets.US_ASCII),
                java.util.Arrays.copyOf(exifSegment, 6));
    }

    @Test
    public void pixelLimitAllowsFiveThousandSquareButRejectsLargerImages() {
        assertEquals(25_000_000L, SidebarImageNormalizationServiceBean.MAX_PIXELS);
    }

    @Test
    public void fiveThousandSquareImageIsDownscaledToSidebarBounds() throws Exception {
        BufferedImage source = new BufferedImage(5_000, 5_000, BufferedImage.TYPE_BYTE_GRAY);

        ProcessedImage result = service.normalize(write(source, "png"), "large.png");

        assertPng(result, 512, 512);
    }

    @Test(expected = DevelopmentException.class)
    public void fakePngIsRejectedByContent() {
        service.normalize("not an image".getBytes(), "fake.png");
    }

    @Test(expected = DevelopmentException.class)
    public void oversizedUploadIsRejectedBeforeDecode() {
        service.normalize(new byte[SidebarImageNormalizationService.MAX_INPUT_BYTES + 1], "huge.png");
    }

    @Test(expected = DevelopmentException.class)
    public void excessivePixelCountIsRejectedFromHeaderBeforeDecode() throws Exception {
        service.normalize(pngHeader(8_000, 8_000), "pixel-bomb.png");
    }

    private void assertPng(ProcessedImage result, int width, int height) throws Exception {
        assertTrue(result.isProcessed());
        assertEquals("png", result.getExtension());
        assertArrayEquals(new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a},
                java.util.Arrays.copyOf(result.getData(), 8));
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result.getData()));
        assertEquals(width, decoded.getWidth());
        assertEquals(height, decoded.getHeight());
    }

    private byte[] image(String format, int width, int height, boolean alpha) throws Exception {
        BufferedImage image = new BufferedImage(width, height,
                alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.BLUE);
            graphics.fillRect(0, 0, width, height);
        } finally {
            graphics.dispose();
        }
        return write(image, format);
    }

    private byte[] write(BufferedImage image, String format) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertTrue("Runtime must provide an ImageIO writer for " + format, ImageIO.write(image, format, output));
        return output.toByteArray();
    }

    private byte[] animatedGif() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("gif");
        assertTrue("Runtime must provide an ImageIO GIF writer", writers.hasNext());
        ImageWriter writer = writers.next();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(bytes)) {
            writer.setOutput(output);
            writer.prepareWriteSequence(null);
            for (Color color : new Color[]{Color.RED, Color.BLUE}) {
                BufferedImage frame = new BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = frame.createGraphics();
                try {
                    graphics.setColor(color);
                    graphics.fillRect(0, 0, frame.getWidth(), frame.getHeight());
                } finally {
                    graphics.dispose();
                }
                ImageWriteParam param = writer.getDefaultWriteParam();
                writer.writeToSequence(new IIOImage(frame, null, null), param);
            }
            writer.endWriteSequence();
        } finally {
            writer.dispose();
        }
        return bytes.toByteArray();
    }

    private byte[] pngHeader(int width, int height) throws Exception {
        ByteArrayOutputStream chunk = new ByteArrayOutputStream();
        try (DataOutputStream data = new DataOutputStream(chunk)) {
            data.write("IHDR".getBytes(StandardCharsets.US_ASCII));
            data.writeInt(width);
            data.writeInt(height);
            data.writeByte(8);
            data.writeByte(6);
            data.writeByte(0);
            data.writeByte(0);
            data.writeByte(0);
        }
        byte[] chunkBytes = chunk.toByteArray();
        CRC32 crc = new CRC32();
        crc.update(chunkBytes);

        ByteArrayOutputStream png = new ByteArrayOutputStream();
        try (DataOutputStream data = new DataOutputStream(png)) {
            data.write(new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});
            data.writeInt(13);
            data.write(chunkBytes);
            data.writeInt((int) crc.getValue());
        }
        return png.toByteArray();
    }

    private byte[] withExifOrientation(byte[] jpeg, int orientation) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (DataOutputStream data = new DataOutputStream(output)) {
            data.write(jpeg, 0, 2); // SOI
            data.writeByte(0xff);
            data.writeByte(0xff); // JPEG marker fill byte
            data.writeByte(0xe1);
            data.writeShort(34); // APP1 length includes its two-byte length field
            data.write("Exif\0\0".getBytes(StandardCharsets.US_ASCII));
            data.writeByte('I');
            data.writeByte('I');
            data.writeShort(0x2a00);
            data.writeInt(0x08000000);
            data.writeShort(0x0100);
            data.writeShort(0x1201);
            data.writeShort(0x0300);
            data.writeInt(0x01000000);
            data.writeByte(orientation);
            data.writeByte(0);
            data.writeShort(0);
            data.writeInt(0);
            data.write(jpeg, 2, jpeg.length - 2);
        }
        return output.toByteArray();
    }
}
