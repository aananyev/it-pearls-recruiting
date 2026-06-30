package com.hunttech.hrm.web.components;

import com.company.hunttech.config.HunttechImageConfig;
import com.company.itpearls.web.util.FileDescriptorImageHelper;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.BeanLocator;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.gui.components.Resource;
import com.haulmont.cuba.gui.components.ThemeResource;
import com.haulmont.cuba.web.gui.components.WebImage;
import com.hunttech.hrm.gui.components.FallbackImage;
import org.apache.commons.lang3.StringUtils;

public class WebFallbackImage extends WebImage implements FallbackImage {

    private Resource fallbackResource;

    public WebFallbackImage() {
        super();
    }

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        initDefaultFallbackFromConfig();
    }

    private void initDefaultFallbackFromConfig() {
        if (fallbackResource != null || beanLocator == null) {
            return;
        }
        Configuration configuration = beanLocator.get(Configuration.class);
        HunttechImageConfig config = configuration.getConfig(HunttechImageConfig.class);
        String defaultPath = config.getDefaultFallbackImagePath();
        if (StringUtils.isNotBlank(defaultPath)) {
            setFallbackThemePath(defaultPath);
        }
    }

    @Override
    public Resource getFallbackResource() {
        return fallbackResource;
    }

    @Override
    public void setFallbackResource(Resource resource) {
        this.fallbackResource = resource;
    }

    @Override
    public void setFallbackThemePath(String path) {
        if (StringUtils.isNotBlank(path)) {
            this.fallbackResource = createResource(ThemeResource.class).setPath(path);
        } else {
            this.fallbackResource = null;
        }
    }

    @Override
    public void applyFallback() {
        if (fallbackResource != null) {
            updateValue(fallbackResource);
        }
    }

    @Override
    protected void updateComponent() {
        if (valueSource != null) {
            Object value = valueSource.getValue();
            if (value == null && fallbackResource != null) {
                updateValue(fallbackResource);
                return;
            }
            if (value instanceof FileDescriptor && fallbackResource != null) {
                FileLoader fileLoader = beanLocator != null ? beanLocator.get(FileLoader.class) : null;
                if (!FileDescriptorImageHelper.fileExists(fileLoader, (FileDescriptor) value)) {
                    updateValue(fallbackResource);
                    return;
                }
            }
        } else if (getSource() == null && fallbackResource != null) {
            updateValue(fallbackResource);
            return;
        }
        super.updateComponent();
    }
}
