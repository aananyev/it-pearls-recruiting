package com.hunttech.hrm.gui.components;

import com.company.hunttech.gui.components.OvalImage;

/**
 * Round avatar image with theme fallback when bound value is null, empty, or missing in storage.
 * Combines {@link OvalImage} sizing API and {@link FallbackImage} placeholder API via a single contract.
 */
public interface OvaFallbackImage extends OvalImage, FallbackImage {

    String NAME = "ovaFallbackImage";
    String ALIAS_NAME = "ovalFallbackImage";

    /**
     * Returns whether the image is stretched to completely fill the oval geometry.
     *
     * @return true if stretch to oval is active, false otherwise (default false)
     */
    boolean isStretchToOval();

    /**
     * Stretches the image independently in horizontal and vertical dimensions to
     * match the component's width and height, clipped by an oval or circular mask.
     *
     * @param stretchToOval true to enable stretch to oval mode, false for standard mode
     */
    void setStretchToOval(boolean stretchToOval);

    /**
     * Returns effective width computed according to OvaFallbackImage rules:
     * explicit width, or height if width is omitted (circle), or null if neither is set.
     */
    String getEffectiveWidth();

    /**
     * Returns effective height computed according to OvaFallbackImage rules:
     * explicit height, or width if height is omitted (circle), or null if neither is set.
     */
    String getEffectiveHeight();
}
