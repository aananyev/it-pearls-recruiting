package com.company.hunttech.web.screens.mainscreen;

import com.company.hunttech.entity.ExtUser;
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

import javax.inject.Inject;
import java.util.UUID;

/**
 * Добавляет к действующему ExtMainScreen только персональный фон рабочей области.
 * Все уведомления, dashboard, favicon и прочая бизнес-логика наследуются без изменений.
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
    private com.haulmont.cuba.gui.components.Component mainDashboard;

    /**
     * Нулевого размера Image владеет динамическим ресурсом в Vaadin connector tree.
     * Компонент остаётся подключённым к UI, но не занимает место в mainVBox.
     */
    private Image backgroundResourceHolder;

    /**
     * Применяет фон после присоединения компонентов к текущему UI. BeforeShow
     * выполняется слишком рано для ResourceReference: connector ещё может не иметь UI.
     */
    @Subscribe
    public void onAfterShowBackground(AfterShowEvent event) {
        try {
            UI currentUi = UI.getCurrent();
            if (currentUi == null) {
                throw new IllegalStateException("Vaadin UI недоступен после открытия главного экрана");
            }
            String themeName = currentUi.getTheme();
            Resource resource = mainScreenBackgroundService.resolveForUser(
                    (ExtUser) userSession.getUser(), themeName);
            applyBackground(currentUi, resource);
        } catch (RuntimeException e) {
            // Ошибка декоративного слоя не должна блокировать открытие главного экрана.
            log.warn("Cannot apply main screen background: {}", e.getMessage(), e);
        }
    }

    private void applyBackground(UI currentUi, Resource resource) {
        AbstractOrderedLayout vaadinLayout = mainVBox.unwrap(AbstractOrderedLayout.class);
        com.vaadin.ui.Component vaadinDashboard = mainDashboard.unwrap(com.vaadin.ui.Component.class);
        ensureAttachedToCurrentUi(currentUi, vaadinLayout, "mainVBox");
        ensureAttachedToCurrentUi(currentUi, vaadinDashboard, "mainDashboard");

        String resourceUrl = registerBackgroundResource(currentUi, vaadinLayout, resource);
        String sessionStyle = "hrm-main-screen-background-"
                + UUID.randomUUID().toString().replace("-", "");

        // Один локальный класс назначается рабочему контейнеру и dashboard:
        // внутренний dashboard не должен перекрывать фон родительского mainVBox.
        vaadinLayout.addStyleName("hrm-main-screen-background");
        vaadinLayout.addStyleName(sessionStyle);
        vaadinDashboard.addStyleName("hrm-main-screen-background");
        vaadinDashboard.addStyleName(sessionStyle);

        Page page = currentUi.getPage();
        if (page == null) {
            throw new IllegalStateException("Vaadin Page недоступна после открытия главного экрана");
        }
        page.getStyles().add(
                "." + sessionStyle + " {"
                        + "background-image:url('" + escapeCssUrl(resourceUrl) + "') !important;"
                        + "background-position:center center !important;"
                        + "background-repeat:no-repeat !important;"
                        + "background-size:cover !important;"
                        + "}");
        log.debug("Main screen background applied: theme={}, resourceUrl={}, layoutClass={}, dashboardClass={}",
                currentUi.getTheme(), resourceUrl,
                vaadinLayout.getClass().getName(), vaadinDashboard.getClass().getName());
    }

    /**
     * Регистрирует StreamResource через штатный ресурсный ключ Image `src`.
     * Компонент добавляется в layout до получения URL, поэтому connector уже
     * связан с UI и способен обслужить системный SVG или пользовательский файл.
     */
    private String registerBackgroundResource(UI currentUi,
                                              AbstractOrderedLayout vaadinLayout,
                                              Resource resource) {
        if (backgroundResourceHolder != null && backgroundResourceHolder.getParent() == vaadinLayout) {
            vaadinLayout.removeComponent(backgroundResourceHolder);
        }

        backgroundResourceHolder = new Image(null, resource);
        backgroundResourceHolder.setWidth(0, Unit.PIXELS);
        backgroundResourceHolder.setHeight(0, Unit.PIXELS);
        vaadinLayout.addComponent(backgroundResourceHolder);
        ensureAttachedToCurrentUi(currentUi, backgroundResourceHolder, "backgroundResourceHolder");

        String resourceUrl = ResourceReference.create(
                resource, backgroundResourceHolder, "src").getURL();
        if (resourceUrl == null || resourceUrl.trim().isEmpty()) {
            throw new IllegalStateException("Vaadin не зарегистрировал ресурс фонового изображения");
        }
        return resourceUrl;
    }

    private void ensureAttachedToCurrentUi(UI currentUi,
                                           com.vaadin.ui.Component component,
                                           String componentName) {
        if (component == null || component.getUI() != currentUi) {
            throw new IllegalStateException(componentName + " не присоединён к текущему Vaadin UI");
        }
    }

    private String escapeCssUrl(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("'", "\\'");
    }
}
