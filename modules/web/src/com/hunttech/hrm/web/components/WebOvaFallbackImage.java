package com.hunttech.hrm.web.components;

import com.company.hunttech.web.gui.components.OvalImageBackgroundSupport;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.BeanLocator;
import com.haulmont.cuba.gui.components.Resource;
import com.haulmont.cuba.gui.components.data.ValueSource;
import com.haulmont.cuba.web.gui.components.WebImage;
import com.haulmont.cuba.web.widgets.CubaImage;
import com.hunttech.hrm.gui.components.OvaFallbackImage;
import com.hunttech.hrm.web.components.delegate.FallbackImageHost;
import com.hunttech.hrm.web.components.delegate.FallbackImageResourceDelegate;
import com.hunttech.hrm.web.components.delegate.OvalImageHost;
import com.hunttech.hrm.web.components.delegate.OvalImageShapeDelegate;
import com.vaadin.ui.AbstractComponent;
import org.apache.commons.lang3.StringUtils;

/**
 * Web implementation combining oval sizing ({@code ht-oval-image}) and fallback placeholder logic
 * via composition/delegation to {@link OvalImageShapeDelegate} and {@link FallbackImageResourceDelegate}.
 * Guarantees that the default fallback image scales along vertical and horizontal dimensions
 * identically to the main image object.
 */
public class WebOvaFallbackImage extends WebImage implements OvaFallbackImage, OvalImageHost, FallbackImageHost {

    private final OvalImageShapeDelegate ovalDelegate;
    private final FallbackImageResourceDelegate fallbackDelegate;

    public WebOvaFallbackImage() {
        super();
        this.ovalDelegate = new OvalImageShapeDelegate(this);
        this.fallbackDelegate = new FallbackImageResourceDelegate(this);
    }

    @Override
    protected void initComponent(CubaImage image) {
        super.initComponent(image);
        ovalDelegate.applyOvalStyle();
        image.addStyleName("ht-oval-fallback-image");
        // По умолчанию режим масштабирования SCALE_DOWN для вписывания по вертикали и горизонтали
        if (getScaleMode() == null || getScaleMode() == ScaleMode.NONE) {
            setScaleMode(ScaleMode.SCALE_DOWN);
        }
    }

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        fallbackDelegate.initDefaultFromConfig();
        syncScaleMode();
    }

    // --- Sizing and Scaling Synchronization ---

    @Override
    public void setWidth(String width) {
        super.setWidth(width);
        if (ovalDelegate != null && StringUtils.isNotBlank(width)) {
            if (StringUtils.isBlank(ovalDelegate.getOvalWidth())) {
                ovalDelegate.setOvalWidth(width);
            }
        }
        syncScaleMode();
    }

    @Override
    public void setHeight(String height) {
        super.setHeight(height);
        if (ovalDelegate != null && StringUtils.isNotBlank(height)) {
            if (StringUtils.isBlank(ovalDelegate.getOvalHeight())) {
                ovalDelegate.setOvalHeight(height);
            }
        }
        syncScaleMode();
    }

    @Override
    public void setScaleMode(ScaleMode scaleMode) {
        super.setScaleMode(scaleMode);
        syncScaleMode();
    }

    private void syncScaleMode() {
        ScaleMode currentMode = getScaleMode();
        if (currentMode == null || currentMode == ScaleMode.NONE) {
            currentMode = ScaleMode.SCALE_DOWN;
            super.setScaleMode(currentMode);
        }
        if (component != null) {
            component.markAsDirty();
        }
    }

    // --- OvalImageHost / OvalImage delegation ---

    @Override
    public String getOvalWidth() {
        return ovalDelegate.getOvalWidth();
    }

    @Override
    public void setOvalWidth(String width) {
        ovalDelegate.setOvalWidth(width);
        syncScaleMode();
    }

    @Override
    public String getOvalHeight() {
        return ovalDelegate.getOvalHeight();
    }

    @Override
    public void setOvalHeight(String height) {
        ovalDelegate.setOvalHeight(height);
        syncScaleMode();
    }

    // --- OvalImage background delegation ---

    private String ovalBackground;

    @Override
    public String getOvalBackground() {
        return ovalBackground;
    }

    @Override
    public void setOvalBackground(String background) {
        this.ovalBackground = background;
        // Фон под прозрачным изображением (логотип после removeAllWhite):
        // динамический CSS-класс через Page.getStyles(), общий с WebOvalImage.
        OvalImageBackgroundSupport.applyBackground((AbstractComponent) component, background);
    }

    // --- FallbackImageHost / FallbackImage delegation ---

    @Override
    public BeanLocator getBeanLocator() {
        return beanLocator;
    }

    @Override
    public ValueSource<FileDescriptor> getBoundValueSource() {
        return valueSource;
    }

    @Override
    public void updateValue(Resource resource) {
        super.updateValue(resource);
        syncScaleMode();
    }

    @Override
    public Resource getFallbackResource() {
        return fallbackDelegate.getFallbackResource();
    }

    @Override
    public void setFallbackResource(Resource resource) {
        fallbackDelegate.setFallbackResource(resource);
        syncScaleMode();
    }

    @Override
    public void setFallbackThemePath(String path) {
        fallbackDelegate.setFallbackThemePath(path);
        syncScaleMode();
    }

    @Override
    public void applyFallback() {
        Resource fallback = fallbackDelegate.getFallbackResource();
        if (fallback != null) {
            updateValue(fallback);
        }
        syncScaleMode();
    }

    @Override
    protected void updateComponent() {
        if (fallbackDelegate.tryApplyFallback()) {
            syncScaleMode();
            return;
        }
        super.updateComponent();
        syncScaleMode();
    }
}
