package com.hunttech.hrm.web.config;

import com.company.hunttech.web.gui.components.WebOvalImage;
import com.company.itpearls.gui.components.OvalImage;
import com.haulmont.cuba.core.sys.events.AppContextInitializedEvent;
import com.haulmont.cuba.web.gui.WebUiComponents;
import com.hunttech.hrm.gui.components.FallbackImage;
import com.hunttech.hrm.gui.components.OvaFallbackImage;
import com.hunttech.hrm.web.components.WebFallbackImage;
import com.hunttech.hrm.web.components.WebOvaFallbackImage;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Ensures HuntTech custom UI components are registered in {@link WebUiComponents}
 * for programmatic creation via {@code uiComponents.create(NAME)}.
 */
@Component
public class HunttechUiComponentsRegistrar {

    private final WebUiComponents webUiComponents;

    public HunttechUiComponentsRegistrar(WebUiComponents webUiComponents) {
        this.webUiComponents = webUiComponents;
    }

    @EventListener
    public void onAppContextInitialized(AppContextInitializedEvent event) {
        webUiComponents.register(OvalImage.NAME, WebOvalImage.class);
        webUiComponents.register(FallbackImage.NAME, WebFallbackImage.class);
        webUiComponents.register(OvaFallbackImage.NAME, WebOvaFallbackImage.class);
    }
}
