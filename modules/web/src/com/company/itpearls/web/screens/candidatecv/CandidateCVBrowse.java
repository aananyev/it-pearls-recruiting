package com.company.itpearls.web.screens.candidatecv;

import com.company.itpearls.BeanNotificationEvent;
import com.company.itpearls.UiNotificationEvent;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.icons.Icons;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.CandidateCV;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.security.global.UserSession;
import org.springframework.context.event.EventListener;

import javax.inject.Inject;
import java.io.File;
import java.io.FileDescriptor;

@UiController("itpearls_CandidateCV.browse")
@UiDescriptor("candidate-cv-browse.xml")
@LookupComponent("candidateCVsTable")
@LoadDataBeforeShow
public class CandidateCVBrowse extends StandardLookup<CandidateCV> {
    @Inject
    private CollectionLoader<CandidateCV> candidateCVsDl;
    @Inject
    private CheckBox checkBoxSetOnlyMy;
    @Inject
    private UserSession userSession;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        if (userSession.getUser().getGroup().getName().equals("Стажер")) {
            candidateCVsDl.setParameter("userName", "%" + userSession.getUser().getLogin() + "%");

            checkBoxSetOnlyMy.setValue(true);
            checkBoxSetOnlyMy.setEditable(false);
        }
    }

    @Subscribe("checkBoxSetOnlyMy")
    public void onCheckBoxSetOnlyMyValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (checkBoxSetOnlyMy.getValue()) {
            candidateCVsDl.setParameter("userName", "%" + userSession.getUser().getLogin() + "%");
        } else {
            candidateCVsDl.removeParameter("userName");
        }

        candidateCVsDl.load();
    }

    @Install(to = "candidateCVsTable.originalFileCVcolumn", subject = "columnGenerator")
    private Icons.Icon candidateCVsTableOriginalFileCVcolumnColumnGenerator(DataGrid.ColumnGeneratorEvent<CandidateCV> event) {
        if (!(event.getItem().getOriginalFileCV() == null &&
                event.getItem().getLinkOriginalCv() == null )) {
            return CubaIcon.FILE_TEXT;
        } else {
            return CubaIcon.FILE_TEXT_O;
        }
    }

    @Install(to = "candidateCVsTable.itPearlsCVcolumn", subject = "columnGenerator")
    private Icons.Icon candidateCVsTableItPearlsCVcolumnColumnGenerator(DataGrid.ColumnGeneratorEvent<CandidateCV> event) {
        if (!(event.getItem().getLinkItPearlsCV() == null)) {
            return CubaIcon.FILE_TEXT;
        } else {
            return CubaIcon.FILE_TEXT_O;
        }
    }

    @Install(to = "candidateCVsTable.itPearlsCVcolumn", subject = "styleProvider")
    private String candidateCVsTableItPearlsCVcolumnStyleProvider(CandidateCV candidateCV) {
        if (!(candidateCV.getLinkItPearlsCV() == null)) {
            return "open-position-pic-center-large-green";
        } else {
            return "open-position-pic-center-large-red";
        }
    }

    @Install(to = "candidateCVsTable.originalFileCVcolumn", subject = "styleProvider")
    private String candidateCVsTableOriginalFileCVcolumnStyleProvider(CandidateCV candidateCV) {
        if (!(candidateCV.getOriginalFileCV() == null &&
                candidateCV.getLinkOriginalCv() == null)) {
            return "open-position-pic-center-large-green";
        } else {
            return "open-position-pic-center-large-red";
        }
    }

    @Install(to = "candidateCVsTable.cvReady", subject = "columnGenerator")
    private Icons.Icon candidateCVsTableCvReadyColumnGenerator(DataGrid.ColumnGeneratorEvent<CandidateCV> event) {
        return CubaIcon.PENCIL_SQUARE_O;
    }

    @Install(to = "candidateCVsTable.cvReady", subject = "styleProvider")
    private String candidateCVsTableCvReadyStyleProvider(CandidateCV candidateCV) {
        if (candidateCV.getLetter() != null &&
                candidateCV.getTextCV() != null &&
                candidateCV.getLinkItPearlsCV() != null) {
            return "open-position-pic-center-large-green";
        } else {
            if ((candidateCV.getLetter() == null ||
                    candidateCV.getLinkItPearlsCV() == null ) &&
                    candidateCV.getOriginalFileCV() != null ) {
                return "open-position-pic-center-large-yellow";
            } else {
                return "open-position-pic-center-large-red";
            }
        }
    }

/*    @Install(to = "candidateCVsTable", subject = "iconProvider")
    protected String candidateCVsTableiconProvider(CandidateCV candidateCV) {
        if (candidateCV.getLetter() != null &&
                candidateCV.getTextCV() != null &&
                candidateCV.getLinkItPearlsCV() != null) {
            return "icons/resume-green.png";
        } else {
            if ( ( candidateCV.getLetter() == null ||
                    candidateCV.getLinkItPearlsCV() == null ) &&
                    candidateCV.getOriginalFileCV() != null  &&
                    candidateCV.getTextCV() != null ) {
                return "icons/resume-yellow.png";
            } else {
                return "icons/resume-red.png";
            }
        }
    }
*/
}