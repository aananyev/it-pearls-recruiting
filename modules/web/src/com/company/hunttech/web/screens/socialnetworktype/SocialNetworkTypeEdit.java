package com.company.hunttech.web.screens.socialnetworktype;

import com.company.hunttech.entity.SocialNetworkType;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.FileDescriptorResource;
import com.haulmont.cuba.gui.components.FileUploadField;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.screen.*;
import com.hunttech.hrm.gui.components.OvaFallbackImage;

import javax.inject.Inject;

@UiController("hunttech_SocialNetworkType.edit")
@UiDescriptor("social-network-type-edit.xml")
@EditedEntityContainer("socialNetworkTypeDc")
@LoadDataBeforeShow
public class SocialNetworkTypeEdit extends StandardEditor<SocialNetworkType> {
    @Inject
    private OvaFallbackImage snLogo;
    @Inject
    private FileUploadField snLogoFileUpload;
    @Inject
    private TextField<String> socialNetworkField;
    @Inject
    private Button mainNav;

    @Subscribe("snLogoFileUpload")
    public void onSnLogoFileUploadFileUploadSucceed(FileUploadField.FileUploadSucceedEvent event) {
        try {
            FileDescriptorResource fileDescriptorResource =
                    snLogo.createResource(FileDescriptorResource.class)
                            .setFileDescriptor(snLogoFileUpload.getFileDescriptor());

            snLogo.setSource(fileDescriptorResource);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        // Если логотип не задан — показать fallback-аватар OvaFallbackImage
        // (эталон JobCandidateEdit: applyFallback при отсутствии файла).
        if (getEditedEntity().getLogo() == null) {
            snLogo.applyFallback();
        }
    }

    /**
     * Презентационная навигация: переводит фокус к первому полю «Основных данных»
     * и подсвечивает активный пункт sidebar. Entity, loaders и lifecycle не затрагиваются.
     */
    public void focusMainSection() {
        socialNetworkField.focus();
        setActiveNavigation(mainNav);
    }

    private void setActiveNavigation(Button activeButton) {
        activeButton.addStyleName("label-nav-item-active");
    }
}
