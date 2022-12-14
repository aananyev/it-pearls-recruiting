package com.company.itpearls.web.screens.applicationrecruitment;

import com.haulmont.bpm.entity.ProcAttachment;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.ApplicationRecruitment;

import javax.inject.Inject;

@UiController("itpearls_ApplicationRecruitment.browse")
@UiDescriptor("application-recruitment-browse.xml")
@LookupComponent("applicationRecruitmentsTable")
@LoadDataBeforeShow
public class ApplicationRecruitmentBrowse extends StandardLookup<ApplicationRecruitment> {
    @Inject
    private CollectionLoader<ProcAttachment> procAttachmentsDl;
    @Inject
    private CollectionContainer<ProcAttachment> procAttachmentsDc;
    @Inject
    private UiComponents uiComponents;

    public Component approvalProcColumn(Entity entity) {
        Label retLabel = uiComponents.create(Label.class);

        procAttachmentsDl.setParameter("entityId", entity.getId());
        procAttachmentsDl.load();

        if (procAttachmentsDc.getItems().size() > 0) {
            retLabel.setIconFromSet(CubaIcon.BELL);
        }

        return retLabel;
    }
}