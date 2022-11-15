package com.company.itpearls.web.screens.iteractionlist;

import com.company.itpearls.entity.IteractionList;
import com.company.itpearls.entity.OpenPosition;
import com.company.itpearls.service.GetRoleService;
import com.company.itpearls.web.StandartRoles;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.gui.Screens;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.components.data.ValueSource;
import com.haulmont.cuba.gui.components.data.value.ContainerValueSource;
import com.haulmont.cuba.gui.export.FileDataProvider;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.model.KeyValueCollectionContainer;
import com.haulmont.cuba.gui.model.KeyValueCollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.JobCandidate;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@UiController("itpearls_RotatingCandidate.browse")
@UiDescriptor("rotating-candidate-browse.xml")
@LookupComponent("jobCandidatesTable")
@LoadDataBeforeShow
public class RotatingCandidateBrowse extends StandardLookup<JobCandidate> {

    @Inject
    private RadioButtonGroup<Integer> recruterRadioButtonsGroup;
    @Inject
    private RadioButtonGroup<Integer> daysIntervalRadioButtonsGroup;

    private Map<String, Integer> recruterRadioButtonsGroupMap = new LinkedHashMap<>();
    private Map<String, Integer> daysIntervalRadioButtonsGroupMap = new LinkedHashMap<>();
    private Map<String, Integer> openOrCloseCaseRadioButtonsGroupMap = new LinkedHashMap<>();

    private Integer recruterRadioButtonsGroupOld = -1;
    private Integer daysIntervalRadioButtonsGroupOld = -1;
    private Integer openOrCloseCaseRadioButtonsGroupOld = -1;

    @Inject
    private UserSession userSession;
    @Inject
    private LookupPickerField<User> recruterLookupPickerField;
    @Inject
    private GetRoleService getRoleService;
    @Inject
    private CollectionLoader<User> userDl;
    @Inject
    private ScrollBoxLayout labelndidatesScrollBox;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private Label<String> candidateCountLabel;
    @Inject
    private CollectionLoader<IteractionList> iteractionListsDl;
    @Inject
    private CollectionContainer<IteractionList> iteractionListsDc;
    @Inject
    private RadioButtonGroup openOrCloseCaseRadioButtonsGroup;
    @Inject
    private CollectionContainer<User> userDc;
    @Inject
    private Label<String> candidateNameLabel;
    @Inject
    private Label<String> candidatePersonPositionLabel;
    @Inject
    private VBoxLayout candidateCardVBox;
    @Inject
    private Image candidatePic;
    @Inject
    private KeyValueCollectionLoader lastProjectDl;
    @Inject
    private KeyValueCollectionContainer lastProjectDc;
    @Inject
    private Table lastProjectTable;
    @Inject
    private Screens screens;

    @Subscribe
    public void onInit(InitEvent event) {
        recruterRadioButtonsSetMap();
        daysIntervalRadioButtonsSetMap();
        openOrCloseCaseRadioButtonsSetMap();
    }

    private void openOrCloseCaseRadioButtonsSetMap() {
        openOrCloseCaseRadioButtonsGroupMap.put("Все", 0);
        openOrCloseCaseRadioButtonsGroupMap.put("Активный", 1);
        openOrCloseCaseRadioButtonsGroupMap.put("Закрытый", 2);

        openOrCloseCaseRadioButtonsGroup.setOptionsMap(openOrCloseCaseRadioButtonsGroupMap);
    }

    @Subscribe("openOrCloseCaseRadioButtonsGroup")
    public void onOpenOrCloseCaseRadioButtonsGroupValueChange(HasValue.ValueChangeEvent<Integer> event) {
        if (!openOrCloseCaseRadioButtonsGroupOld.equals(event.getValue())) {
            switch (event.getValue()) {
                case 0:
                    iteractionListsDl.removeParameter("endCase");
                    break;
                case 1:
                    iteractionListsDl.setParameter("endCase", false);
                    break;
                case 2:
                    iteractionListsDl.setParameter("endCase", true);
                    break;
                default:
                    iteractionListsDl.removeParameter("endCase");
                    break;
            }

            iteractionListsDl.load();
            openOrCloseCaseRadioButtonsGroupOld = event.getValue();
        }
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
        if (!daysIntervalRadioButtonsGroupOld.equals(event.getValue())) {
            switch (event.getValue()) {
                case 0:
                    iteractionListsDl.removeParameter("daysBetween");
                    break;
                case 1:
                    iteractionListsDl.setParameter("daysBetween", 3);
                    break;
                case 2:
                    iteractionListsDl.setParameter("daysBetween", 7);
                    break;
                case 3:
                    iteractionListsDl.setParameter("daysBetween", 30);
                    break;
                default:
                    iteractionListsDl.removeParameter("daysBetween");
                    break;
            }

            iteractionListsDl.load();
            daysIntervalRadioButtonsGroupOld = event.getValue();
        }
    }

    private void recruterRadioButtonsSetMap() {
        recruterRadioButtonsGroupMap.put("Все", 0);
        recruterRadioButtonsGroupMap.put("Только мои", 1);
        recruterRadioButtonsGroupMap.put("Рекрутера", 2);

        recruterRadioButtonsGroup.setOptionsMap(recruterRadioButtonsGroupMap);
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        if (getRoleService.isUserRoles(userSession.getUser(), StandartRoles.RECRUITER) ||
                getRoleService.isUserRoles(userSession.getUser(), StandartRoles.RESEARCHER)) {
            userDl.setParameter("active", true);
        } else {
            userDl.removeParameter("active");
        }

        userDl.load();
        recruterLookupPickerField.setOptionsList(userDc.getItems());

        daysIntervalRadioButtonsGroup.setValue(2);
        recruterRadioButtonsGroup.setValue(1);
        openOrCloseCaseRadioButtonsGroup.setValue(0);
    }

    private void setCandidateEntityLabels() {
        for (Component c : labelndidatesScrollBox.getComponents()) {
            labelndidatesScrollBox.remove(c);
        }

        List<IteractionList> setList = new ArrayList<>();
        // Долбанный алгоритм получения уникальной последовательности списка кандидатов
        for (int i = 0; i < iteractionListsDc.getItems().size(); i++) {
            IteractionList il1 = iteractionListsDc.getItems().get(i);
            Boolean flag = true;
            for (int j = 0; j < iteractionListsDc.getItems().size(); j++) {
                if (i != j) {
                    if (il1.getCandidate().equals(iteractionListsDc.getItems().get(j).getCandidate())) {
                        if (i > j)
                            break;

                        setList.add(il1);
                        flag = false;
                        break;
                    }
                }
            }

            if (false) {
                setList.add(il1);
            }
        }

        for (IteractionList iteractionList : setList) {
            Button candidateButton = uiComponents.create(Button.NAME);

            candidateButton.setCaption(iteractionList.getCandidate().getFullName());
            candidateButton.setStyleName("label_button_green");
            candidateButton.addClickListener(e -> {
                setCandidateCard(iteractionList);
            });
            labelndidatesScrollBox.add(candidateButton);
        }

        candidateCountLabel.setValue("Кандидатов: "
                + String.valueOf(setList.size()));
    }

    private void setCandidateCard(IteractionList iteractionList) {
        selectedJobCandidate = iteractionList.getCandidate();

        candidateCardVBox.setVisible(true);

        candidateNameLabel.setValue(iteractionList.getCandidate().getFullName());
        candidatePersonPositionLabel.setValue(iteractionList.getCandidate().getPersonPosition().getPositionRuName()
                + " / "
                + iteractionList.getCandidate().getPersonPosition().getPositionEnName());

        lastProjectDl.setParameter("candidate", iteractionList.getCandidate());
        lastProjectDl.load();
    }

    @Subscribe("recruterLookupPickerField")
    public void onRecruterLookupPickerFieldValueChange(HasValue.ValueChangeEvent<User> event) {
        if (recruterLookupPickerField.getValue() != null) {
            iteractionListsDl.setParameter("recrutier", recruterLookupPickerField.getValue().getLogin());
        } else {
            iteractionListsDl.removeParameter("recrutier");
        }

        iteractionListsDl.load();
    }

    @Subscribe(id = "iteractionListsDl", target = Target.DATA_LOADER)
    public void onIteractionListsDlPostLoad(CollectionLoader.PostLoadEvent<IteractionList> event) {
        setCandidateEntityLabels();
    }

    @Install(to = "recruterLookupPickerField", subject = "optionIconProvider")
    private String recruterLookupPickerFieldOptionIconProvider(User user) {
        return user.getActive() ? CubaIcon.PLUS_CIRCLE.source() : CubaIcon.MINUS_CIRCLE.source();
    }

    @Subscribe("recruterRadioButtonsGroup")
    public void onRecruterRadioButtonsGroupValueChange(HasValue.ValueChangeEvent<Integer> event) {
        if (!recruterRadioButtonsGroupOld.equals(event.getValue())) {
            switch (event.getValue()) {
                case 0:
                    iteractionListsDl.removeParameter("recrutier");
                    recruterLookupPickerField.setEnabled(false);
                    break;
                case 1:
                    iteractionListsDl.setParameter("recrutier", userSession.getUser().getLogin());
                    recruterLookupPickerField.setEnabled(false);
                    break;
                default:
                    iteractionListsDl.removeParameter("recrutier");
                    recruterLookupPickerField.setEnabled(true);
                    break;
            }

            iteractionListsDl.load();
            recruterRadioButtonsGroupOld = event.getValue();
        }
    }


    public Component lastIteractionCount(Entity entity) {
        Label retLabel = uiComponents.create(Label.NAME);
        int lastIteractionCount = 0;

        for (int i = 0; i < lastProjectDc.getItems().size(); i++) {
            if (entity.equals(lastProjectDc.getItems().get(i))) {
                lastIteractionCount = i;
                break;
            }
        }

        retLabel.setValue(++lastIteractionCount);

        return retLabel;
    }

    private JobCandidate selectedJobCandidate;

    public Component lastInteractionGeneratorColumn(Entity entity) {
        Label retLabel = uiComponents.create(Label.NAME);

        if (selectedJobCandidate != null) {
            OpenPosition openPosition = entity.getValue("vacancy");
            IteractionList lastInteraction = null;

            if (selectedJobCandidate.getIteractionList().size() != 0) {
                for (int i = 0; i < selectedJobCandidate.getIteractionList().size(); i++) {
                    if (lastInteraction != null) {
                        if (openPosition.equals(selectedJobCandidate.getIteractionList()
                                .get(i)
                                .getVacancy())) {
                            if (lastInteraction.getDateIteraction().before(selectedJobCandidate
                                    .getIteractionList()
                                    .get(i)
                                    .getDateIteraction())) {
                                lastInteraction = selectedJobCandidate.getIteractionList()
                                        .get(i);
                            }
                        }
                    } else {
                        if (openPosition.equals(selectedJobCandidate.getIteractionList()
                                .get(i)
                                .getVacancy())) {
                            lastInteraction = selectedJobCandidate.getIteractionList()
                                    .get(i);
                        }
                    }
                }
            }

            StringBuffer retStr = new StringBuffer(lastInteraction.getIteractionType().getIterationName());
            retLabel.setValue(retStr);
        }

        return retLabel;
    }

    public Component whoIsResearcherGeneratorColumn(Entity entity) {
        Label retLabel = uiComponents.create(Label.NAME);

        if (selectedJobCandidate != null) {

            for (IteractionList iteractionList : selectedJobCandidate.getIteractionList()) {
                OpenPosition op = entity.getValue("vacancy");

                if (op != null) {
                    if (iteractionList.getIteractionType().getSignOurInterviewAssigned() != null) {
                        if (iteractionList.getVacancy() != null) {
                            if (iteractionList.getVacancy().equals(op) &&
                                    iteractionList.getIteractionType().getSignOurInterviewAssigned()) {
                                retLabel.setValue(iteractionList.getRecrutier().getName());
                            }
                        }
                    }
                }
            }
        }

        return retLabel;
    }

    public Component whoIsRecruterGeneratorColumn(Entity entity) {
        Label retLabel = uiComponents.create(Label.NAME);

        if (selectedJobCandidate != null) {
            for (IteractionList iteractionList : selectedJobCandidate.getIteractionList()) {
                OpenPosition op = entity.getValue("vacancy");

                if (op != null) {
                    if (iteractionList.getIteractionType().getSignOurInterview() != null) {
                        if (iteractionList.getVacancy() != null) {
                            if (iteractionList.getVacancy().equals(op) &&
                                    iteractionList.getIteractionType().getSignOurInterview()) {
                                retLabel.setValue(iteractionList.getRecrutier().getName());
                            }
                        }
                    }
                }
            }
        }

        return retLabel;
    }


    public Component addInteractionsViewButton(Entity entity) {
        Button retButton = uiComponents.create(Button.NAME);
        retButton.setCaption("Просмотр");

        retButton.setAction(new BaseAction("listIteraction")
                .withHandler(actionPerformedEvent -> {
                    IteractionListSimpleBrowse iteractionListSimpleBrowse =
                            screens.create(IteractionListSimpleBrowse.class);

                    iteractionListSimpleBrowse.setSelectedCandidate(selectedJobCandidate);
                    iteractionListSimpleBrowse.setJobCandidate(selectedJobCandidate);

                    OpenPosition openPosition = lastProjectDc.getItem(lastProjectTable
                            .getSingleSelected()).getValue("vacancy");

                    iteractionListSimpleBrowse.setOpenPosition(openPosition);

                    screens.show(iteractionListSimpleBrowse);

                }));

        return retButton;
    }
}