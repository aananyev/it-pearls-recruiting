package com.hunttech.hrm.web.loaders;

import com.haulmont.cuba.gui.xml.layout.loaders.ImageLoader;
import com.hunttech.hrm.gui.components.FallbackImage;
import org.apache.commons.lang3.StringUtils;

public class FallbackImageLoader extends ImageLoader {

    @Override
    public void createComponent() {
        resultComponent = factory.create(FallbackImage.NAME);
        loadId(resultComponent, element);
    }

    @Override
    public void loadComponent() {
        super.loadComponent();

        String fallbackThemePath = element.attributeValue("fallbackThemePath");
        if (StringUtils.isNotBlank(fallbackThemePath)) {
            ((FallbackImage) resultComponent).setFallbackThemePath(fallbackThemePath);
        }
    }
}
