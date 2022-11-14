package com.company.itpearls.web.screens.rotatingcandidates;

import com.company.itpearls.service.GetRoleService;
import com.company.itpearls.web.StandartRoles;
import com.company.itpearls.web.widgets.Diagrams.ResearcherDiagramWidget;
import com.haulmont.cuba.core.entity.KeyValueEntity;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.model.KeyValueCollectionContainer;
import com.haulmont.cuba.gui.model.KeyValueCollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.JobCandidate;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.Map;

@UiController("itpearls_RotatingCandidate.browse")
@UiDescriptor("rotating-candidate-browse.xml")
@LookupComponent("jobCandidatesTable")
@LoadDataBeforeShow
public class RotatingCandidateBrowse extends StandardLookup<JobCandidate> {

    @Inject
    private RadioButtonGroup<Integer> recruterRadioButtonsGroup;

    private Map<String, Integer> recruterRadioButtonsGroupMap = new LinkedHashMap<>();
    @Inject
    private UserSession userSession;
    @Inject
    private KeyValueCollectionLoader interactionListDl;
    @Inject
    private RadioButtonGroup daysIntervalRadioButtonsGroup;
    private Map<String, Integer> daysIntervalRadioButtonsGroupMap = new LinkedHashMap<>();
    @Inject
    private LookupPickerField<User> recruterLookupPickerField;
    @Inject
    private GetRoleService getRoleService;
    @Inject
    private CollectionLoader<User> userDl;
    @Inject
    private ScrollBoxLayout labelndidatesScrollBox;
    @Inject
    private KeyValueCollectionContainer interactionListDc;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private Label<String> candidateCountLabel;
    @Inject
    private Label<String> candidateNameLabel;

    @Subscribe
    public void onInit(InitEvent event) {
        recruterRadioButtonsSetMap();
        daysIntervalRadioButtonsSetMap();
    }

    private void daysIntervalRadioButtonsSetMap() {
        daysIntervalRadioButtonsGroupMap.put("За все время", 0);
        daysIntervalRadioButtonsGroupMap.put("За 3 дня", 1);
        daysIntervalRadioButtonsGroupMap.put("За 7 дней", 2);
        daysIntervalRadioButtonsGroupMap.put("За месяц", 3);
        daysIntervalRadioButtonsGroup.setOptionsMap(daysIntervalRadioButtonsGroupMap);
    }

    @Subscribe("daysIntervalRadioButtonsGroup")
    public void onDaysIntervalRadioButtonsGroupValueChange(HasValue.ValueChangeEvent<Integer> event) {
        switch (event.getValue()) {
            case 0:
                interactionListDl.removeParameter("daysBetween");
                break;
            case 1:
                interactionListDl.setParameter("daysBetween", 3);
                break;
            case 2:
                interactionListDl.setParameter("daysBetween", 7);
                break;
            case 3:
                interactionListDl.setParameter("daysBetween", 30);
                break;
            default:
                interactionListDl.removeParameter("daysBetween");
                break;
        }
        interactionListDl.load();

    }

    private void recruterRadioButtonsSetMap() {
        recruterRadioButtonsGroupMap.put("Все", 0);
        recruterRadioButtonsGroupMap.put("Только мои", 1);
        recruterRadioButtonsGroupMap.put("Рекрутера", 2);

        recruterRadioButtonsGroup.setOptionsMap(recruterRadioButtonsGroupMap);
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        recruterRadioButtonsGroup.setValue(1);

        if (getRoleService.isUserRoles(userSession.getUser(), StandartRoles.RECRUITER) ||
                getRoleService.isUserRoles(userSession.getUser(), StandartRoles.RESEARCHER)) {
            userDl.setParameter("active", true);
        } else {
            userDl.removeParameter("active");
        }

        userDl.load();

        setCandidateLabels();
    }

    private void setCandidateLabels() {
        for (Component c : labelndidatesScrollBox.getComponents()) {
            labelndidatesScrollBox.remove(c);
        }

        for (KeyValueEntity entity : interactionListDc.getItems()) {
            Button label = uiComponents.create(Button.NAME);

            label.setCaption(((JobCandidate) entity.getValue("candidate")).getFullName()
                    + " ("
                    + entity.getValue("count")
                    + ") ");

            label.setStyleName("label_button_green");
            label.addClickListener(e -> {
                candidateNameLabel.setValue(e.getButton().getCaption());
            });
            labelndidatesScrollBox.add(label);

            candidateCountLabel.setValue("Кандидатов в работе: "
                    + String.valueOf(interactionListDc.getItems().size()));
        }
    }

    @Subscribe("recruterLookupPickerField")
    public void onRecruterLookupPickerFieldValueChange(HasValue.ValueChangeEvent<User> event) {
        if (recruterLookupPickerField.getValue() != null) {
            interactionListDl.setParameter("recrutier", recruterLookupPickerField.getValue());
        } else {
            interactionListDl.removeParameter("recrutier");
        }

        interactionListDl.load();
        setCandidateLabels();
    }

    @Install(to = "recruterLookupPickerField", subject = "optionIconProvider")
    private String recruterLookupPickerFieldOptionIconProvider(User user) {
        return user.getActive() ? CubaIcon.PLUS_CIRCLE.source() : CubaIcon.MINUS_CIRCLE.source();
    }

    @Subscribe("recruterRadioButtonsGroup")
    public void onRecruterRadioButtonsGroupValueChange(HasValue.ValueChangeEvent<Integer> event) {
        switch (event.getValue()) {
            case 0:
                interactionListDl.removeParameter("recrutier");
                recruterLookupPickerField.setEnabled(false);
                break;
            case 1:
                interactionListDl.setParameter("recrutier", userSession.getUser());
                recruterLookupPickerField.setEnabled(false);
                break;
            default:
                interactionListDl.removeParameter("recrutier");
                recruterLookupPickerField.setEnabled(true);
                break;
        }

        interactionListDl.load();
        setCandidateLabels();
    }
}