package com.company.hunttech.web.screens.jobcandidate;

import com.company.hunttech.entity.JobCandidate;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.FileDescriptorResource;
import com.haulmont.cuba.gui.components.HBoxLayout;
import com.haulmont.cuba.gui.components.Image;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.VBoxLayout;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.StandardLookup;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.hunttech.hrm.web.components.WebOvaFallbackImage;

import javax.inject.Inject;
import java.util.List;

@UiController("hunttech_JobCandidateTest3.browse")
@UiDescriptor("job-candidate-test3-browse.xml")
@LoadDataBeforeShow
public class JobCandidateTest3Browse extends StandardLookup<JobCandidate> {

    @Inject
    private CollectionContainer<JobCandidate> jobCandidatesDc;
    @Inject
    private UiComponents uiComponents;

    @Inject
    private Label<String> totalCountLabel;
    @Inject
    private Label<String> newCountLabel;
    @Inject
    private Label<String> interviewCountLabel;
    @Inject
    private Label<String> hiredCountLabel;

    @Inject
    private VBoxLayout containerColNew;
    @Inject
    private VBoxLayout containerColInterview;
    @Inject
    private VBoxLayout containerColReview;
    @Inject
    private VBoxLayout containerColOffer;

    @Subscribe
    public void onBeforeShow(Screen.BeforeShowEvent event) {
        buildKanbanBoard();
    }

    private void buildKanbanBoard() {
        List<JobCandidate> candidates = jobCandidatesDc.getItems();
        totalCountLabel.setValue(String.valueOf(candidates.size()));

        containerColNew.removeAll();
        containerColInterview.removeAll();
        containerColReview.removeAll();
        containerColOffer.removeAll();

        int newCnt = 0;
        int interviewCnt = 0;
        int reviewCnt = 0;
        int offerCnt = 0;

        for (int i = 0; i < candidates.size(); i++) {
            JobCandidate candidate = candidates.get(i);
            VBoxLayout card = createCandidateCard(candidate);

            // Имитация распределения по колонкам для демонстрации Kanban
            int stage = i % 4;
            switch (stage) {
                case 0:
                    containerColNew.add(card);
                    newCnt++;
                    break;
                case 1:
                    containerColInterview.add(card);
                    interviewCnt++;
                    break;
                case 2:
                    containerColReview.add(card);
                    reviewCnt++;
                    break;
                case 3:
                    containerColOffer.add(card);
                    offerCnt++;
                    break;
                default:
                    break;
            }
        }

        newCountLabel.setValue(String.valueOf(newCnt));
        interviewCountLabel.setValue(String.valueOf(interviewCnt));
        hiredCountLabel.setValue(String.valueOf(offerCnt));
    }

    private VBoxLayout createCandidateCard(JobCandidate candidate) {
        VBoxLayout card = uiComponents.create(VBoxLayout.class);
        card.setStyleName("edit-card");
        card.setWidthFull();
        card.setSpacing(true);

        HBoxLayout header = uiComponents.create(HBoxLayout.class);
        header.setSpacing(true);
        header.setWidthFull();

        WebOvaFallbackImage img = uiComponents.create(WebOvaFallbackImage.class);
        img.setWidth("40px");
        img.setHeight("40px");
        img.setOvalWidth("40px");
        img.setOvalHeight("40px");
        img.setFallbackThemePath("icons/no-programmer.jpeg");
        img.setScaleMode(Image.ScaleMode.SCALE_DOWN);

        if (candidate.getFileImageFace() != null) {
            img.setSource(FileDescriptorResource.class).setFileDescriptor(candidate.getFileImageFace());
        }

        VBoxLayout nameBox = uiComponents.create(VBoxLayout.class);
        nameBox.setSpacing(false);

        Label<String> nameLbl = uiComponents.create(Label.TYPE_STRING);
        nameLbl.setValue(candidate.getFullName() != null ? candidate.getFullName() : "Без имени");
        nameLbl.setStyleName("bold");

        Label<String> posLbl = uiComponents.create(Label.TYPE_STRING);
        String pos = candidate.getPersonPosition() != null ? candidate.getPersonPosition().getPositionRuName() : "";
        posLbl.setValue(pos != null ? pos : "");
        posLbl.setStyleName("small");

        nameBox.add(nameLbl);
        nameBox.add(posLbl);

        header.add(img);
        header.add(nameBox);
        header.expand(nameBox);

        card.add(header);
        return card;
    }
}
