package com.company.itpearls.web.screens.personelreserve;

import com.company.itpearls.entity.IteractionList;
import com.company.itpearls.entity.JobCandidate;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.filter.LogicalCondition;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.data.value.ContainerValueSource;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.PersonelReserve;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import javax.swing.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

@UiController("itpearls_PersonelReserve.browse")
@UiDescriptor("personel-reserve-browse.xml")
@LookupComponent("personelReservesTable")
@LoadDataBeforeShow
public class PersonelReserveBrowse extends StandardLookup<PersonelReserve> {
    @Inject
    private CheckBox allCandidatesCheckBox;
    @Inject
    private CollectionLoader<PersonelReserve> personelReservesDl;
    @Inject
    private UserSession userSession;
    @Inject
    private CheckBox activesCheckBox;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private MessageBundle messageBundle;
    @Inject
    private CheckBox inNotWorkCheckBox;
    @Inject
    private DataGrid<PersonelReserve> personelReservesTable;

    private final static String statusColumn = "statusColumn";
    private final static String inWorkColumn = "inWorkColumn";
    private final static String faceImage = "faceImage";
    private final static String tableWordWrapStyle = "table-wordwrap";

    @Subscribe
    public void onInit(InitEvent event) {
//        candidateImageColumnRenderer();
        initInWorkColumnRenderer();
        initStatusColumnRenderer();
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        allCandidatesCheckBox.setValue(true);
        activesCheckBox.setValue(true);
        inNotWorkCheckBox.setValue(true);
    }

    @Install(to = "personelReservesTable.fileImageFace", subject = "columnGenerator")
    private Component personelReservesTableFileImageFaceColumnGenerator(DataGrid.ColumnGeneratorEvent<PersonelReserve> event) {
        HBoxLayout hBox = uiComponents.create(HBoxLayout.class);
        Image image = uiComponents.create(Image.NAME);

        if (event.getItem().getJobCandidate().getFileImageFace() != null) {
            try {
                image.setSource(FileDescriptorResource.class)
                        .setFileDescriptor(event
                                .getItem()
                                .getJobCandidate()
                                .getFileImageFace());
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
    }

    private Boolean currentDateBeforeCheck(Date date, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, days);
        Date currentDate = calendar.getTime();

        return date.after(currentDate) ? false : true;
    }

    @Subscribe("inNotWorkCheckBox")
    public void onInNotWorkCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        String QUERY_ITERACTION_IN_WORK = "select e from itpearls_IteractionList e where e.iteractionDate ";

        if (event.getValue()) {
        }
    }

    private void initInWorkColumnRenderer() {
        personelReservesTable.addGeneratedColumn(inWorkColumn, entity -> {
            HBoxLayout retHBoxLayout = uiComponents.create(HBoxLayout.class);

            Label retLabel = uiComponents.create(Label.class);
            retLabel.setIconFromSet(CubaIcon.QUESTION_CIRCLE);
            retLabel.setAlignment(Component.Alignment.MIDDLE_CENTER);
            retLabel.setStyleName("pic-center-large-gray");
            retLabel.setDescription(messageBundle.getMessage("msgEmptyWork"));

            for (IteractionList iteractionList :
                    entity.getItem().getJobCandidate().getIteractionList()) {
                if (iteractionList
                        .getDateIteraction()
                        .after(entity.getItem().getDate())) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy");

                    retLabel.setStyleName("pic-center-large-red");
                    retLabel.setIconFromSet(CubaIcon.SIGN_IN);
                    String vacancy = "";

                    if (iteractionList.getVacancy() != null) {
                        if (iteractionList.getVacancy().getVacansyName() != null) {
                            iteractionList.getVacancy().getVacansyName();
                        }
                    }

                    retLabel.setDescription(messageBundle.getMessage("msgInWork")
                            + " "
                            + vacancy
                            + "\n\n"
                            + messageBundle.getMessage("msgLastInteraction")
                            + " \'"
                            + iteractionList.getIteractionType().getIterationName()
                            + "\' "
                            + messageBundle.getMessage("msgFrom")
                            + " "
                            + sdf.format(iteractionList.getDateIteraction()));
                    break;
                } else {
                    retLabel.setStyleName("pic-center-large-green");
                    retLabel.setIconFromSet(CubaIcon.CIRCLE_O);
                    retLabel.setDescription(messageBundle.getMessage("msgNotWork"));
                }
            }

            retHBoxLayout.setAlignment(Component.Alignment.MIDDLE_CENTER);

            retHBoxLayout.setWidthFull();
            retHBoxLayout.setHeightFull();
            retHBoxLayout.add(retLabel);

            return retHBoxLayout;
        });
    }

    private void initStatusColumnRenderer() {
        personelReservesTable.addGeneratedColumn(statusColumn, entity -> {
            HBoxLayout retHBoxLayout = uiComponents.create(HBoxLayout.class);

            Label retLabel = uiComponents.create(Label.class);
            retLabel.setAlignment(Component.Alignment.MIDDLE_CENTER);


            if (entity.getItem().getEndDate().before(new Date())) { // просрочено
                retLabel.setIconFromSet(CubaIcon.CANCEL);
                retLabel.setStyleName("pic-center-large-red");
                retLabel.setDescription(messageBundle.getMessage("msgReserveIsOverdue"));
            } else {

                if (currentDateBeforeCheck(entity.getItem().getEndDate(), 7)) {
                    retLabel.setIconFromSet(CubaIcon.CIRCLE);
                    retLabel.setStyleName("pic-center-large-orange");
                    retLabel.setDescription(messageBundle.getMessage("msgReserveIsSevenDays"));
                } else {
                    if (currentDateBeforeCheck(entity.getItem().getEndDate(), 30)) {
                        retLabel.setIconFromSet(CubaIcon.CIRCLE);
                        retLabel.setStyleName("pic-center-large-green");
                        retLabel.setDescription(messageBundle.getMessage("msgReserveLessMonth"));
                    } else {
                        retLabel.setIconFromSet(CubaIcon.CIRCLE);
                        retLabel.setStyleName("pic-center-large-gray");
                        retLabel.setDescription(messageBundle.getMessage("msgReserveMoreMonth"));
                    }
                }
            }

            retHBoxLayout.setAlignment(Component.Alignment.MIDDLE_CENTER);

            retHBoxLayout.setWidthFull();
            retHBoxLayout.setHeightFull();
            retHBoxLayout.add(retLabel);

            return retHBoxLayout;
        });
    }

    private void candidateImageColumnRenderer() {
        personelReservesTable.addGeneratedColumn("jobCandidate", entity -> {
            HBoxLayout hBox = uiComponents.create(HBoxLayout.class);
            Image image = uiComponents.create(Image.NAME);

            if (entity.getItem().getJobCandidate().getFileImageFace() != null) {
                try {
                    image.setSource(FileDescriptorResource.class)
                            .setFileDescriptor(entity
                                    .getItem()
                                    .getJobCandidate()
                                    .getFileImageFace());
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

    @Subscribe("allCandidatesCheckBox")
    public void onAllCandidatesCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (event.getValue()) {
            personelReservesDl.setParameter("recruter", userSession.getUser());
        } else {
            personelReservesDl.removeParameter("recruter");
        }

        personelReservesDl.load();
    }

    @Subscribe("activesCheckBox")
    public void onActivesCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (event.getValue()) {
            personelReservesDl.setParameter("actives", true);
        } else {
            personelReservesDl.removeParameter("actives");
        }

        personelReservesDl.load();
    }

    @Install(to = "personelReservesTable.jobCandidate", subject = "styleProvider")
    private String personelReservesTableJobCandidateStyleProvider(PersonelReserve personelReserve) {
        return tableWordWrapStyle;
    }

    @Install(to = "personelReservesTable.recruter", subject = "styleProvider")
    private String personelReservesTableRecruterStyleProvider(PersonelReserve personelReserve) {
        return tableWordWrapStyle;
    }

    @Install(to = "personelReservesTable.personPosition", subject = "styleProvider")
    private String personelReservesTablePersonPositionStyleProvider(PersonelReserve personelReserve) {
        return tableWordWrapStyle;
    }

    @Install(to = "personelReservesTable.openPosition", subject = "styleProvider")
    private String personelReservesTableOpenPositionStyleProvider(PersonelReserve personelReserve) {
        return tableWordWrapStyle;
    }



    public void closePersonalReserveButtonInvoke() {
    }
}