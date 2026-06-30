package com.company.hunttech.web.gui.components;

import com.company.itpearls.gui.components.OvalImage;
import com.haulmont.cuba.web.gui.components.WebImage;
import org.apache.commons.lang3.StringUtils;

public class WebOvalImage extends WebImage implements OvalImage {

    private String ovalWidth;
    private String ovalHeight;

    public WebOvalImage() {
        super();
        this.setStyleName("ht-oval-image");
    }

    @Override
    public String getOvalWidth() {
        return ovalWidth;
    }

    @Override
    public void setOvalWidth(String width) {
        this.ovalWidth = width;
        setWidth(width);
        if (StringUtils.isBlank(ovalHeight)) {
            setOvalHeightInternal(width);
        }
    }

    @Override
    public String getOvalHeight() {
        return ovalHeight;
    }

    @Override
    public void setOvalHeight(String height) {
        this.ovalHeight = height;
        setHeight(height);
        if (StringUtils.isBlank(ovalWidth)) {
            setOvalWidthInternal(height);
        }
    }

    private void setOvalWidthInternal(String width) {
        this.ovalWidth = width;
        setWidth(width);
    }

    private void setOvalHeightInternal(String height) {
        this.ovalHeight = height;
        setHeight(height);
    }
}
