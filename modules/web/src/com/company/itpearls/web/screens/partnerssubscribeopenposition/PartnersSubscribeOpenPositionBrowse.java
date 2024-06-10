package com.company.itpearls.web.screens.partnerssubscribeopenposition;

import com.company.itpearls.core.StdImage;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.PartnersSubscribeOpenPosition;
import com.haulmont.cuba.gui.screen.LookupComponent;

import javax.inject.Inject;

@UiController("itpearls_PartnersSubscribeOpenPosition.browse")
@UiDescriptor("partners-subscribe-open-position-browse.xml")
@LookupComponent("partnersSubscribeOpenPositionsTable")
@LoadDataBeforeShow
public class PartnersSubscribeOpenPositionBrowse extends StandardLookup<PartnersSubscribeOpenPosition> {
    @Inject
    private UiComponents uiComponents;

    public Component partnerColumnGenerator(PartnersSubscribeOpenPosition partnersSubscribeOpenPosition) {
        return getImageLabel(partnersSubscribeOpenPosition.getPartner().getFileCompanyLogo(),
                partnersSubscribeOpenPosition.getPartner().getComanyName());
    }

    public Component openPositionProjectGenerator(PartnersSubscribeOpenPosition partnersSubscribeOpenPosition) {
        return getImageLabel(partnersSubscribeOpenPosition.getOpenPosition().getProjectName().getProjectLogo(),
                partnersSubscribeOpenPosition.getOpenPosition().getVacansyName());
    }

    private HBoxLayout getImageLabel(FileDescriptor fileDescriptorImage, String description) {
        HBoxLayout retHbox = uiComponents.create(HBoxLayout.class);
        retHbox.setSpacing(true);
        retHbox.setWidth("100%");

        Image retImage = uiComponents.create(Image.class);
        retImage.setScaleMode(Image.ScaleMode.SCALE_DOWN);
        retImage.setWidth("50px");
        retImage.setAlignment(Component.Alignment.MIDDLE_CENTER);

        if (fileDescriptorImage != null) {
            retImage.setSource(FileDescriptorResource.class)
                    .setFileDescriptor(fileDescriptorImage);
        } else {
            retImage.setSource(ThemeResource.class).setPath(StdImage.NO_COMPANY);
        }

        Label retLabel = uiComponents.create(Label.class);
        retLabel.setStyleName("table-wordwrap");
        retLabel.setWidthFull();
        retLabel.setAlignment(Component.Alignment.MIDDLE_LEFT);
        retLabel.setValue(description);

        retHbox.add(retImage);
        retHbox.add(retLabel);

        retHbox.expand(retLabel);

        return retHbox;
    }
}