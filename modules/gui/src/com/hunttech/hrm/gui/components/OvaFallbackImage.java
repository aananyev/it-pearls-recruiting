package com.hunttech.hrm.gui.components;

import com.company.itpearls.gui.components.OvalImage;

/**
 * Round avatar image with theme fallback when bound value is null, empty, or missing in storage.
 * Combines {@link OvalImage} sizing API and {@link FallbackImage} placeholder API via a single contract.
 */
public interface OvaFallbackImage extends OvalImage, FallbackImage {

    String NAME = "ovaFallbackImage";
}
