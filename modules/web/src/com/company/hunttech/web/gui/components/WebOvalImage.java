package com.company.hunttech.web.gui.components;

import com.company.hunttech.gui.components.OvalImage;
import com.haulmont.cuba.web.gui.components.WebImage;
import com.haulmont.cuba.web.widgets.CubaImage;
import com.vaadin.ui.AbstractComponent;
import org.apache.commons.lang3.StringUtils;

public class WebOvalImage extends WebImage implements OvalImage {

    public static final String OVAL_STYLE_NAME = "ht-oval-image";

    private String ovalWidth;
    private String ovalHeight;
    private String ovalBackground;

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

    @Override
    public String getOvalBackground() {
        return ovalBackground;
    }

    @Override
    public void setOvalBackground(String background) {
        this.ovalBackground = background;
        // Инлайн-стилей в Vaadin 8 нет — фон задаётся динамическим CSS-классом
        // через Page.getStyles() (паттерн SignIconsEdit.injectColorCss). Класс
        // переиспользуется для одинаковых значений, поэтому соседние овалы
        // с другим фоном не перекрашиваются.
        OvalImageBackgroundSupport.applyBackground((AbstractComponent) component, background);
    }
}
