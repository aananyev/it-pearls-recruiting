package com.company.hunttech.web.screens.jobcandidate;

import com.company.hunttech.entity.JobCandidate;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.FileDescriptorResource;
import com.haulmont.cuba.gui.components.HBoxLayout;
import com.haulmont.cuba.gui.components.Image;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.components.VBoxLayout;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.OpenMode;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.StandardLookup;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.hunttech.hrm.web.components.WebOvaFallbackImage;

import javax.inject.Inject;
import java.util.List;

@UiController("hunttech_JobCandidateTest4.browse")
@UiDescriptor("job-candidate-test4-browse.xml")
@LoadDataBeforeShow
public class JobCandidateTest4Browse extends StandardLookup<JobCandidate> {

    @Inject
    private CollectionContainer<JobCandidate> jobCandidatesDc;
    @Inject
    private CollectionLoader<JobCandidate> jobCandidatesDl;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private ScreenBuilders screenBuilders;

    @Inject
    private VBoxLayout gridContainer;
    @Inject
    private TextField<String> searchField;

    @Subscribe
    public void onBeforeShow(Screen.BeforeShowEvent event) {
        renderCardGrid();
    }

    private void renderCardGrid() {
        gridContainer.removeAll();
        List<JobCandidate> candidates = jobCandidatesDc.getItems();

        HBoxLayout currentRow = null;
        for (int i = 0; i < candidates.size(); i++) {
            if (i % 3 == 0) {
                currentRow = uiComponents.create(HBoxLayout.class);
                currentRow.setWidthFull();
                currentRow.setSpacing(true);
                gridContainer.add(currentRow);
            }

            JobCandidate candidate = candidates.get(i);
            VBoxLayout card = createExecutiveCard(candidate);
            if (currentRow != null) {
                currentRow.add(card);
                currentRow.expand(card);
            }
        }
    }

    private VBoxLayout createExecutiveCard(JobCandidate candidate) {
        VBoxLayout card = uiComponents.create(VBoxLayout.class);
        card.setStyleName("edit-card");
        card.setWidthFull();
        card.setSpacing(true);

        HBoxLayout topRow = uiComponents.create(HBoxLayout.class);
        topRow.setSpacing(true);
        topRow.setWidthFull();

        WebOvaFallbackImage img = uiComponents.create(WebOvaFallbackImage.class);
        img.setWidth("64px");
        img.setHeight("64px");
        img.setOvalWidth("64px");
        img.setOvalHeight("64px");
        img.setFallbackThemePath("icons/no-programmer.jpeg");
        img.setScaleMode(Image.ScaleMode.SCALE_DOWN);

        if (candidate.getFileImageFace() != null) {
            img.setSource(FileDescriptorResource.class).setFileDescriptor(candidate.getFileImageFace());
        }

        VBoxLayout infoBox = uiComponents.create(VBoxLayout.class);
        infoBox.setSpacing(false);

        Label<String> nameLbl = uiComponents.create(Label.TYPE_STRING);
        nameLbl.setValue(candidate.getFullName() != null ? candidate.getFullName() : "Без имени");
        nameLbl.setStyleName("h3");

        Label<String> posLbl = uiComponents.create(Label.TYPE_STRING);
        String pos = candidate.getPersonPosition() != null ? candidate.getPersonPosition().getPositionRuName() : "";
        posLbl.setValue(pos != null ? pos : "");
        posLbl.setStyleName("bold");

        Label<String> cityLbl = uiComponents.create(Label.TYPE_STRING);
        String city = candidate.getCityOfResidence() != null ? candidate.getCityOfResidence().getCityRuName() : "";
        cityLbl.setValue(city != null ? city : "");
        cityLbl.setStyleName("small");

        infoBox.add(nameLbl);
        infoBox.add(posLbl);
        infoBox.add(cityLbl);

        topRow.add(img);
        topRow.add(infoBox);
        topRow.expand(infoBox);

        HBoxLayout actionRow = uiComponents.create(HBoxLayout.class);
        actionRow.setSpacing(true);
        actionRow.setWidthFull();

        Button editBtn = uiComponents.create(Button.class);
        editBtn.setCaption("Открыть профиль");
        editBtn.setStyleName("primary");
        editBtn.setIcon("font-icon:USER");
        editBtn.addClickListener(e -> {
            screenBuilders.editor(JobCandidate.class, this)
                    .editEntity(candidate)
                    .withOpenMode(OpenMode.DIALOG)
                    .show();
        });

        actionRow.add(editBtn);

        card.add(topRow);
        card.add(actionRow);

        return card;
    }

    @Subscribe("searchBtn")
    public void onSearchBtnClick(Button.ClickEvent event) {
        String q = searchField.getValue();
        if (q != null && !q.trim().isEmpty()) {
            jobCandidatesDl.setQuery("select e from hunttech_JobCandidate e where lower(e.fullName) like :q order by e.createTs desc");
            jobCandidatesDl.setParameter("q", "%" + q.trim().toLowerCase() + "%");
        } else {
            jobCandidatesDl.setQuery("select e from hunttech_JobCandidate e order by e.createTs desc");
        }
        jobCandidatesDl.load();
        renderCardGrid();
    }

    @Subscribe("refreshBtn")
    public void onRefreshBtnClick(Button.ClickEvent event) {
        jobCandidatesDl.load();
        renderCardGrid();
    }
}
