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
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;

import javax.inject.Inject;
import java.util.UUID;

/**
 * Чекпоинт-версия: каждый шаг логируется для диагностики отсутствия фона.
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

    // --- ЧЕКПОИНТ 0: конструктор ---
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

    private void applyBackground(UI currentUi, Resource resource) {
        log.warn("### CHECKPOINT-8: applyBackground() called ###");
        AbstractOrderedLayout vaadinVBox = mainVBox.unwrap(AbstractOrderedLayout.class);
        log.warn("### CHECKPOINT-9: vaadinVBox = {} ###", vaadinVBox);

        String resourceUrl = registerBackgroundResource(currentUi, vaadinVBox, resource);
        log.warn("### CHECKPOINT-10: resourceUrl = {} ###", resourceUrl);

        Page page = Page.getCurrent();
        log.warn("### CHECKPOINT-11: Page.getCurrent() = {} ###", page);
        if (page == null) {
            log.error("### CHECKPOINT-FAIL: Page.getCurrent() is NULL ###");
            throw new IllegalStateException("Vaadin Page недоступна");
        }

        AbstractOrderedLayout vaadinDashboard = mainDashboard.unwrap(AbstractOrderedLayout.class);
        log.warn("### CHECKPOINT-12: vaadinDashboard = {} ###", vaadinDashboard);

        currentBackgroundStyleName = "hrm-bg-" + UUID.randomUUID().toString().replace("-", "");
        log.warn("### CHECKPOINT-13: styleName = {} ###", currentBackgroundStyleName);

        vaadinVBox.addStyleName(currentBackgroundStyleName);
        vaadinDashboard.addStyleName("hrm-dashboard-transparent");
        log.warn("### CHECKPOINT-14: styleNames added ###");

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
        log.warn("### CHECKPOINT-R1: registerBackgroundResource() ###");
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
        log.warn("### CHECKPOINT-R2: Image added to layout ###");

        String resourceUrl = ResourceReference.create(
                resource, backgroundResourceHolder, "src").getURL();
        // Vaadin возвращает app://APP/..., браузер не резолвит в CSS.
        // Преобразуем в HTTP-путь относительно контекста приложения.
        if (resourceUrl != null && resourceUrl.startsWith("app://APP")) {
            resourceUrl = resourceUrl.replace("app://APP", "");
        }
        log.warn("### CHECKPOINT-R3: ResourceReference URL = {} ###", resourceUrl);
        if (resourceUrl == null || resourceUrl.trim().isEmpty()) {
            log.error("### CHECKPOINT-FAIL: ResourceReference returned empty URL ###");
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
