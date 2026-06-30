package com.hunttech.hrm.web.components;

import com.company.hunttech.config.HunttechImageConfig;
import com.haulmont.cuba.core.config.Config;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.BeanLocator;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.gui.components.FileDescriptorResource;
import com.haulmont.cuba.gui.components.Resource;
import com.haulmont.cuba.gui.components.ThemeResource;
import com.haulmont.cuba.gui.components.data.ValueSource;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WebOvaFallbackImageTest {

    private static final String GLOBAL_FALLBACK_PATH = "images/global-placeholder.svg";
    private static final String LOCAL_FALLBACK_PATH = "icons/local-placeholder.jpeg";

    @Test
    public void afterPropertiesSet_withoutLocalFallback_usesHunttechImageConfigPath() throws Exception {
        WebOvaFallbackImage image = new WebOvaFallbackImage();
        setBeanLocator(image, beanLocatorWithFallbackPath(GLOBAL_FALLBACK_PATH));

        image.afterPropertiesSet();

        Resource fallback = image.getFallbackResource();
        assertNotNull(fallback);
        assertTrue(fallback instanceof ThemeResource);
        assertEquals(GLOBAL_FALLBACK_PATH, ((ThemeResource) fallback).getPath());
    }

    @Test
    public void setFallbackThemePath_overridesGlobalConfig() throws Exception {
        WebOvaFallbackImage image = new WebOvaFallbackImage();
        setBeanLocator(image, beanLocatorWithFallbackPath(GLOBAL_FALLBACK_PATH));
        image.afterPropertiesSet();

        image.setFallbackThemePath(LOCAL_FALLBACK_PATH);

        Resource fallback = image.getFallbackResource();
        assertNotNull(fallback);
        assertEquals(LOCAL_FALLBACK_PATH, ((ThemeResource) fallback).getPath());
    }

    @Test
    public void updateComponent_withNullValue_usesFallbackResource() throws Exception {
        TrackingWebOvaFallbackImage image = new TrackingWebOvaFallbackImage();
        image.setFallbackThemePath(LOCAL_FALLBACK_PATH);

        @SuppressWarnings("unchecked")
        ValueSource<FileDescriptor> valueSource = mock(ValueSource.class);
        when(valueSource.getValue()).thenReturn(null);
        setValueSource(image, valueSource);

        invokeUpdateComponent(image);

        assertNotNull(image.lastUpdateValue);
        assertTrue(image.lastUpdateValue instanceof ThemeResource);
        assertEquals(LOCAL_FALLBACK_PATH, ((ThemeResource) image.lastUpdateValue).getPath());
    }

    @Test
    public void updateComponent_withMissingFileDescriptor_usesFallbackResource() throws Exception {
        TrackingWebOvaFallbackImage image = new TrackingWebOvaFallbackImage();
        image.setFallbackThemePath(LOCAL_FALLBACK_PATH);

        FileDescriptor fileDescriptor = new FileDescriptor();
        FileLoader fileLoader = mock(FileLoader.class);
        when(fileLoader.fileExists(fileDescriptor)).thenReturn(false);

        @SuppressWarnings("unchecked")
        ValueSource<FileDescriptor> valueSource = mock(ValueSource.class);
        when(valueSource.getValue()).thenReturn(fileDescriptor);
        setValueSource(image, valueSource);
        setBeanLocator(image, beanLocatorWithFileLoader(fileLoader));

        invokeUpdateComponent(image);

        assertNotNull(image.lastUpdateValue);
        assertTrue(image.lastUpdateValue instanceof ThemeResource);
        assertEquals(LOCAL_FALLBACK_PATH, ((ThemeResource) image.lastUpdateValue).getPath());
    }

    @Test
    public void updateComponent_withNonNullValue_delegatesToSuperWithoutFallbackSubstitution() throws Exception {
        TrackingWebOvaFallbackImage image = new TrackingWebOvaFallbackImage();
        image.setFallbackThemePath(LOCAL_FALLBACK_PATH);

        FileDescriptor fileDescriptor = new FileDescriptor();
        FileLoader fileLoader = mock(FileLoader.class);
        when(fileLoader.fileExists(fileDescriptor)).thenReturn(true);

        @SuppressWarnings("unchecked")
        ValueSource<FileDescriptor> valueSource = mock(ValueSource.class);
        when(valueSource.getValue()).thenReturn(fileDescriptor);
        setValueSource(image, valueSource);
        setBeanLocator(image, beanLocatorWithFileLoader(fileLoader));

        invokeUpdateComponent(image);

        assertNotNull(image.lastUpdateValue);
        assertTrue(image.lastUpdateValue instanceof FileDescriptorResource);
    }

    @Test
    public void updateComponent_withNullValueAndNoFallback_doesNotSubstitutePlaceholder() throws Exception {
        TrackingWebOvaFallbackImage image = new TrackingWebOvaFallbackImage();

        @SuppressWarnings("unchecked")
        ValueSource<FileDescriptor> valueSource = mock(ValueSource.class);
        when(valueSource.getValue()).thenReturn(null);
        setValueSource(image, valueSource);

        invokeUpdateComponent(image);

        assertNull(image.lastUpdateValue);
    }

    @Test
    public void setOvalWidth_syncsHeightWhenHeightBlank() {
        WebOvaFallbackImage image = new WebOvaFallbackImage();

        image.setOvalWidth("80px");

        assertEquals("80px", image.getOvalWidth());
        assertEquals("80px", image.getOvalHeight());
    }

    @Test
    public void setOvalHeight_syncsWidthWhenWidthBlank() {
        WebOvaFallbackImage image = new WebOvaFallbackImage();

        image.setOvalHeight("64px");

        assertEquals("64px", image.getOvalHeight());
        assertEquals("64px", image.getOvalWidth());
    }

    @Test
    public void afterPropertiesSet_appliesOvalStyleName() throws Exception {
        WebOvaFallbackImage image = new WebOvaFallbackImage();
        image.afterPropertiesSet();

        assertTrue(image.getStyleName().contains("ht-oval-image"));
    }

    private static BeanLocator beanLocatorWithFallbackPath(String fallbackPath) {
        HunttechImageConfig config = new StubHunttechImageConfig(fallbackPath);
        Configuration configuration = new Configuration() {
            @Override
            @SuppressWarnings("unchecked")
            public <T extends Config> T getConfig(Class<T> configClass) {
                if (configClass == HunttechImageConfig.class) {
                    return (T) config;
                }
                throw new IllegalArgumentException("Unknown config: " + configClass.getName());
            }
        };
        BeanLocator beanLocator = mock(BeanLocator.class);
        when(beanLocator.get(Configuration.class)).thenReturn(configuration);
        return beanLocator;
    }

    private static BeanLocator beanLocatorWithFileLoader(FileLoader fileLoader) {
        BeanLocator beanLocator = mock(BeanLocator.class);
        when(beanLocator.get(FileLoader.class)).thenReturn(fileLoader);
        return beanLocator;
    }

    private static void setBeanLocator(WebOvaFallbackImage image, BeanLocator beanLocator) throws Exception {
        Field field = findField(WebOvaFallbackImage.class, "beanLocator");
        field.setAccessible(true);
        field.set(image, beanLocator);
    }

    private static void setValueSource(WebOvaFallbackImage image, ValueSource<FileDescriptor> valueSource)
            throws Exception {
        Field field = findField(WebOvaFallbackImage.class, "valueSource");
        field.setAccessible(true);
        field.set(image, valueSource);
    }

    private static void invokeUpdateComponent(WebOvaFallbackImage image) throws Exception {
        Method method = WebOvaFallbackImage.class.getDeclaredMethod("updateComponent");
        method.setAccessible(true);
        method.invoke(image);
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static final class TrackingWebOvaFallbackImage extends WebOvaFallbackImage {
        private Resource lastUpdateValue;

        @Override
        public void updateValue(Resource resource) {
            lastUpdateValue = resource;
        }
    }

    private static final class StubHunttechImageConfig implements HunttechImageConfig {
        private final String defaultFallbackImagePath;

        private StubHunttechImageConfig(String defaultFallbackImagePath) {
            this.defaultFallbackImagePath = defaultFallbackImagePath;
        }

        @Override
        public int getTargetImageSize() {
            return 1024;
        }

        @Override
        public String getTargetImageFormat() {
            return "png";
        }

        @Override
        public String getDefaultFallbackImagePath() {
            return defaultFallbackImagePath;
        }
    }
}
