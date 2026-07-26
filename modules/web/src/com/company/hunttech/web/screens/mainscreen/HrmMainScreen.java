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
 * Применяет фоновое изображение главного экрана через CSS-инъекцию в Page.
 * Не зависит от Vaadin layout-менеджера: фон накладывается на mainVBox
 * средствами браузерного CSS, не конфликтуя с Dashboard.
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
        log.info("HrmMainScreen.onAfterShowBackground called, applying background");
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
                throw new IllegalStateException("Vaadin UI недоступен для обновления фона главного экрана");
            }
            Resource resource = mainScreenBackgroundService.resolveForUser(
                    userSession.getUser(), currentUi.getTheme(), userSession);
            applyBackground(currentUi, resource);
        } catch (RuntimeException e) {
            log.warn("Cannot apply main screen background: {}", e.getMessage(), e);
        }
    }

    private void applyBackground(UI currentUi, Resource resource) {
        AbstractOrderedLayout vaadinVBox = mainVBox.unwrap(AbstractOrderedLayout.class);
        AbstractOrderedLayout vaadinDashboard = mainDashboard.unwrap(AbstractOrderedLayout.class);

        String resourceUrl = registerBackgroundResource(currentUi, vaadinVBox, resource);

        // Удаляем предыдущий динамический стиль фона, если есть
        Page page = Page.getCurrent();
        if (page == null) {
            throw new IllegalStateException("Vaadin Page недоступна");
        }

        // Генерируем уникальное имя класса для этого сеанса
        if (currentBackgroundStyleName != null) {
            // Старый стиль остаётся в DOM, но новый класс переопределяет фон
        }
        currentBackgroundStyleName = "hrm-bg-" + UUID.randomUUID().toString().replace("-", "");

        // Добавляем класс к mainVBox и dashboard
        vaadinVBox.addStyleName(currentBackgroundStyleName);
        vaadinDashboard.addStyleName("hrm-dashboard-transparent");

        // Инжектируем CSS в head страницы
        String css = "." + currentBackgroundStyleName + " {"
                + "background-image: url('" + escapeCssUrl(resourceUrl) + "') !important;"
                + "background-position: center center !important;"
                + "background-repeat: no-repeat !important;"
                + "background-size: cover !important;"
                + "}"
                + ".hrm-dashboard-transparent {"
                + "background: transparent !important;"
                + "}";

        page.getStyles().add(css);

        // Маркеры для диагностики
        htmlAttributes.setDomAttribute(mainVBox, "data-hrm-main-background", "applied");
        htmlAttributes.setDomAttribute(mainVBox, "data-hrm-main-controller", HrmMainScreen.class.getSimpleName());
        lastAppliedResourceUrl = resourceUrl;

        log.info("Main screen background applied: theme={}, class={}",
                currentUi.getTheme(), currentBackgroundStyleName);
    }

    /**
     * Регистрирует StreamResource через нулевого размера Image.
     * Компонент не влияет на layout, но обеспечивает обслуживание
     * динамического ресурса Vaadin connector-ом.
     */
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
