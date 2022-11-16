package com.company.itpearls.web.screens.iteractionlist;

import com.company.itpearls.entity.CandidateCV;
import com.company.itpearls.entity.IteractionList;
import com.company.itpearls.entity.OpenPosition;
import com.company.itpearls.service.GetRoleService;
import com.company.itpearls.web.StandartRoles;
import com.company.itpearls.web.screens.candidatecv.CandidateCVSimpleBrowse;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.Screens;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.components.data.TableItems;
import com.haulmont.cuba.gui.components.data.ValueSource;
import com.haulmont.cuba.gui.components.data.datagrid.ContainerDataGridItems;
import com.haulmont.cuba.gui.components.data.table.ContainerTableItems;
import com.haulmont.cuba.gui.components.data.value.ContainerValueSource;
import com.haulmont.cuba.gui.export.FileDataProvider;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.model.*;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.JobCandidate;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.net.MalformedURLException;
import java.net.URL;
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
    //    @Inject
//    private ScrollBoxLayout labelndidatesScrollBox;
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

    private CollectionContainer<CandidateCV> candidateCVDc;
    @Inject
    private DataGrid<IteractionList> jobCandidateTable;

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

        jobCandidateTable.addStyleName("borderless");
        jobCandidateTable.addStyleName("no-horizontal-lines");
        jobCandidateTable.addStyleName("no-vertical-lines");
    }

    private void createCollectionContainerCandidateCVTable() {
        candidateCVDc = dataComponents.createCollectionContainer(CandidateCV.class);

        if (selectedJobCandidate != null) {
            for (CandidateCV candidateCV : selectedJobCandidate.getCandidateCv()) {
            }
        }

    }

    private CollectionContainer<IteractionList> iteractionListNewDc;

    private void setCandidateEntityLabels() {
/*        for (Component c : labelndidatesScrollBox.getComponents()) {
            labelndidatesScrollBox.remove(c);
        } */

/*        if (iteractionListNewDc != null) {
            for (int i = 0; i < iteractionListNewDc.getItems().size(); i++) {
                iteractionListNewDc.getItems().remove(i);
            }
        } */

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

            if (flag) {
                setList.add(il1);
            }
        }

        iteractionListNewDc = dataComponents.createCollectionContainer(IteractionList.class);
        iteractionListNewDc.setItems(setList);
        jobCandidateTable.setItems(new ContainerDataGridItems<>(iteractionListNewDc));

        for (IteractionList iteractionList : setList) {
            Button candidateButton = uiComponents.create(Button.NAME);

            candidateButton.setCaption(
                    "<b>"
                            + iteractionList.getCandidate().getFullName()
                            + "</b><br><i>"
                            + iteractionList.getCandidate().getPersonPosition().getPositionRuName()
                            + " / "
                            + iteractionList.getCandidate().getPersonPosition().getPositionEnName()
                            + "</i>");
            candidateButton.setStyleName("label_button_green");
            candidateButton.setCaptionAsHtml(true);
            candidateButton.setWidthFull();
            candidateButton.setHeightAuto();
            candidateButton.addClickListener(e -> {
                setCandidateCard(iteractionList);
            });
//            labelndidatesScrollBox.add(candidateButton);
        }

        candidateCountLabel.setValue("Кандидатов: "
                + String.valueOf(setList.size()));
    }

    @Install(to = "jobCandidateTable.candidatePhoto", subject = "columnGenerator")
    private Component jobCandidateTableCandidatePhotoColumnGenerator(DataGrid.ColumnGeneratorEvent<IteractionList> event) {
        /*
                    if (customer.getAvatar() != null) {
                Image avatarImage = uiComponents.create(Image.class);
                avatarImage.setScaleMode(ScaleMode.SCALE_DOWN);
                avatarImage.setAlignment(Alignment.MIDDLE_CENTER);
                avatarImage.setWidth("100%");
                avatarImage.setHeight("30px");

                avatarImage.setValueSource(new ContainerValueSource<>(table.getInstanceContainer(customer), "avatar"));
                return avatarImage;
            }
            return null;
        */

        Image retImage = uiComponents.create(Image.NAME);
        retImage.setScaleMode(Image.ScaleMode.SCALE_DOWN);
        retImage.setAlignment(Component.Alignment.MIDDLE_CENTER);
        retImage.setWidth("100%");
        retImage.setHeight("60px");

        if (event.getItem().getCandidate().getFileImageFace() != null) {
            try {
                retImage.setValueSource(
                        new ContainerValueSource<>(event.getContainer(), "candidate.fileImageFace"));
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                String address = "https://st3.depositphotos.com/11953928/35822/v/450/depositphotos_358227294-stock-illustration-teen-with-laptop-computer-home.jpg";

                URL url = null;

                try {
                    url = new URL(address);
                } catch (MalformedURLException g) {
                    g.printStackTrace();
                }

                retImage.setSource(UrlResource.class).setUrl(url);
            }
        } else {
            String address = "https://st3.depositphotos.com/11953928/35822/v/450/depositphotos_358227294-stock-illustration-teen-with-laptop-computer-home.jpg";

            URL url = null;

            try {
                url = new URL(address);
            } catch (MalformedURLException g) {
                g.printStackTrace();
            }

            retImage.setSource(UrlResource.class).setUrl(url);
        }

        return retImage;
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

/*    @Install(to = "jobCandidateTable.candidateCard", subject = "columnGenerator")
    private Component jobCandidateTableCandidateCardColumnGenerator(DataGrid.ColumnGeneratorEvent<IteractionList> event) {
        VBoxLayout retBox = uiComponents.create(VBoxLayout.NAME);
        Label nameLabel = uiComponents.create(Label.NAME);
        Label positionLabel = uiComponents.create(Label.NAME);

        nameLabel.setValue(event.getItem().getCandidate().getFullName());
        positionLabel.setValue(event.getItem().getCandidate().getPersonPosition().getPositionRuName()
                + " / "
                + event.getItem().getCandidate().getPersonPosition().getPositionEnName());

        nameLabel.setStyleName("h3");
        positionLabel.setStyleName("h4");

        retBox.add(nameLabel);
        retBox.add(positionLabel);

        return retBox;
    } */

    @Subscribe("jobCandidateTable")
    public void onJobCandidateTableSelection1(DataGrid.SelectionEvent<IteractionList> event) {
        setCandidateCard(jobCandidateTable.getSingleSelected());
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

        try {
            FileDescriptorResource fileDescriptorResource = candidatePic.createResource(FileDescriptorResource.class)
                    .setFileDescriptor(selectedJobCandidate.getFileImageFace());

            candidatePic.setSource(fileDescriptorResource);
            candidatePic.setScaleMode(Image.ScaleMode.SCALE_DOWN);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            String address = "https://st3.depositphotos.com/11953928/35822/v/450/depositphotos_358227294-stock-illustration-teen-with-laptop-computer-home.jpg";

            URL url = null;

            try {
                url = new URL(address);
            } catch (MalformedURLException g) {
                g.printStackTrace();
            }

            candidatePic.setSource(UrlResource.class).setUrl(url);
        }

        lastProjectTable.addStyleName("borderless");
        lastProjectTable.addStyleName("no-horizontal-lines");
        lastProjectTable.addStyleName("no-vertical-lines");
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
        retLabel.setAlignment(Component.Alignment.MIDDLE_LEFT);

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

            if (lastInteraction != null) {
                if (lastInteraction.getIteractionType() != null) {
                    if (lastInteraction.getIteractionType().getIterationName() != null) {
                        StringBuffer retStr = new StringBuffer(lastInteraction.getIteractionType().getIterationName());
                        retLabel.setValue(lastInteraction.getIteractionType().getIterationName());
                    }
                }
            }

//            retLabel.setValue(retStr);
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
        int counter = 0;

        for (int i = 0; i < selectedJobCandidate.getCandidateCv().size(); i++) {
            if (entity.equals(selectedJobCandidate.getCandidateCv().get(i))) {
                counter = i;
                break;
            }
        }

        retLabel.setValue(++counter);

        return retLabel;
    }
}