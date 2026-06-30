package com.company.hunttech.web.gui.components;

import com.company.itpearls.gui.components.OvalImage;
import com.haulmont.cuba.web.gui.components.WebImage;
import com.haulmont.cuba.web.widgets.CubaImage;
import org.apache.commons.lang3.StringUtils;

public class WebOvalImage extends WebImage implements OvalImage {

    public static final String OVAL_STYLE_NAME = "ht-oval-image";

    private String ovalWidth;
    private String ovalHeight;

    public WebOvalImage() {
        super();
    }

    @Override
    protected void initComponent(CubaImage image) {
        super.initComponent(image);
        image.addStyleName(OVAL_STYLE_NAME);
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
