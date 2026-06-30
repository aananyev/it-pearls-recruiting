package com.hunttech.hrm.gui.components;

import com.haulmont.cuba.gui.components.Image;
import com.haulmont.cuba.gui.components.Resource;

public interface FallbackImage extends Image {

    String NAME = "fallbackImage";

    Resource getFallbackResource();

    void setFallbackResource(Resource resource);

    void setFallbackThemePath(String path);
}
