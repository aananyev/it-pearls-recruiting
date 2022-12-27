package com.company.itpearls.web.screens.extuser;

import com.company.itpearls.entity.ExtUser;
import com.company.itpearls.entity.IteractionList;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.Image;
import com.haulmont.cuba.gui.components.data.value.ContainerValueSource;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.security.entity.User;

import javax.inject.Inject;
import javax.swing.text.html.parser.Entity;
import java.awt.*;

@UiController("itpearls_ExtUserBrowse")
@UiDescriptor("ext-user-browse.xml")
public class ExtUserBrowse extends Screen {

    @Inject
    private UiComponents uiComponents;
    @Inject
    private GroupTable<User> usersTable;

    @Subscribe
    public void onInit(InitEvent event) {
        setColumnIconGenerator();
    }

    private void setColumnIconGenerator() {
    }

    @Install(to = "usersTable.icon", subject = "columnGenerator")
    private Component usersTableIconColumnGenerator(User user) {
        HBoxLayout retBox = uiComponents.create(HBoxLayout.class);
        Image retImage = uiComponents.create(Image.class);

        retBox.setWidthFull();
        retBox.setHeightFull();
        retBox.setAlignment(Component.Alignment.MIDDLE_CENTER);

        retImage.setAlignment(Component.Alignment.MIDDLE_CENTER);
        retImage.setScaleMode(Image.ScaleMode.SCALE_DOWN);
        retImage.setWidth("20px");
        retImage.setStyleName("circle-20px");

        retImage.setSource(FileDescriptorResource.class).setFileDescriptor( ((ExtUser)user).getFileImageFace());

        retBox.add(retImage);

        return retBox;
    }

    public Component userFaceColumn(ExtUser entity) {
        HBoxLayout retBox = uiComponents.create(HBoxLayout.class);
        Image retImage = uiComponents.create(Image.class);

        retBox.setWidthFull();
        retBox.setHeightFull();
        retBox.setAlignment(Component.Alignment.MIDDLE_CENTER);

        retImage.setWidthFull();
        retImage.setHeightFull();
        retImage.setAlignment(Component.Alignment.MIDDLE_CENTER);
        retImage.setScaleMode(Image.ScaleMode.SCALE_DOWN);

        retBox.add(retImage);

        return retBox;
    }
}