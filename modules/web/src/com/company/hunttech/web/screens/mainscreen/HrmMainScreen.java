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

    /**
     * Выбирает фон заново при создании главного экрана после входа. Если сервис
     * возвращает пользовательский файл, случайный theme-каталог не используется.
     */
    @Subscribe
    public void onBeforeShowBackground(BeforeShowEvent event) {
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
        String resourceUrl = ResourceReference.create(resource, vaadinLayout, "hrmMainBackground").getURL();
        String sessionStyle = "hrm-main-screen-background-"
                + UUID.randomUUID().toString().replace("-", "");

        vaadinLayout.addStyleName("hrm-main-screen-background");
        vaadinLayout.addStyleName(sessionStyle);

        Page.getCurrent().getStyles().add(
                "." + sessionStyle + " {"
                        + "background-image:url('" + escapeCssUrl(resourceUrl) + "') !important;"
                        + "background-position:center center !important;"
                        + "background-repeat:no-repeat !important;"
                        + "background-size:cover !important;"
                        + "}");
    }

    private String escapeCssUrl(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("'", "\\'");
    }
}
