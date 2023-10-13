package com.company.itpearls.web.widgets.others;

import com.company.itpearls.entity.ExtUser;
import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.cuba.gui.components.FileDescriptorResource;
import com.haulmont.cuba.gui.components.Image;
import com.haulmont.cuba.gui.model.InstanceContainer;
import com.haulmont.cuba.gui.model.InstanceLoader;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;

@UiController("itpearls_MyPhotoWidget")
@UiDescriptor("my-photo-widget.xml")
@DashboardWidget(name="My Photo")
public class MyPhotoWidget extends ScreenFragment {
    @Inject
    private InstanceLoader<ExtUser> userDl;
    @Inject
    private UserSession userSession;
    @Inject
    private InstanceContainer<ExtUser> userDc;
    @Inject
    private Image myPhotoImage;

    @Subscribe
    public void onInit(InitEvent event) {
       setMyPhoto();
    }

    private void setMyPhoto() {

        try {
            userDl.setParameter("login", userSession.getUser().getLogin());
            userDl.load();

            ExtUser curUser = userDc.getItem();
            myPhotoImage.setSource(FileDescriptorResource.class)
                    .setFileDescriptor(curUser.getFileImageFace());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}