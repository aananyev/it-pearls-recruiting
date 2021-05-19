package com.company.itpearls.web.screens.openposition;

import com.company.itpearls.core.PdfParserService;
import com.company.itpearls.entity.*;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import org.jsoup.Jsoup;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@UiController("itpearls_SuggestJobCandidate")
@UiDescriptor("SuggestJobCandidate.xml")
public class Suggestjobcandidate extends Screen {
    @Inject
    private DataGrid<CandidateCV> suitableCheckDataGrid;
    @Inject
    private DataManager dataManager;
    @Inject
    private PdfParserService pdfParserService;

    private OpenPosition openPosition = null;
    @Inject
    private CollectionLoader<CandidateCV> candidateCVDl;
    @Inject
    private LookupPickerField<Position> jobPositionLookupPickerField;
    @Inject
    private CheckBox useLocationCheckBox;
    @Inject
    private RichTextArea candidateCVRichTextArea;

    @Subscribe
    public void onInit(InitEvent event) {
        DataGrid.ButtonRenderer<OpenPosition> suitableCheckDataGridRelevanceRenderer = suitableCheckDataGrid
                .createRenderer(DataGrid.ButtonRenderer.class);

        suitableCheckDataGridRelevanceRenderer.setRendererClickListener(clickableTextRendererClickEvent -> {
        });

        suitableCheckDataGrid.getColumn("relevance").setRenderer(suitableCheckDataGridRelevanceRenderer);
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        if (openPosition != null) {
            if (openPosition.getPositionType() != null) {
                candidateCVDl.setParameter("resumePosition", this.openPosition.getPositionType());
            } else {
                candidateCVDl.removeParameter("resumePosition");
            }
        } else {
            candidateCVDl.removeParameter("resumePosition");
        }

        candidateCVDl.load();

        jobPositionLookupPickerField.setValue(this.openPosition.getPositionType());
        useLocationCheckBox.setValue(false);

        setCityPosition();
    }

    private void setCityPosition() {
            if (useLocationCheckBox.getValue()) {
                candidateCVDl.setParameter("cityOfResidence", openPosition.getCityPosition());
            } else {
                candidateCVDl.removeParameter("cityOfResidence");
            }
        candidateCVDl.load();
    }

    @Subscribe("useLocationCheckBox")
    public void onUseLocationCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        setCityPosition();
    }

    @Install(to = "suitableCheckDataGrid.relevance", subject = "columnGenerator")
    private String suitableCheckDataGridRelevanceColumnGenerator(DataGrid.ColumnGeneratorEvent<CandidateCV> event) {
        List<SkillTree> skillsFromCV = pdfParserService.parseSkillTree(event.getItem().getTextCV());
        List<SkillTree> skillsFromJD = pdfParserService.parseSkillTree(openPosition.getComment());

        Integer counter = 0;

        for (SkillTree skillTree : skillsFromJD) {
            for (SkillTree st : skillsFromCV) {
                if (skillTree.equals(st))
                    counter++;
            }
        }

        Integer percent = counter * 100 / skillsFromJD.size();

        return percent.toString() + "%";

    }

    public List<SkillTree> rescanResume(CandidateCV candidateCV) {
        if (candidateCV.getTextCV() != null) {
            String inputText = Jsoup.parse(candidateCV.getTextCV()).text();
            List<SkillTree> skillTrees = pdfParserService.parseSkillTree(inputText);

            candidateCV.setSkillTree(skillTrees);

            return skillTrees;
        } else {
            return null;
        }
    }

    public void setOpenPosition(OpenPosition openPosition) {
        this.openPosition = openPosition;
    }

    public void rescanSuitable() {
        candidateCVDl.setParameter("resumePosition", jobPositionLookupPickerField.getValue());
        candidateCVDl.load();
    }

    @Subscribe("suitableCheckDataGrid")
    public void onSuitableCheckDataGridSelection(DataGrid.SelectionEvent<CandidateCV> event) {
        if(suitableCheckDataGrid.getSingleSelected() != null) {
            suitableCheckDataGrid
                    .getSingleSelected()
                    .getTextCV();
        }
    }

    @Install(to = "suitableCheckDataGrid.toVacancy", subject = "descriptionProvider")
    private String suitableCheckDataGridToVacancyDescriptionProvider(CandidateCV candidateCV) {
        if(candidateCV.getToVacancy() != null) {
            return candidateCV.getToVacancy().getVacansyName();
        } else {
            return null;
        }
    }
}