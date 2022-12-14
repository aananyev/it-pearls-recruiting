package com.company.itpearls.web.screens.applicationrecruitment;

import com.haulmont.bpm.entity.ProcAttachment;
import com.haulmont.bpm.gui.procactionsfragment.ProcActionsFragment;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.app.core.file.FileDownloadHelper;
import com.haulmont.cuba.gui.components.CheckBox;
import com.haulmont.cuba.gui.components.DateField;
import com.haulmont.cuba.gui.components.Table;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.model.InstanceContainer;
import com.haulmont.cuba.gui.model.InstanceLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.ApplicationRecruitment;

import javax.inject.Inject;
import java.util.Date;

@UiController("itpearls_ApplicationRecruitment.edit")
@UiDescriptor("application-recruitment-edit.xml")
@EditedEntityContainer("applicationRecruitmentDc")
@LoadDataBeforeShow
public class ApplicationRecruitmentEdit extends StandardEditor<ApplicationRecruitment> {
    @Inject
    private CheckBox activeCheckBox;
    @Inject
    private DateField<Date> applicationDateField;
    @Inject
    private InstanceLoader<ApplicationRecruitment> applicationRecruitmentDl;
    @Inject
    private CollectionLoader<ProcAttachment> procAttachmentsDl;
    @Inject
    private InstanceContainer<ApplicationRecruitment> applicationRecruitmentDc;
    @Inject
    private ProcActionsFragment procActionsFragment;
    @Inject
    private Table<ProcAttachment> attachmentsTable;

    private static final String PROCESS_CODE = "approvalApplicationRecruiting";

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            activeCheckBox.setValue(false);
            applicationDateField.setValue(new Date());
        }

        procBPMNinit();
    }

    private void procBPMNinit() {
        applicationRecruitmentDl.load();
        procAttachmentsDl.setParameter("entityId",applicationRecruitmentDc.getItem().getId());
        procAttachmentsDl.load();

        procActionsFragment.initializer()
                .standard()
                .init(PROCESS_CODE, getEditedEntity());

        FileDownloadHelper.initGeneratedColumn(attachmentsTable, "file");
    }

}