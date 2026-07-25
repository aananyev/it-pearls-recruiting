package com.company.hunttech.web.screens.extsettingswindow;

import com.haulmont.cuba.gui.components.CheckBox;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.HBoxLayout;
import com.haulmont.cuba.gui.components.HasOrientation;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.LookupField;
import com.haulmont.cuba.gui.components.OptionsGroup;

import javax.inject.Inject;
import java.util.Map;

/**
 * Выравнивает штатные компоненты вкладки «Интерфейс» без изменения их значений,
 * привязок и обработчиков базового SettingsWindow.
 */
public class ExtSettingsWindowInterfaceLayout extends ExtSettingsWindowEmailNavigation {

    private static final String INTERFACE_LABEL_WIDTH = "190px";
    private static final String INTERFACE_CONTROL_WIDTH = "100%";
    private static final String AUTO_TIME_ZONE_WIDTH = "96px";

    @Inject
    private Label mainWindowLabel;
    @Inject
    private Label visualThemeLabel;
    @Inject
    private Label languageLabel;
    @Inject
    private Label timeZoneLabel;
    @Inject
    private Label defaultScreenLabel;

    @Inject
    private OptionsGroup modeOptions;
    @Inject
    private LookupField appThemeField;
    @Inject
    private LookupField appLangField;
    @Inject
    private HBoxLayout timeZoneBox;
    @Inject
    private LookupField timeZoneLookup;
    @Inject
    private CheckBox timeZoneAutoField;
    @Inject
    private LookupField defaultScreenField;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);
        alignInterfaceSettingsForm();
    }

    /**
     * Формирует устойчивую двухколоночную сетку: подписи не переносятся на несколько
     * строк, элементы управления используют общую ширину, а checkbox автоматического
     * часового пояса не отбирает пространство у lookup-поля.
     */
    private void alignInterfaceSettingsForm() {
        alignInterfaceLabel(mainWindowLabel);
        alignInterfaceLabel(visualThemeLabel);
        alignInterfaceLabel(languageLabel);
        alignInterfaceLabel(timeZoneLabel);
        alignInterfaceLabel(defaultScreenLabel);

        modeOptions.setOrientation(HasOrientation.Orientation.HORIZONTAL);
        modeOptions.setWidth(INTERFACE_CONTROL_WIDTH);
        modeOptions.setAlignment(Component.Alignment.MIDDLE_LEFT);

        appThemeField.setWidth(INTERFACE_CONTROL_WIDTH);
        appLangField.setWidth(INTERFACE_CONTROL_WIDTH);
        defaultScreenField.setWidth(INTERFACE_CONTROL_WIDTH);

        timeZoneBox.resetExpanded();
        timeZoneBox.expand(timeZoneLookup);
        timeZoneBox.setWidth(INTERFACE_CONTROL_WIDTH);
        timeZoneLookup.setWidth(INTERFACE_CONTROL_WIDTH);
        timeZoneLookup.setAlignment(Component.Alignment.MIDDLE_LEFT);
        timeZoneAutoField.setWidth(AUTO_TIME_ZONE_WIDTH);
        timeZoneAutoField.setAlignment(Component.Alignment.MIDDLE_LEFT);
    }

    private void alignInterfaceLabel(Label label) {
        label.setWidth(INTERFACE_LABEL_WIDTH);
        label.setAlignment(Component.Alignment.MIDDLE_LEFT);
    }
}
