package com.company.itpearls.web.screens.partnersperson;

import com.company.itpearls.core.StdImage;
import com.company.itpearls.entity.Partners;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.company.itpearls.web.screens.person.PersonEdit;

import javax.inject.Inject;

@UiController("itpearls_PartnersPerson.edit")
@UiDescriptor("partners-person-edit.xml")
public class PartnersPersonEdit extends PersonEdit {
    @Inject
    private LookupPickerField<Partners> partnersField;
    @Inject
    private UiComponents uiComponents;

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
    }

    private Resource setPartnersLogo(Partners partners) {
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

    @Subscribe
    public void onBeforeShow1(BeforeShowEvent event) {
        partnersField.setOptionImageProvider(this::setPartnersLogo);
    }
}