package com.company.hunttech.web.components;

import com.haulmont.cuba.gui.components.Image;
import com.hunttech.hrm.gui.components.OvaFallbackImage;
import com.hunttech.hrm.web.components.WebOvaFallbackImage;
import com.hunttech.hrm.web.loaders.OvaFallbackImageLoader;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тестирует компонент OvalFallbackImage (WebOvaFallbackImage) и лоадер OvaFallbackImageLoader:
 * 1. Новое свойство stretchToOval (default false, переключение в true);
 * 2. Правило «один размер -> круг» (width -> width x width, height -> height x height);
 * 3. Овальная геометрия (width != height);
 * 4. Runtime-изменение размеров и режима stretchToOval;
 * 5. Наличие стилей .ht-oval-stretch во всех 7 темах оформления;
 * 6. Корректность регистрации дескриптора и алиаса ovalFallbackImage.
 */
public class OvalFallbackImageStretchTest {

    private static final String[] SUPPORTED_THEMES = {
            "halo",
            "havana",
            "helium",
            "hover",
            "hunttech-modern",
            "hunttech-modern-light",
            "hunttech-modern-dark"
    };

    private File resolveFile(String relativePath) {
        File f = new File(relativePath);
        if (f.exists()) return f;
        f = new File("../" + relativePath);
        if (f.exists()) return f;
        f = new File("../../" + relativePath);
        if (f.exists()) return f;
        return new File(relativePath);
    }

    @Test
    @DisplayName("Тест 1: Квадратный круг 80x80 с stretchToOval=true занимает всю область и получает ScaleMode.FILL")
    public void test1_SquareCircleWithStretchToOval() {
        WebOvaFallbackImage img = new WebOvaFallbackImage();
        img.setWidth("80px");
        img.setHeight("80px");
        img.setStretchToOval(true);

        assertTrue(img.isStretchToOval());
        assertEquals("80px", img.getEffectiveWidth());
        assertEquals("80px", img.getEffectiveHeight());
        assertEquals(Image.ScaleMode.FILL, img.getScaleMode());
        assertTrue(img.getStyleName().contains(WebOvaFallbackImage.STRETCH_STYLE_NAME));
    }

    @Test
    @DisplayName("Тест 2: Овал 120x80 с stretchToOval=true сохраняет разные размеры и режим FILL")
    public void test2_OvalWithStretchToOval() {
        WebOvaFallbackImage img = new WebOvaFallbackImage();
        img.setWidth("120px");
        img.setHeight("80px");
        img.setStretchToOval(true);

        assertTrue(img.isStretchToOval());
        assertEquals("120px", img.getEffectiveWidth());
        assertEquals("80px", img.getEffectiveHeight());
        assertEquals(Image.ScaleMode.FILL, img.getScaleMode());
        assertTrue(img.getStyleName().contains(WebOvaFallbackImage.STRETCH_STYLE_NAME));
    }

    @Test
    @DisplayName("Тест 3: Задана только ширина 80px -> трактуется как круг 80x80")
    public void test3_OnlyWidthGivesCircle() {
        WebOvaFallbackImage img = new WebOvaFallbackImage();
        img.setWidth("80px");
        img.setStretchToOval(true);

        assertTrue(img.isStretchToOval());
        assertEquals("80px", img.getEffectiveWidth());
        assertEquals("80px", img.getEffectiveHeight());
        assertEquals(Image.ScaleMode.FILL, img.getScaleMode());
    }

    @Test
    @DisplayName("Тест 4: Задана только высота 80px -> трактуется как круг 80x80")
    public void test4_OnlyHeightGivesCircle() {
        WebOvaFallbackImage img = new WebOvaFallbackImage();
        img.setHeight("80px");
        img.setStretchToOval(true);

        assertTrue(img.isStretchToOval());
        assertEquals("80px", img.getEffectiveWidth());
        assertEquals("80px", img.getEffectiveHeight());
        assertEquals(Image.ScaleMode.FILL, img.getScaleMode());
    }

    @Test
    @DisplayName("Тест 5: stretchToOval по умолчанию false, сохраняет стандартный SCALE_DOWN и старое поведение")
    public void test5_DefaultStretchToOvalIsFalseAndPreservesOldBehavior() {
        WebOvaFallbackImage img = new WebOvaFallbackImage();
        img.setWidth("80px");
        img.setHeight("80px");

        assertFalse(img.isStretchToOval(), "По умолчанию stretchToOval должен быть false");
        assertFalse(img.getStyleName().contains(WebOvaFallbackImage.STRETCH_STYLE_NAME));
        assertEquals(Image.ScaleMode.SCALE_DOWN, img.getScaleMode());
    }

    @Test
    @DisplayName("Тест 6: Явный stretchToOval=false сохраняет старое поведение")
    public void test6_ExplicitStretchToOvalFalsePreservesOldBehavior() {
        WebOvaFallbackImage img = new WebOvaFallbackImage();
        img.setWidth("80px");
        img.setHeight("80px");
        img.setStretchToOval(false);

        assertFalse(img.isStretchToOval());
        assertFalse(img.getStyleName().contains(WebOvaFallbackImage.STRETCH_STYLE_NAME));
        assertEquals(Image.ScaleMode.SCALE_DOWN, img.getScaleMode());
    }

    @Test
    @DisplayName("Тест 7: Динамическое переключение setStretchToOval(true/false) в runtime")
    public void test7_RuntimeTogglingStretchToOval() {
        WebOvaFallbackImage img = new WebOvaFallbackImage();
        img.setWidth("80px");
        img.setHeight("80px");

        // Изначально выключен
        assertFalse(img.isStretchToOval());
        assertEquals(Image.ScaleMode.SCALE_DOWN, img.getScaleMode());

        // Включаем динамически
        img.setStretchToOval(true);
        assertTrue(img.isStretchToOval());
        assertEquals(Image.ScaleMode.FILL, img.getScaleMode());
        assertTrue(img.getStyleName().contains(WebOvaFallbackImage.STRETCH_STYLE_NAME));

        // Выключаем динамически - должен вернуться SCALE_DOWN
        img.setStretchToOval(false);
        assertFalse(img.isStretchToOval());
        assertEquals(Image.ScaleMode.SCALE_DOWN, img.getScaleMode());
        assertFalse(img.getStyleName().contains(WebOvaFallbackImage.STRETCH_STYLE_NAME));
    }

    @Test
    @DisplayName("Тест 8: Изменение размеров в runtime с круга на овал и обратно")
    public void test8_RuntimeResizeCircleToOvalAndBack() {
        WebOvaFallbackImage img = new WebOvaFallbackImage();
        img.setWidth("80px");
        img.setStretchToOval(true);

        // Круг 80x80
        assertEquals("80px", img.getEffectiveWidth());
        assertEquals("80px", img.getEffectiveHeight());

        // Меняем на овал 120x80
        img.setWidth("120px");
        img.setHeight("80px");
        assertEquals("120px", img.getEffectiveWidth());
        assertEquals("80px", img.getEffectiveHeight());

        // Меняем на вертикальный овал 80x120
        img.setWidth("80px");
        img.setHeight("120px");
        assertEquals("80px", img.getEffectiveWidth());
        assertEquals("120px", img.getEffectiveHeight());
    }

    @Test
    @DisplayName("Тест 9: OvaFallbackImageLoader корректно парсит stretchToOval и недостающие размеры")
    public void test9_LoaderResolvesStretchToOvalAndMissingDimensions() {
        OvaFallbackImageLoader loader = new OvaFallbackImageLoader();

        // Тест с width="64px" и stretchToOval="true"
        Element el1 = DocumentHelper.createElement("custom:ovalFallbackImage");
        el1.addAttribute("id", "logo1");
        el1.addAttribute("width", "64px");
        el1.addAttribute("stretchToOval", "true");

        WebOvaFallbackImage comp1 = new WebOvaFallbackImage();
        setResultComponent(loader, comp1);
        setElement(loader, el1);

        loader.loadComponent();

        assertTrue(comp1.isStretchToOval());
        assertEquals("64px", comp1.getEffectiveWidth());
        assertEquals("64px", comp1.getEffectiveHeight());
        assertEquals(Image.ScaleMode.FILL, comp1.getScaleMode());

        // Тест с овала 120x80
        Element el2 = DocumentHelper.createElement("custom:ovalFallbackImage");
        el2.addAttribute("id", "logo2");
        el2.addAttribute("width", "120px");
        el2.addAttribute("height", "80px");
        el2.addAttribute("stretchToOval", "true");

        WebOvaFallbackImage comp2 = new WebOvaFallbackImage();
        setResultComponent(loader, comp2);
        setElement(loader, el2);

        loader.loadComponent();

        assertTrue(comp2.isStretchToOval());
        assertEquals("120px", comp2.getEffectiveWidth());
        assertEquals("80px", comp2.getEffectiveHeight());
    }

    @Test
    @DisplayName("Тест 10: Стили .ht-oval-stretch с object-fit: fill присутствуют во всех 7 темах")
    public void test10_ThemesContainStretchStyles() throws IOException {
        for (String theme : SUPPORTED_THEMES) {
            String relative = "modules/web/themes/" + theme + "/com.company.hunttech/" +
                    (theme.equals("hunttech-modern") ? "hunttech-modern-ext.scss" :
                     theme.equals("hunttech-modern-dark") ? "hunttech-modern-dark-ext.scss" :
                     theme.equals("hunttech-modern-light") ? "hunttech-modern-light-ext.scss" :
                     theme + "-ext.scss");
            File themeFile = resolveFile(relative);
            assertTrue(themeFile.exists(), "Файл темы должен существовать: " + relative);

            String scss = new String(Files.readAllBytes(themeFile.toPath()), StandardCharsets.UTF_8);

            assertTrue(scss.contains(".ht-oval-stretch"),
                    theme + " должна содержать стиль .ht-oval-stretch");
            assertTrue(scss.contains("object-fit: fill !important;"),
                    theme + " должна содержать object-fit: fill для натягивания на овал");
        }
    }

    @Test
    @DisplayName("Тест 11: Регистрация дескрипторов и алиасов ovalFallbackImage")
    public void test11_ComponentDescriptorAndRegistrations() throws IOException {
        File compFile = resolveFile("modules/web/src/com/hunttech/hrm/web/cuba-ui-component.xml");
        String componentXml = new String(Files.readAllBytes(compFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(componentXml.contains("<name>ovaFallbackImage</name>"),
                "cuba-ui-component.xml должен содержать тег ovaFallbackImage");
        assertTrue(componentXml.contains("<name>ovalFallbackImage</name>"),
                "cuba-ui-component.xml должен содержать тег ovalFallbackImage");

        File regFile = resolveFile("modules/web/src/com/hunttech/hrm/web/config/HunttechUiComponentsRegistrar.java");
        String registrar = new String(Files.readAllBytes(regFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(registrar.contains("webUiComponents.register(OvaFallbackImage.NAME, WebOvaFallbackImage.class);"));
        assertTrue(registrar.contains("webUiComponents.register(OvaFallbackImage.ALIAS_NAME, WebOvaFallbackImage.class);"));
    }

    private void setResultComponent(OvaFallbackImageLoader loader, WebOvaFallbackImage comp) {
        try {
            java.lang.reflect.Field field = com.haulmont.cuba.gui.xml.layout.loaders.AbstractComponentLoader.class
                    .getDeclaredField("resultComponent");
            field.setAccessible(true);
            field.set(loader, comp);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setElement(OvaFallbackImageLoader loader, Element el) {
        try {
            java.lang.reflect.Field field = com.haulmont.cuba.gui.xml.layout.loaders.AbstractComponentLoader.class
                    .getDeclaredField("element");
            field.setAccessible(true);
            field.set(loader, el);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
