package com.hunttech.hrm.gui.components;

import com.haulmont.cuba.gui.components.Image;
import com.haulmont.cuba.gui.components.Resource;

public interface FallbackImage extends Image {

    String NAME = "fallbackImage";

    Resource getFallbackResource();

    void setFallbackResource(Resource resource);

    void setFallbackThemePath(String path);

    /**
     * Shows the configured fallback resource without binding to a datasource.
     * Use in column generators and other programmatic contexts instead of
     * {@code setSource(ThemeResource.class)}.
     */
    void applyFallback();
}
