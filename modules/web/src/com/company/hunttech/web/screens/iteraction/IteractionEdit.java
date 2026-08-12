package com.company.hunttech.web.screens.iteraction;

import com.company.hunttech.core.EmailGenerationService;
import com.company.hunttech.entity.EmployeeWorkStatus;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.core.global.ViewBuilder;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.Iteraction;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@UiController("hunttech_Iteraction.edit")
@UiDescriptor("iteraction-edit.xml")
@EditedEntityContainer("iteractionDc")
@LoadDataBeforeShow
public class IteractionEdit extends StandardEditor<Iteraction> {
    @Inject
    private CheckBox checkBoxCalendar;
    @Inject
    private TextField<String> textFieldCalendarItemStyle;
    @Inject
    private Image embeddedPict;
    @Inject
    private CheckBox checkBoxCallDialog;
    @Inject
    private TextField<String> textFieldCallButtonText;
    @Inject
    private TextField<String> textFieldCallForm;
    @Inject
    private TextField<String> iteractionFieldPic;
    @Inject
    private RadioButtonGroup<Integer> radioButtonAddType;
    @Inject
    private CheckBox checkBoxFlag;
    @Inject
    private TextField textFieldCaption;
    @Inject
    private Label<String> labelItercationName;
    @Inject
    private Label<String> labelWarning;
    @Inject
    private Messages messages;
    @Inject
    private RadioButtonGroup radioButtonTypeNotifications;
    @Inject
    private TextField<String> lookupFieldEmails;
    @Inject
    private CheckBox checkBoxSetDefaultDateTime;
    @Inject
    private RadioButtonGroup typeTraceRadioButtons;
    @Inject
    private TwinColumn checkTraceTwinColumn;
    @Inject
    private CheckBox neetToSendEmailCheckBox;
    @Inject
    private RichTextArea textEmailToSendRichTextArea;
    @Inject
    private EmailGenerationService emailGenerationService;
    @Inject
    private RichTextArea commentKeysRichTextArea;
    @Inject
    private RadioButtonGroup notificationPeriodRadioButton;
    @Inject
    private TextField<Integer> dayBeforeAfterTextField;
    @Inject
    private CheckBox signEndCaseCheckBox;

    @Inject
    private RadioButtonGroup<Integer> whenSendMessageRadioButton;

    private Map<String, Integer> mapAddType = new LinkedHashMap<>();
    private Map<String, Integer> mapTypeNotifications = new LinkedHashMap<>();
    private Map<String, Integer> mapCheckTrace = new LinkedHashMap<>();
    private Map<String, Integer> mapNotificationPeriod = new LinkedHashMap<>();
    private Map<String, Integer> mapWhenSendMessage = new LinkedHashMap<>();

    @Inject
    private HBoxLayout notificationSetupHBox;
    @Inject
    private CheckBox signOurInterviewAssignedCheckBox;
    @Inject
    private CheckBox signOurInterviewCheckBox;
    @Inject
    private CheckBox signClientInterviewCheckBox;
    @Inject
    private CheckBox signSentToClientCheckBox;
    @Inject
    private CheckBox statisticsCheckBox;
    @Inject
    private CheckBox signPriorityNews;
    @Inject
    private CheckBox signViewOnlyManagersCheckBox;

    @Inject
    private CollectionLoader<Iteraction> iteractionElementDl;

    @Inject
    private CollectionLoader<EmployeeWorkStatus> workStatusDl;

    @Inject
    private DataManager dataManager;

    private boolean iteractionElementsLoaded;
    private boolean textEmailToSendLoaded;
    private boolean workStatusLoaded;

    // Presentation-only sidebar-навигация по вкладкам (контракт §3.1/§3.4/§3.6):
    // пункты label-навигации переключают TabSheet, данные и lifecycle не затрагиваются.
    @Inject
    private TabSheet tabSheet;
    @Inject
    private VBoxLayout iteractionNavigation;
    @Inject
    private Button typeTabNav;
    @Inject
    private Button signsTabNav;
    @Inject
    private Button outstaffingNav;
    @Inject
    private Button iconsTabNav;
    @Inject
    private Button notificationsNav;
    @Inject
    private Button setupNav;
    @Inject
    private Button widgetsNav;
    @Inject
    private Button checkTraceNav;

    private static final String ACTIVE_NAV_STYLE = "label-nav-item-active";
    @Subscribe
    public void onBeforeCommitChanges(BeforeCommitChangesEvent event) {
        if (signEndCaseCheckBox.getValue() == null) {
            signEndCaseCheckBox.setValue(false);
        }

        if (signOurInterviewAssignedCheckBox.getValue() == null) {
            signOurInterviewAssignedCheckBox.setValue(false);
        }

        if (signOurInterviewCheckBox.getValue() == null) {
            signOurInterviewCheckBox.setValue(false);
        }

        if (signClientInterviewCheckBox.getValue() == null) {
            signClientInterviewCheckBox.setValue(false);
        }

        if (signSentToClientCheckBox.getValue() == null) {
            signSentToClientCheckBox.setValue(false);
        }

        if (statisticsCheckBox.getValue() == null) {
            statisticsCheckBox.setValue(false);
        }

        if (signPriorityNews.getValue() == null) {
            signPriorityNews.setValue(false);
        }

        if (signViewOnlyManagersCheckBox.getValue() == null) {
            signViewOnlyManagersCheckBox.setValue(false);
        }
    }

    @Subscribe("checkBoxCalendar")
    public void onCheckBoxCalendarValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        textFieldCalendarItemStyle.setRequired(checkBoxCalendar.getValue());
    }

    @Subscribe
    public void onInit(InitEvent event) {
        addRadioButtonAddType();
        addTypeNotifications();
        addNotificationPeriod();
        addNotificationWhenSend();
        addCheckTrace();
        initTabNavigation();
        // @LoadDataBeforeShow should load only the first-tab data; heavy tab dictionaries are loaded on tab activation.
        iteractionElementDl.addPreLoadListener(e -> {
            if (!iteractionElementsLoaded) {
                e.preventLoad();
            }
        });
        workStatusDl.addPreLoadListener(e -> {
            if (!workStatusLoaded) {
                e.preventLoad();
            }
        });
    }

    /**
     * Строит sidebar-навигацию по вкладкам: клик переключает TabSheet и помечает
     * активный пункт (presentation-only; loaders, commit и данные не затрагиваются).
     * Навигация видна на всех вкладках формы.
     */
    private void initTabNavigation() {
        typeTabNav.addClickListener(e -> switchToTab("tabType", typeTabNav));
        signsTabNav.addClickListener(e -> switchToTab("tabSigns", signsTabNav));
        outstaffingNav.addClickListener(e -> switchToTab("outstaffingTab", outstaffingNav));
        iconsTabNav.addClickListener(e -> switchToTab("tabIcons", iconsTabNav));
        notificationsNav.addClickListener(e -> switchToTab("tabNotifictions", notificationsNav));
        setupNav.addClickListener(e -> switchToTab("tabSetup", setupNav));
        widgetsNav.addClickListener(e -> switchToTab("setupWidgets", widgetsNav));
        checkTraceNav.addClickListener(e -> switchToTab("checkTrace", checkTraceNav));

        tabSheet.addSelectedTabChangeListener(event -> {
            TabSheet.Tab selected = event.getSelectedTab();
            if (selected != null && selected.getName() != null) {
                updateActiveNavigation(selected.getName());
            }
        });

        TabSheet.Tab initialTab = tabSheet.getSelectedTab();
        String initialName = initialTab != null && initialTab.getName() != null
                ? initialTab.getName() : "tabType";
        updateActiveNavigation(initialName);
    }

    private void switchToTab(String tabName, Button activeButton) {
        tabSheet.setSelectedTab(tabName);
        updateActiveNavigation(tabName);
    }

    private void updateActiveNavigation(String tabName) {
        for (Button navigationButton : navigationButtons()) {
            navigationButton.removeStyleName(ACTIVE_NAV_STYLE);
        }
        navigationButtonFor(tabName).addStyleName(ACTIVE_NAV_STYLE);
    }

    private List<Button> navigationButtons() {
        return Arrays.asList(typeTabNav, signsTabNav, outstaffingNav, iconsTabNav,
                notificationsNav, setupNav, widgetsNav, checkTraceNav);
    }

    private Button navigationButtonFor(String tabName) {
        switch (tabName) {
            case "tabSigns":
                return signsTabNav;
            case "outstaffingTab":
                return outstaffingNav;
            case "tabIcons":
                return iconsTabNav;
            case "tabNotifictions":
                return notificationsNav;
            case "tabSetup":
                return setupNav;
            case "setupWidgets":
                return widgetsNav;
            case "checkTrace":
                return checkTraceNav;
            default:
                return typeTabNav;
        }
    }

    /**
     * Правило 3.6 контракта ранее скрывало label-navigation на одноблочных вкладках;
     * по решению 2026-08-11 навигация видна на всех вкладках, метод удалён.
     */

    private void addNotificationWhenSend() {
        mapWhenSendMessage.put("На момент создания", 1);
        mapWhenSendMessage.put("В указанное время", 2);

        whenSendMessageRadioButton.setOptionsMap(mapWhenSendMessage);
    }

    private void addNotificationPeriod() {
        mapNotificationPeriod.put("Только текущий день", 0);
        mapNotificationPeriod.put("Текущая неделя с первого дня недели по по последний", 1);
        mapNotificationPeriod.put("Текущий неделя с даты итерации до конца недели", 2);
        mapNotificationPeriod.put("Текущий месяц с первого по последнее число месяца", 3);
        mapNotificationPeriod.put("Текущий месяц с даты итерации до конца месяца", 4);
        mapNotificationPeriod.put("Фиксированное число дней до и после", 5);

        notificationPeriodRadioButton.setOptionsMap(mapNotificationPeriod);
    }

    @Subscribe
    public void onAfterShow1(AfterShowEvent event) {
        dayBeforeAfterTextField.setEnabled(notificationPeriodRadioButton.getValue()
                == NOTIFICATION_PERIOD_BEFORE_AFTER_DAY);

        radioButtonsEnable();
    }

    private void radioButtonsEnable() {
        if (whenSendMessageRadioButton.getValue() == null) {
            notificationPeriodRadioButton.setEnabled(false);
            radioButtonTypeNotifications.setEnabled(false);
        } else {
            notificationPeriodRadioButton.setEnabled(true);
            radioButtonTypeNotifications.setEnabled(true);
        }
    }

    @Subscribe("whenSendMessageRadioButton")
    public void onWhenSendMessageRadioButtonValueChange(HasValue.ValueChangeEvent<Integer> event) {
        radioButtonsEnable();
    }

    private void addCheckTrace() {
        mapCheckTrace.put("Нет отслеживания", 1);
        mapCheckTrace.put("Отслеживание до появления события по октрытой позиции", 2);
        mapCheckTrace.put("Отслеживание по всем позициям", 3);

        typeTraceRadioButtons.setOptionsMap(mapCheckTrace);
    }

    static Integer NOTIFICATION_PERIOD_BEFORE_AFTER_DAY = 5;

    @Subscribe("notificationPeriodRadioButton")
    public void onNotificationPeriodRadioButtonValueChange(HasValue.ValueChangeEvent<Integer> event) {
        if (event.getValue() == NOTIFICATION_PERIOD_BEFORE_AFTER_DAY) {
            dayBeforeAfterTextField.setEnabled(true);
        } else {
            dayBeforeAfterTextField.setEnabled(false);
        }
    }


    @Subscribe("typeTraceRadioButtons")
    public void onTypeTraceRadioButtonsValueChange(HasValue.ValueChangeEvent<Integer> event) {
        switch (event.getValue()) {
            case 1:
                checkTraceTwinColumn.setEnabled(false);
                break;
            case 2:
                checkTraceTwinColumn.setEnabled(true);
                break;
            case 3:
                checkTraceTwinColumn.setEnabled(true);
                break;
            default:
                checkTraceTwinColumn.setEnabled(false);
                break;
        }
    }

    private void addTypeNotifications() {
        mapTypeNotifications.put("Нет", 1);
        mapTypeNotifications.put("Только менеджеру", 2);
        mapTypeNotifications.put("Подписчику вакансии", 3);
        mapTypeNotifications.put("Подписчику кандидата", 4);
        mapTypeNotifications.put("Определенным адресам (список)", 5);
        mapTypeNotifications.put("Всем", 6);

        radioButtonTypeNotifications.setOptionsMap(mapTypeNotifications);
    }

    private void addRadioButtonAddType() {
        mapAddType.put("Data", 1);
        mapAddType.put("String", 2);
        mapAddType.put("Integer", 3);

        radioButtonAddType.setOptionsMap(mapAddType);
    }

    @Subscribe("radioButtonAddType")
    public void onRadioButtonAddTypeValueChange(HasValue.ValueChangeEvent<Integer> event) {
        if (radioButtonAddType.getValue() == 1) {
            checkBoxSetDefaultDateTime.setEnabled(true);
        } else {
            checkBoxSetDefaultDateTime.setEnabled(false);
        }
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {

        if (!PersistenceHelper.isNew(getEditedEntity()))
            labelItercationName.setValue(getEditedEntity().getIterationName());

        labelWarning.setValue(messages.getMessage(getClass(), "msgForAdmin"));

        disablePicAndButton();

        setDisableElements();

        if (radioButtonTypeNotifications.getValue() != null)
            lookupFieldEmails.setEnabled(radioButtonTypeNotifications.getValue().equals(5));
        else
            lookupFieldEmails.setEnabled(false);

        needSendEmail();
    }

    private void needSendEmail() {
        neetToSendEmailCheckBox.addValueChangeListener(event -> {
            textEmailToSendRichTextArea.setEnabled(false);

            if (event.getValue() != null) {
                if (event.getValue()) {
                    textEmailToSendRichTextArea.setEnabled(true);
                } else {
                    textEmailToSendRichTextArea.setEnabled(false);
                }
            }
        });

        HashMap<String, String> emailKeys = emailGenerationService.generateKeys();
//        String retStr = "";
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, String> entry : emailKeys.entrySet()) {
            sb.append(entry.getKey())
                    .append(" - ")
                    .append(entry.getValue())
                    .append("<br>")
                    .toString();
//            retStr = retStr + entry.getKey() + " - " + entry.getValue() + "<br>";
        }

        commentKeysRichTextArea.setValue(sb.toString());

    }

    @Subscribe("radioButtonTypeNotifications")
    public void onRadioButtonTypeNotificationsValueChange(HasValue.ValueChangeEvent event) {
        lookupFieldEmails.setEnabled(radioButtonTypeNotifications.getValue().equals(5));
    }

    @Subscribe("checkBoxFlag")
    public void onCheckBoxFlagValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        checkBoxCallDialog.setValue(!checkBoxFlag.getValue());
        radioButtonAddType.setEnabled(checkBoxFlag.getValue());
        radioButtonAddType.setRequired(checkBoxFlag.getValue());
        textFieldCaption.setRequired(checkBoxFlag.getValue());

        setDisableElements();
    }

    protected void setDisableElements() {
        textFieldCaption.setEditable(checkBoxFlag.getValue());
        radioButtonAddType.setEditable(checkBoxFlag.getValue());
    }

    @Subscribe("checkBoxCallDialog")
    public void onCheckBoxCallDialogValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (checkBoxCallDialog.getValue())
            checkBoxFlag.setValue(false);

        disablePicAndButton();
    }

    private void disablePicAndButton() {
        if (!checkBoxCallDialog.getValue()) {
            textFieldCallButtonText.setEditable(false);
            textFieldCallForm.setEditable(false);
            // iteractionFieldPic.setEditable( false );
        } else {
            textFieldCallButtonText.setEditable(true);
            textFieldCallForm.setEditable(true);
            // iteractionFieldPic.setEditable( true );
        }
    }

    @Subscribe("iteractionFieldPic")
    public void onIteractionFieldPicValueChange(HasValue.ValueChangeEvent<String> event) {
        String iconURL = iteractionFieldPic.getValue();
        embeddedPict.setSource(ThemeResource.class).setPath(iconURL);
    }

    @Inject
    private LookupPickerField<Iteraction> iteractionTreeField;
    @Inject
    private TextField<String> iterationNameField;

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            getEditedEntity().setMandatoryIteraction(false);
        }
    }

    @Subscribe("iteractionCheckBoxMandatory")
    public void onIteractionCheckBoxMandatoryValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (getEditedEntity().getMandatoryIteraction()) {
            iteractionTreeField.setEditable(false);
            iterationNameField.setEditable(false);
        } else {
            iteractionTreeField.setEditable(true);
            iterationNameField.setEditable(true);
        }
    }

    @Subscribe("notificationNeedSendCheckBox")
    public void onNotificationNeedSendCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        notificationSetupHBox.setEnabled(event.getValue());
    }

    @Subscribe("tabSheet")
    public void onTabSheetSelectedTabChange(TabSheet.SelectedTabChangeEvent event) {
        if (event.getSelectedTab() == null) {
            return;
        }
        String tabName = event.getSelectedTab().getName();
        if ("checkTrace".equals(tabName) && !iteractionElementsLoaded) {
            // Child interaction options are needed only by the trace tab twin column.
            iteractionElementsLoaded = true;
            iteractionElementDl.load();
        }
        if ("tabSetup".equals(tabName) && !textEmailToSendLoaded) {
            loadTextEmailToSend();
            textEmailToSendLoaded = true;
        }
        if ("outstaffingTab".equals(tabName) && !workStatusLoaded) {
            // Work status dictionary is needed only by the outstaffing tab.
            workStatusLoaded = true;
            workStatusDl.load();
        }
    }

    private void loadTextEmailToSend() {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            return;
        }
        Iteraction reloaded = dataManager.reload(getEditedEntity(), ViewBuilder.of(Iteraction.class)
                .add("textEmailToSend")
                .build());
        getEditedEntity().setTextEmailToSend(reloaded.getTextEmailToSend());
    }
}
