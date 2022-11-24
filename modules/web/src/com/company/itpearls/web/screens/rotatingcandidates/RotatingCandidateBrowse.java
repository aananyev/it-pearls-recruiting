package com.company.itpearls.web.screens.rotatingcandidates;

import com.company.itpearls.entity.*;
import com.company.itpearls.service.GetRoleService;
import com.company.itpearls.web.StandartRoles;
import com.company.itpearls.web.screens.candidatecv.CandidateCVSimpleBrowse;
import com.company.itpearls.web.screens.fragments.Skillsbar;
import com.company.itpearls.web.screens.iteractionlist.IteractionListSimpleBrowse;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.*;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.components.data.datagrid.ContainerDataGridItems;
import com.haulmont.cuba.gui.components.data.table.ContainerTableItems;
import com.haulmont.cuba.gui.components.data.value.ContainerValueSource;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.model.*;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import javax.swing.*;
import java.util.*;

@UiController("itpearls_RotatingCandidate.browse")
@UiDescriptor("rotating-candidate-browse.xml")
@LookupComponent("jobCandidatesTable")
@LoadDataBeforeShow
public class RotatingCandidateBrowse extends StandardLookup<JobCandidate> {

    private static final String DESCRIPTION_VACANCY_OPEN = "Вакансия открыта на текущий момент";
    private static final String DESCRIPTION_VACANCY_CLOSE = "Вакансия закрыта на текущий момент";
    private static final String CASE_IS_CLOSED = "Кейс с кандидатом закрыт по этой вакансии";
    private static final String CASE_IS_OPENED = "Кейс с кандидатом по этой вакансии продолжаетcя";

    @Inject
    private RadioButtonGroup<Integer> recruterRadioButtonsGroup;
    @Inject
    private RadioButtonGroup<Integer> daysIntervalRadioButtonsGroup;
    @Inject
    private UserSession userSession;
    @Inject
    private LookupPickerField<User> recruterLookupPickerField;
    @Inject
    private GetRoleService getRoleService;
    @Inject
    private CollectionLoader<User> userDl;
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
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private DataComponents dataComponents;
    @Inject
    private DataGrid<IteractionList> jobCandidateTable;
    @Inject
    private Fragments fragments;
    @Inject
    private HBoxLayout skillBox;
    @Inject
    private Table<CandidateCV> candidatesCVTable;
    @Inject
    private Label<String> candidateCityLocationLabel;
    @Inject
    private WebBrowserTools webBrowserTools;
    @Inject
    private LinkButton emailLinkButton;
    @Inject
    private Label<String> emailLabel;
    @Inject
    private LinkButton phoneLinkButton;
    @Inject
    private Label<String> phoneLabel;
    @Inject
    private LinkButton telegramLinkButton;
    @Inject
    private Label<String> telegramLabel;
    @Inject
    private LinkButton skypeLinkButton;
    @Inject
    private Label<String> skypeLabel;
    @Inject
    private Table<OpenPosition> suggestVacancyTable;
    @Inject
    private CollectionLoader<OpenPosition> suggestOpenPositionDl;
    @Inject
    private LookupPickerField<Iteraction> interactionLookupPickerField;
    @Inject
    private LookupPickerField<Position> positionLookupPickerField;
    @Inject
    private Label<String> activeInactiveLabel;

    // Fields
    private JobCandidate selectedJobCandidate;
    private Skillsbar skillBoxFragment = null;
    private boolean reloadDataLoaders = false;
    private Map<String, Integer> recruterRadioButtonsGroupMap = new LinkedHashMap<>();
    private Map<String, Integer> daysIntervalRadioButtonsGroupMap = new LinkedHashMap<>();
    private Map<String, Integer> openOrCloseCaseRadioButtonsGroupMap = new LinkedHashMap<>();
    private Integer recruterRadioButtonsGroupOld = -1;
    private Integer daysIntervalRadioButtonsGroupOld = -1;
    private Integer openOrCloseCaseRadioButtonsGroupOld = -1;
    private String CANDIDATE_INACTIVE = "Inactive";
    private String CANDIDATE_ACTIVE = "Active";

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
//        openOrCloseCaseRadioButtonsGroupMap.put("Завис", 3);
        openOrCloseCaseRadioButtonsGroupMap.put("Должность", 4);

        openOrCloseCaseRadioButtonsGroup.setOptionsMap(openOrCloseCaseRadioButtonsGroupMap);
    }

    @Install(to = "jobCandidateTable.activeUnactiveColumn", subject = "columnGenerator")
    private Component jobCandidateTableActiveUnactiveColumnColumnGenerator(DataGrid.ColumnGeneratorEvent<IteractionList> event) {
        Label retLabel = uiComponents.create(Label.class);
        retLabel.setStyleName("h4");
        retLabel.setAlignment(Component.Alignment.MIDDLE_CENTER);
        retLabel.setHeightAuto();
        retLabel.setWidthFull();

        if (isActiveCandidate(event.getItem().getCandidate())) {
            retLabel.addStyleName("label_table_green");
            retLabel.setValue(CANDIDATE_ACTIVE);
        }

        if (isInactiveCandidate(event.getItem().getCandidate())) {
            retLabel.addStyleName("label_table_red");
            retLabel.setHeightAuto();
            retLabel.setWidthFull();
            retLabel.setValue(CANDIDATE_INACTIVE);

        }

        return retLabel;
    }

    @Subscribe("interactionLookupPickerField")
    public void onInteractionLookupPickerFieldValueChange(HasValue.ValueChangeEvent<Iteraction> event) {
        setInteractionsFilter(event);
    }

    private void setInteractionsFilter(HasValue.ValueChangeEvent<Iteraction> event) {
        setCandidateEntityLabels();
    }

    @Subscribe("positionLookupPickerField")
    public void onPositionLookupPickerFieldValueChange(HasValue.ValueChangeEvent<Position> event) {
        if (positionLookupPickerField.getValue() != null) {
            iteractionListsDl.setParameter("personPosition", positionLookupPickerField.getValue());
        } else {
            iteractionListsDl.removeParameter("personPosition");
        }

        iteractionListsDl.load();
        setCandidateEntityLabels();
    }


    @Subscribe("openOrCloseCaseRadioButtonsGroup")
    public void onOpenOrCloseCaseRadioButtonsGroupValueChange(HasValue.ValueChangeEvent<Integer> event) {
        if (!openOrCloseCaseRadioButtonsGroupOld.equals(event.getValue())) {
            if (event.getValue() == 4) { // Должность
                setPositionsFieldFilter();

                if (positionLookupPickerField.getValue() != null) {
                    iteractionListsDl.setParameter("personPosition", positionLookupPickerField.getValue());
                } else {
                    iteractionListsDl.removeParameter("personPosition");
                }

                iteractionListsDl.load();
            }

            if (event.getValue() == 3) { // Должность
                if (interactionLookupPickerField.getValue() != null) {
                }
            }

            setCandidateEntityLabels();
            openOrCloseCaseRadioButtonsGroupOld = event.getValue();

            if (event.getValue() != 4)
                positionLookupPickerField.setValue(null);

            if (event.getValue() != 3)
                interactionLookupPickerField.setValue(null);
        }

        positionLookupPickerField.setVisible(event.getValue() == 4);
        positionLookupPickerField.setEnabled(event.getValue() == 4);
        interactionLookupPickerField.setEnabled(event.getValue() == 3);
        interactionLookupPickerField.setVisible(event.getValue() == 3);
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
        recruterRadioButtonsGroupMap.put("Рекрутер", 2);

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
//        setPositionsFieldFilter();

        daysIntervalRadioButtonsGroup.setValue(2);
        recruterRadioButtonsGroup.setValue(1);
        openOrCloseCaseRadioButtonsGroup.setValue(0);

        jobCandidateTable.addStyleName("borderless");
        jobCandidateTable.addStyleName("no-horizontal-lines");
        jobCandidateTable.addStyleName("no-vertical-lines");

        candidatesCVTable.addStyleName("borderless");
        candidatesCVTable.addStyleName("no-horizontal-lines");
        candidatesCVTable.addStyleName("no-vertical-lines");

        suggestVacancyTable.addStyleName("borderless");
        suggestVacancyTable.addStyleName("no-horizontal-lines");
        suggestVacancyTable.addStyleName("no-vertical-lines");

    }

    private void setPositionsFieldFilter() {
        Set<Position> positions = new HashSet<>();

        for (IteractionList iteractionList : iteractionListsDc.getItems()) {
            positions.add(iteractionList.getCandidate().getPersonPosition());
        }

        List<Position> list = new ArrayList<>(positions);
        positionLookupPickerField.setOptionsList(list);
    }

    private boolean isActiveCandidate(JobCandidate jobCandidate) {
        // Сначала найдем уникальный список кандидатов из коллеiкции взаимодействий
        HashMap<OpenPosition, Boolean> candidateOpenPositionClose = new HashMap<>();
        List<OpenPosition> openPositions = getOpenPositionsFromCandidate(jobCandidate);

        // выделили все проекты где был кандидат задействован
        // бежим по каждому кандидату и определяем из его взаимодействий список проектов,
        // где он учавствовал за конкретный интервал времени
        for (OpenPosition openPosition : openPositions) {
            Boolean flag = false;
            for (IteractionList list : jobCandidate.getIteractionList()) {
                if (list.getVacancy() != null) {
                    if (!list.getVacancy().getVacansyName().equals("Default")) {
                        if (list.getVacancy().equals(openPosition)) {
                            if (list.getIteractionType().getSignEndCase()) {
                                candidateOpenPositionClose.put(openPosition, true);
                                flag = true;
                                break;
                            }
                        }
                    }
                }
            }

            if (!flag) {
                candidateOpenPositionClose.put(openPosition, false);
            }
        }

        Boolean retBoolean = false;
        for (Map.Entry<OpenPosition, Boolean> entrySet : candidateOpenPositionClose.entrySet()) {
            if (entrySet.getKey() != null) {
                if (!entrySet.getKey().getVacansyName().equals("Default")) {
                    if (entrySet.getValue() == false) {
                        retBoolean = true;
                        break;
                    }
                }
            }
        }

        return retBoolean;
    }

    private boolean isInactiveCandidate(JobCandidate jobCandidate) {
        // Сначала найдем уникальный список кандидатов из коллеiкции взаимодействий
        HashMap<OpenPosition, Boolean> candidateOpenPositionClose = new HashMap<>();
        List<OpenPosition> openPositions = getOpenPositionsFromCandidate(jobCandidate);

        // выделили все проекты где был кандидат задействован
        // бежим по каждому кандидату и определяем из его взаимодействий список проектов,
        // где он учавствовал за конкретный интервал времени
        for (OpenPosition openPosition : openPositions) {
            Boolean flag = false;
            for (IteractionList list : jobCandidate.getIteractionList()) {
                if (list.getVacancy() != null) {
                    if (!list.getVacancy().getVacansyName().equals("Default")) {
                        if (list.getVacancy().equals(openPosition)) {
                            if (list.getIteractionType().getSignEndCase()) {
                                candidateOpenPositionClose.put(openPosition, true);
                                flag = true;
                                break;
                            }
                        }
                    }
                }
            }

            if (!flag) {
                candidateOpenPositionClose.put(openPosition, false);
            }
        }

        Boolean retBoolean = true;
        for (Map.Entry<OpenPosition, Boolean> entrySet : candidateOpenPositionClose.entrySet()) {
            if (entrySet.getKey() != null) {
                if (!entrySet.getKey().getVacansyName().equals("Default")) {
                    if (entrySet.getValue() == false) {
                        retBoolean = false;
                    }
                }
            }
        }

        return retBoolean;
    }

    private List<OpenPosition> getOpenPositionsFromCandidate(JobCandidate jobCandidate) {
        Set<OpenPosition> openPositionSet = new HashSet<>();

        for (IteractionList iteractionList : jobCandidate.getIteractionList()) {
            openPositionSet.add(iteractionList.getVacancy());
        }

        List<OpenPosition> openPositions = new ArrayList<>(openPositionSet);
        return openPositions;
    }

    private void setCandidateEntityLabels() {
        if (openOrCloseCaseRadioButtonsGroup.getValue() != null) {
            CollectionContainer<IteractionList> iteractionListNewDc;

            List<IteractionList> setList = new ArrayList<>();
            // Долбанный алгоритм получения уникальной последовательности списка кандидатов
            for (int i = 0; i < iteractionListsDc.getItems().size(); i++) {
                IteractionList il1 = iteractionListsDc.getItems().get(i);
                Boolean flag = true;
                for (int j = 0; j < iteractionListsDc.getItems().size(); j++) {
                    if (i != j) {
                        if (il1.getCandidate().equals(iteractionListsDc.getItems().get(j).getCandidate())) {
                            if (i > j) {
                                flag = false;
                                break;
                            }

                            if (openOrCloseCaseRadioButtonsGroup.getValue() != null) {
                                switch ((int) openOrCloseCaseRadioButtonsGroup.getValue()) {
                                    case 0:
                                        setList.add(il1);
                                        break;
                                    case 1:
                                        if (isActiveCandidate(il1.getCandidate())) {
                                            setList.add(il1);
                                        }
                                        break;
                                    case 2:
                                        if (isInactiveCandidate(il1.getCandidate())) {
                                            setList.add(il1);
                                        }
                                        break;
                                    case 3: // проверки на зависшего
                                        if (interactionLookupPickerField.getValue() != null) {
                                            if (isLostIteraction(il1.getCandidate())) {
                                                setList.add(il1);
                                            }
                                        }
                                        break;
                                    default:
                                        setList.add(il1);
                                        break;
                                }
                                flag = false;
                                break;
                            }
                        }
                    }
                }

                if (flag) {
                    if (openOrCloseCaseRadioButtonsGroup.getValue() != null) {
                        switch ((int) openOrCloseCaseRadioButtonsGroup.getValue()) {
                            case 0:
                                setList.add(il1);
                                break;
                            case 1:
                                if (isActiveCandidate(il1.getCandidate())) {
                                    setList.add(il1);
                                }
                                break;
                            case 2:
                                if (isInactiveCandidate(il1.getCandidate())) {
                                    setList.add(il1);
                                }
                                break;
                            case 3: // проверки на зависшего
                                if (interactionLookupPickerField.getValue() != null) {
                                    if (isLostIteraction(il1.getCandidate())) {
                                        setList.add(il1);
                                    }
                                }
                                break;
                            default:
                                setList.add(il1);
                                break;
                        }
                    }
                }
            }

            iteractionListNewDc = dataComponents.createCollectionContainer(IteractionList.class);
            iteractionListNewDc.setItems(setList);
            jobCandidateTable.setItems(new ContainerDataGridItems<>(iteractionListNewDc));

            candidateCountLabel.setValue("Кандидатов: "
                    + String.valueOf(setList.size()));
        }
    }

    private boolean isLostIteraction(JobCandidate candidate) {
        Boolean retBool = false;
        Set<Project> projects = new HashSet<>();

        for (IteractionList iteractionList : candidate.getIteractionList()) {
            if (iteractionList.getVacancy().getProjectName().getDefaultProject() == null) {
                projects.add((iteractionList.getVacancy().getProjectName()));
            } else {
                if (!iteractionList.getVacancy().getProjectName().getDefaultProject()) {
                    projects.add(iteractionList.getVacancy().getProjectName());
                }
            }
        }

        for (Project project : projects) {
            int numberFoundInteraction = -1;
            Boolean foundInteraction = false;
            Boolean foundNextInteraction = false;

            for (int i = 0; i < candidate.getIteractionList().size(); i++) {
                if (candidate
                        .getIteractionList()
                        .get(i)
                        .getVacancy()
                        .getProjectName()
                        .equals(project)) {
                    if (candidate
                            .getIteractionList()
                            .get(i)
                            .getIteractionType()
                            .equals(interactionLookupPickerField.getValue())) {
                        foundInteraction = true;
                        numberFoundInteraction = i;
                    }

                    if (foundInteraction && i > numberFoundInteraction) {
                        foundNextInteraction = true;

                        break;
                    }
                }
            }

            if (foundNextInteraction && !foundNextInteraction) {
                retBool = true;
                break;
            }
        }

        return retBool != null ? !retBool : false;
    }

    private void setCandidateCVTable() {
        CollectionContainer candidateCVContainer = dataComponents.createCollectionContainer(CandidateCV.class);
        List<CandidateCV> cvs = new ArrayList<>();

        if (selectedJobCandidate != null) {
            for (CandidateCV cv : selectedJobCandidate.getCandidateCv()) {
                cvs.add(cv);
            }

            candidateCVContainer.setItems(cvs);
            candidatesCVTable.setItems(new ContainerTableItems<>(candidateCVContainer));
        }
    }

    @Install(to = "jobCandidateTable.candidatePhoto", subject = "columnGenerator")
    private Component jobCandidateTableCandidatePhotoColumnGenerator(DataGrid.ColumnGeneratorEvent<IteractionList> event) {

        HBoxLayout hBox = uiComponents.create(HBoxLayout.class);
        Image retImage = uiComponents.create(Image.NAME);
        retImage.setScaleMode(Image.ScaleMode.FILL);
        retImage.setAlignment(Component.Alignment.MIDDLE_CENTER);
        retImage.setWidth("45px");
        retImage.setHeight("45px");
        retImage.setStyleName("circle");

        if (event.getItem().getCandidate().getFileImageFace() != null) {
            try {
                retImage.setValueSource(
                        new ContainerValueSource<>(event.getContainer(), "candidate.fileImageFace"));
            } catch (IllegalArgumentException e) {
                e.printStackTrace();

                String address = "icons/no-programmer.jpeg";
                retImage.setSource(ThemeResource.class).setPath(address);
            }
        } else {
            String address = "icons/no-programmer.jpeg";
            retImage.setSource(ThemeResource.class).setPath(address);
        }

        hBox.setWidthFull();
        hBox.setHeightFull();
        hBox.add(retImage);

        return hBox;
    }

    @Install(to = "jobCandidateTable.candidateCard", subject = "columnGenerator")
    private String jobCandidateTableCandidateCardColumnGenerator(DataGrid.ColumnGeneratorEvent<IteractionList> event) {
        String retStr =
                "<b>" + event.getItem().getCandidate().getFullName()
                        + "</b><br><i>"
                        + event.getItem().getCandidate().getPersonPosition().getPositionRuName()
                        + "</i>";
        return retStr;
    }

    @Subscribe("jobCandidateTable")
    public void onJobCandidateTableSelection1(DataGrid.SelectionEvent<IteractionList> event) {
        setCandidateCard(jobCandidateTable.getSingleSelected());
    }

    private void setCandidateCard(IteractionList iteractionList) {
        selectedJobCandidate = iteractionList.getCandidate();

        candidateCardVBox.setVisible(true);

        if (iteractionList.getCandidate().getFullName() != null)
            candidateNameLabel.setValue(iteractionList.getCandidate().getFullName());
        else
            candidateNameLabel.setValue(iteractionList.getCandidate().getSecondName()
                    + " "
                    + iteractionList.getCandidate().getFirstName());

        if (isInactiveCandidate(iteractionList.getCandidate())) {
            activeInactiveLabel.setValue(CANDIDATE_INACTIVE);
            activeInactiveLabel.setStyleName("label_button_red");
        }

        if (isActiveCandidate(iteractionList.getCandidate())) {
            activeInactiveLabel.setValue(CANDIDATE_ACTIVE);
            activeInactiveLabel.setStyleName("label_button_green");
        }

        candidatePersonPositionLabel.setValue(iteractionList.getCandidate().getPersonPosition().getPositionRuName()
                + " / "
                + iteractionList.getCandidate().getPersonPosition().getPositionEnName());

        candidateCityLocationLabel.setValue("(" + selectedJobCandidate.getCityOfResidence().getCityRuName() + ")");

        lastProjectDl.setParameter("candidate", iteractionList.getCandidate());
        lastProjectDl.load();

//        candidateCVDl.setParameter("candidate", iteractionList.getCandidate());
//        candidateCVDl.load();

        try {
            FileDescriptorResource fileDescriptorResource = candidatePic.createResource(FileDescriptorResource.class)
                    .setFileDescriptor(selectedJobCandidate.getFileImageFace());

            candidatePic.setSource(fileDescriptorResource);
            candidatePic.setScaleMode(Image.ScaleMode.SCALE_DOWN);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            String address = "icons/no-programmer.jpeg";
            candidatePic.setSource(ThemeResource.class).setPath(address);
        }

        setupSkillBox();

        lastProjectTable.addStyleName("borderless");
        lastProjectTable.addStyleName("no-horizontal-lines");
        lastProjectTable.addStyleName("no-vertical-lines");

        setLinkButtonEmail();
        setLinkButtonPhone();
        setLinkButtonTelegram();
        setLinkButtonSkype();

        setSuggestOpenPositionTable();
        setCandidateCVTable();
    }

    @Subscribe("emailLinkButton")
    public void onEmailLinkButtonClick(Button.ClickEvent event) {
        webBrowserTools.showWebPage("mailto:" + event.getButton().getCaption(), null);
    }

    @Subscribe("telegramLinkButton")
    public void onTelegramLinkButtonClick(Button.ClickEvent event) {
        String retStr = event.getButton().getCaption();

        if (retStr.charAt(0) != '@') {
            webBrowserTools.showWebPage("http://t.me/" + retStr, null);
        } else {
            retStr = retStr.substring(1);
            webBrowserTools.showWebPage("http://t.me/" + retStr.substring(1, retStr.length() - 1), null);
        }
    }

    private void setLinkButtonEmail() {
        if (selectedJobCandidate != null) {
            if (selectedJobCandidate.getEmail() != null) {
                emailLinkButton.setCaption(selectedJobCandidate.getEmail());
                emailLabel.setVisible(true);
                emailLinkButton.setVisible(true);
            } else {
                emailLabel.setVisible(false);
                emailLinkButton.setVisible(false);
            }
        }
    }

    private void setLinkButtonPhone() {
        if (selectedJobCandidate != null) {
            if (selectedJobCandidate.getPhone() != null) {
                phoneLinkButton.setCaption(selectedJobCandidate.getPhone());
                phoneLabel.setVisible(true);
                phoneLinkButton.setVisible(true);
            } else {
                phoneLabel.setVisible(false);
                phoneLinkButton.setVisible(false);
            }
        }
    }

    private void setLinkButtonTelegram() {
        if (selectedJobCandidate != null) {
            if (selectedJobCandidate.getTelegramName() != null) {
                telegramLinkButton.setCaption(selectedJobCandidate.getTelegramName());
                telegramLabel.setVisible(true);
                telegramLinkButton.setVisible(true);
            } else {
                telegramLabel.setVisible(false);
                telegramLinkButton.setVisible(false);
            }
        }
    }

    private void setLinkButtonSkype() {
        if (selectedJobCandidate != null) {
            if (selectedJobCandidate.getSkypeName() != null) {
                skypeLinkButton.setCaption(selectedJobCandidate.getSkypeName());
                skypeLabel.setVisible(true);
                skypeLinkButton.setVisible(true);
            } else {
                skypeLabel.setVisible(false);
                skypeLinkButton.setVisible(false);
            }
        }
    }

    @Subscribe("skypeLinkButton")
    public void onSkypeLinkButtonClick(Button.ClickEvent event) {
        webBrowserTools.showWebPage("skype:" + event.getButton().getCaption() + "?chat", null);
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

    public Component lastInteractionGeneratorColumn(Entity entity) {
        Label retLabel = uiComponents.create(Label.NAME);
        retLabel.setAlignment(Component.Alignment.MIDDLE_LEFT);

        if (selectedJobCandidate != null) {
            OpenPosition openPosition = entity.getValue("vacancy");
            IteractionList lastInteraction = getLastInteraction(selectedJobCandidate, openPosition);

            if (lastInteraction != null) {
                if (lastInteraction.getIteractionType() != null) {
                    if (lastInteraction.getIteractionType().getIterationName() != null) {
                        StringBuffer retStr = new StringBuffer(lastInteraction.getIteractionType().getIterationName());
                        retLabel.setValue(lastInteraction.getIteractionType().getIterationName());
                        retLabel.setDescription(lastInteraction.getIteractionType().getIterationName());
                    }
                }
            }
        }

        return retLabel;
    }

    private IteractionList getLastInteraction(JobCandidate candidate, OpenPosition openPosition) {
        IteractionList lastInteraction = null;

        if (candidate.getIteractionList().size() != 0) {
            for (int i = 0; i < candidate.getIteractionList().size(); i++) {
                if (lastInteraction != null) {
                    if (openPosition.equals(candidate.getIteractionList()
                            .get(i)
                            .getVacancy())) {
                        if (lastInteraction.getDateIteraction().before(candidate
                                .getIteractionList()
                                .get(i)
                                .getDateIteraction())) {
                            lastInteraction = candidate.getIteractionList()
                                    .get(i);
                        }
                    }
                } else {
                    if (openPosition.equals(candidate.getIteractionList()
                            .get(i)
                            .getVacancy())) {
                        lastInteraction = candidate.getIteractionList()
                                .get(i);
                    }
                }
            }
        }

        return lastInteraction;
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
                                retLabel.setDescription(iteractionList.getRecrutier().getName());
                            }
                        }
                    }
                }
            }
        }

        return retLabel;
    }

    private void setupSkillBox() {
        if (skillBoxFragment != null) {
            skillBox.remove(skillBoxFragment.getFragment());
        }
        if (!PersistenceHelper.isNew(selectedJobCandidate)) {
            skillBoxFragment = fragments.create(this, Skillsbar.class);
            if (skillBoxFragment.generateSkillLabels(getLastCVText(selectedJobCandidate))) {
                skillBox.add(skillBoxFragment.getFragment());
            }
        }
    }

    private String getLastCVText(JobCandidate singleSelected) {
        if (singleSelected != null) {
            if (singleSelected.getCandidateCv().size() != 0) {
                CandidateCV lastCV = singleSelected.getCandidateCv().get(0);

                for (CandidateCV candidateCV : singleSelected.getCandidateCv()) {
                    if (lastCV.getDatePost().before((candidateCV.getDatePost()))) {
                        lastCV = candidateCV;
                    }
                }

                return lastCV.getTextCV();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private void setSuggestOpenPositionTable() {
        List<Position> positions = new ArrayList<>();

        if (selectedJobCandidate != null)
            if (selectedJobCandidate.getPositionList() != null) {
                for (JobCandidatePositionLists positionLists : selectedJobCandidate.getPositionList()) {
                    positions.add(positionLists.getPositionList());
                }

                suggestOpenPositionDl.setParameter("positionType", selectedJobCandidate.getPersonPosition());
                if (positions.size() > 0) {
                    suggestOpenPositionDl.setParameter("positionTypes", positions);
                }
                suggestOpenPositionDl.load();
            }

        suggestVacancyTable.addStyleName("borderless");
        suggestVacancyTable.addStyleName("no-horizontal-lines");
        suggestVacancyTable.addStyleName("no-vertical-lines");
    }

    @Install(to = "suggestVacancyTable.notSendedIconColumn", subject = "columnGenerator")
    private Component suggestVacancyTableNotSendedIconColumnColumnGenerator(OpenPosition openPosition) {
        String retStr = "font-icon:CHECK";
        String retStyle = "h2-green";
        String retDescriplion = "<b>Можно начинать процесс с кандидатом.</b><br> Кандидату не предлагали эту вакансию.";

        Label retIcon = uiComponents.create(Label.class);

        if (selectedJobCandidate != null) {
            for (IteractionList list : selectedJobCandidate.getIteractionList()) {
                if (openPosition.equals(list.getVacancy())) {
                    if (list.getIteractionType() != null) {
                        if (list.getIteractionType().getSignSendToClient() != null ?
                                list.getIteractionType().getSignSendToClient() != null : false) {
                            if (list.getIteractionType().getSignSendToClient()) {
                                retStr = "font-icon:REFRESH";
                                retStyle = "h2-blue";
                                retDescriplion = "<b>Можно послать еще раз.</b><br> Резюме отправлено клиенту, но не было ответа";
                                break;
                            }
                        }

                        if (list.getIteractionType().getSignEndCase() != null ?
                                list.getIteractionType().getSignEndCase() : false) {
                            retStr = "font-icon:CLOSE";
                            retStyle = "h2-red";
                            retDescriplion = "<b>Слать резюме не рекомендуется.</b><br> Процесс с заказчиком закончен.";
                            break;
                        }

                        retStr = "font-icon:QUESTION";
                        retStyle = "h2-orange";
                        retDescriplion = "<b>Можно выслать заказчику.</b><br> Процесс с кандидатом начат, но резюме не отослали.";
                    }
                }
            }

            retIcon.setIcon(retStr);
            retIcon.setAlignment(Component.Alignment.MIDDLE_CENTER);
            retIcon.setStyleName(retStyle);
            retIcon.setDescriptionAsHtml(true);
            retIcon.setDescription(retDescriplion);
        }

        return retIcon;
    }

    @Install(to = "suggestVacancyTable", subject = "itemDescriptionProvider")
    private String suggestVacancyTableItemDescriptionProvider(OpenPosition openPosition, String string) {
        String retStr = "<b>Вакансия:</b><br><br>";

        retStr += "<i>" + openPosition.getVacansyName() + "</i><br>"
                + "<i>Проект: </i>" + openPosition.getProjectName().getProjectName()
                + "<br><i>Ответственный за проект у заказчика:</i>"
                + openPosition.getProjectName().getProjectOwner().getSecondName()
                + openPosition.getProjectName().getProjectOwner().getSecondName()
                + "<br><i>Ответственный за проект на нашей стороне: </i>"
                + openPosition.getOwner().getName()
                + "<br><i>Дата открытия вакансии: "
                + openPosition.getLastOpenDate()
                + "<br><br><i>Описание вакансии: </i><br>" + openPosition.getComment();

        return retStr;
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
                                retLabel.setDescription(iteractionList.getRecrutier().getName());
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

    public void candidateCardButton() {
        screenBuilders.editor(JobCandidate.class, this)
                .editEntity(selectedJobCandidate)
                .withOpenMode(OpenMode.NEW_TAB)
                .show();
    }

    public void candidateCVSimpleBrowseButton() {
        CandidateCVSimpleBrowse candidateCVSimpleBrowse = screens.create(CandidateCVSimpleBrowse.class);
        candidateCVSimpleBrowse.setSelectedCandidate(selectedJobCandidate);
        candidateCVSimpleBrowse.setJobCandidate(selectedJobCandidate);
        screens.show(candidateCVSimpleBrowse);
    }

    public void candidateInteractionsButton() {
        IteractionListSimpleBrowse iteractionListSimpleBrowse = screens.create(IteractionListSimpleBrowse.class);
        iteractionListSimpleBrowse.setSelectedCandidate(selectedJobCandidate);
        iteractionListSimpleBrowse.setJobCandidate(selectedJobCandidate);
        screens.show(iteractionListSimpleBrowse);
    }

    public Component countCVCandidate(Entity entity) {
        Label retLabel = uiComponents.create(Label.NAME);

        if (selectedJobCandidate != null) {
            int counter = 0;

            for (int i = 0; i < selectedJobCandidate.getCandidateCv().size(); i++) {
                if (entity.equals(selectedJobCandidate.getCandidateCv().get(i))) {
                    counter = i;
                    break;
                }
            }

            retLabel.setValue(++counter);
        }

        return retLabel;
    }

    public Component addViewCVButton(Entity entity) {
        Button retButton = uiComponents.create(Button.NAME);
        retButton.setCaption("Просмотр");

        retButton.setAction(new BaseAction("viewCV")
                .withHandler(actionPerformedEvent -> {
                    screenBuilders.editor(CandidateCV.class, this)
                            .editEntity(candidatesCVTable.getSingleSelected())
                            .withOpenMode(OpenMode.NEW_TAB)
                            .show();
                }));

        return retButton;
    }

    @Install(to = "lastProjectTable", subject = "itemDescriptionProvider")
    private String lastProjectTableItemDescriptionProvider(Object object, String string) {
        if (string.equals("vacancy")) {
            return ((OpenPosition) object).getVacansyName();
        }

        return null;
    }

    @Install(to = "lastProjectTable", subject = "styleProvider")
    protected String lastProjectTableStyleProvider(IteractionList iteractionList, String property) {
        if (property.equals("lastInteraction")) {
            if (iteractionList.getIteractionType().getSignOurInterview() ||
                    iteractionList.getIteractionType().getSignClientInterview()) {
                return "label_table_red";
            }

            if (iteractionList.getIteractionType().getSignOurInterviewAssigned()) {
                return "label_table_orange";
            }

            if (iteractionList.getIteractionType().getSignSendToClient()) {
                return "label_table_green";
            }
        }

        return null;
    }

    public Component statusInteractions(Entity entity) {
        Label retLabel = uiComponents.create(Label.NAME);
        retLabel.setAlignment(Component.Alignment.MIDDLE_LEFT);

        if (selectedJobCandidate != null) {
            OpenPosition openPosition = entity.getValue("vacancy");
            IteractionList lastInteraction = getLastInteraction(selectedJobCandidate, openPosition);

            if (lastInteraction != null) {
                Boolean flag = false;
                for (IteractionList iteractionList : selectedJobCandidate.getIteractionList()) {
                    if (entity.getValue("vacancy").equals(iteractionList.getVacancy())) {
                        if (iteractionList.getIteractionType().getSignEndCase()) {
                            flag = true;
                            break;
                        }
                    }
                }

                if (lastInteraction.getIteractionType() != null) {
                    if (flag) {
//                    if (lastInteraction.getIteractionType().getSignEndCase()) {
                        retLabel.setIcon(CubaIcon.CLOSE.source());
                        retLabel.setStyleName("open-position-pic-center-large-red");
                        retLabel.setDescription(CASE_IS_CLOSED);
                    } else {
                        retLabel.setIcon(CubaIcon.YES.source());
                        retLabel.setStyleName("open-position-pic-center-large-green");
                        retLabel.setDescription(CASE_IS_OPENED);
                    }
                }
            }
        }

        return retLabel;
    }

    public Component addOpenPositionViewButton(Entity entity) {
        Button retButton = uiComponents.create(Button.NAME);
        retButton.setCaption("Просмотр");

        retButton.setAction(new BaseAction("viewVacancy")
                .withHandler(actionPerformedEvent -> {
                    screenBuilders.editor(OpenPosition.class, this)
                            .editEntity(suggestVacancyTable.getSingleSelected())
                            .withOpenMode(OpenMode.NEW_TAB)
                            .show();
                }));

        return retButton;
    }

    public Component addVacancyNameColumn(Entity entity) {
        Label retLabel = uiComponents.create(Label.class);

        retLabel.setValue(((OpenPosition) entity.getValue("vacancy")).getVacansyName());
        retLabel.setDescription(((OpenPosition) entity.getValue("vacancy")).getVacansyName());

        return retLabel;
    }

    public Component addOpenClosePosition(Entity entity) {
        HBoxLayout hBoxLayout = uiComponents.create(HBoxLayout.class);
        Label retLabel = uiComponents.create(Label.class);

        if (((OpenPosition) entity.getValue("vacancy")).getOpenClose() != null) {
            if (((OpenPosition) entity.getValue("vacancy")).getOpenClose()) {
                retLabel.setIcon(CubaIcon.MINUS_CIRCLE.source());
                retLabel.setStyleName("label_table_red");
                retLabel.setDescription(DESCRIPTION_VACANCY_CLOSE);

            } else {
                retLabel.setIcon(CubaIcon.PLUS_CIRCLE.source());
                retLabel.setStyleName("label_table_green");
                retLabel.setDescription(DESCRIPTION_VACANCY_OPEN);

            }
        } else {
            retLabel.setValue(CubaIcon.PLUS_CIRCLE);
            retLabel.setStyleName("label_table_green");
            retLabel.setDescription(DESCRIPTION_VACANCY_OPEN);
        }

        hBoxLayout.add(retLabel);
        hBoxLayout.setHeightFull();
        hBoxLayout.setWidthFull();
        return hBoxLayout;
    }
}