package com.company.hunttech.web.screens.aifunctionconfiguration;

import com.company.hunttech.entity.ai.AiCapability;
import com.company.hunttech.entity.ai.AiExecutionPolicy;
import com.company.hunttech.entity.ai.AiFallbackPolicy;
import com.company.hunttech.entity.ai.AiFunctionConfiguration;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.LookupPickerField;
import com.haulmont.cuba.gui.components.TextArea;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.screen.EditedEntityContainer;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.StandardEditor;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;

@UiController("hunttech_AiFunctionConfiguration.edit")
@UiDescriptor("ai-function-configuration-edit.xml")
@EditedEntityContainer("aiFunctionDc")
@LoadDataBeforeShow
public class AiFunctionConfigurationEdit extends StandardEditor<AiFunctionConfiguration> {
    @Inject
    private TextField<String> codeField;
    @Inject
    private LookupPickerField<?> adminConfigurationField;
    @Inject
    private TextArea<String> systemPromptField;
    @Inject
    private TextField<Double> temperatureField;
    @Inject
    private Button mainNav;
    @Inject
    private Button routingNav;
    @Inject
    private Button promptNav;
    @Inject
    private Button modelNav;

    @Subscribe
    public void onInitEntity(InitEntityEvent<AiFunctionConfiguration> event) {
        AiFunctionConfiguration entity = event.getEntity();
        entity.setCapability(AiCapability.TEXT_GENERATION);
        entity.setExecutionPolicy(AiExecutionPolicy.USER_OVERRIDE_ALLOWED);
        entity.setFallbackPolicy(AiFallbackPolicy.FALLBACK_TO_ADMIN);
        entity.setTemperature(0.7);
        entity.setAllowModelOverride(false);
        entity.setActive(true);
        entity.setConfigurationVersion(1);
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        // Стабильный function code является API-контрактом потребителей и после создания не редактируется.
        codeField.setEditable(PersistenceHelper.isNew(getEditedEntity()));
    }

    /** Presentation-only навигация по карточке AI-функции; entity и loaders не меняются. */
    public void focusMainSection() {
        codeField.focus();
        setActiveNavigation(mainNav);
    }

    /** Presentation-only навигация к правилам выбора credentials. */
    public void focusRoutingSection() {
        adminConfigurationField.focus();
        setActiveNavigation(routingNav);
    }

    /** Presentation-only навигация к prompt-контракту функции. */
    public void focusPromptSection() {
        systemPromptField.focus();
        setActiveNavigation(promptNav);
    }

    /** Presentation-only навигация к параметрам модели. */
    public void focusModelSection() {
        temperatureField.focus();
        setActiveNavigation(modelNav);
    }

    private void setActiveNavigation(Button activeButton) {
        Button[] buttons = {mainNav, routingNav, promptNav, modelNav};
        for (Button button : buttons) {
            if (button == activeButton) {
                button.addStyleName("label-nav-item-active");
            } else {
                button.removeStyleName("label-nav-item-active");
            }
        }
    }
}
