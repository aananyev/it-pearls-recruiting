package com.company.itpearls.web.screens.personelreserve;

import com.company.itpearls.entity.JobCandidate;
import com.haulmont.cuba.core.entity.FileDescriptor;
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

    private final static String statusColumn = "statusColumn";
    private final static String faceImage = "faceImage";

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        allCandidatesCheckBox.setValue(true);
        activesCheckBox.setValue(true);

        initPersonelReservesTable();
    }

    private Boolean currentDateBeforeCheck(Date date, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_YEAR, days);
        Date currentDate = calendar.getTime();

        return date.after(currentDate) ? true : false;
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
                    retLabel.setStyleName("pic-center-large-yellow");
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
}