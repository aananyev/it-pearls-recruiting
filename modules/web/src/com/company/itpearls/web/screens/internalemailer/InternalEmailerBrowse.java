package com.company.itpearls.web.screens.internalemailer;

import com.company.itpearls.entity.InternalEmailerTemplate;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.InternalEmailer;
import com.haulmont.cuba.gui.screen.LookupComponent;

import javax.inject.Inject;

@UiController("itpearls_InternalEmailer.browse")
@UiDescriptor("internal-emailer-browse.xml")
@LookupComponent("emailersTable")
@LoadDataBeforeShow
public class InternalEmailerBrowse extends StandardLookup<InternalEmailer> {
    @Inject
    private UiComponents uiComponents;
    @Inject
    private DataManager dataManager;
    @Inject
    private DataGrid<InternalEmailer> emailersTable;

    @Install(to = "emailersTable.replyInternalEmailerColumn", subject = "columnGenerator")
    private Component emailersTableReplyInternalEmailerColumnColumnGenerator(DataGrid.ColumnGeneratorEvent<InternalEmailer> event) {
        final String QUERY_GET_REPLY = "select e from itpearls_InternalEmailer e " +
                "where e.replyInternalEmailer = :replyInternalEmailer";

        HBoxLayout retHbox = uiComponents.create(HBoxLayout.class);
        retHbox.setHeightFull();
        retHbox.setWidthFull();

        Label replyLabel = uiComponents.create(Label.class);
        replyLabel.setAlignment(Component.Alignment.MIDDLE_CENTER);
        replyLabel.setStyleName("h1-green");

        if (dataManager
                .load(InternalEmailer.class)
                .query(QUERY_GET_REPLY)
                .parameter("replyInternalEmailer", event.getItem())
                .view("internalEmailer-view")
                .list()
                .size() > 0) {
            replyLabel.setVisible(true);
            replyLabel.setIconFromSet(CubaIcon.MAIL_REPLY);
            retHbox.add(replyLabel);
        } else {
            replyLabel.setVisible(false);
        }


        return retHbox;
    }

    @Install(to = "emailersTable.toEmail", subject = "columnGenerator")
    private Component emailersTableToEmailColumnGenerator(DataGrid.ColumnGeneratorEvent<InternalEmailer> event) {
        if (event.getItem().getToEmail().getFileImageFace() != null) {
            return generateImageWithLabel(event.getItem().getToEmail().getFileImageFace(),
                    event.getItem().getToEmail().getFullName());
        } else {
            return generateImageWithLabel(null, event.getItem().getToEmail().getFullName());
        }
    }

    @Install(to = "emailersTable.fromEmail", subject = "columnGenerator")
    private Component emailersTableFromEmailColumnGenerator(DataGrid.ColumnGeneratorEvent<InternalEmailer> event) {
        if (event.getItem().getFromEmail().getFileImageFace() != null) {
            return generateImageWithLabel(event.getItem().getFromEmail().getFileImageFace(),
                    event.getItem().getFromEmail().getName());
        } else {
            return generateImageWithLabel(null, event.getItem().getFromEmail().getName());
        }
    }

    private HBoxLayout generateImageWithLabel(FileDescriptor fileDescriptor, String name) {
        HBoxLayout retHBox = uiComponents.create(HBoxLayout.class);
        retHBox.setWidthFull();
        retHBox.setHeightFull();
        retHBox.setSpacing(true);

        Image image = uiComponents.create(Image.class);
        image.setWidth("20px");
        image.setHeight("20px");
        image.setAlignment(Component.Alignment.MIDDLE_CENTER);
        image.setScaleMode(Image.ScaleMode.SCALE_DOWN);

        if (fileDescriptor != null) {
            image.setSource(FileDescriptorResource.class)
                    .setFileDescriptor(fileDescriptor);
        } else {
            image.setSource(ThemeResource.class)
                    .setPath("icons/no-programmer.jpeg");
        }

        image.setStyleName("circle-20px");

        Label label = uiComponents.create(Label.class);
        label.setValue(name);
        label.setStyleName("table-wordwrap");
        label.setAlignment(Component.Alignment.MIDDLE_LEFT);

        retHBox.add(image);
        retHBox.add(label);

        retHBox.expand(label);

        return retHBox;
    }
}
