package com.hunttech.hrm.web.components.delegate;

import org.apache.commons.lang3.StringUtils;

/**
 * Oval sizing and {@code ht-oval-image} style delegation extracted from {@code WebOvalImage}.
 */
public class OvalImageShapeDelegate {

    public static final String OVAL_STYLE_NAME = "ht-oval-image";

    private final OvalImageHost host;
    private String ovalWidth;
    private String ovalHeight;

    public OvalImageShapeDelegate(OvalImageHost host) {
        this.host = host;
    }

    public void applyOvalStyle() {
        host.addStyleName(OVAL_STYLE_NAME);
    }

    public String getOvalWidth() {
        return ovalWidth;
    }

    public void setOvalWidth(String width) {
        this.ovalWidth = width;
        host.setWidth(width);
        if (StringUtils.isBlank(ovalHeight)) {
            setOvalHeightInternal(width);
        }
    }

    public String getOvalHeight() {
        return ovalHeight;
    }

    public void setOvalHeight(String height) {
        this.ovalHeight = height;
        host.setHeight(height);
        if (StringUtils.isBlank(ovalWidth)) {
            setOvalWidthInternal(height);
        }
    }

    private void setOvalWidthInternal(String width) {
        this.ovalWidth = width;
        host.setWidth(width);
    }

    private void setOvalHeightInternal(String height) {
        this.ovalHeight = height;
        host.setHeight(height);
    }
}
