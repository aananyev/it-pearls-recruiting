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
    private GroupTable<PersonelReserve> personelReservesTable;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private MessageBundle messageBundle;
    @Inject
    private CheckBox inNotWorkCheckBox;

    private final static String statusColumn = "statusColumn";
    private final static String inWorkColumn = "inWorkColumn";
    private final static String faceImage = "faceImage";


    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        allCandidatesCheckBox.setValue(true);
        activesCheckBox.setValue(true);
        inNotWorkCheckBox.setValue(true);

        initPersonelReservesTable();
    }

    private Boolean currentDateBeforeCheck(Date date, int days) {
        Calendar calendar = Calendar.getInstance();
//        calendar.setTime(date);
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

    private void initPersonelReservesTable() {
        personelReservesTable.addGeneratedColumn(statusColumn, entity -> {
            Label retLabel = uiComponents.create(Label.class);

            if (entity.getEndDate().before(new Date())) { // просрочено
                retLabel.setIconFromSet(CubaIcon.CANCEL);
                retLabel.setStyleName("pic-center-large-red");
                retLabel.setDescription(messageBundle.getMessage("msgReserveIsOverdue"));
            } else {

                if (currentDateBeforeCheck(entity.getEndDate(), 7)) {
                    retLabel.setIconFromSet(CubaIcon.CIRCLE);
                    retLabel.setStyleName("pic-center-large-orange");
                    retLabel.setDescription(messageBundle.getMessage("msgReserveIsSevenDays"));
                } else {
                    if (currentDateBeforeCheck(entity.getEndDate(), 30)) {
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

            return retLabel;
        });

        personelReservesTable.addGeneratedColumn(faceImage, entity -> {
            HBoxLayout hBox = uiComponents.create(HBoxLayout.class);
            Image image = uiComponents.create(Image.NAME);

            if (entity.getJobCandidate().getFileImageFace() != null) {
                try {
/*                    image.setValueSource(personelReservesTable.getInstanceContainer(entity),
                            "jobCandidate.fileImageFace"); */

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

        personelReservesTable.getColumn(statusColumn).setWidth(50);
        personelReservesTable.getColumn(statusColumn).setCollapsed(false);
        personelReservesTable.getColumn(statusColumn).setAlignment(Table.ColumnAlignment.CENTER);
        personelReservesTable.getColumn(statusColumn).setCaption(
                messageBundle.getMessage("msgStatusColumn"));

        personelReservesTable.getColumn(inWorkColumn).setWidth(50);
        personelReservesTable.getColumn(inWorkColumn).setCollapsed(false);
        personelReservesTable.getColumn(inWorkColumn).setAlignment(Table.ColumnAlignment.CENTER);
        personelReservesTable.getColumn(inWorkColumn).setCaption(
                messageBundle.getMessage("msgInWorkColumn"));
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

    public Component inWorkColumnGenerator(Entity entity) {
        Label retLabel = uiComponents.create(Label.class);
        retLabel.setIconFromSet(CubaIcon.QUESTION_CIRCLE);
        retLabel.setAlignment(Component.Alignment.MIDDLE_CENTER);
        retLabel.setStyleName("pic-center-large-gray");
        retLabel.setDescription(messageBundle.getMessage("msgEmptyWork"));

        for (IteractionList iteractionList :
                ((JobCandidate) entity.getValue("jobCandidate")).getIteractionList()) {
            if (iteractionList
                    .getDateIteraction()
                    .after((Date) entity.getValue("date"))) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy");

                retLabel.setStyleName("pic-center-large-red");
                retLabel.setIconFromSet(CubaIcon.SIGN_IN);
                retLabel.setDescription(messageBundle.getMessage("msgInWork")
                        + " "
                        + iteractionList.getVacancy().getVacansyName()
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

        return retLabel;
    }
}