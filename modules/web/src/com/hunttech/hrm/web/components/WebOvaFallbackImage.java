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

    public static final String STRETCH_STYLE_NAME = "ht-oval-stretch";

    private final OvalImageShapeDelegate ovalDelegate;
    private final FallbackImageResourceDelegate fallbackDelegate;

    private String configuredWidth;
    private String configuredHeight;
    private boolean stretchToOval = false;
    private ScaleMode originalScaleMode;

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
            super.setScaleMode(ScaleMode.SCALE_DOWN);
        }
    }

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        fallbackDelegate.initDefaultFromConfig();
        applyGeometrySizing();
        syncScaleMode();
    }

    // --- StretchToOval API ---

    @Override
    public boolean isStretchToOval() {
        return stretchToOval;
    }

    @Override
    public void setStretchToOval(boolean stretchToOval) {
        this.stretchToOval = stretchToOval;
        if (stretchToOval) {
            addStyleName(STRETCH_STYLE_NAME);
            ScaleMode current = getScaleMode();
            if (current != ScaleMode.FILL && current != null && current != ScaleMode.NONE) {
                this.originalScaleMode = current;
            }
            super.setScaleMode(ScaleMode.FILL);
        } else {
            removeStyleName(STRETCH_STYLE_NAME);
            if (originalScaleMode != null) {
                super.setScaleMode(originalScaleMode);
            } else {
                super.setScaleMode(ScaleMode.SCALE_DOWN);
            }
        }
        applyGeometrySizing();
        syncScaleMode();
    }

    // --- Effective Sizing and Geometry ---

    @Override
    public String getEffectiveWidth() {
        if (StringUtils.isNotBlank(configuredWidth)) {
            return configuredWidth;
        }
        if (StringUtils.isNotBlank(configuredHeight)) {
            return configuredHeight;
        }
        if (ovalDelegate != null) {
            if (StringUtils.isNotBlank(ovalDelegate.getOvalWidth())) {
                return ovalDelegate.getOvalWidth();
            }
            if (StringUtils.isNotBlank(ovalDelegate.getOvalHeight())) {
                return ovalDelegate.getOvalHeight();
            }
        }
        return null;
    }

    @Override
    public String getEffectiveHeight() {
        if (StringUtils.isNotBlank(configuredHeight)) {
            return configuredHeight;
        }
        if (StringUtils.isNotBlank(configuredWidth)) {
            return configuredWidth;
        }
        if (ovalDelegate != null) {
            if (StringUtils.isNotBlank(ovalDelegate.getOvalHeight())) {
                return ovalDelegate.getOvalHeight();
            }
            if (StringUtils.isNotBlank(ovalDelegate.getOvalWidth())) {
                return ovalDelegate.getOvalWidth();
            }
        }
        return null;
    }

    private void applyGeometrySizing() {
        String effW = getEffectiveWidth();
        String effH = getEffectiveHeight();

        if (effW != null && effH != null) {
            if (ovalDelegate != null) {
                ovalDelegate.setOvalWidthExplicit(effW);
                ovalDelegate.setOvalHeightExplicit(effH);
            }
            super.setWidth(effW);
            super.setHeight(effH);
        }
    }

    // --- Sizing and Scaling Synchronization ---

    @Override
    public void setWidth(String width) {
        this.configuredWidth = width;
        super.setWidth(width);
        applyGeometrySizing();
        syncScaleMode();
    }

    @Override
    public void setHeight(String height) {
        this.configuredHeight = height;
        super.setHeight(height);
        applyGeometrySizing();
        syncScaleMode();
    }

    @Override
    public void setScaleMode(ScaleMode scaleMode) {
        if (scaleMode != null && scaleMode != ScaleMode.FILL && scaleMode != ScaleMode.NONE) {
            this.originalScaleMode = scaleMode;
        }
        if (stretchToOval) {
            super.setScaleMode(ScaleMode.FILL);
        } else {
            super.setScaleMode(scaleMode);
        }
        syncScaleMode();
    }

    private void syncScaleMode() {
        if (stretchToOval) {
            if (getScaleMode() != ScaleMode.FILL) {
                super.setScaleMode(ScaleMode.FILL);
            }
            addStyleName(STRETCH_STYLE_NAME);
        } else {
            removeStyleName(STRETCH_STYLE_NAME);
            ScaleMode currentMode = getScaleMode();
            if (currentMode == null || currentMode == ScaleMode.NONE) {
                currentMode = ScaleMode.SCALE_DOWN;
                super.setScaleMode(currentMode);
            }
        }
        if (component != null) {
            component.markAsDirty();
        }
    }

    // --- OvalImageHost / OvalImage delegation ---

    @Override
    public String getOvalWidth() {
        return getEffectiveWidth();
    }

    @Override
    public void setOvalWidth(String width) {
        this.configuredWidth = width;
        applyGeometrySizing();
        syncScaleMode();
    }

    @Override
    public String getOvalHeight() {
        return getEffectiveHeight();
    }

    @Override
    public void setOvalHeight(String height) {
        this.configuredHeight = height;
        applyGeometrySizing();
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
