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
import com.haulmont.cuba.gui.components.data.value.ContainerValueSource;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.model.*;
import com.haulmont.cuba.gui.screen.*;
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
    private CollectionLoader<CandidateCV> candidateCVDl;
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

        candidatesCVTable.addStyleName("borderless");
        candidatesCVTable.addStyleName("no-horizontal-lines");
        candidatesCVTable.addStyleName("no-vertical-lines");

        suggestVacancyTable.addStyleName("borderless");
        suggestVacancyTable.addStyleName("no-horizontal-lines");
        suggestVacancyTable.addStyleName("no-vertical-lines");

    }

    private void setCandidateEntityLabels() {
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

        candidateCountLabel.setValue("Кандидатов: "
                + String.valueOf(setList.size()));
    }

    @Install(to = "jobCandidateTable.candidatePhoto", subject = "columnGenerator")
    private Component jobCandidateTableCandidatePhotoColumnGenerator(DataGrid.ColumnGeneratorEvent<IteractionList> event) {

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
//                String address = "https://st3.depositphotos.com/11953928/35822/v/450/depositphotos_358227294-stock-illustration-teen-with-laptop-computer-home.jpg";

                String address = "icons/no-programmer.jpg";
                URL url = null;

                try {
                    url = new URL(address);
                } catch (MalformedURLException g) {
                    g.printStackTrace();
                }

//                retImage.setSource(UrlResource.class).setUrl(url);
                retImage.setSource(ThemeResource.class).setPath(address);
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

        candidateCityLocationLabel.setValue("(" + selectedJobCandidate.getCityOfResidence().getCityRuName() + ")");

        lastProjectDl.setParameter("candidate", iteractionList.getCandidate());
        lastProjectDl.load();

        candidateCVDl.setParameter("candidate", iteractionList.getCandidate());
        candidateCVDl.load();

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

        setupSkillBox();

        lastProjectTable.addStyleName("borderless");
        lastProjectTable.addStyleName("no-horizontal-lines");
        lastProjectTable.addStyleName("no-vertical-lines");

        setLinkButtonEmail();
        setLinkButtonPhone();
        setLinkButtonTelegram();
        setLinkButtonSkype();
        setSuggestOpenPositionTable();
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
                telegramLabel.setVisible(false);
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
                skypeLabel.setVisible(false);
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

    private JobCandidate selectedJobCandidate;

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

    private Skillsbar skillBoxFragment = null;

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

    @Install(to = "lastProjectTable", subject = "styleProvider")
    protected String lastProjectTableStyleProvider(IteractionList iteractionList, String property) {
        return null;
    }

    public Component statusInteractions(Entity entity) {
        Label retLabel = uiComponents.create(Label.NAME);
        retLabel.setAlignment(Component.Alignment.MIDDLE_LEFT);

        if (selectedJobCandidate != null) {
            OpenPosition openPosition = entity.getValue("vacancy");
            IteractionList lastInteraction = getLastInteraction(selectedJobCandidate, openPosition);

            if (lastInteraction != null) {
                if (lastInteraction.getIteractionType() != null) {
                    if (lastInteraction.getIteractionType().getSignEndCase()) {
                        retLabel.setIcon(CubaIcon.CLOSE.source());
                        retLabel.setStyleName("open-position-pic-center-large-red");
                    } else {
                        retLabel.setIcon(CubaIcon.YES.source());
                        retLabel.setStyleName("open-position-pic-center-large-green");
                    }
                }
            }
        }

        return retLabel;
    }
}