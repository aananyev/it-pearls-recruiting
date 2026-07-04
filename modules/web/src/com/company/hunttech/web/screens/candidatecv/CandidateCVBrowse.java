package com.company.hunttech.web.screens.candidatecv;

import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.core.entity.KeyValueEntity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.icons.Icons;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.CandidateCV;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@UiController("hunttech_CandidateCV.browse")
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
    @Inject
    private DataManager dataManager;

    private static final String QUERY_CVS_WITH_LETTER =
            "select e.id from hunttech_CandidateCV e where e in :candidateCVs and e.letter is not null";
    private static final String QUERY_CVS_WITH_TEXT =
            "select e.id from hunttech_CandidateCV e where e in :candidateCVs and e.textCV is not null";

    static String GROUP_INTERN = "Стажер";
    private Set<UUID> cvsWithLetter = Collections.emptySet();
    private Set<UUID> cvsWithText = Collections.emptySet();
    private boolean suppressOnlyMyReload;

    @Subscribe
    public void onInit(InitEvent event) {
        // Prepare the first @LoadDataBeforeShow query with a page limit and trainee filter before data is requested.
        candidateCVsDl.setMaxResults(100);
        initDefaultCandidateCvFilters();
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        if (userSession.getUser().getGroup().getName().equals(GROUP_INTERN)) {
            checkBoxSetOnlyMy.setEditable(false);
        }
    }

    @Subscribe(id = "candidateCVsDl", target = Target.DATA_LOADER)
    private void onCandidateCVsDlPostLoad(CollectionLoader.PostLoadEvent<CandidateCV> event) {
        // Batch-load LOB presence flags once per page instead of fetching TEXT_CV/LETTER for every row.
        refreshCvReadinessCache(event.getLoadedEntities());
    }

    private void initDefaultCandidateCvFilters() {
        suppressOnlyMyReload = true;
        try {
            if (userSession.getUser().getGroup().getName().equals(GROUP_INTERN)) {
                candidateCVsDl.setParameter("userName", "%" + userSession.getUser().getLogin() + "%");
                checkBoxSetOnlyMy.setValue(true);
            }
        } finally {
            suppressOnlyMyReload = false;
        }
    }

    @Subscribe("checkBoxSetOnlyMy")
    public void onCheckBoxSetOnlyMyValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (suppressOnlyMyReload) {
            return;
        }

        if (Boolean.TRUE.equals(checkBoxSetOnlyMy.getValue())) {
            candidateCVsDl.setParameter("userName", "%" + userSession.getUser().getLogin() + "%");
        } else {
            candidateCVsDl.removeParameter("userName");
        }

        candidateCVsDl.load();
    }

    private void refreshCvReadinessCache(List<CandidateCV> candidateCVs) {
        if (candidateCVs.isEmpty()) {
            cvsWithLetter = Collections.emptySet();
            cvsWithText = Collections.emptySet();
            return;
        }

        cvsWithLetter = loadCvIdSet(QUERY_CVS_WITH_LETTER, candidateCVs);
        cvsWithText = loadCvIdSet(QUERY_CVS_WITH_TEXT, candidateCVs);
    }

    private Set<UUID> loadCvIdSet(String query, List<CandidateCV> candidateCVs) {
        List<KeyValueEntity> rows = dataManager.loadValues(query)
                .properties("id")
                .parameter("candidateCVs", candidateCVs)
                .list();

        Set<UUID> ids = new HashSet<>();
        for (KeyValueEntity row : rows) {
            UUID id = row.getValue("id");
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    private boolean hasLetter(CandidateCV candidateCV) {
        return candidateCV != null && candidateCV.getId() != null && cvsWithLetter.contains(candidateCV.getId());
    }

    private boolean hasText(CandidateCV candidateCV) {
        return candidateCV != null && candidateCV.getId() != null && cvsWithText.contains(candidateCV.getId());
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

    @Install(to = "candidateCVsTable.huntTechCVcolumn", subject = "columnGenerator")
    private Icons.Icon candidateCVsTableHuntTechCVcolumnColumnGenerator(DataGrid.ColumnGeneratorEvent<CandidateCV> event) {
        if (!(event.getItem().getLinkHuntTechCV() == null)) {
            return CubaIcon.FILE_TEXT;
        } else {
            return CubaIcon.FILE_TEXT_O;
        }
    }

    @Install(to = "candidateCVsTable.huntTechCVcolumn", subject = "styleProvider")
    private String candidateCVsTableHuntTechCVcolumnStyleProvider(CandidateCV candidateCV) {
        if (!(candidateCV.getLinkHuntTechCV() == null)) {
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
        if (hasLetter(candidateCV) &&
                hasText(candidateCV) &&
                candidateCV.getLinkHuntTechCV() != null) {
            return "open-position-pic-center-large-green";
        } else {
            if ((!hasLetter(candidateCV) ||
                    candidateCV.getLinkHuntTechCV() == null ) &&
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
                candidateCV.getLinkHuntTechCV() != null) {
            return "icons/resume-green.png";
        } else {
            if ( ( candidateCV.getLetter() == null ||
                    candidateCV.getLinkHuntTechCV() == null ) &&
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
