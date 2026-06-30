package com.hunttech.hrm.web.components;

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

/**
 * Web implementation combining oval sizing ({@code ht-oval-image}) and fallback placeholder logic
 * via composition/delegation to {@link OvalImageShapeDelegate} and {@link FallbackImageResourceDelegate}.
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
    }

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        fallbackDelegate.initDefaultFromConfig();
    }

    // --- OvalImageHost / OvalImage delegation ---

    @Override
    public String getOvalWidth() {
        return ovalDelegate.getOvalWidth();
    }

    @Override
    public void setOvalWidth(String width) {
        ovalDelegate.setOvalWidth(width);
    }

    @Override
    public String getOvalHeight() {
        return ovalDelegate.getOvalHeight();
    }

    @Override
    public void setOvalHeight(String height) {
        ovalDelegate.setOvalHeight(height);
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
    }

    @Override
    public Resource getFallbackResource() {
        return fallbackDelegate.getFallbackResource();
    }

    @Override
    public void setFallbackResource(Resource resource) {
        fallbackDelegate.setFallbackResource(resource);
    }

    @Override
    public void setFallbackThemePath(String path) {
        fallbackDelegate.setFallbackThemePath(path);
    }

    @Override
    public void applyFallback() {
        Resource fallback = fallbackDelegate.getFallbackResource();
        if (fallback != null) {
            updateValue(fallback);
        }
    }

    @Override
    protected void updateComponent() {
        if (fallbackDelegate.tryApplyFallback()) {
            return;
        }
        super.updateComponent();
    }
}
