package com.company.itpearls.web.screens.openposition.openpositionfragments;

import com.company.itpearls.entity.*;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;
import java.util.*;

@UiController("itpearls_OpenPositionDetailScreenFragment")
@UiDescriptor("open-position-detail-screen-fragment.xml")
public class OpenPositionDetailScreenFragment extends ScreenFragment {
    @Inject
    private CollectionContainer<OpenPosition> openPositionsDc;
    @Inject
    private Image companyLogoImage;

    @Subscribe
    public void onAttach(AttachEvent event) {
       setDefaultCompanyLogo();
    }

    public void setDefaultCompanyLogo() {
        if (companyLogoImage.getSource() == null) {
            companyLogoImage.setSource(ThemeResource.class).setPath("icons/no-company.png");
        }
    }

    protected OpenPosition openPosition = null;
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

    Map<Integer, String> remoteWork = new HashMap<Integer, String>();
    @Inject
    private Label<String> remoteWorkTextField;

    public void setRemoteLabel() {
        String ret = remoteWork.get(openPosition.getRemoteWork());
        remoteWorkTextField.setValue(remoteWork.get(openPosition.getRemoteWork()));
    }

    public void setOpenPosition(OpenPosition openPosition) {
        this.openPosition = openPosition;
    }

    public OpenPosition getOpenPosition() {
        return openPosition;
    }

    public void setLabels() {
        remoteWork.put(0, "Нет");
        remoteWork.put(1, "Удаленная работа");
        remoteWork.put(2, "Частично 50/50");

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

        setRemoteLabel();
    }

    private static final String QUERY_SUBSCRIBERS = "select e from itpearls_RecrutiesTasks e where e.endDate >= :currentDate and e.openPosition = :openPosition";

    public void setSubscribersRecruters() {

        List<RecrutiesTasks> tasks = dataManager.load(RecrutiesTasks.class)
                .query(QUERY_SUBSCRIBERS)
                .parameter("openPosition", openPosition)
                .parameter("currentDate", new Date())
                .view("recrutiesTasks-view")
                .list();

        if (tasks.size() != 0) {
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
                recrutersGroupBox.setVisible(true);
            }
        } else {
            recrutersGroupBox.setVisible(false);
        }
    }
}