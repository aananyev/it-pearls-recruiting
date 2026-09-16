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

        OvaFallbackImage component = (OvaFallbackImage) resultComponent;

        String stretchToOval = element.attributeValue("stretchToOval");
        if (StringUtils.isNotBlank(stretchToOval)) {
            component.setStretchToOval(Boolean.parseBoolean(stretchToOval));
        }

        String width = element.attributeValue("width");
        String height = element.attributeValue("height");
        if (StringUtils.isNotBlank(width)) {
            component.setWidth(width);
        }
        if (StringUtils.isNotBlank(height)) {
            component.setHeight(height);
        }

        String ovalWidth = element.attributeValue("ovalWidth");
        String ovalHeight = element.attributeValue("ovalHeight");

        if (StringUtils.isNotBlank(ovalWidth) && StringUtils.isBlank(ovalHeight)) {
            ovalHeight = ovalWidth;
        } else if (StringUtils.isNotBlank(ovalHeight) && StringUtils.isBlank(ovalWidth)) {
            ovalWidth = ovalHeight;
        }

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

        // Фон-подложка под прозрачное изображение (например, логотип после removeAllWhite):
        // атрибут тот же, что у ovalImage, общий CSS-класс через OvalImageBackgroundSupport.
        String ovalBackground = element.attributeValue("ovalBackground");
        if (StringUtils.isNotBlank(ovalBackground)) {
            component.setOvalBackground(ovalBackground);
        }
    }
}
