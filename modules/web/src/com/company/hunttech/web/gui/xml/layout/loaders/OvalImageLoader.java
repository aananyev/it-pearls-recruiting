package com.company.hunttech.web.gui.xml.layout.loaders;

import com.company.itpearls.gui.components.OvalImage;
import com.haulmont.cuba.gui.xml.layout.loaders.ImageLoader;
import org.apache.commons.lang3.StringUtils;

public class OvalImageLoader extends ImageLoader {

    @Override
    public void createComponent() {
        resultComponent = factory.create(OvalImage.NAME);
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

        OvalImage ovalImage = (OvalImage) resultComponent;
        if (StringUtils.isNotBlank(ovalWidth)) {
            ovalImage.setOvalWidth(ovalWidth);
        }
        if (StringUtils.isNotBlank(ovalHeight)) {
            ovalImage.setOvalHeight(ovalHeight);
        }
    }
}
