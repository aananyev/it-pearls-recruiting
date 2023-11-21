package com.company.itpearls.web.screens.simplebrowsers;

import com.company.itpearls.core.InteractionService;
import com.company.itpearls.entity.JobCandidate;
import com.company.itpearls.entity.OpenPosition;
import com.company.itpearls.web.screens.iteractionlist.iteractionlistbrowse.IteractionListSimpleBrowse;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.components.data.value.ContainerValueSource;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.IteractionList;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.Map;

@UiController("itpearls_JobCandidateSimple.browse")
@UiDescriptor("job-candidate-simple-browse.xml")
@LookupComponent("iteractionListsTable")
@LoadDataBeforeShow
public class JobCandidateSimpleBrowse extends StandardLookup<IteractionList> {
    @Inject
    private DataGrid<JobCandidate> iteractionListsTable;
    @Inject
    private InteractionService interactionService;
    @Inject
    private UserSession userSession;
    @Inject
    private MessageBundle messageBundle;
    @Inject
    private Image projectLogoImage;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        projectLogoImage.setSource(ThemeResource.class).setPath("icons/no-company.png");

        setFileImageCandidate(event);
    }

    @Subscribe
    public void onAfterShow1(AfterShowEvent event) {
        setProjectLogoImage();
        vacancyLabel.addStyleName("table-wordwrap");
    }

    private void setProjectLogoImage() {
        if (openPosition != null) {
            if (openPosition.getProjectName() != null) {
                if (openPosition.getProjectName().getProjectLogo() != null) {
                    projectLogoImage.setSource(FileDescriptorResource.class)
                            .setFileDescriptor(openPosition.getProjectName().getProjectLogo());
                } else {
                    projectLogoImage.setSource(ThemeResource.class).setPath("icons/no-company.png");
                }
            } else {
                projectLogoImage.setSource(ThemeResource.class).setPath("icons/no-company.png");
            }
        } else {
            projectLogoImage.setSource(ThemeResource.class).setPath("icons/no-company.png");
        }
    }

    private void setFileImageCandidate(BeforeShowEvent event) {
        iteractionListsTable.addGeneratedColumn("fileImageFace", entity -> {
            HBoxLayout hBox = uiComponents.create(HBoxLayout.class);
            Image image = uiComponents.create(Image.NAME);

            if (entity.getItem().getFileImageFace() != null) {
                try {
                    image.setValueSource(new ContainerValueSource<JobCandidate, FileDescriptor>(entity.getContainer(),
                            "fileImageFace"));

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                image.setSource(ThemeResource.class).setPath("icons/no-programmer.jpeg");
            }

            image.setWidth("20px");
            image.setStyleName("circle-20px");

            image.setScaleMode(Image.ScaleMode.CONTAIN);
            image.setAlignment(Component.Alignment.MIDDLE_CENTER);

            hBox.setWidthFull();
            hBox.setHeightFull();
            hBox.add(image);

            return hBox;
        });
    }

    @Inject
    private CollectionLoader<JobCandidate> jobCandidateDl;
    JobCandidate jobCandidate = null;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private Label<String> vacancyLabel;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private RadioButtonGroup interactionsCanidatesPeriodRadioButtonGroup;
    @Inject
    private RadioButtonGroup beforeAnAfterRadioButtonsGroup;

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        initInteractionsCanidatesPeriodCheckBoxGroup();
        initBeforeAnAfterRadioButtonsGroup();
    }

    private void initBeforeAnAfterRadioButtonsGroup() {
        Map<String, Integer> afterBeforeMap = new LinkedHashMap<>();

        afterBeforeMap.put("До", 1);
        afterBeforeMap.put("После", 2);

        beforeAnAfterRadioButtonsGroup.setOptionsMap(afterBeforeMap);
    }

    @Subscribe("beforeAnAfterRadioButtonsGroup")
    public void onBeforeAnAfterRadioButtonsGroupValueChange(HasValue.ValueChangeEvent event) {
        Date currDate = new Date();
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(currDate);

        if (event.getValue() != null) {
            if (interactionsCanidatesPeriodRadioButtonGroup.getValue() != null) {
                switch ((int) event.getValue()) {
                    case 1:
                        switch ((int) interactionsCanidatesPeriodRadioButtonGroup.getValue()) {
                            case 1:
                                gc.add(GregorianCalendar.DAY_OF_MONTH, -3);
                                jobCandidateDl.removeParameter("dateAfter");
                                jobCandidateDl.setParameter("dateBefore", gc.getTime());
                                break;
                            case 2:
                                gc.add(GregorianCalendar.DAY_OF_MONTH, -7);
                                jobCandidateDl.removeParameter("dateAfter");
                                jobCandidateDl.setParameter("dateBefore", gc.getTime());
                                break;
                            case 3:
                                gc.add(GregorianCalendar.MONTH, -1);
                                jobCandidateDl.removeParameter("dateAfter");
                                jobCandidateDl.setParameter("dateBefore", gc.getTime());
                                break;
                            default:
                                break;
                        }

                        break;
                    case 2:
                        switch ((int) interactionsCanidatesPeriodRadioButtonGroup.getValue()) {
                            case 1:
                                gc.add(GregorianCalendar.DAY_OF_MONTH, -3);
                                jobCandidateDl.removeParameter("dateBefore");
                                jobCandidateDl.setParameter("dateAfter", gc.getTime());
                                break;
                            case 2:
                                jobCandidateDl.removeParameter("dateBefore");
                                gc.add(GregorianCalendar.DAY_OF_MONTH, -7);
                                jobCandidateDl.setParameter("dateAfter", gc.getTime());
                                break;
                            case 3:
                                jobCandidateDl.removeParameter("dateBefore");
                                gc.add(GregorianCalendar.MONTH, -1);
                                jobCandidateDl.setParameter("dateAfter", gc.getTime());
                                break;
                            default:
                                break;
                        }

                        break;
                    default:
                        break;
                }
            }

            jobCandidateDl.load();
        }
    }

    private void initInteractionsCanidatesPeriodCheckBoxGroup() {
        Map<String, Integer> onlyOpenedPositionMap = new LinkedHashMap<>();

        onlyOpenedPositionMap.put("Открытые за 3 дня", 1);
        onlyOpenedPositionMap.put("Открытые за неделю", 2);
        onlyOpenedPositionMap.put("Открытые за месяц", 3);

        interactionsCanidatesPeriodRadioButtonGroup.setOptionsMap(onlyOpenedPositionMap);
    }

    public void setJobCandidate(JobCandidate jobCandidate) {
        this.jobCandidate = jobCandidate;
    }

    public JobCandidate getJobCandidate() {
        return jobCandidate;
    }

    private OpenPosition openPosition;

    public OpenPosition getOpenPosition() {
        return openPosition;
    }

    public void setOpenPosition(OpenPosition openPosition) {
        this.openPosition = openPosition;
        jobCandidateDl.setParameter("vacancy", this.openPosition);
        jobCandidateDl.load();
    }

    public void setSignSendToClent(Boolean signSendToClent) {
        jobCandidateDl.setParameter("signSendToClient", true);
        jobCandidateDl.load();
    }

    @Install(to = "iteractionListsTable.active", subject = "columnGenerator")
    private Component iteractionListsTableActiveColumnGenerator(DataGrid.ColumnGeneratorEvent<JobCandidate> event) {
        HBoxLayout retHBox = uiComponents.create(HBoxLayout.class);
        retHBox.setWidthFull();
        retHBox.setHeightFull();

        Label retLabel = uiComponents.create(Label.class);
        retLabel.setAlignment(Component.Alignment.MIDDLE_CENTER);

        Boolean flag = false;

        for (IteractionList iteractionList : event.getItem().getIteractionList()) {
            if (iteractionList.getIteractionType() != null) {
                if (iteractionList.getIteractionType().getSignEndCase() != null) {
                    if (iteractionList.getIteractionType().getSignEndCase()) {
                        retLabel.setIconFromSet(CubaIcon.MINUS_CIRCLE);
                        retLabel.setStyleName("open-position-pic-center-large-red");
                        retLabel.setDescription("С кандидатом кейс завершен по этой вакансии");

                        flag = true;
                        break;
                    }
                }
            }
        }

        if (!flag) {
            retLabel.setIconFromSet(CubaIcon.PLUS_CIRCLE);
            retLabel.setStyleName("open-position-pic-center-large-green");
            retLabel.setDescription("С кандидатом кейс НЕ завершен по этой вакансии");
        }

        retHBox.add(retLabel);

        return retHBox;
    }

    @Install(to = "iteractionListsTable.lastInterationName", subject = "columnGenerator")
    private Component iteractionListsTableLastInterationNameColumnGenerator(DataGrid.ColumnGeneratorEvent<JobCandidate> event) {

        Date startDate = event.getItem().getIteractionList().get(0).getDateIteraction();
        String startInteractionName = null;
        HBoxLayout retBox = uiComponents.create(HBoxLayout.class);
        retBox.setWidthFull();
        retBox.setHeightFull();
        retBox.setAlignment(Component.Alignment.MIDDLE_CENTER);

        if (event.getItem().getIteractionList() != null) {
            if (event.getItem().getIteractionList().get(0) != null) {
                if (event.getItem().getIteractionList().get(0).getDateIteraction() != null) {
                    startInteractionName = event
                            .getItem()
                            .getIteractionList()
                            .get(0)
                            .getIteractionType()
                            .getIterationName();
                }
            }
        }

        if (startDate != null) {
            Label retLabel = uiComponents.create(Label.class);
            retLabel.setStyleName("table-wordwrap");

            for (IteractionList iteractionList : event.getItem().getIteractionList()) {
                if (iteractionList.getDateIteraction() != null) {
                    if (startDate.before(iteractionList.getDateIteraction())) {
                        startDate = iteractionList.getDateIteraction();
                        startInteractionName = iteractionList.getIteractionType().getIterationName();
                    }
                }
            }


            retLabel.setValue(startInteractionName);
            retLabel.setAlignment(Component.Alignment.MIDDLE_CENTER);

            retBox.add(retLabel);
        }

        return retBox;
    }

    @Install(to = "iteractionListsTable.recruter", subject = "columnGenerator")
    private Component iteractionListsTableRecruterColumnGenerator(DataGrid.ColumnGeneratorEvent<JobCandidate> event) {
        Label retLabel = uiComponents.create(Label.class);
        HBoxLayout retBox = uiComponents.create(HBoxLayout.class);
        User recruter = null;

        for (IteractionList iteractionList : event.getItem().getIteractionList()) {
            if (iteractionList.getIteractionType() != null) {
                if (iteractionList.getIteractionType().getSignOurInterview() != null) {
                    if (iteractionList.getIteractionType().getSignOurInterview()) {
                        recruter = iteractionList.getRecrutier();
                    }
                }
            }
        }

        if (recruter != null) {
            retLabel.setValue(recruter.getName());
            retLabel.setAlignment(Component.Alignment.MIDDLE_LEFT);
            retBox.setWidthFull();
            retBox.setHeightFull();
            retBox.setAlignment(Component.Alignment.MIDDLE_CENTER);
            retBox.add(retLabel);
        }
        return retBox;
    }

    @Install(to = "iteractionListsTable.sourcer", subject = "columnGenerator")
    private Component iteractionListsTableSourcerColumnGenerator(DataGrid.ColumnGeneratorEvent<JobCandidate> event) {
        Label retLabel = uiComponents.create(Label.class);
        HBoxLayout retBox = uiComponents.create(HBoxLayout.class);
        User sourcer = null;

        for (IteractionList iteractionList : event.getItem().getIteractionList()) {
            if (iteractionList.getIteractionType() != null) {
                if (iteractionList.getIteractionType().getSignOurInterviewAssigned() != null) {
                    if (iteractionList.getIteractionType().getSignOurInterviewAssigned()) {
                        sourcer = iteractionList.getRecrutier();
                    }
                }
            }
        }

        if (sourcer != null) {
            retLabel.setValue(sourcer.getName());
            retLabel.setAlignment(Component.Alignment.MIDDLE_LEFT);
            retBox.setWidthFull();
            retBox.setHeightFull();
            retBox.setAlignment(Component.Alignment.MIDDLE_CENTER);
            retBox.add(retLabel);
        }
        return retBox;
    }

    public void setHeader(OpenPosition openPosition) {
        vacancyLabel.setValue(openPosition.getVacansyName());
    }

    @Install(to = "iteractionListsTable.jobCandidateCard", subject = "columnGenerator")
    private Component iteractionListsTableJobCandidateCardColumnGenerator(DataGrid.ColumnGeneratorEvent<JobCandidate> event) {
        HBoxLayout retBox = uiComponents.create(HBoxLayout.class);
        retBox.setHeightFull();
        retBox.setWidthFull();
        retBox.setAlignment(Component.Alignment.MIDDLE_CENTER);

        PopupButton retButton = uiComponents.create(PopupButton.class);
        retButton.setWidthAuto();
        retButton.setHeightAuto();
        retButton.setIcon(CubaIcon.BARS.source());
        retButton.setAlignment(Component.Alignment.MIDDLE_CENTER);

        retButton.addAction(new BaseAction("jobCandidateCardAction")
                .withCaption(messageBundle.getMessage("msgJobCandidate"))
                .withHandler(event1 -> {
                    screenBuilders.editor(JobCandidate.class, this)
                            .editEntity(event.getItem())
                            .build()
                            .show();
                }));

        retButton.addAction(new BaseAction("interationListAction")
        .withCaption(messageBundle.getMessage("msgInteractionList"))
        .withHandler(event1 -> {
            IteractionListSimpleBrowse screen = screenBuilders.lookup(IteractionList.class, this)
                    .withScreenClass(IteractionListSimpleBrowse.class)
                    .build();
            screen.setJobCandidate(event.getItem());
            screen.setOpenPosition(this.openPosition);
            screen.show();
        }));

        retBox.add(retButton);
        return retBox;
}

    @Install(to = "iteractionListsTable.lastIteraction", subject = "columnGenerator")
    private Component iteractionListsTableLastIteractionColumnGenerator(DataGrid.ColumnGeneratorEvent<JobCandidate> event) {
        IteractionList iteractionList = interactionService.getLastIteraction(event.getItem());
        HBoxLayout retHBox = uiComponents.create(HBoxLayout.class);
        retHBox.setWidthFull();
        retHBox.setHeightFull();

        Label retLabel = uiComponents.create(Label.class);
        retLabel.setWidthAuto();
        retLabel.setHeightAuto();
        retLabel.setAlignment(Component.Alignment.MIDDLE_CENTER);

        String date = null;
        String style = null;

        try {
            date = new SimpleDateFormat("dd-MM-yyyy").format(iteractionList.getDateIteraction());
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

//        String retStr = "";
        StringBuilder sb = new StringBuilder();
        Boolean checkBlockCandidate = event.getItem().getBlockCandidate() == null ? false : event.getItem().getBlockCandidate();

        if (checkBlockCandidate != null) {
            if (checkBlockCandidate != true) {
                if (iteractionList != null) {
                    Calendar calendar = Calendar.getInstance();

                    if (iteractionList.getDateIteraction() != null) {
                        calendar.setTime(iteractionList.getDateIteraction());
                    } else {
                        calendar.setTime(event.getItem().getCreateTs());
                    }

                    calendar.add(Calendar.MONTH, 1);

                    Calendar calendar1 = Calendar.getInstance();

                    if (calendar.after(calendar1)) {
                        if (iteractionList.getRecrutier() != null) {
                            if (!iteractionList.getRecrutier().equals(userSession.getUser())) {
//                                retStr = "button_table_red";
                                style = "button_table_red";
                            } else {
//                                retStr = "button_table_yellow";
                                style = "button_table_yellow";
                            }
                        }
                    } else {
//                        retStr = "button_table_green";
                        style = "button_table_green";
                    }
                } else {
//                    retStr = "button_table_white";
                    style = "button_table_white";
                }
            } else {
//                retStr = "button_table_black";
                style = "button_table_black";
            }
        } else {
//            retStr = "button_table_green";
            style = "button_table_green";
        }

        retLabel.setValue(date != null ? date : "нет");
        retLabel.setStyleName(style);
        retLabel.setAlignment(Component.Alignment.MIDDLE_CENTER);
        retLabel.setDescription(iteractionList.getIteractionType().getIterationName());

        retHBox.add(retLabel);

        return retHBox;
    }

    @Install(to = "iteractionListsTable.fullName", subject = "columnGenerator")
    private Component iteractionListsTableFullNameColumnGenerator(DataGrid.ColumnGeneratorEvent<JobCandidate> event) {
        HBoxLayout retHBox = uiComponents.create(HBoxLayout.class);
        retHBox.setWidthFull();
        retHBox.setHeightFull();

        Label retLabel = uiComponents.create(Label.class);
        retLabel.setWidthAuto();
        retLabel.setHeightAuto();
        retLabel.setAlignment(Component.Alignment.MIDDLE_LEFT);
        retLabel.addStyleName("table-wordwrap");
        retLabel.setValue(event.getItem().getFullName());

        retHBox.add(retLabel);
        return retHBox;
    }
}