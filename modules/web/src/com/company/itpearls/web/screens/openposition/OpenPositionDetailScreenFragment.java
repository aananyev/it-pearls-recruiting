package com.company.itpearls.web.screens.openposition;

import com.company.itpearls.entity.ExtUser;
import com.company.itpearls.entity.JobCandidate;
import com.company.itpearls.entity.OpenPosition;
import com.company.itpearls.entity.RecrutiesTasks;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.data.value.ContainerValueSource;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

@UiController("itpearls_OpenPositionDetailScreenFragment")
@UiDescriptor("open-position-detail-screen-fragment.xml")
public class OpenPositionDetailScreenFragment extends ScreenFragment {
    private OpenPosition openPosition = null;
    @Inject
    private Label<String> needExeciseLabel;
    @Inject
    private Label<String> needLetterLabel;
    @Inject
    private Label<String> salaryComment1;
    @Inject
    private Label<String> salaryComment2;
    @Inject
    private DataManager dataManager;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private GroupBoxLayout recrutersGroupBox;
    @Inject
    private HBoxLayout recrutersHBox;

    public void setOpenPosition(OpenPosition openPosition) {
        this.openPosition = openPosition;
    }

    public OpenPosition getOpenPosition() {
        return openPosition;
    }

    public void setLabels() {
        if (openPosition != null) {
            if (openPosition.getNeedExercise() != null) {
                if (openPosition.getNeedExercise()) {
                    needExeciseLabel.setVisible(true);
                } else {
                    needExeciseLabel.setVisible(false);
                }
            }

            if (openPosition.getNeedLetter() != null) {
                if (openPosition.getNeedLetter()) {
                    needLetterLabel.setVisible(true);
                } else {
                    needLetterLabel.setVisible(false);
                }
            }
        }

        if (openPosition.getSalaryComment() != null) {
            salaryComment1.setValue("\ud83d\udcc3");
            salaryComment2.setValue(" \ud83d\udcc3");

            salaryComment1.setDescription(openPosition.getSalaryComment());
            salaryComment2.setDescription(openPosition.getSalaryComment());
        }
    }

    public void setSubscribersRecruters() {
        final String QUERY_SUBSCRIBERS = "select e "
                + "from itpearls_RecrutiesTasks e "
                + "where e.endDate >= :currentDate and "
                + "e.openPosition = :openPosition";

        List<RecrutiesTasks> tasks = dataManager.load(RecrutiesTasks.class)
                .query(QUERY_SUBSCRIBERS)
                .parameter("openPosition", openPosition)
                .parameter("currentDate", new Date())
                .view("recrutiesTasks-view")
                .list();

        for (RecrutiesTasks user : tasks) {
            Image image = uiComponents.create(Image.class);

            image.setScaleMode(Image.ScaleMode.SCALE_DOWN);
            image.setWidth("30px");
            image.setStyleName("circle-30px");
            image.setDescription(user.getReacrutier().getName());

            try {
                ExtUser extUser = (ExtUser) user.getReacrutier();

                if (extUser.getFileImageFace() != null) {
                    image.setSource(FileDescriptorResource.class)
                            .setFileDescriptor(extUser.getFileImageFace());
                } else {
                    image.setSource(ThemeResource.class).setPath("icons/no-programmer.jpeg");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            recrutersHBox.add(image);
        }
    }
}