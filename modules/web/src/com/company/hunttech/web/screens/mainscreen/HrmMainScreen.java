package com.company.hunttech.web.screens.mainscreen;

import com.company.hunttech.entity.ExtUser;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.HtmlAttributes;
import com.haulmont.cuba.gui.components.VBoxLayout;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.security.global.UserSession;
import com.vaadin.server.Resource;
import com.vaadin.server.ResourceReference;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

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
    private Component mainDashboard;
    @Inject
    private HtmlAttributes htmlAttributes;

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

        /*
         * HtmlAttributes передаёт CSS через штатный connector CUBA и назначает inline-style
         * фактическим DOM-элементам. Это устраняет гонку динамического Page CSS, при которой
         * правило присутствовало на странице, но не применялось после рендеринга UI.
         */
        applyInlineBackground(mainVBox, resourceUrl);
        applyInlineBackground(mainDashboard, resourceUrl);
        htmlAttributes.setDomAttribute(mainVBox, "data-hrm-main-background", "applied");
        htmlAttributes.setDomAttribute(mainDashboard, "data-hrm-main-background", "applied");

        log.debug("Main screen background applied inline: theme={}, resourceUrl={}, layoutClass={}, dashboardClass={}",
                currentUi.getTheme(), resourceUrl,
                vaadinLayout.getClass().getName(), vaadinDashboard.getClass().getName());
    }

    private void applyInlineBackground(Component component, String resourceUrl) {
        String backgroundImage = "url('" + escapeCssUrl(resourceUrl) + "')";
        htmlAttributes.setCssProperty(component, "background-image", backgroundImage);
        htmlAttributes.setCssProperty(component, "background-position", "center center");
        htmlAttributes.setCssProperty(component, "background-repeat", "no-repeat");
        htmlAttributes.setCssProperty(component, "background-size", "cover");
        htmlAttributes.setCssProperty(component, "background-color", "transparent");
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
