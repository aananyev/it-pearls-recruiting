package com.company.hunttech.gui.components;

import com.haulmont.cuba.gui.components.Image;

public interface OvalImage extends Image {

    String NAME = "ovalImage";

    String getOvalWidth();

    void setOvalWidth(String width);

    String getOvalHeight();

    void setOvalHeight(String height);

    /**
     * Returns the CSS background of the component, configured via the {@code ovalBackground}
     * XML attribute (a backing layer for images with transparent background).
     */
    String getOvalBackground();

    /**
     * Sets the CSS background of the component, e.g. {@code "#ffffff"} or
     * {@code "rgba(255,255,255,0.9)"}. Use it when the component is bound to an image
     * with a transparent background: the background makes transparent areas visible
     * on any form background.
     */
    void setOvalBackground(String background);
}
