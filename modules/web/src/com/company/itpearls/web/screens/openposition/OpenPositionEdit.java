package com.company.itpearls.web.screens.openposition;

import com.company.itpearls.UiNotificationEvent;
import com.company.itpearls.entity.*;
import com.company.itpearls.service.GetRoleService;
import com.company.itpearls.web.screens.position.PositionEdit;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.Screens;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.model.DataContext;
import com.haulmont.cuba.gui.model.InstanceLoader;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import javax.inject.Named;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;


@UiController("itpearls_OpenPosition.edit")
@UiDescriptor("open-position-edit.xml")
@EditedEntityContainer("openPositionDc")
@LoadDataBeforeShow
public class OpenPositionEdit extends StandardEditor<OpenPosition> {
    @Inject
    private LookupPickerField<City> cityOpenPositionField;
    @Inject
    private LookupPickerField<CompanyDepartament> companyDepartamentField;
    @Inject
    private LookupPickerField<Company> companyNameField;
    @Inject
    private TextField<Integer> numberPositionField;
    @Inject
    private LookupPickerField<Position> positionTypeField;
    @Inject
    private CheckBox needExerciseCheckBox;
    @Inject
    private RichTextArea exerciseRichTextArea;
    @Inject
    private LookupPickerField<Project> projectNameField;
    @Inject
    private TextField<String> vacansyNameField;
    @Inject
    private Notifications notifications;
    @Inject
    private LookupField<Integer> priorityField;
    @Inject
    private CheckBox openClosePositionCheckBox;
    @Inject
    private DataManager dataManager;
    @Inject
    private ScreenBuilders screenBuilders;

    private Boolean booOpenClosePosition = false;
    private Boolean entityIsChanged = false;
    private EmailInfo emailInfo;
    private String emails = "";
    private Boolean setOK;
    private static final String MANAGEMENT_GROUP = "Менеджмент";
    private static final String HUNTING_GROUP = "Хантинг";

    @Inject
    private Dialogs dialogs;
    private boolean r;
    @Inject
    private Events events;
    @Inject
    private GetRoleService getRoleService;
    @Inject
    private UserSession userSession;
    @Inject
    private RadioButtonGroup radioButtonGroupPaymentsType;
    @Inject
    private RadioButtonGroup radioButtonGroupResearcherSalary;
    @Inject
    private RadioButtonGroup radioButtonGroupRecrutierSalary;
    @Inject
    private GroupBoxLayout groupBoxPaymentsDetail;
    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private GroupBoxLayout groupBoxPaymentsResearcher;
    @Inject
    private GroupBoxLayout groupBoxPaymentsRecrutier;
    @Inject
    private TextField<String> textFieldPercentOrSum;
    @Inject
    private TextField<String> textFieldCompanyPayment;
    @Inject
    private TextField<BigDecimal> openPositionFieldSalaryMin;
    @Inject
    private TextField<BigDecimal> openPositionFieldSalaryMax;
    @Inject
    private CheckBox checkBoxUseNDFL;
    @Inject
    private TextField<String> textFieldResearcherSalaryPercentOrSum;
    @Inject
    private TextField<String> textFieldResearcherSalary;
    @Inject
    private TextField<String> textFieldRecrutierPercentOrSum;
    @Inject
    private TextField<String> textFieldRecrutierSalary;
    @Inject
    private Label<String> labelResearcherSalary;
    @Inject
    private Label<String> labelRecrutierSalary;
    @Inject
    private CollectionLoader<Project> projectNamesLc;
    @Inject
    private CollectionLoader<CompanyDepartament> companyDepartamentsLc;
    @Inject
    private LookupField<Integer> remoteWorkField;
    @Inject
    private Label<String> labelOpenPosition;
    @Inject
    private Label<String> labelTopComissionResearcher;
    @Inject
    private Label<String> labelTopComissionRecrutier;

    static String RESEARCHER = "Researcher";
    static String RECRUITER = "Recruiter";
    static String MANAGER = "Manager";
    static String ADMINISTRATOR = "Administrators";
    static String QUERY_SELECT_COMMAND = "select e from itpearls_OpenPosition e where e.parentOpenPosition = :parentOpenPosition and e.openClose = false";
    private OpenPosition beforeEdit = null;

    @Inject
    private RadioButtonGroup workExperienceRadioButton;
    @Inject
    private RadioButtonGroup commanExperienceRadioButton;
    @Inject
    private CheckBox internalProjectCheckBox;
    @Inject
    private DataContext dataContext;
    @Inject
    private CollectionLoader<Position> positionTypesLc;
    @Inject
    private Screens screens;
    @Inject
    private RadioButtonGroup commandOrPosition;
    @Inject
    private LookupPickerField<OpenPosition> parentOpenPositionField;
    @Inject
    private Label<String> citiesLabel;
    @Named("tabSheetOpenPosition.tabPayments")
    private VBoxLayout tabPayments;
    @Inject
    private CollectionLoader<OpenPosition> openPositionParentDl;
    @Inject
    private InstanceLoader<OpenPosition> openPositionDl;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {

        beforeEdit = getEditedEntity();
        booOpenClosePosition = getEditedEntity().getOpenClose();
        parentOpenPositionField.setEditable(commandOrPosition.getValue() != null);

        // проверка на ноль
        booOpenClosePosition = booOpenClosePosition == null ? false : booOpenClosePosition;

        setTopLabel();
        setInternalProject();
        setHiddeField();
        setDisableTwoField();
        setWorkExperienceRadioButton();
        setCommandExperienceRadioButton();
        changeCityListsLabel();
    }

    private void setInternalProject() {
        if (getRoleService.isUserRoles(userSession.getUser(), MANAGER) ||
                getRoleService.isUserRoles(userSession.getUser(), ADMINISTRATOR)) {
            internalProjectCheckBox.setVisible(true);
        } else {
            internalProjectCheckBox.setVisible(false);
        }
    }

    private void setCommandExperienceRadioButton() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("Нет требований", 0);
        map.put("Без опыта", 1);
        map.put("1 год", 2);
        map.put("3 года", 3);
        map.put("5 лет и более", 4);
        map.put("Управление командой", 5);

        commanExperienceRadioButton.setOptionsMap(map);
    }

    private void setWorkExperienceRadioButton() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("Нет требований", 0);
        map.put("Без опыта", 1);
        map.put("1 год", 2);
        map.put("2 года", 3);
        map.put("3 года", 4);
        map.put("5 лет и более", 5);

        workExperienceRadioButton.setOptionsMap(map);
    }

    @Subscribe("needExerciseCheckBox")
    public void onNeedExerciseCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (needExerciseCheckBox.getValue() != null) {
            exerciseRichTextArea.setEditable(needExerciseCheckBox.getValue());
            exerciseRichTextArea.setRequired(needExerciseCheckBox.getValue());
        } else {
            exerciseRichTextArea.setRequired(false);
            exerciseRichTextArea.setEditable(false);
        }
    }

    private void setDisableTwoField() {
    }

    @Subscribe("vacansyNameField")
    public void onVacansyNameFieldValueChange(HasValue.ValueChangeEvent<String> event) {
        setTopLabel();
    }

    @Subscribe("openPositionFieldSalaryMin")
    public void onOpenPositionFieldSalaryMinValueChange(HasValue.ValueChangeEvent<BigDecimal> event) {
        setCalculateCompanyPercentField();
        calculateResearcherSalary();
        calculateRecrutierSalary();
    }

    @Subscribe("openPositionFieldSalaryMax")
    public void onOpenPositionFieldSalaryMaxValueChange(HasValue.ValueChangeEvent<BigDecimal> event) {
        setCalculateCompanyPercentField();
        calculateResearcherSalary();
        calculateRecrutierSalary();
    }

    private void setCompanyDepartmentFromProject() {
        if (projectNameField.getValue() != null) {
            companyDepartamentField.setValue(projectNameField.getValue().getProjectDepartment());
        }
    }

    private void setCompanyNameFromDepartment() {
        if (companyDepartamentField.getValue() != null) {
            companyNameField.setValue(companyDepartamentField.getValue().getCompanyName());
        }
    }

    @Subscribe("companyDepartamentField")
    public void onCompanyDepartamentFieldValueChange(HasValue.ValueChangeEvent<CompanyDepartament> event) {
        // сократить список проектов
        if (projectNameField.getValue() == null) {
            if (companyDepartamentField.getValue() != null) {
                projectNamesLc.setParameter("department", companyDepartamentField.getValue());
            } else {
                setCompanyNameFromDepartment();
                setPersonTableEmpty();
            }
        } else {
            projectNamesLc.removeParameter("department");

            setCompanyNameFromDepartment();
            setPersonTableEmpty();
        }

        projectNamesLc.load();
        setTopLabel();
    }

    private void setPersonTableEmpty() {
        if (PersistenceHelper.isNew(getEditedEntity())) {
//            personTable.setVisible(false);
        } else {
            if (companyDepartamentField.getValue() != null) {
//                personsDl.setParameter("companyDepartment", companyDepartamentField.getValue());
//                personsDl.load();

//                personTable.setVisible(true);
            }
        }
    }

    private void setCityNameOfCompany() {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            if (companyNameField.getValue() != null && cityOpenPositionField.getValue() == null) {
                if (companyDepartamentField.getValue().getCompanyName() != null) {
                    if (companyDepartamentField.getValue().getCompanyName().getCityOfCompany() != null)
                        cityOpenPositionField.setValue(companyDepartamentField.getValue().getCompanyName().getCityOfCompany());
                }
            }
        }
    }

    @Subscribe("companyNameField")
    public void onCompanyNameFieldValueChange(HasValue.ValueChangeEvent<Company> event) {
        // сократить список департаментов
        if (companyNameField.getValue() != null) {
            companyDepartamentsLc.setParameter("company", companyNameField.getValue());
        } else {
            companyDepartamentsLc.removeParameter("company");
        }

        setCityNameOfCompany();
        companyDepartamentsLc.load();

        setTopLabel();
    }

    @Subscribe("commandOrPosition")
    public void onCommandOrPositionValueChange(HasValue.ValueChangeEvent event) {
        switch ((int) commandOrPosition.getValue()) {
            case 0: // вакансия
                parentOpenPositionField.setEditable(true);
                openPositionFieldSalaryMin.setEditable(true);
                openPositionFieldSalaryMax.setEditable(true);
                tabPayments.setEnabled(false);
                tabPayments.setVisible(false);
                numberPositionField.setCaption("Количество персонала");
                break;
            case 1: // команда
                parentOpenPositionField.setEditable(false);
                openPositionFieldSalaryMax.setEditable(false);
                openPositionFieldSalaryMin.setEditable(false);
                tabPayments.setEnabled(true);
                tabPayments.setVisible(true);
                numberPositionField.setCaption("Количество команд");
                break;
        }
    }

    @Subscribe("parentOpenPositionField")
    public void onParentOpenPositionFieldValueChange(HasValue.ValueChangeEvent<OpenPosition> event) {
        if(projectNameField.getValue() == null
                && parentOpenPositionField.getValue() != null) {
            projectNameField.setValue(parentOpenPositionField.getValue().getProjectName());
        }
    }

    @Subscribe("openClosePositionCheckBox")
    public void onOpenClosePositionCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {

        List<OpenPosition> openPositions = dataManager.load(OpenPosition.class)
                .query(QUERY_SELECT_COMMAND)
                .parameter("parentOpenPosition", getEditedEntity())
                .list();

        String magPos = "";

        if (openPositions.size() != 0) {
            for (OpenPosition a : openPositions) {
                magPos = magPos + "<li><i>" + a.getVacansyName() + "</i></li>";
            }

            dialogs.createOptionDialog()
                    .withType(Dialogs.MessageType.WARNING)
                    .withContentMode(ContentMode.HTML)
                    .withCaption("ВНИМАНИЕ")
                    .withMessage((!getEditedEntity().getOpenClose() ? "Открыть" : "Закрыть") +
                            " вакансии группы?<br><ul>" + magPos + "</ul>")
                    .withActions(new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY).withHandler(e -> {
                        for (OpenPosition a : openPositions) {
                            a.setOpenClose(getEditedEntity().getOpenClose());
                        }
                    }), new DialogAction(DialogAction.Type.NO))
                    .show();

        }

        if (getEditedEntity().getOpenClose()) {
            cityOpenPositionField.setEditable(false);
            companyDepartamentField.setEditable(false);
            companyNameField.setEditable(false);
            numberPositionField.setEditable(false);
            positionTypeField.setEditable(false);
            projectNameField.setEditable(false);
            vacansyNameField.setEditable(false);
            companyDepartamentField.setEditable(false);
        } else {
            cityOpenPositionField.setEditable(true);
            companyDepartamentField.setEditable(true);
            companyNameField.setEditable(true);
            numberPositionField.setEditable(true);
            positionTypeField.setEditable(true);
            projectNameField.setEditable(true);
            vacansyNameField.setEditable(true);
            companyDepartamentField.setEditable(true);

        }
    }

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            getEditedEntity().setOpenClose(false);
        }
    }

    private void sendMessage() {
        events.publish(new UiNotificationEvent(this, "Открыта новая позиция: " +
                getEditedEntity().getVacansyName()));
    }

    private Boolean sendOpenCloseMessage() {
        OpenPosition openPosition = getEditedEntity();
        r = false;

        setOK = getEditedEntity().getOpenClose();

        int a = setOK ? 1 : 0;
        int b = booOpenClosePosition ? 1 : 0;


        // Админ пусть правит без последствий
        if (!getRoleService.isUserRoles(userSession.getUser(), ADMINISTRATOR)) {
            // если что-то изменилось
            if (PersistenceHelper.isNew(getEditedEntity())) {
                emails = getAllSubscibers();
                // позиция открылась
                events.publish(new UiNotificationEvent(this, "Открыта новая позиция: " +
                        getEditedEntity().getVacansyName()));
                r = true;
            } else {
                if (a != b) {
                    if (!getEditedEntity().getOpenClose()) {

                        emails = getAllSubscibers();
                        // позиция открылась
                        events.publish(new UiNotificationEvent(this, "Открыта новая позиция: " +
                                getEditedEntity().getVacansyName()));

                        r = true;
                    } else {
                        setOK = true;
                    }

                    r = true;
                } else
                    r = false;
            }
        }

        return r;
    }

    @Subscribe
    public void onBeforeCommitChanges(BeforeCommitChangesEvent event) {
        OpenPosition dublicateOpenPosition = checkDublicateOpenPosition(event);

        if (dublicateOpenPosition != null && PersistenceHelper.isNew(getEditedEntity())) {
            dialogs.createOptionDialog()
                    .withCaption("ВНИМАНИЕ!")
                    .withMessage("Вакансия " + vacansyNameField.getValue() + "\n" + "уже есть в базе.\n" +
                            "\nОткрыта ранее: " + dublicateOpenPosition.getCreatedBy() +
                            "\nСтатус: " + (dublicateOpenPosition.getOpenClose() ? "Закрыта" : "Открыта" +
                            "\nПродолжить сохранение?"))
                    .withActions(new DialogAction(DialogAction.Type.OK, Action.Status.PRIMARY).withHandler(e -> {
                        event.resume();
                        // вернуться и не закомитить
                    }), new DialogAction(DialogAction.Type.CANCEL).withHandler(f -> {
                        // закончить
                    }))
                    .show();

            event.preventCommit();
        }
    }

    @Subscribe
    public void onAfterCommitChanges(AfterCommitChangesEvent event) {
        if (beforeEdit.getOpenClose() != getEditedEntity().getOpenClose()) {
            if (!getEditedEntity().getOpenClose()) {
                sendOpenPositionMessage();
            } else {
                sendClosePositionMessage();
            }
        }
    }

    private void sendClosePositionMessage() {
        events.publish(new UiNotificationEvent(this, "Закрыта вакансия: " +
                getEditedEntity().getVacansyName()));
    }

    private void sendOpenPositionMessage() {
        events.publish(new UiNotificationEvent(this, "Открыта новая вакансия: " +
                getEditedEntity().getVacansyName()));
    }

    @Subscribe
    public void onBeforeCommitChanges1(BeforeCommitChangesEvent event) {
        if (openClosePositionCheckBox.getValue() == null)
            openClosePositionCheckBox.setValue(false);

        if (internalProjectCheckBox.getValue() == null)
            internalProjectCheckBox.setValue(false);
    }

    private OpenPosition checkDublicateOpenPosition(BeforeCommitChangesEvent event) {
        List<OpenPosition> openPositions = dataManager.load(OpenPosition.class)
                .query("select e from itpearls_OpenPosition e " +
                        "where e.positionType = :positionType " +
                        "and e.vacansyName like :vacansyName " +
                        "and e.projectName = :projectName " +
                        "and e.parentOpenPosition = :parentOpenPosition " +
                        "and e.vacansyName = :vacansyName " +
                        "and e.remoteWork = :remoteWork " +
                        "and e.cityPosition = :cityPosition")
                .parameter("vacansyName", vacansyNameField.getValue())
                .parameter("positionType", positionTypeField.getValue())
                .parameter("projectName", projectNameField.getValue())
                .parameter("cityPosition", cityOpenPositionField.getValue())
                .parameter("parentOpenPosition", parentOpenPositionField.getValue())
                .parameter("remoteWork", remoteWorkField.getValue())
                .parameter("vacansyName", vacansyNameField.getValue())
                .view("openPosition-view")
                .list();

        if (openPositions.size() == 0) {
            return null;
        } else {
            return openPositions.get(0);
        }
    }

    private String getAllSubscibers() {
        LoadContext<User> loadContext = LoadContext.create(User.class)
                .setQuery(LoadContext.createQuery("select e from sec$User e"));

        List<User> listManagers = dataManager.loadList(loadContext);

        String maillist = "";

        for (User user : listManagers) {
            if (user.getEmail() != null && user.getActive())
                maillist = maillist + user.getEmail() + ";";
        }

        return maillist;
    }

    private String getSubscriberMaillist(Entity entity) {
        List<RecrutiesTasks> listResearchers = dataManager.load(RecrutiesTasks.class)
                .query("select e " +
                        "from itpearls_RecrutiesTasks e " +
                        "where e.endDate  >= :currentDate and " +
                        "e.openPosition = :openPosition")
                .parameter("currentDate", new Date())
                .parameter("openPosition", entity)
                .view("recrutiesTasks-view")
                .list();


        String maillist = "";
        Boolean subs = false;

        for (RecrutiesTasks address : listResearchers) {
            String email = address.getReacrutier().getEmail();

            if (address.getSubscribe() == null ? false : address.getSubscribe())
                // if( address.getSubscribe() )
                if (email != null)
                    maillist = maillist + email + ";";
        }

        return maillist;
    }

    private String getRecrutiersMaillist() {
        return "alan@itpearls.ru";
    }

    private void setRadioButtons() {
        Map<String, Integer> priorityMap = new LinkedHashMap<>();

        priorityMap.put("Paused", 0);
        priorityMap.put("Low", 1);
        priorityMap.put("Normal", 2);
        priorityMap.put("High", 3);
        priorityMap.put("Critical", 4);

        priorityField.setOptionsMap(priorityMap);

        Map<String, Integer> paymentsType = new LinkedHashMap<>();
        paymentsType.put("Фиксированная оплата", 0);
        paymentsType.put("Процент от годового оклада", 1);
        paymentsType.put("Процент от месячной зарплаты", 2);

        radioButtonGroupPaymentsType.setOptionsMap(paymentsType);

        Map<String, Integer> researcherSalary = new LinkedHashMap<>();
        researcherSalary.put("Фиксированная комиссия", 0);
        researcherSalary.put("Процент комиссии компании, 20%", 1);
        researcherSalary.put("Процент комиссии компании", 2);

        radioButtonGroupResearcherSalary.setOptionsMap(researcherSalary);

        Map<String, Integer> recrutierSalary = new LinkedHashMap<>();
        recrutierSalary.put("Фиксированная комиссия", 0);
        recrutierSalary.put("Процент комиссии компании, 10%", 1);
        recrutierSalary.put("Процент комиссии компании", 2);

        radioButtonGroupRecrutierSalary.setOptionsMap(recrutierSalary);

        Map<String, Integer> remoteWork = new LinkedHashMap<>();
        remoteWork.put("Нет", 0);
        remoteWork.put("Удаленная работа", 1);
        remoteWork.put("Частично 50/50", 2);

        remoteWorkField.setOptionsMap(remoteWork);
    }

    private void setHiddeField() {
        // скрыть менеджерские пункты
        if (isUserRoles(userSession.getUser(), MANAGER) || isUserRoles(userSession.getUser(), ADMINISTRATOR)) {
            groupBoxPaymentsDetail.setVisible(true);
            groupBoxPaymentsDetail.setCollapsable(true);
            groupBoxPaymentsResearcher.setVisible(true);
            groupBoxPaymentsRecrutier.setVisible(true);
        } else {
            groupBoxPaymentsDetail.setVisible(false);
            groupBoxPaymentsRecrutier.setVisible(false);
            groupBoxPaymentsResearcher.setVisible(false);

            if (isUserRoles(userSession.getUser(), RESEARCHER)) {
                groupBoxPaymentsResearcher.setVisible(true);
            }

            if (isUserRoles(userSession.getUser(), RECRUITER)) {
                groupBoxPaymentsRecrutier.setVisible(true);
            }
        }
    }

    @Subscribe("radioButtonGroupPaymentsType")
    public void onRadioButtonGroupPaymentsTypeValueChange(HasValue.ValueChangeEvent<Integer> event) {

        switch ((int) radioButtonGroupPaymentsType.getValue()) {
            case 0:
                textFieldPercentOrSum.setCaption("Сумма комиссии");
                textFieldPercentOrSum.setVisible(false);
                textFieldCompanyPayment.setVisible(true);
                textFieldCompanyPayment.setEditable(true);
                break;
            case 1:
                textFieldPercentOrSum.setCaption("Процент, %");
                textFieldCompanyPayment.setVisible(true);
                textFieldPercentOrSum.setVisible(true);
                textFieldCompanyPayment.setEditable(false);
                break;
            case 2:
                textFieldPercentOrSum.setCaption("Процент, %");
                textFieldCompanyPayment.setVisible(true);
                textFieldPercentOrSum.setVisible(true);
                textFieldCompanyPayment.setEditable(false);
                break;
        }

        setCalculateCompanyPercentField();

        calculateRecrutierSalary();
        calculateResearcherSalary();
    }

    @Subscribe("checkBoxUseNDFL")
    public void onCheckBoxUseNDFLValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        setCalculateCompanyPercentField();
        calculateResearcherSalary();
        calculateRecrutierSalary();
    }


    @Subscribe("textFieldPercentOrSum")
    public void onTextFieldPercentOrSumValueChange(HasValue.ValueChangeEvent<String> event) {
        setCalculateCompanyPercentField();
        calculateResearcherSalary();
        calculateRecrutierSalary();
    }

    protected void setCalculateCompanyPercentField() {
        if (textFieldPercentOrSum.getValue() != null) {
            textFieldCompanyPayment.setValue(calculateComission(textFieldPercentOrSum.getValue(),
                    (Integer) radioButtonGroupPaymentsType.getValue(),
                    checkBoxUseNDFL.getValue(),
                    openPositionFieldSalaryMin.getValue(),
                    openPositionFieldSalaryMax.getValue())
            );
        }
    }


    protected BigDecimal minCompanyComission = new BigDecimal(BigInteger.ZERO);
    protected BigDecimal maxCompanyComission = new BigDecimal(BigInteger.ZERO);

    protected String calculateComission(String percent, Integer type, boolean ndflFlag, BigDecimal
            minSalary, BigDecimal maxSalary) {

        String retValue = new String("");
        BigDecimal p = new BigDecimal(percent);
        BigDecimal ndfl = new BigDecimal(1.13);
        BigDecimal mounths = new BigDecimal(12);
        BigDecimal hungred = new BigDecimal(100);

        if (minSalary == null)
            minSalary = BigDecimal.ZERO;

        if (maxSalary == null)
            maxSalary = BigDecimal.ZERO;

        switch (type) {
            case 0:
                retValue = percent;
                minSalary = new BigDecimal(percent);
                maxSalary = new BigDecimal(percent);

                break;
            case 1:
                minSalary = minSalary.multiply(p).multiply(mounths).divide(hungred)
                        .multiply(ndflFlag ? ndfl : BigDecimal.ONE);
                maxSalary = maxSalary.multiply(p).multiply(mounths).divide(hungred)
                        .multiply(ndflFlag ? ndfl : BigDecimal.ONE);

                minSalary = minSalary.setScale(0, RoundingMode.HALF_EVEN);
                maxSalary = maxSalary.setScale(0, RoundingMode.HALF_EVEN);

                retValue = "От " +
                        minSalary.toString() +
                        " до " +
                        maxSalary.toString();
                break;
            case 2:
                minSalary = minSalary.multiply(p).multiply(ndflFlag ? ndfl : BigDecimal.ONE).divide(hungred);
                maxSalary = maxSalary.multiply(p).multiply(ndflFlag ? ndfl : BigDecimal.ONE).divide(hungred);

                minSalary = minSalary.setScale(0, RoundingMode.HALF_EVEN);
                maxSalary = maxSalary.setScale(0, RoundingMode.HALF_EVEN);

                retValue = "От " +
                        minSalary.toString() +
                        " до " +
                        maxSalary.toString();
                break;
        }

        minCompanyComission = minSalary;
        maxCompanyComission = maxSalary;

        return retValue;
    }

    @Subscribe("radioButtonGroupResearcherSalary")
    public void onRadioButtonGroupResearcherSalaryValueChange(HasValue.ValueChangeEvent event) {
        calculateResearcherSalary();

        setResearcherSalaryLabel();
    }

    @Subscribe("radioButtonGroupRecrutierSalary")
    public void onRadioButtonGroupRecrutierSalaryValueChange(HasValue.ValueChangeEvent event) {
        calculateRecrutierSalary();

        setRecrutierSalaryLabel();
    }

    protected void calculateResearcherSalary() {
        BigDecimal hungred = new BigDecimal(100);
        BigDecimal minSalary = new BigDecimal(String.valueOf(minCompanyComission));
        BigDecimal maxSalary = new BigDecimal(String.valueOf(maxCompanyComission));
        String textSalaryMessage = null;

        if (radioButtonGroupResearcherSalary.getValue() != null) {
            switch ((int) radioButtonGroupResearcherSalary.getValue()) {
                case 0:
                    textFieldResearcherSalaryPercentOrSum.setCaption("Сумма комиссии");
                    textFieldResearcherSalary.setVisible(false);
                    textFieldResearcherSalaryPercentOrSum.setVisible(true);
                    textFieldResearcherSalary.setEditable(true);

                    textSalaryMessage = textFieldResearcherSalaryPercentOrSum.getValue() + " рублей.";
                    textFieldResearcherSalary.setValue(textSalaryMessage);

                    break;
                case 1:
                    textFieldResearcherSalaryPercentOrSum.setCaption("Процент комиссии, %");
                    textFieldResearcherSalaryPercentOrSum.setVisible(false);
                    textFieldResearcherSalary.setVisible(true);
                    textFieldResearcherSalary.setEditable(false);

                    if (!maxCompanyComission.equals(BigDecimal.ZERO) &&
                            !minCompanyComission.equals(BigDecimal.ZERO)) {

                        BigDecimal percent = new BigDecimal(20);

                        textSalaryMessage = "От " +
                                minSalary.multiply(percent).divide(hungred).setScale(0, RoundingMode.HALF_EVEN) +
                                " до " +
                                maxSalary.multiply(percent).divide(hungred).setScale(0, RoundingMode.HALF_EVEN);

                        textFieldResearcherSalary.setValue(textSalaryMessage);
                    }

                    break;

                case 2:
                    textFieldResearcherSalaryPercentOrSum.setCaption("Процент комиссии, %");
                    textFieldResearcherSalaryPercentOrSum.setVisible(true);
                    textFieldResearcherSalary.setVisible(true);
                    textFieldResearcherSalary.setEditable(false);
                    textFieldResearcherSalary.setValue(null);

                    if (textFieldPercentOrSum.getValue() != null &&
                            !maxCompanyComission.equals(BigDecimal.ZERO) &&
                            !minCompanyComission.equals(BigDecimal.ZERO)) {

                        if (textFieldResearcherSalaryPercentOrSum.getValue() != null) {

                            BigDecimal percent = new BigDecimal(textFieldResearcherSalaryPercentOrSum.getValue());

                            textSalaryMessage = "От " +
                                    minSalary.multiply(percent).divide(hungred)
                                            .setScale(0, RoundingMode.HALF_EVEN) +
                                    " до " +
                                    maxSalary.multiply(percent).divide(hungred)
                                            .setScale(0, RoundingMode.HALF_EVEN);

                            textFieldResearcherSalary.setValue(textSalaryMessage);
                        } else {
                            textFieldResearcherSalary.setValue(null);
                        }
                    } else {
                        textFieldResearcherSalary.setValue(null);
                    }

                    break;
            }
            setResearcherSalaryLabel();
        }
    }

    protected void calculateRecrutierSalary() {
        BigDecimal hungred = new BigDecimal(100);
        BigDecimal minSalary = new BigDecimal(String.valueOf(minCompanyComission));
        BigDecimal maxSalary = new BigDecimal(String.valueOf(maxCompanyComission));
        String textSalaryMessage = null;

        if (radioButtonGroupRecrutierSalary.getValue() != null) {
            switch ((int) radioButtonGroupRecrutierSalary.getValue()) {
                case 0:
                    textFieldRecrutierPercentOrSum.setCaption("Сумма комиссии");
                    textFieldRecrutierSalary.setVisible(false);
                    textFieldRecrutierPercentOrSum.setVisible(true);
                    textFieldRecrutierSalary.setEditable(true);

                    textSalaryMessage = textFieldRecrutierPercentOrSum.getValue() + " рублей.";
                    textFieldRecrutierSalary.setValue(textSalaryMessage);

                    break;
                case 1:
                    textFieldRecrutierPercentOrSum.setCaption("Процент комиссии, %");
                    textFieldRecrutierPercentOrSum.setVisible(false);
                    textFieldRecrutierSalary.setVisible(true);
                    textFieldRecrutierSalary.setEditable(false);
                    // textFieldRecrutierSalary.setValue("");

                    if (!maxCompanyComission.equals(BigDecimal.ZERO) &&
                            !minCompanyComission.equals(BigDecimal.ZERO)) {

                        BigDecimal percent = new BigDecimal(10);

                        textSalaryMessage = "От " +
                                minSalary.multiply(percent).divide(hungred).setScale(0, RoundingMode.HALF_EVEN) +
                                " до " +
                                maxSalary.multiply(percent).divide(hungred).setScale(0, RoundingMode.HALF_EVEN);

                        textFieldRecrutierSalary.setValue(textSalaryMessage);
                    }

                    break;

                case 2:
                    textFieldRecrutierPercentOrSum.setCaption("Процент комиссии, %");
                    textFieldRecrutierPercentOrSum.setVisible(true);
                    textFieldRecrutierSalary.setVisible(true);
                    textFieldRecrutierSalary.setEditable(false);
                    textFieldRecrutierSalary.setValue(null);

                    if (textFieldPercentOrSum.getValue() != null &&
                            !maxCompanyComission.equals(BigDecimal.ZERO) &&
                            !minCompanyComission.equals(BigDecimal.ZERO)) {
                        if (textFieldRecrutierPercentOrSum.getValue() != null) {

                            BigDecimal percent = new BigDecimal(textFieldResearcherSalaryPercentOrSum.getValue());

                            textSalaryMessage = "От " +
                                    minSalary.multiply(percent).divide(hungred)
                                            .setScale(0, RoundingMode.HALF_EVEN) +
                                    " до " +
                                    maxSalary.multiply(percent).divide(hungred)
                                            .setScale(0, RoundingMode.HALF_EVEN);

                            textFieldRecrutierSalary.setValue(textSalaryMessage);
                        } else {
                            textFieldRecrutierSalary.setValue(null);
                        }
                    } else {
                        textFieldRecrutierSalary.setValue(null);
                    }

                    break;
            }
            setRecrutierSalaryLabel();
        }
    }

    @Subscribe("textFieldRecrutierPercentOrSum")
    public void onTextFieldRecrutierPercentOrSumValueChange(HasValue.ValueChangeEvent<String> event) {
        calculateRecrutierSalary();
    }

    @Subscribe("textFieldResearcherSalaryPercentOrSum")
    public void onTextFieldResearcherSalaryPercentOrSumValueChange(HasValue.ValueChangeEvent<String> event) {
        calculateResearcherSalary();
    }

    @Subscribe("textFieldRecrutierSalary")
    public void onTextFieldRecrutierSalaryValueChange(HasValue.ValueChangeEvent<String> event) {
        // calculateRecrutierSalary();

        // setRecrutierSalaryLabel();
    }

    @Subscribe("textFieldResearcherSalary")
    public void onTextFieldResearcherSalaryValueChange(HasValue.ValueChangeEvent<String> event) {
        // calculateResearcherSalary();

        // setResearcherSalaryLabel();
    }

    private void setResearcherSalaryLabel() {
        if (radioButtonGroupResearcherSalary.getValue() != null) {
            if ((int) radioButtonGroupResearcherSalary.getValue() == 0) {
                if (textFieldResearcherSalary.getValue() != null) {
                    labelResearcherSalary.setValue("Зарплата ресерчера после закрытия вакансии \"<i>" +
                            vacansyNameField.getValue() + "</i>\" составит " +
                            textFieldResearcherSalary.getValue() + " рублей.");

                    groupBoxPaymentsResearcher.setVisible(true);
                } else
                    groupBoxPaymentsResearcher.setVisible(false);
            } else {
                if (textFieldResearcherSalary.getValue() != null) {
                    labelResearcherSalary.setValue("Зарплата ресерчера после закрытия вакансии \"<i>" +
                            vacansyNameField.getValue() + "</i>\" составит " +
                            textFieldResearcherSalary.getValue() + " рублей.");
                    groupBoxPaymentsResearcher.setVisible(true);
                    groupBoxPaymentsResearcher.setVisible(true);
                } else
                    groupBoxPaymentsResearcher.setVisible(false);
            }
        }
    }

    private void setRecrutierSalaryLabel() {
        if (radioButtonGroupRecrutierSalary.getValue() != null) {
            if ((int) radioButtonGroupRecrutierSalary.getValue() == 0) {
                if (textFieldRecrutierSalary.getValue() != null) {
                    labelRecrutierSalary.setValue("Зарплата рекрутера после закрытия вакансии \"<i>" +
                            vacansyNameField.getValue() + "</i>\" составит " +
                            textFieldRecrutierSalary.getValue() + " рублей.");
                }
            } else {
                if (textFieldRecrutierSalary.getValue() != null) {
                    labelRecrutierSalary.setValue("Зарплата рекрутера после закрытия вакансии \"<i>" +
                            vacansyNameField.getValue() + "</i>\" составит " +
                            textFieldRecrutierSalary.getValue() + " рублей.");
                }
            }
        }

        setHiddeField();
    }

    @Subscribe
    public void onBeforeShow1(BeforeShowEvent event) {
        // показываем или нет все строки ввода в оплаты
        if (radioButtonGroupPaymentsType.getValue() == null) {
            textFieldPercentOrSum.setVisible(false);
            textFieldCompanyPayment.setVisible(false);
        }

        if (radioButtonGroupResearcherSalary.getValue() == null) {
            textFieldResearcherSalaryPercentOrSum.setVisible(false);
            textFieldResearcherSalary.setVisible(false);
        }

        if (radioButtonGroupRecrutierSalary.getValue() == null) {
            textFieldRecrutierPercentOrSum.setVisible(false);
            textFieldRecrutierSalary.setVisible(false);
        }

        setTopLabel();

        openClosePositionCheckBox.setValue(openClosePositionCheckBox.getValue() == null ? false: true);
    }

    private void setTopLabel() {
        if (vacansyNameField.getValue() != null && projectNameField.getValue() != null) {
            if (projectNameField.getValue() != null) {
                if (projectNameField.getValue().getProjectDepartment() != null) {
                    if (projectNameField.getValue().getProjectDepartment().getCompanyName() != null) {
                        if (projectNameField.getValue().getProjectDepartment().getCompanyName() != null) {
                            if (projectNameField.getValue().getProjectDepartment().getCompanyName().getComanyName() != null) {
                                String comanyName = projectNameField.getValue().getProjectDepartment().getCompanyName().getComanyName();

                                labelOpenPosition.setValue(vacansyNameField.getValue() +
                                        " (" +
                                        (comanyName != null ? comanyName : "") +
                                        " : " +
                                        projectNameField.getValue().getProjectName() +
                                        ")");
                            }

                            // а еще вывести комиссию
                            if (getRoleService.isUserRoles(userSession.getUser(), RESEARCHER)) {
                                labelTopComissionResearcher.setValue(labelResearcherSalary.getValue());
                                labelTopComissionResearcher.setVisible(true);

                                labelTopComissionRecrutier.setVisible(false);
                            } else {
                                labelTopComissionResearcher.setVisible(false);
                            }

                            if (getRoleService.isUserRoles(userSession.getUser(), RECRUITER)) {
                                labelTopComissionRecrutier.setValue(labelRecrutierSalary.getValue());
                                labelTopComissionRecrutier.setVisible(true);

                                labelTopComissionResearcher.setVisible(false);
                            } else {
                                labelTopComissionRecrutier.setVisible(false);
                            }

                            if (getRoleService.isUserRoles(userSession.getUser(), MANAGER)) {
                                labelTopComissionRecrutier.setVisible(false);
                                labelTopComissionResearcher.setVisible(false);
                            }
                        }
                    }
                }
            }
        }
    }

    @Subscribe("labelRecrutierSalary")
    public void onLabelRecrutierSalaryValueChange(HasValue.ValueChangeEvent<String> event) {
        setTopLabel();
    }

    @Subscribe("labelResearcherSalary")
    public void onLabelResearcherSalaryValueChange(HasValue.ValueChangeEvent<String> event) {
        setTopLabel();
    }

    @Subscribe
    public void onInit(InitEvent event) {

        setRadioButtons();
        setGroupSubscribeButton();
        setGroupCommandRadioButtin();
    }

    private void setGroupCommandRadioButtin() {
        Map<String, Integer> map = new LinkedHashMap<>();

        map.put("Команда", 1);
        map.put("Вакансия", 0);

        commandOrPosition.setOptionsMap(map);
    }

    @Subscribe("projectNameField")
    public void onProjectNameFieldValueChange1(HasValue.ValueChangeEvent<Project> event) {
        if (event.getValue().getProjectIsClosed()) {
            dialogs.createOptionDialog(Dialogs.MessageType.WARNING)
                    .withContentMode(ContentMode.HTML)
                    .withMessage("Вы пытаетесь открыть позицию по закрытому проекту.<br>" +
                            "Открыть проект заново?")
                    .withActions(new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY).withHandler(e -> {
                        event.getValue().setEndProjectDate(null);
                        event.getValue().setProjectIsClosed(false);
                    }), new DialogAction(DialogAction.Type.NO).withHandler((f -> {
                        projectNameField.setValue(null);
                    })));
        }

    }

    private void setGroupSubscribeButton() {
//        groupSubscribe.setVisible(userSession.getUser().getGroup().getName().equals(MANAGEMENT_GROUP) ||
//                userSession.getUser().getGroup().getName().equals(HUNTING_GROUP));
    }

    @Install(to = "remoteWorkField", subject = "optionIconProvider")
    private String remoteWorkFieldOptionIconProvider(Integer integer) {
        String returnIcon = "";

        switch (integer) {
            case 1:
                returnIcon = "font-icon:PLUS_CIRCLE";
                break;
            case 0:
                returnIcon = "font-icon:MINUS_CIRCLE";
                break;
            case 2:
                returnIcon = "font-icon:QUESTION_CIRCLE";
                break;
            default:
                returnIcon = "fint-icon:QUESTION_CIRCLE";
                break;
        }

        return returnIcon;
    }

    @Install(to = "priorityField", subject = "optionIconProvider")
    private String priorityFieldOptionIconProvider(Integer integer) {

        String icon = null;

        switch (integer) {
            case 0: //"Paused"
                icon = "icons/remove.png";
                break;
            case 1: //"Low"
                icon = "icons/traffic-lights_blue.png";
                break;
            case 2: //"Normal"
                icon = "icons/traffic-lights_green.png";
                break;
            case 3: //"High"
                icon = "icons/traffic-lights_yellow.png";
                break;
            case 4: //"Critical"
                icon = "icons/traffic-lights_red.png";
                break;
        }

        return icon;
    }

    @Subscribe(target = Target.DATA_CONTEXT)
    public void onChange(DataContext.ChangeEvent event) {
        entityIsChanged = true;
    }

    public void subscribePosition() {
        Screen opScreen = screenBuilders
                .editor(RecrutiesTasks.class, this)
                .newEntity()
                .withInitializer(data -> {
                    data.setOpenPosition(this.getEditedEntity());
                })
                .newEntity()
                .withParentDataContext(dataContext)
                .withScreenId("itpearls_RecrutiesTasks.edit")
                .withLaunchMode(OpenMode.DIALOG)
                .build();


        opScreen.show();
    }

    public Boolean isUserRoles(User user, String role) {
        Collection<String> s = userSessionSource.getUserSession().getRoles();
        Boolean c = false;
        // установить поле рекрутера
        for (String a : s) {
            if (a.equalsIgnoreCase(role)) {
                c = true;
                break;
            }
        }
        return c;
    }

    @Subscribe("positionTypeField")
    public void onPositionTypeFieldValueChange(HasValue.ValueChangeEvent<Position> event) {
        if (vacansyNameField.getValue() == null || vacansyNameField.getValue() == "") {
            vacansyNameField.setValue(generatePositionName());
            if(projectNameField.getValue() != null) {
                vacansyNameField.setValue(generatePositionNameInProject());
                if(cityOpenPositionField.getValue() != null) {
                    vacansyNameField.setValue(generatePositionNameCity());
                }
            }
        }
    }

    @Subscribe("projectNameField")
    public void onProjectNameFieldValueChange(HasValue.ValueChangeEvent<Project> event) {
        vacansyNameField.setValue(generatePositionNameInProject());

        setCompanyDepartmentFromProject();
    }

    private String generatePositionNameInProject() {
        String retValue = vacansyNameField.getValue();

        if (vacansyNameField.getValue() != null) {
            if (generatePositionName().equals(retValue)) {
                if (projectNameField.getValue().getProjectName() != null) {
                    retValue = retValue + " (" + projectNameField.getValue().getProjectName() + ")";
                }
            }
        }

        return retValue;
    }

    private String generatePositionNameCity() {
        String retValue = vacansyNameField.getValue();

        if (PersistenceHelper.isNew(getEditedEntity())) {
            if (cityOpenPositionField.getValue() != null) {
                if(retValue != null) {
                    if (generatePositionNameInProject().equals(retValue)) {
                        retValue = retValue.substring(0, retValue.length() - 1) + ", " + cityOpenPositionField.getValue().getCityRuName() + ")";
                    }
                } else {
                    retValue = null;
                }
            }
        }

        return retValue;
    }

    @Subscribe("cityOpenPositionField")
    public void onCityOpenPositionFieldValueChange(HasValue.ValueChangeEvent<City> event) {
        vacansyNameField.setValue(generatePositionNameCity());
    }

    protected String generatePositionName() {
        String retPosName = "";


        if (positionTypeField.getValue() != null) {
            if (positionTypeField.getValue().getPositionEnName() == null) {
                notifications.create(Notifications.NotificationType.WARNING)
                        .withHideDelayMs(3000)
                        .withDescription("Не заполнено наименование типа позиции")
                        .show();

                screenBuilders.editor(positionTypeField)
                        .editEntity(positionTypeField.getValue())
                        .withScreenClass(PositionEdit.class)
                        .withLaunchMode(OpenMode.DIALOG)
                        .withParentDataContext(dataContext)
                        .build()
                        .show();

                positionTypesLc.load();
            }

                retPosName =
                        (positionTypeField.getValue().getPositionRuName() != null ? positionTypeField.getValue().getPositionRuName() : "")
                                + " \\ "
                                + (positionTypeField.getValue().getPositionEnName() != null ? positionTypeField.getValue().getPositionEnName() : "");
        }

        return retPosName;
    }

    public void openClosePosition() {
        String message = "";

        if (!openClosePositionCheckBox.getValue())
            message = "Закрыть";
        else
            message = "Открыть";

        dialogs.createOptionDialog()
                .withMessage(message + " позицию \"" + vacansyNameField.getValue() + "\"?")
                .withCaption("ВНИМАНИЕ!")
                .withActions(new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY).withHandler(e -> {

                    if (this.openClosePositionCheckBox.getValue())
                        this.closeWithCommit();
                    return;

                }), new DialogAction(DialogAction.Type.NO))
                .show();
    }

    public void addListCity() {
        SelectCitiesLocation selectCitiesLocation = screens.create(SelectCitiesLocation.class);

        selectCitiesLocation.addAfterShowListener(e -> {
            selectCitiesLocation.setCitiesList(this.getEditedEntity().getCities());
        });
        selectCitiesLocation.addAfterCloseListener(e -> {
            this.getEditedEntity().setCities(selectCitiesLocation.getCitiesList());
            changeCityListsLabel();
        });
        selectCitiesLocation.show();

    }

    private void changeCityListsLabel() {
        String outStr = "";
        String description = "";

        if (getEditedEntity().getCities() != null) {
            for (City s : getEditedEntity().getCities()) {
                if (!outStr.equals("")) {
                    outStr = outStr + ",";
                    description = description + "\n";
                }

                outStr = outStr + s.getCityRuName();
                description = description + s.getCityRuName();
            }

        }
        if (!outStr.equals("")) {
            citiesLabel.setValue(outStr);
            citiesLabel.setDescription(description);
        }
    }
}
