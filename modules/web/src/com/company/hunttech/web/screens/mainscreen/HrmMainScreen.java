package com.company.hunttech.web.screens.mainscreen;

import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.CssLayout;
import com.haulmont.cuba.gui.components.HtmlAttributes;
import com.haulmont.cuba.gui.components.VBoxLayout;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.security.global.UserSession;
import com.vaadin.server.Resource;
import com.vaadin.server.ResourceReference;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.Image;
import com.vaadin.ui.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;

import javax.inject.Inject;

/**
 * Добавляет к действующему ExtMainScreen изолированный слой фонового изображения.
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
    private CssLayout mainScreenBackgroundLayer;
    @Inject
    private Component mainDashboard;
    @Inject
    private HtmlAttributes htmlAttributes;

    /**
     * Нулевого размера Image владеет динамическим ресурсом в Vaadin connector tree.
     * Компонент остаётся внутри background layer, но не участвует в компоновке dashboard.
     */
    private Image backgroundResourceHolder;
    private String lastAppliedResourceUrl;

    /**
     * Применяет фон после присоединения компонентов к текущему UI. BeforeShow
     * выполняется слишком рано для ResourceReference: connector ещё может не иметь UI.
     */
    @Subscribe
    public void onAfterShowBackground(AfterShowEvent event) {
        log.info("HrmMainScreen.onAfterShowBackground called, applying background");
        refreshBackground();
    }

    /**
     * UiEvent доставляется только в текущую браузерную вкладку после успешного сохранения
     * SettingsWindow и позволяет обновить фон без повторного входа.
     */
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
            // Ошибка декоративного слоя не должна блокировать открытие главного экрана.
            log.warn("Cannot apply main screen background: {}", e.getMessage(), e);
        }
    }

    private void applyBackground(UI currentUi, Resource resource) {
        com.vaadin.ui.CssLayout vaadinLayer =
                mainScreenBackgroundLayer.unwrap(com.vaadin.ui.CssLayout.class);
        com.vaadin.ui.Component vaadinDashboard =
                mainDashboard.unwrap(com.vaadin.ui.Component.class);

        ensureAttachedToCurrentUi(currentUi, vaadinLayer, "mainScreenBackgroundLayer");
        ensureAttachedToCurrentUi(currentUi, vaadinDashboard, "mainDashboard");

        String resourceUrl = registerBackgroundResource(currentUi, vaadinLayer, resource);
        configureLayerLayout();
        applyInlineBackground(mainScreenBackgroundLayer, resourceUrl);

        htmlAttributes.setDomAttribute(mainScreenBackgroundLayer,
                "data-hrm-main-background", "applied");
        htmlAttributes.setDomAttribute(mainScreenBackgroundLayer,
                "data-hrm-main-background-resource", resourceUrl);
        htmlAttributes.setDomAttribute(mainVBox,
                "data-hrm-main-controller", HrmMainScreen.class.getSimpleName());
        lastAppliedResourceUrl = resourceUrl;

        log.debug("Main screen background applied: theme={}, resourceUrl={}, layerClass={}, dashboardClass={}",
                currentUi.getTheme(), resourceUrl,
                vaadinLayer.getClass().getName(), vaadinDashboard.getClass().getName());
    }

    /**
     * Фон принадлежит только выделенному слою. Dashboard остаётся прозрачной рабочей
     * поверхностью поверх него; mainVBox задаёт локальный positioning context.
     */
    private void configureLayerLayout() {
        htmlAttributes.setCssProperty(mainVBox, "position", "relative");
        htmlAttributes.setCssProperty(mainVBox, "overflow", "hidden");

        htmlAttributes.setCssProperty(mainScreenBackgroundLayer, "position", "absolute");
        htmlAttributes.setCssProperty(mainScreenBackgroundLayer, "top", "0");
        htmlAttributes.setCssProperty(mainScreenBackgroundLayer, "right", "0");
        htmlAttributes.setCssProperty(mainScreenBackgroundLayer, "bottom", "0");
        htmlAttributes.setCssProperty(mainScreenBackgroundLayer, "left", "0");
        htmlAttributes.setCssProperty(mainScreenBackgroundLayer, "width", "100%");
        htmlAttributes.setCssProperty(mainScreenBackgroundLayer, "height", "100%");
        htmlAttributes.setCssProperty(mainScreenBackgroundLayer, "z-index", "0");
        htmlAttributes.setCssProperty(mainScreenBackgroundLayer, "pointer-events", "none");

        htmlAttributes.setCssProperty(mainDashboard, "position", "relative");
        htmlAttributes.setCssProperty(mainDashboard, "z-index", "1");
        htmlAttributes.setCssProperty(mainDashboard, "background-color", "transparent");
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
     * Компонент добавляется в layer до получения URL, поэтому connector уже
     * связан с UI и способен обслужить системный SVG или пользовательский файл.
     */
    private String registerBackgroundResource(UI currentUi,
                                              ComponentContainer vaadinLayer,
                                              Resource resource) {
        if (backgroundResourceHolder != null
                && backgroundResourceHolder.getParent() instanceof ComponentContainer) {
            ((ComponentContainer) backgroundResourceHolder.getParent())
                    .removeComponent(backgroundResourceHolder);
        }

        backgroundResourceHolder = new Image(null, resource);
        backgroundResourceHolder.setWidth(0, Unit.PIXELS);
        backgroundResourceHolder.setHeight(0, Unit.PIXELS);
        vaadinLayer.addComponent(backgroundResourceHolder);
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

    String getLastAppliedResourceUrl() {
        return lastAppliedResourceUrl;
    }

    private String escapeCssUrl(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("'", "\\'");
    }
}
