package com.hunttech.hrm.web.loaders;

import com.haulmont.cuba.gui.xml.layout.loaders.ImageLoader;
import com.hunttech.hrm.gui.components.OvaFallbackImage;
import org.apache.commons.lang3.StringUtils;

public class OvaFallbackImageLoader extends ImageLoader {

    @Override
    public void createComponent() {
        resultComponent = factory.create(OvaFallbackImage.NAME);
        loadId(resultComponent, element);
    }

    @Override
    public void loadComponent() {
        super.loadComponent();

        String ovalWidth = element.attributeValue("ovalWidth");
        String ovalHeight = element.attributeValue("ovalHeight");

        if (StringUtils.isNotBlank(ovalWidth) && StringUtils.isBlank(ovalHeight)) {
            ovalHeight = ovalWidth;
        } else if (StringUtils.isNotBlank(ovalHeight) && StringUtils.isBlank(ovalWidth)) {
            ovalWidth = ovalHeight;
        }

        OvaFallbackImage component = (OvaFallbackImage) resultComponent;
        if (StringUtils.isNotBlank(ovalWidth)) {
            component.setOvalWidth(ovalWidth);
        }
        if (StringUtils.isNotBlank(ovalHeight)) {
            component.setOvalHeight(ovalHeight);
        }

        String fallbackThemePath = element.attributeValue("fallbackThemePath");
        if (StringUtils.isNotBlank(fallbackThemePath)) {
            component.setFallbackThemePath(fallbackThemePath);
        }
    }
}
