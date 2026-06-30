package com.hunttech.hrm.web.components;

import com.company.hunttech.config.HunttechImageConfig;
import com.haulmont.cuba.core.global.Configuration;
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
    protected void updateComponent() {
        if (valueSource != null) {
            Object value = valueSource.getValue();
            if (value == null && fallbackResource != null) {
                updateValue(fallbackResource);
                return;
            }
        }
        super.updateComponent();
    }
}
