package com.company.hunttech.web.gui.components;

import com.company.itpearls.gui.components.OvalImage;
import com.haulmont.cuba.web.gui.components.WebImage;

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
    }

    @Override
    public String getOvalHeight() {
        return ovalHeight;
    }

    @Override
    public void setOvalHeight(String height) {
        this.ovalHeight = height;
        setHeight(height);
    }
}
