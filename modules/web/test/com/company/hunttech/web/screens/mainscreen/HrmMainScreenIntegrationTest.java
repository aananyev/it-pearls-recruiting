package com.company.hunttech.web.screens.mainscreen;

import com.company.hunttech.web.HrmWebTestContainer;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Events;
import com.haulmont.cuba.gui.Screens;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.HtmlAttributes;
import com.haulmont.cuba.gui.screen.OpenMode;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.web.testsupport.TestUiEnvironment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Screen-level integration проверяет реальный CUBA controller/component tree и assigned HtmlAttributes.
 * Computed DOM style, HTTP connector URL и screenshot подтверждаются browser smoke Hermes.
 */
public class HrmMainScreenIntegrationTest {

    @RegisterExtension
    TestUiEnvironment environment =
            new TestUiEnvironment(HrmWebTestContainer.Common.INSTANCE)
                    .withUserLogin("admin");

    @Test
    public void configuredRootScreenUsesDedicatedBackgroundLayer() {
        Screens screens = environment.getScreens();
        Screen root = screens.create("hrmMainScreen", OpenMode.ROOT);
        root.show();

        assertTrue(root instanceof HrmMainScreen);
        HrmMainScreen mainScreen = (HrmMainScreen) root;

        Component layer = root.getWindow().getComponent("mainScreenBackgroundLayer");
        Component mainVBox = root.getWindow().getComponent("mainVBox");
        Component dashboard = root.getWindow().getComponent("mainDashboard");
        assertNotNull(layer);
        assertNotNull(mainVBox);
        assertNotNull(dashboard);

        HtmlAttributes html = AppBeans.get(HtmlAttributes.class);
        assertEquals("applied",
                html.getDomAttribute(layer, "data-hrm-main-background"));
        assertEquals("HrmMainScreen",
                html.getDomAttribute(mainVBox, "data-hrm-main-controller"));

        String layerImage = html.getCssProperty(layer, "background-image");
        assertNotNull(layerImage);
        assertTrue(layerImage.contains("url('"));
        assertNull(html.getCssProperty(mainVBox, "background-image"));
        assertNull(html.getCssProperty(dashboard, "background-image"));
        assertEquals("none",
                html.getCssProperty(layer, "pointer-events"));
        assertEquals("0",
                html.getCssProperty(layer, "z-index"));
        assertEquals("1",
                html.getCssProperty(dashboard, "z-index"));

        String firstUrl = mainScreen.getLastAppliedResourceUrl();
        Events events = AppBeans.get(Events.class);
        events.publish(new MainScreenBackgroundChangedEvent(this));
        String secondUrl = mainScreen.getLastAppliedResourceUrl();

        assertNotNull(firstUrl);
        assertNotNull(secondUrl);
        assertNotEquals(firstUrl, secondUrl);
        assertTrue(html.getCssProperty(layer, "background-image")
                .contains(secondUrl));
    }

    @Test
    public void imageProcessorNormalizesContentAndRejectsExtensionSpoofing()
            throws Exception {
        MainScreenBackgroundImageProcessor processor =
                AppBeans.get(MainScreenBackgroundImageProcessor.class);

        BufferedImage source = new BufferedImage(
                3200, 1800, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = source.createGraphics();
        try {
            graphics.setColor(new Color(40, 80, 120, 180));
            graphics.fillRect(0, 0, source.getWidth(), source.getHeight());
        } finally {
            graphics.dispose();
        }

        byte[] png;
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            assertTrue(ImageIO.write(source, "png", output));
            png = output.toByteArray();
        }

        MainScreenBackgroundImageProcessor.ProcessedImage processed =
                processor.process(png, "background.png");
        assertEquals("jpg", processed.getExtension());
        assertEquals("image/jpeg", processed.getMimeType());
        assertTrue(processed.getWidth()
                <= MainScreenBackgroundImageProcessor.TARGET_WIDTH);
        assertTrue(processed.getHeight()
                <= MainScreenBackgroundImageProcessor.TARGET_HEIGHT);
        assertTrue(processed.getBytes().length
                <= MainScreenBackgroundImageProcessor.MAX_OUTPUT_BYTES);
        assertFalse(processed.getBytes().length == 0);

        assertThrows(MainScreenBackgroundImageProcessor.ImageValidationException.class,
                () -> processor.process(png, "spoofed.jpg"));
        assertTrue(ImageIO.getImageReadersByFormatName("webp").hasNext(),
                "TwelveMonkeys WEBP reader должен быть зарегистрирован в web classpath");
    }
}
