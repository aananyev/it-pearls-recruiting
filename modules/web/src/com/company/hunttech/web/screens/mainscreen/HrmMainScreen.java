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
 * Пользовательский фон — из FileStorage через StreamResource.
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

    public HrmMainScreen() {
        log.warn("### CHECKPOINT-0: HrmMainScreen constructor called ###");
    }

    @Subscribe
    public void onAfterShowBackground(AfterShowEvent event) {
        log.warn("### CHECKPOINT-1: AfterShowEvent fired ###");
        refreshBackground();
    }

    @EventListener
    public void onMainScreenBackgroundChanged(MainScreenBackgroundChangedEvent event) {
        log.warn("### CHECKPOINT-EVENT: MainScreenBackgroundChangedEvent received ###");
        refreshBackground();
    }

    private void refreshBackground() {
        log.warn("### CHECKPOINT-2: refreshBackground() called ###");
        try {
            UI currentUi = UI.getCurrent();
            log.warn("### CHECKPOINT-3: UI.getCurrent() = {} ###", currentUi);
            if (currentUi == null) {
                log.error("### CHECKPOINT-FAIL: UI.getCurrent() is NULL ###");
                throw new IllegalStateException("Vaadin UI недоступен");
            }
            log.warn("### CHECKPOINT-4: UI theme = {} ###", currentUi.getTheme());
            log.warn("### CHECKPOINT-5: user = {} ###", userSession.getUser());
            Resource resource = mainScreenBackgroundService.resolveForUser(
                    userSession.getUser(), currentUi.getTheme(), userSession);
            log.warn("### CHECKPOINT-6: resource = {} ###", resource);
            applyBackground(currentUi, resource);
            log.warn("### CHECKPOINT-7: applyBackground() returned ###");
        } catch (RuntimeException e) {
            log.error("### CHECKPOINT-FAIL: {} ###", e.getMessage(), e);
        }
    }

    /**
     * Строит CSS-совместимый URL для фонового изображения.
     * ThemeResource даёт theme://... — браузер не понимает, конвертируем в HTTP-путь.
     * StreamResource (пользовательский фон) использует ResourceReference.
     */
    private String buildBackgroundUrl(UI currentUi, Resource resource) {
        if (resource instanceof ThemeResource) {
            ThemeResource themeResource = (ThemeResource) resource;
            // ThemeResource для "backgrounds/8.jpg" → VAADIN/themes/halo/backgrounds/8.jpg
            String themeName = currentUi.getTheme();
            return "VAADIN/themes/" + themeName + "/" + themeResource.getResourceId();
        }

        // StreamResource (пользовательский фон или legacy SVG)
        AbstractOrderedLayout vaadinVBox = mainVBox.unwrap(AbstractOrderedLayout.class);
        return registerBackgroundResource(currentUi, vaadinVBox, resource);
    }

    private void applyBackground(UI currentUi, Resource resource) {
        AbstractOrderedLayout vaadinVBox = mainVBox.unwrap(AbstractOrderedLayout.class);
        AbstractOrderedLayout vaadinDashboard = mainDashboard.unwrap(AbstractOrderedLayout.class);

        String resourceUrl = buildBackgroundUrl(currentUi, resource);

        Page page = Page.getCurrent();
        if (page == null) {
            throw new IllegalStateException("Vaadin Page недоступна");
        }

        if (currentBackgroundStyleName != null) {
            // Старый стиль остаётся в DOM, новый класс переопределяет
        }
        currentBackgroundStyleName = "hrm-bg-" + UUID.randomUUID().toString().replace("-", "");

        vaadinVBox.addStyleName(currentBackgroundStyleName);
        vaadinDashboard.addStyleName("hrm-dashboard-transparent");

        String css = "." + currentBackgroundStyleName + " {"
                + "background-image: url('" + escapeCssUrl(resourceUrl) + "') !important;"
                + "background-position: center center !important;"
                + "background-repeat: no-repeat !important;"
                + "background-size: cover !important;"
                + "}"
                + ".hrm-dashboard-transparent {"
                + "background: transparent !important;"
                + "}";
        log.warn("### CHECKPOINT-15: css = {} ###", css);

        page.getStyles().add(css);
        log.warn("### CHECKPOINT-16: page.getStyles().add() returned ###");

        htmlAttributes.setDomAttribute(mainVBox, "data-hrm-main-background", "applied");
        htmlAttributes.setDomAttribute(mainVBox, "data-hrm-main-controller", HrmMainScreen.class.getSimpleName());
        lastAppliedResourceUrl = resourceUrl;
        log.warn("### CHECKPOINT-17: ALL DONE, background applied ###");
    }

    private String registerBackgroundResource(UI currentUi,
                                              AbstractOrderedLayout vaadinLayout,
                                              Resource resource) {
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
        // Vaadin возвращает app://APP/... для StreamResource, браузер не резолвит в CSS
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
