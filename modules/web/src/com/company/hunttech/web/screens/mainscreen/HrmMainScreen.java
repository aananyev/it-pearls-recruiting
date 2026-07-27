package com.company.hunttech.web.screens.mainscreen;

import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.HtmlAttributes;
import com.haulmont.cuba.gui.components.VBoxLayout;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.security.global.UserSession;
import com.vaadin.server.Page;
import com.vaadin.server.Resource;
import com.vaadin.server.ResourceReference;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.server.StreamResource;
import com.vaadin.server.ThemeResource;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;

import javax.inject.Inject;
import java.util.UUID;

/**
 * Применяет фоновое изображение через CSS-инъекцию в Page.
 * Системные фоны загружаются из VAADIN/themes/{name}/backgrounds/{n}.jpg.
 * Пользовательский фон — StreamResource с байтами файла из FileStorage.
 */
@UiController("hrmMainScreen")
@UiDescriptor("hrm-main-screen.xml")
public class HrmMainScreen extends ExtMainScreen {

    private static final Logger log = LoggerFactory.getLogger(HrmMainScreen.class);

    @Inject
    private MainScreenBackgroundService mainScreenBackgroundService;
    @Inject
    private UserSession userSession;
    @Inject
    private VBoxLayout mainVBox;
    @Inject
    private Component mainDashboard;
    @Inject
    private HtmlAttributes htmlAttributes;

    private Image backgroundResourceHolder;
    private String lastAppliedResourceUrl;
    private String currentBackgroundStyleName;

    @Subscribe
    public void onAfterShowBackground(AfterShowEvent event) {
        refreshBackground();
    }

    @EventListener
    public void onMainScreenBackgroundChanged(MainScreenBackgroundChangedEvent event) {
        refreshBackground();
    }

    private void refreshBackground() {
        try {
            UI currentUi = UI.getCurrent();
            if (currentUi == null) {
                throw new IllegalStateException("Vaadin UI недоступен");
            }
            Resource resource = mainScreenBackgroundService.resolveForUser(
                    userSession.getUser(), currentUi.getTheme(), userSession);
            applyBackground(currentUi, resource);
        } catch (RuntimeException e) {
            log.warn("Cannot apply main screen background: {}", e.getMessage(), e);
        }
    }

    /**
     * Строит CSS-совместимый URL.
     * ThemeResource → VAADIN/themes/{theme}/backgrounds/{n}.jpg.
     * StreamResource → регистрируется через Vaadin connector → HTTP-путь.
     */
    private String buildBackgroundUrl(UI currentUi, Resource resource) {
        if (resource instanceof ThemeResource) {
            ThemeResource themeResource = (ThemeResource) resource;
            String themeName = currentUi.getTheme();
            return "VAADIN/themes/" + themeName + "/" + themeResource.getResourceId();
        }
        if (resource instanceof StreamResource) {
            AbstractOrderedLayout vaadinVBox = mainVBox.unwrap(AbstractOrderedLayout.class);
            return registerBackgroundResource(vaadinVBox, (StreamResource) resource);
        }
        throw new IllegalArgumentException("Неподдерживаемый тип фонового ресурса: "
                + (resource == null ? "null" : resource.getClass().getName()));
    }

    private void applyBackground(UI currentUi, Resource resource) {
        AbstractOrderedLayout vaadinVBox = mainVBox.unwrap(AbstractOrderedLayout.class);
        AbstractOrderedLayout vaadinDashboard = mainDashboard.unwrap(AbstractOrderedLayout.class);

        String resourceUrl = buildBackgroundUrl(currentUi, resource);

        Page page = Page.getCurrent();
        if (page == null) {
            throw new IllegalStateException("Vaadin Page недоступна");
        }

        currentBackgroundStyleName = "hrm-bg-" + UUID.randomUUID().toString().replace("-", "");

        vaadinVBox.addStyleName(currentBackgroundStyleName);
        vaadinDashboard.addStyleName("hrm-dashboard-transparent");

        String css = "." + currentBackgroundStyleName + " {"
                + "background-image: url('" + escapeCssUrl(resourceUrl) + "') !important;"
                + "background-position: center center !important;"
                + "background-repeat: no-repeat !important;"
                + "background-size: 100% 100% !important;"
                + "}"
                + ".hrm-dashboard-transparent {"
                + "background: transparent !important;"
                + "}";

        page.getStyles().add(css);

        htmlAttributes.setDomAttribute(mainVBox, "data-hrm-main-background", "applied");
        htmlAttributes.setDomAttribute(mainVBox, "data-hrm-main-controller", HrmMainScreen.class.getSimpleName());
        lastAppliedResourceUrl = resourceUrl;
    }

    /**
     * Регистрирует пользовательский StreamResource через нулевого размера Image,
     * который остаётся владельцем ресурса в Vaadin connector tree. Возвращаемый URL
     * преобразуется из внутренней схемы app://APP в HTTP-путь для CSS background-image.
     */
    private String registerBackgroundResource(AbstractOrderedLayout vaadinLayout,
                                              StreamResource resource) {
        if (backgroundResourceHolder != null
                && backgroundResourceHolder.getParent() instanceof com.vaadin.ui.ComponentContainer) {
            ((com.vaadin.ui.ComponentContainer) backgroundResourceHolder.getParent())
                    .removeComponent(backgroundResourceHolder);
        }

        backgroundResourceHolder = new Image(null, resource);
        backgroundResourceHolder.setWidth(0, Unit.PIXELS);
        backgroundResourceHolder.setHeight(0, Unit.PIXELS);
        backgroundResourceHolder.setVisible(false);
        vaadinLayout.addComponent(backgroundResourceHolder);

        String resourceUrl = ResourceReference.create(
                resource, backgroundResourceHolder, "src").getURL();
        if (resourceUrl != null && resourceUrl.startsWith("app://APP")) {
            resourceUrl = resourceUrl.replace("app://APP", "");
        }
        if (resourceUrl == null || resourceUrl.trim().isEmpty()) {
            throw new IllegalStateException("Vaadin не зарегистрировал ресурс фонового изображения");
        }
        return resourceUrl;
    }

    String getLastAppliedResourceUrl() {
        return lastAppliedResourceUrl;
    }

    private String escapeCssUrl(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("'", "\\'");
    }
}
