package com.company.hunttech.config;

import com.haulmont.cuba.core.config.Config;
import com.haulmont.cuba.core.config.Property;
import com.haulmont.cuba.core.config.Source;
import com.haulmont.cuba.core.config.SourceType;
import com.haulmont.cuba.core.config.defaults.DefaultInt;
import com.haulmont.cuba.core.config.defaults.DefaultString;

@Source(type = SourceType.DATABASE)
public interface HunttechImageConfig extends Config {

    @Property("hunttech.image.resize.size")
    @DefaultInt(1024)
    int getTargetImageSize();

    @Property("hunttech.image.resize.format")
    @DefaultString("png")
    String getTargetImageFormat();

    @Property("hunttech.defaultFallbackImagePath")
    @DefaultString("images/hunttech-placeholder.svg")
    String getDefaultFallbackImagePath();
}
