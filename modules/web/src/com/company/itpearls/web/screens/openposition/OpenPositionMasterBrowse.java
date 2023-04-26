package com.company.itpearls.web.screens.openposition;

import com.company.itpearls.entity.*;
import com.company.itpearls.web.screens.openposition.openpositionviews.TextViewScreen;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.icons.Icons;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.gui.screen.LookupComponent;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.*;

@UiController("itpearls_OpenPositionMaster.browse")
@UiDescriptor("open-position-master-browse.xml")
@LookupComponent("openPositionsTable")
@LoadDataBeforeShow
public class OpenPositionMasterBrowse extends StandardLookup<OpenPosition> {
    @Inject
    private CollectionLoader<Project> projectNameDl;
    @Inject
    private CollectionLoader<Position> openPositionsDl;
    @Inject
    private DataGrid<Position> openPositionsTable;
    @Inject
    private CollectionLoader<OpenPosition> vacansyNameDl;
    @Inject
    private DataGrid<Project> projectNameTable;
    @Inject
    private DataGrid<Company> companyTable;
    @Inject
    private RichTextArea companyInfoRichTextArea;
    @Inject
    private RichTextArea projectInfoRichTextArea;
    @Inject
    private DataGrid<OpenPosition> vacansyNameTable;
    @Inject
    private RichTextArea vacansyInfoRictTextArea;
    @Inject
    private LookupField notLowerRatingLookupField;
    @Inject
    private CollectionLoader<Company> companyDl;
    @Inject
    private TextField<String> headerTextField;
    @Inject
    private DataManager dataManager;
    @Inject
    private Label<String> minSalaryLabel;
    @Inject
    private Label<String> maxSalaryLabel;
    @Inject
    private Accordion mainAccordion;

    private Map<String, Integer> priorityMap = new LinkedHashMap<>();
    private Map<Integer, String> tabCaption = new HashMap<>();

    @Inject
    private Button nextButton;
    @Inject
    private Button previonsButton;
    @Inject
    private LookupField<JobCandidate> jobCandidateField;
    @Inject
    private Label<String> jobCandidateLabel;
    @Inject
    private Label<String> priorityLabel;
    @Inject
    private Label<String> selectPositionLabel;

    JobCandidate jobCandidate = null;
//    @Inject
//    private Label<String> interviewTabHeaderLabel;
//    @Inject
//    private Label<String> candidateNameLabel;

    public void setJobCandidate(JobCandidate jobCandidate) {
        this.jobCandidate = jobCandidate;
    }

    public JobCandidate getJobCandidate() {
        return jobCandidate;
    }

    @Subscribe("openPositionsTable")
    public void onOpenPositionsTableSelection(DataGrid.SelectionEvent<Position> event) {
        projectNameTable.deselectAll();
        vacansyNameTable.deselectAll();
        companyTable.deselectAll();

        if (openPositionsTable.getSelected().size() <= 1) {
            if (openPositionsTable.getSingleSelected() != null) {
                vacansyNameDl.setParameter("positionTypeSet", openPositionsTable.getSelected());
                projectNameDl.setParameter("positionTypeSet", openPositionsTable.getSelected());
                companyDl.setParameter("positionTypeSet", openPositionsTable.getSelected());
            } else {
                vacansyNameDl.removeParameter("positionType");
                projectNameDl.removeParameter("positionType");
                companyDl.removeParameter("positionType");
            }
        } else {
            vacansyNameDl.setParameter("positionTypeSet", openPositionsTable.getSelected());
            projectNameDl.setParameter("positionTypeSet", openPositionsTable.getSelected());
            companyDl.setParameter("positionTypeSet", openPositionsTable.getSelected());
        }

        projectNameDl.load();
        vacansyNameDl.load();
        companyDl.load();
    }

    @Subscribe("companyTable")
    public void onCompanyTableSelection(DataGrid.SelectionEvent<Company> event) {
        if (event.getSelected() != null) {
            if (companyTable.getSingleSelected() != null) {
                if (companyTable.getSingleSelected().getCompanyDescription() != null) {
                    companyInfoRichTextArea.setValue(companyTable.getSingleSelected().getCompanyDescription());
                } else {
                    companyInfoRichTextArea.setValue("");
                }
            }

            projectNameDl.setParameter("companySet", companyTable.getSelected());
            vacansyNameDl.setParameter("companySet", companyTable.getSelected());

//            projectNameDl.setParameter("company", companyTable.getSingleSelected());
//            vacansyNameDl.setParameter("company", companyTable.getSingleSelected());
        } else {
            projectNameDl.removeParameter("companySet");
            vacansyNameDl.removeParameter("companySet");

            projectNameDl.removeParameter("company");
            vacansyNameDl.removeParameter("company");
        }

        projectNameDl.load();
        vacansyNameDl.load();
    }

    @Subscribe("jobCandidateField")
    public void onJobCandidateFieldValueChange(HasValue.ValueChangeEvent<JobCandidate> event) {
        setCandidateHeaderLabel();
    }

    @Subscribe("mainAccordion")
    public void onMainAccordionSelectedTabChange(Accordion.SelectedTabChangeEvent event) {
        setCandidateHeaderLabel();
    }

    private void setCandidateHeaderLabel() {
        if (jobCandidateField.getValue() != null) {
            jobCandidateLabel.setValue(jobCandidateField.getValue().getFullName()
                    + " / "
                    + jobCandidateField.getValue().getPersonPosition().getPositionEnName());
        }
    }

    @Subscribe("projectNameTable")
    public void onProjectNameTableSelection(DataGrid.SelectionEvent<Project> event) {
        if (event.getSelected() != null) {
            if (projectNameTable.getSingleSelected() != null) {
                if (projectNameTable.getSingleSelected().getProjectDescription() != null) {
                    projectInfoRichTextArea.setValue(projectNameTable.getSingleSelected().getProjectDescription());
                } else {
                    projectInfoRichTextArea.setValue("");
                }
            }
        }

        if (projectNameTable.getSingleSelected() != null) {
            vacansyNameDl.setParameter("projectNameSet", projectNameTable.getSelected());
        } else {
            vacansyNameDl.removeParameter("projectNameSet");
        }

        vacansyNameDl.load();
    }

    @Subscribe("vacansyNameTable")
    public void onVacansyNameTableSelection(DataGrid.SelectionEvent<OpenPosition> event) {
        if (event.getSelected() != null) {
            if (vacansyNameTable.getSingleSelected() != null) {
                if (vacansyNameTable.getSingleSelected().getComment() != null) {
                    vacansyInfoRictTextArea.setValue(vacansyNameTable.getSingleSelected().getComment());
                } else {
                    vacansyInfoRictTextArea.setValue("");
                }
            }
        }
    }

    @Install(to = "vacansyNameTable.priorityColumn", subject = "columnGenerator")
    private Icons.Icon vacansyNameTablePriorityColumnColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {

        switch ((int) event.getItem().getPriority()) {
            case 0:
                return CubaIcon.ANGLE_DOUBLE_DOWN;
            case 1:
                return CubaIcon.ANGLE_DOWN;
            case 2:
                return CubaIcon.CIRCLE;
            case 3:
                return CubaIcon.ANGLE_UP;
            case 4:
                return CubaIcon.ANGLE_DOUBLE_UP;
        }

        return null;
    }

    @Install(to = "vacansyNameTable.priorityColumn", subject = "styleProvider")
    private String vacansyNameTablePriorityColumnStyleProvider(OpenPosition openPosition) {
        switch ((int) openPosition.getPriority()) {
            case 0:
                return "pic-center-large-grey";
            case 1:
                return "pic-center-large-blue";
            case 2:
                return "pic-center-large-green";
            case 3:
                return "pic-center-large-yellow";
            case 4:
                return "pic-center-large-red";
        }

        return null;
    }

    @Install(to = "notLowerRatingLookupField", subject = "optionIconProvider")
    private String notLowerRatingLookupFieldOptionIconProvider(Object object) {
        String icon = null;

        switch ((int) object) {
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

    private void setMapOfPriority() {
        priorityMap.put("Paused", 0);
        priorityMap.put("Low", 1);
        priorityMap.put("Normal", 2);
        priorityMap.put("High", 3);
        priorityMap.put("Critical", 4);
    }

    @Subscribe
    public void onInit(InitEvent event) {
        setTabCaption();
        setPrevNextButton();
        setMapOfPriority();
    }

    private void setPrevNextButton() {
        mainAccordion.addSelectedTabChangeListener(e -> {
            Integer maxTab = 1;

            String a = mainAccordion.getSelectedTab().getCaption().substring(0, 1);

            if (mainAccordion.getSelectedTab().getCaption().substring(0, 1).equals(maxTab.toString())) {
                previonsButton.setEnabled(false);
            } else {
                previonsButton.setEnabled(true);
            }

            for (Accordion.Tab tab : mainAccordion.getTabs()) {
                maxTab++;
            }


            if (mainAccordion.getSelectedTab().getCaption().substring(0, 1).equals((--maxTab).toString())) {
                nextButton.setEnabled(false);
            } else {
                nextButton.setEnabled(true);
            }
        });
    }

    private void setTabCaption() {
        tabCaption.put(1, "1. Выберите кандидата, которого предстоит собеседовать");
        tabCaption.put(2, "2. Выберите приоритет поиска позиции");
        tabCaption.put(3, "3. Выбрать специализацию");
        tabCaption.put(4, "4. Выбрать компанию раюотодателя");
        tabCaption.put(5, "5. Выбрать вакансию");
        tabCaption.put(6, "6. Детали собеседования");
        tabCaption.put(7, "7. Сопроводиетльное письмо");

        for (Accordion.Tab tab : mainAccordion.getTabs()) {
            for (Map.Entry entry : tabCaption.entrySet()) {
                String a = tab.getCaption();
                String b = entry.getKey().toString();

                if (tab.getCaption().equals(entry.getKey().toString())) {
                    tab.setCaption(tabCaption.get(entry.getKey()));
                }
            }
        }
    }

    private void setStatusNotLower() {
        notLowerRatingLookupField.setOptionsMap(priorityMap);

        notLowerRatingLookupField.addValueChangeListener(e -> {
            if (notLowerRatingLookupField.getValue() != null) {
                openPositionsDl.setParameter("priorityfield", notLowerRatingLookupField.getValue());
                vacansyNameDl.setParameter("priorityfield", notLowerRatingLookupField.getValue());
//                companyDl.setParameter("priorityfield", notLowerRatingLookupField.getValue());
//                projectNameDl.setParameter("priorityfield", notLowerRatingLookupField.getValue());
            } else {
                openPositionsDl.setParameter("priorityfield", 1);
                vacansyNameDl.setParameter("priorityfield", 1);
//                openPositionsDl.removeParameter("priorityfield");
//                vacansyNameDl.removeParameter("priorityfield");
            }

            vacansyNameDl.load();
            openPositionsDl.load();
        });
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        setStatusNotLower();
        setHeaderBox();
        setMinMaxSalaryLabel();
        setCompanyTable();
        clearFilters();

        if(jobCandidate != null) {
            jobCandidateField.setValue(jobCandidate);
        }

        companyDl.setParameter("ourClient", true);
        companyDl.load();
    }

    private void setCompanyTable() {
        companyTable.addSelectionListener(e -> {
            if (companyTable.getSingleSelected() != null) {
                projectNameDl.setParameter("companySet", companyTable.getSelected());
//                projectNameDl.setParameter("company", companyTable.getSingleSelected());
            } else {
                projectNameDl.removeParameter("companySet");
                projectNameDl.removeParameter("company");
            }
        });
    }

    private void setMinMaxSalaryLabel() {
        openPositionsTable.addSelectionListener(e -> {
            String QUERY_MIN_MAX_SALARY = "select e " +
                    "from itpearls_OpenPosition e " +
                    "where e.positionType = :position " +
                    "and (not e.openClose = true)";

            if (openPositionsTable.getSingleSelected() != null) {
                List<OpenPosition> openPositionList = dataManager.load(OpenPosition.class)
                        .query(QUERY_MIN_MAX_SALARY)
                        .parameter("position", openPositionsTable.getSingleSelected())
                        .list();

                if (openPositionList != null) {
                    BigDecimal min = openPositionList.get(0).getSalaryMin(),
                            max = openPositionList.get(0).getSalaryMax();

                    for (OpenPosition op : openPositionList) {
                        if (op.getSalaryMin() != null) {
                            if (min.compareTo(op.getSalaryMin()) > 0) {
                                min = op.getSalaryMin();
                            }
                        }

                        if (op.getSalaryMax() != null) {
                            if (max.compareTo(op.getSalaryMax()) < 0) {
                                max = op.getSalaryMax();
                            }
                        }
                    }

                    minSalaryLabel.setValue(min.toString());
                    maxSalaryLabel.setValue(max.toString());
                }
            }
        });
    }

    @Install(to = "vacansyNameTable.salaryMinMaxColumn", subject = "columnGenerator")
    private String vacansyNameTableSalaryMinMaxColumnColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        String retStr = "";

        try {
            retStr = getSalaryString(event.getItem());
        } catch (NullPointerException e) {
            retStr = "";
        }

        return retStr;
    }

    private void setHeaderBox() {
        headerTextField.setValue("Мастер собеседования: ");
    }

    private String getSalaryString(OpenPosition openPosition) {
        int minLength = openPosition.getSalaryMin().toString().length();
        int maxLength = openPosition.getSalaryMax().toString().length();

        BigDecimal salaryMin = openPosition.getSalaryMin().divide(BigDecimal.valueOf(1000));
        BigDecimal salaryMax = openPosition.getSalaryMax().divide(BigDecimal.valueOf(1000));

        String retStr = "";

        try {
            int salMin = salaryMin.divide(BigDecimal.valueOf(1000)).intValue();
            if (salMin != 0) {
                retStr = salaryMin.toString().substring(0, salaryMin.toString().length() - 3)
                        + " т.р./"
                        + salaryMax.toString().substring(0, salaryMax.toString().length() - 3)
                        + " т.р.";
            } else {
                retStr = "До "
                        + salaryMax.toString().substring(0, salaryMax.toString().length() - 3)
                        + " т.р.";
            }
        } catch (NullPointerException | StringIndexOutOfBoundsException e) {
            retStr = "";
        }

        if (salaryMax.intValue() == 0) {
            retStr = "неопределено";
        }

        return retStr;
    }

    @Install(to = "vacansyNameTable.salaryMinMaxColumn", subject = "descriptionProvider")
    private String vacansyNameTableSalaryMinMaxColumnDescriptionProvider(OpenPosition openPosition) {
        String retStr = "";

        if (openPosition.getSalaryFixLimit() != null) {
            if (openPosition.getSalaryFixLimit()) {
                retStr = "Фиксированное запрлатное предложение\n";
            }
        }

        try {
            retStr = retStr + getSalaryStringCaption(openPosition);
        } catch (NullPointerException e) {
            retStr = "";
        }

        return retStr;
    }

    private String getSalaryStringCaption(OpenPosition openPosition) {
        int minLength = openPosition.getSalaryMin().toString().length();
        int maxLength = openPosition.getSalaryMax().toString().length();

        BigDecimal salaryMin = openPosition.getSalaryMin().divide(BigDecimal.valueOf(1000));
        BigDecimal salaryMax = openPosition.getSalaryMax().divide(BigDecimal.valueOf(1000));

        String retStr = "";

        try {
            retStr = salaryMin.toString().substring(0, salaryMin.toString().length() - 3)
                    + " т.р./"
                    + salaryMax.toString().substring(0, salaryMax.toString().length() - 3)
                    + " т.р.";
        } catch (NullPointerException e) {
            retStr = "";
        }

        if (salaryMin.intValue() == 0) {
            retStr = "неопределено";
        }

        return retStr;
    }

    @Install(to = "vacansyNameTable.salaryMinMaxColumn", subject = "styleProvider")
    private String vacansyNameTableSalaryMinMaxColumnStyleProvider(OpenPosition openPosition) {
        String retStr = "";

        if (openPosition.getSalaryFixLimit() != null) {
            if (openPosition.getSalaryFixLimit()) {
                retStr = "salary-fix-limit";
            }
        }

        return retStr;
    }

    @Install(to = "vacansyNameTable.vacansyName", subject = "descriptionProvider")
    private String vacansyNameTableVacansyNameDescriptionProvider(OpenPosition openPosition) {
        return openPosition.getVacansyName();
    }

    public void clearFilters() {
        companyDl.removeParameter("positionType");
        companyDl.removeParameter("openPosition");

        projectNameDl.removeParameter("companySet");
        projectNameDl.removeParameter("positionType");
        projectNameDl.removeParameter("company");
        projectNameDl.removeParameter("openPosition");

        vacansyNameDl.removeParameter("priorityfield");
        vacansyNameDl.removeParameter("positionType");

        openPositionsDl.removeParameter("priorityfield");

        vacansyNameDl.load();
        openPositionsDl.load();
        projectNameDl.load();
        companyDl.load();

        notLowerRatingLookupField.setValue(null);

        deselectTables();
    }

    private void deselectTables() {
        openPositionsTable.deselectAll();
        vacansyNameTable.deselectAll();
        companyTable.deselectAll();
        projectNameTable.deselectAll();

    }

    public void nextTab() {
        Integer nextTab = 0;

        String first = mainAccordion.getSelectedTab().getCaption();
        for (Map.Entry entry : tabCaption.entrySet()) {
            if (entry.getValue().equals(first)) {
                nextTab = (Integer) entry.getKey();

            }
        }

        nextTab++;

        for (Accordion.Tab tab : mainAccordion.getTabs()) {
            if (tab.getCaption().equals(tabCaption.get(nextTab))) {
                mainAccordion.setSelectedTab(tab);
                break;
            }
        }
    }

    public void previonsTab() {
        Integer previonsTab = 0;

        String first = mainAccordion.getSelectedTab().getCaption();
        for (Map.Entry entry : tabCaption.entrySet()) {
            if (entry.getValue().equals(first)) {
                previonsTab = (Integer) entry.getKey();

            }
        }

        previonsTab--;

        for (Accordion.Tab tab : mainAccordion.getTabs()) {
            if (tab.getCaption().equals(tabCaption.get(previonsTab))) {
                mainAccordion.setSelectedTab(tab);
                break;
            }
        }
    }

    @Subscribe("notLowerRatingLookupField")
    public void onNotLowerRatingLookupFieldValueChange(HasValue.ValueChangeEvent event) {
        String retValue = "";

        for( Map.Entry<String, Integer> entry: priorityMap.entrySet()) {
            if(event.getValue().equals(entry.getValue())) {
                retValue = entry.getKey();
            }
        }

        if(event.getValue() != null) {
            priorityLabel.setValue(retValue);
        }
    }

    @Subscribe("openPositionsTable")
    public void onOpenPositionsTableSelection1(DataGrid.SelectionEvent<Position> event) {
        if (openPositionsTable.getSelected().size() <= 1) {
            if (openPositionsTable.getSingleSelected() != null) {
                selectPositionLabel.setValue(openPositionsTable.getSingleSelected().getPositionRuName()
                        + " / "
                        + openPositionsTable.getSingleSelected().getPositionEnName());
            }
        } else {
            String label = "";
            for (Position p : openPositionsTable.getSelected()) {
                label += p.getPositionRuName() + ",";
            }

            selectPositionLabel.setValue(label.substring(0, label.length() - 1));
            selectPositionLabel.setDescription(label.substring(0, label.length() - 1));
        }
    }

    @Install(to = "companyInfoRichTextArea", subject = "contextHelpIconClickHandler")
    private void companyInfoRichTextAreaContextHelpIconClickHandler(HasContextHelp.ContextHelpIconClickEvent contextHelpIconClickEvent) {
        if (companyInfoRichTextArea.getValue() != null) {
            TextViewScreen viewTextScreen = new TextViewScreen();
            viewTextScreen.setTextView(companyInfoRichTextArea.getValue());

            viewTextScreen.show();
        }
    }

    @Subscribe("jobCandidateField")
    public void onJobCandidateFieldValueChange1(HasValue.ValueChangeEvent<JobCandidate> event) {
        if (event.getValue() != null) {
            String jobCandidateLabel = event.getValue().getFullName()
                    + " ("
                    + event.getValue().getPersonPosition().getPositionEnName()
                    + " / "
                    + event.getValue().getPersonPosition().getPositionRuName()
                    + ")";

//            interviewTabHeaderLabel.setValue(jobCandidateLabel);
//            candidateNameLabel.setValue(jobCandidateLabel);
        }
    }

    public void saveLetter() {
    }
}