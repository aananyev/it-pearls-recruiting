package com.company.itpearls.web.screens.socialnetworktype;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.SocialNetworkType;
import com.haulmont.cuba.gui.screen.LookupComponent;

import javax.inject.Inject;

@UiController("itpearls_SocialNetworkType.browse")
@UiDescriptor("social-network-type-browse.xml")
@LookupComponent("socialNetworkTypesTable")
@LoadDataBeforeShow
public class SocialNetworkTypeBrowse extends StandardLookup<SocialNetworkType> {
    @Inject
    private UiComponents uiComponents;

    public Component snLogoGenerator(SocialNetworkType entity) {
        HBoxLayout retBox = uiComponents.create(HBoxLayout.class);
        retBox.setWidthFull();
        retBox.setHeightFull();

        StringBuilder sb = new StringBuilder();
        sb.append("<h4>")
                .append(entity.getSocialNetwork())
                .append("</h4><br><br>")
                .append((entity.getComment() != null ? entity.getComment() : ""));

        Image image = uiComponents.create(Image.class);
        image.setDescriptionAsHtml(true);
        image.setScaleMode(Image.ScaleMode.SCALE_DOWN);
        image.setWidth("30px");
        image.setHeight("30px");
        image.setStyleName("icon-no-border-30px");
        image.setAlignment(Component.Alignment.MIDDLE_CENTER);
        image.setDescription(sb.toString());

        if (entity.getLogo() != null) {
            image.setSource(FileDescriptorResource.class)
                    .setFileDescriptor(entity
                            .getLogo());
        } else {
            image.setSource(ThemeResource.class).setPath("icons/no-company.png");
        }

        retBox.add(image);
        return retBox;
    }
}