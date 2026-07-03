package com.company.hunttech.gui.components;

import com.haulmont.cuba.gui.components.Image;

public interface OvalImage extends Image {

    String NAME = "ovalImage";

    String getOvalWidth();

    void setOvalWidth(String width);

    String getOvalHeight();

    void setOvalHeight(String height);
}
