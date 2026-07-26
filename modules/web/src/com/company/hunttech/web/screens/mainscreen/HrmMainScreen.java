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
     * Скрытый Image владеет динамическим ресурсом в Vaadin connector tree.
     * Без такого владельца URL StreamResource формируется, но запрос браузера
     * не обслуживается connector-ом и фон остаётся пустым.
     */
    private Image backgroundResourceHolder;

    /**
     * Применяет фон после присоединения компонентов к текущему UI. BeforeShow
     * выполняется слишком рано для ResourceReference: connector ещё может не иметь UI.
     */
    @Subscribe
    public void onAfterShowBackground(AfterShowEvent event) {
        try {
            String themeName = UI.getCurrent() == null ? null : UI.getCurrent().getTheme();
            Resource resource = mainScreenBackgroundService.resolveForUser(
                    (ExtUser) userSession.getUser(), themeName);
            applyBackground(resource);
        } catch (RuntimeException e) {
            // Ошибка декоративного слоя не должна блокировать открытие главного экрана.
            log.warn("Cannot apply main screen background: {}", e.getMessage(), e);
        }
    }

    private void applyBackground(Resource resource) {
        AbstractOrderedLayout vaadinLayout = mainVBox.unwrap(AbstractOrderedLayout.class);
        com.vaadin.ui.Component vaadinDashboard = mainDashboard.unwrap(com.vaadin.ui.Component.class);
        String resourceUrl = registerBackgroundResource(vaadinLayout, resource);
        String sessionStyle = "hrm-main-screen-background-"
                + UUID.randomUUID().toString().replace("-", "");

        // Один локальный класс назначается рабочему контейнеру и dashboard:
        // внутренний dashboard не должен перекрывать фон родительского mainVBox.
        vaadinLayout.addStyleName("hrm-main-screen-background");
        vaadinLayout.addStyleName(sessionStyle);
        vaadinDashboard.addStyleName("hrm-main-screen-background");
        vaadinDashboard.addStyleName(sessionStyle);

        Page page = Page.getCurrent();
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
    }

    /**
     * Регистрирует StreamResource через штатный ресурсный ключ Image `src`.
     * Компонент добавляется в layout до получения URL, поэтому connector уже
     * связан с UI и способен обслужить системный SVG или пользовательский файл.
     */
    private String registerBackgroundResource(AbstractOrderedLayout vaadinLayout, Resource resource) {
        backgroundResourceHolder = new Image(null, resource);
        backgroundResourceHolder.setVisible(false);
        vaadinLayout.addComponent(backgroundResourceHolder);

        String resourceUrl = ResourceReference.create(
                resource, backgroundResourceHolder, "src").getURL();
        if (resourceUrl == null || resourceUrl.trim().isEmpty()) {
            throw new IllegalStateException("Vaadin не зарегистрировал ресурс фонового изображения");
        }
        return resourceUrl;
    }

    private String escapeCssUrl(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("'", "\\'");
    }
}
