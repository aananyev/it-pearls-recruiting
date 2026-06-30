package com.hunttech.hrm.web.components.delegate;

import com.company.hunttech.config.HunttechImageConfig;
import com.company.itpearls.web.util.FileDescriptorImageHelper;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.BeanLocator;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.gui.components.Resource;
import com.haulmont.cuba.gui.components.ThemeResource;
import com.haulmont.cuba.gui.components.data.ValueSource;
import org.apache.commons.lang3.StringUtils;

/**
 * Fallback placeholder delegation extracted from {@code WebFallbackImage}.
 */
public class FallbackImageResourceDelegate {

    private final FallbackImageHost host;
    private Resource fallbackResource;

    public FallbackImageResourceDelegate(FallbackImageHost host) {
        this.host = host;
    }

    public Resource getFallbackResource() {
        return fallbackResource;
    }

    public void setFallbackResource(Resource resource) {
        this.fallbackResource = resource;
    }

    public void setFallbackThemePath(String path) {
        if (StringUtils.isNotBlank(path)) {
            this.fallbackResource = host.createResource(ThemeResource.class).setPath(path);
        } else {
            this.fallbackResource = null;
        }
    }

    public void initDefaultFromConfig() {
        if (fallbackResource != null) {
            return;
        }
        BeanLocator beanLocator = host.getBeanLocator();
        if (beanLocator == null) {
            return;
        }
        Configuration configuration = beanLocator.get(Configuration.class);
        HunttechImageConfig config = configuration.getConfig(HunttechImageConfig.class);
        String defaultPath = config.getDefaultFallbackImagePath();
        if (StringUtils.isNotBlank(defaultPath)) {
            setFallbackThemePath(defaultPath);
        }
    }

    /**
     * @return {@code true} when fallback was applied and caller should skip default update
     */
    public boolean tryApplyFallback() {
        ValueSource<FileDescriptor> valueSource = host.getBoundValueSource();
        if (valueSource == null || fallbackResource == null) {
            return false;
        }
        Object value = valueSource.getValue();
        if (!shouldUseFallback(value)) {
            return false;
        }
        host.updateValue(fallbackResource);
        return true;
    }

    public boolean isFallbackResource(Resource resource) {
        return resource != null && resource == fallbackResource;
    }

    private boolean shouldUseFallback(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof CharSequence && StringUtils.isBlank((CharSequence) value)) {
            return true;
        }
        if (value instanceof FileDescriptor) {
            FileLoader fileLoader = resolveFileLoader();
            return !FileDescriptorImageHelper.fileExists(fileLoader, (FileDescriptor) value);
        }
        return false;
    }

    private FileLoader resolveFileLoader() {
        BeanLocator beanLocator = host.getBeanLocator();
        if (beanLocator == null) {
            return null;
        }
        return beanLocator.get(FileLoader.class);
    }
}
