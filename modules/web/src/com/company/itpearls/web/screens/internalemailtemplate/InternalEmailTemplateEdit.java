package com.company.itpearls.web.screens.internalemailtemplate;

import com.company.itpearls.core.EmailGenerationService;
import com.company.itpearls.entity.OpenPosition;
import com.company.itpearls.web.StandartPriorityVacancy;
import com.haulmont.cuba.core.global.Resources;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.InternalEmailTemplate;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;

@UiController("itpearls_InternalEmailTemplate.edit")
@UiDescriptor("internal-email-template-edit.xml")
@EditedEntityContainer("internalEmailTemplateDc")
@LoadDataBeforeShow
public class InternalEmailTemplateEdit extends StandardEditor<InternalEmailTemplate> {
    @Inject
    private EmailGenerationService emailGenerationService;
//    @Inject
//    private RichTextArea commentKeysRichTextArea;
    @Inject
    private LookupPickerField<User> templateAuthorField;
    @Inject
    private UserSession userSession;
    @Inject
    private CheckBox visibleOtherCheckBox;
    @Inject
    private OptionsList commentKeysOptionList;
    @Inject
    private RichTextArea templateTextField;
    @Inject
    private CheckBox onlyMySubscribeCheckBox;
    @Inject
    private CollectionLoader<OpenPosition> templateOpenPositionsDl;
    @Inject
    private Notifications notifications;
    @Inject
    private MessageBundle messageBundle;
    @Inject
    private CollectionContainer<OpenPosition> templateOpenPositionsDc;
    @Inject
    private LookupPickerField<OpenPosition> templateOpenPositionField;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private Resources resources;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        setEmailKeys();
        setOnlyMySubscribeCheckBox();
        templateAuthorField.setValue(userSession.getUser());
        initVacancyFiels();
    }


    @Install(to = "templateOpenPositionField", subject = "optionIconProvider")
    private String vacancyFielsOptionIconProvider(OpenPosition openPosition) {
        if (!openPosition.getOpenClose()) {
            return CubaIcon.PLUS_CIRCLE.source();
        } else {
            return CubaIcon.MINUS_CIRCLE.source();
        }
    }

    @Install(to = "templateOpenPositionField", subject = "optionStyleProvider")
    private String vacancyFielsOptionStyleProvider(OpenPosition openPosition) {
        if (!openPosition.getOpenClose()) {
            return "open-position-lookup-field-black";
        } else {
            return "open-position-lookup-field-gray";
        }
    }

    @Install(to = "templateOpenPositionField", subject = "optionImageProvider")
    private Resource vacancyFielsOptionImageProvider(OpenPosition openPosition) {
        String icon = "";

        switch (openPosition.getPriority()) {
            case -1:
                icon = StandartPriorityVacancy.DRAFT_ICON;
                break;
            case 0: //"Paused"
                icon = StandartPriorityVacancy.PAUSED_ICON;
                break;
            case 1: //"Low"
                icon = StandartPriorityVacancy.LOW_ICON;
                break;
            case 2: //"Normal"
                icon = StandartPriorityVacancy.NORMAL_ICON;
                break;
            case 3: //"High"
                icon = StandartPriorityVacancy.HIGH_ICON;
                break;
            case 4: //"Critical"
                icon = StandartPriorityVacancy.CRITICAL_ICON;
                break;
        }

        return (Resource) resources.getResource(icon);
    }

    private void setOnlyMySubscribeCheckBox() {
        onlyMySubscribeCheckBox.setValue(true);
        templateOpenPositionsDl.setParameter("subscriber", userSession.getUser());
        templateOpenPositionsDl.load();

        if (templateOpenPositionsDc.getItems().size() == 0) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption(messageBundle.getMessage("msgWarning"))
                    .withDescription(messageBundle.getMessage("msgNoSubscribeVacansies"))
                    .withPosition(Notifications.Position.BOTTOM_RIGHT)
                    .withHideDelayMs(10000)
                    .withType(Notifications.NotificationType.WARNING)
                    .show();
        }

        onlyMySubscribeCheckBox.addValueChangeListener(e -> {
            if (e.getValue()) {
                templateOpenPositionsDl.setParameter("subscriber", userSession.getUser());
                templateOpenPositionsDl.load();

                if (templateOpenPositionsDc.getItems().size() == 0) {
                    notifications.create(Notifications.NotificationType.WARNING)
                            .withCaption(messageBundle.getMessage("msgWarning"))
                            .withDescription(messageBundle.getMessage("msgNoSubscribeVacansies"))
                            .withPosition(Notifications.Position.BOTTOM_RIGHT)
                            .withHideDelayMs(10000)
                            .withType(Notifications.NotificationType.WARNING)
                            .show();
                }
            } else {
                templateOpenPositionsDl.removeParameter("subscriber");
                templateOpenPositionsDl.load();
            }

        });
    }


    private void initVacancyFiels() {
        templateOpenPositionField.setOptionImageProvider(this::vacancyFielsImageProvider);
    }

    protected Resource vacancyFielsImageProvider(OpenPosition openPosition) {
        Image retImage = uiComponents.create(Image.class);
        retImage.setScaleMode(Image.ScaleMode.SCALE_DOWN);
        retImage.setWidth("30px");

        if (openPosition.getProjectName() != null) {
            if (openPosition.getProjectName().getProjectLogo() != null) {
                return retImage.createResource(FileDescriptorResource.class)
                        .setFileDescriptor(
                                openPosition
                                        .getProjectName()
                                        .getProjectLogo());
            } else {
                return retImage.createResource(ThemeResource.class).setPath("icons/no-company.png");
            }
        } else {
            return retImage.createResource(ThemeResource.class).setPath("icons/no-company.png");
        }
    }


    private void setEmailKeys() {
        HashMap<String, String> emailKeys = emailGenerationService.generateKeys();
        String retStr = "";

        for (Map.Entry<String, String> entry : emailKeys.entrySet()) {
            retStr = retStr + entry.getKey() + " - " + entry.getValue() + "<br>";
        }

        commentKeysOptionList.setOptionsMap(emailKeys);
//        commentKeysRichTextArea.setValue(retStr);
    }

    @Subscribe("templateTextField")
    public void onTemplateTextFieldValueChange(HasValue.ValueChangeEvent<String> event) {
    }

    @Subscribe("commentKeysOptionList")
    public void onCommentKeysOptionListDoubleClick(OptionsList.DoubleClickEvent event) {

        String text = event.getItem() + " " +
                templateTextField.getValue();

        templateTextField.setValue(text);
    }

    @Subscribe
    public void onBeforeCommitChanges(BeforeCommitChangesEvent event) {
        if (visibleOtherCheckBox.getValue() == null) {
            visibleOtherCheckBox.setValue(false);
        }
    }
}