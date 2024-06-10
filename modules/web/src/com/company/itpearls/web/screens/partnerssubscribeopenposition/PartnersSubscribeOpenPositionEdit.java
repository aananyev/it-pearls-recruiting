package com.company.itpearls.web.screens.partnerssubscribeopenposition;

import com.company.itpearls.core.StdImage;
import com.company.itpearls.entity.OpenPosition;
import com.company.itpearls.entity.Partners;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.PartnersSubscribeOpenPosition;

import javax.inject.Inject;

@UiController("itpearls_PartnersSubscribeOpenPosition.edit")
@UiDescriptor("partners-subscribe-open-position-edit.xml")
@EditedEntityContainer("partnersSubscribeOpenPositionDc")
@LoadDataBeforeShow
public class PartnersSubscribeOpenPositionEdit extends StandardEditor<PartnersSubscribeOpenPosition> {
    @Inject
    private LookupPickerField<Partners> partnerField;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private LookupPickerField<OpenPosition> openPositionField;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        partnerField.setOptionImageProvider(this::setPartnerField);
        openPositionField.setOptionImageProvider(this::setOpenPositionField);
    }

    private Resource setOpenPositionField(OpenPosition openPosition) {
        Image retImage = uiComponents.create(Image.class);
        retImage.setScaleMode(Image.ScaleMode.SCALE_DOWN);
        retImage.setWidth("30px");

        if (openPosition.getProjectName().getProjectLogo() != null) {
            return retImage.createResource(FileDescriptorResource.class)
                    .setFileDescriptor(openPosition.getProjectName().getProjectLogo());
        } else {
            return retImage.createResource(ThemeResource.class).setPath(StdImage.NO_COMPANY);
        }
    }

    private Resource setPartnerField(Partners partners) {
        Image retImage = uiComponents.create(Image.class);
        retImage.setScaleMode(Image.ScaleMode.SCALE_DOWN);
        retImage.setWidth("30px");

        if (partners.getFileCompanyLogo() != null) {
            return retImage.createResource(FileDescriptorResource.class)
                    .setFileDescriptor(partners
                            .getFileCompanyLogo());
        } else {
            return retImage.createResource(ThemeResource.class).setPath(StdImage.NO_COMPANY);
        }
    }

}