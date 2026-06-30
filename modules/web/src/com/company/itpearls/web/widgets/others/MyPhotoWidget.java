package com.company.itpearls.web.widgets.others;

import com.company.itpearls.entity.ExtUser;
import com.company.itpearls.gui.components.OvalImage;
import com.company.itpearls.web.util.FileDescriptorImageHelper;
import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.HBoxLayout;
import com.haulmont.cuba.gui.components.Image;
import com.haulmont.cuba.gui.model.InstanceContainer;
import com.haulmont.cuba.gui.model.InstanceLoader;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;

@UiController("itpearls_MyPhotoWidget")
@UiDescriptor("my-photo-widget.xml")
@DashboardWidget(name="Моё фото")
public class MyPhotoWidget extends ScreenFragment {
    @Inject
    private InstanceLoader<ExtUser> userDl;
    @Inject
    private UserSession userSession;
    @Inject
    private InstanceContainer<ExtUser> userDc;
    @Inject
    private HBoxLayout photoHBox;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private FileLoader fileLoader;

    private OvalImage myPhotoImage;

    @Subscribe
    public void onInit(InitEvent event) {
        myPhotoImage = uiComponents.create(OvalImage.NAME);
        myPhotoImage.setOvalWidth("150px");
        myPhotoImage.setScaleMode(Image.ScaleMode.SCALE_DOWN);
        myPhotoImage.setAlignment(Component.Alignment.BOTTOM_LEFT);
        photoHBox.add(myPhotoImage);
        setMyPhoto();
    }

    private void setMyPhoto() {
        try {
            userDl.setParameter("login", userSession.getUser().getLogin());
            userDl.load();

            ExtUser curUser = userDc.getItem();
            FileDescriptorImageHelper.setUserProfilePhoto(myPhotoImage, fileLoader, curUser);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
