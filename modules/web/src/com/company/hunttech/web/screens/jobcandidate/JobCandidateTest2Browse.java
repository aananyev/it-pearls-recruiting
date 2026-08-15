package com.company.hunttech.web.screens.jobcandidate;

import com.company.hunttech.entity.JobCandidate;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.FileDescriptorResource;
import com.haulmont.cuba.gui.components.GroupTable;
import com.haulmont.cuba.gui.components.Image;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.StandardLookup;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.hunttech.hrm.web.components.WebOvaFallbackImage;

import javax.inject.Inject;

@UiController("hunttech_JobCandidateTest2.browse")
@UiDescriptor("job-candidate-test2-browse.xml")
@LookupComponent("candidatesTable")
@LoadDataBeforeShow
public class JobCandidateTest2Browse extends StandardLookup<JobCandidate> {

    @Inject
    private GroupTable<JobCandidate> candidatesTable;
    @Inject
    private CollectionLoader<JobCandidate> jobCandidatesDl;
    @Inject
    private UiComponents uiComponents;

    @Inject
    private TextField<String> filterNameField;
    @Inject
    private TextField<String> filterCityField;

    @Subscribe
    public void onInit(Screen.InitEvent event) {
        candidatesTable.addGeneratedColumn("avatar", candidate -> {
            WebOvaFallbackImage avatarImg = uiComponents.create(WebOvaFallbackImage.class);
            avatarImg.setWidth("30px");
            avatarImg.setHeight("30px");
            avatarImg.setOvalWidth("30px");
            avatarImg.setOvalHeight("30px");
            avatarImg.setFallbackThemePath("icons/no-programmer.jpeg");
            avatarImg.setScaleMode(Image.ScaleMode.SCALE_DOWN);
            if (candidate.getFileImageFace() != null) {
                avatarImg.setSource(FileDescriptorResource.class).setFileDescriptor(candidate.getFileImageFace());
            }
            return avatarImg;
        });
    }

    @Subscribe("applyFilterBtn")
    public void onApplyFilterBtnClick(Button.ClickEvent event) {
        String nameQuery = filterNameField.getValue();
        String cityQuery = filterCityField.getValue();

        StringBuilder jpql = new StringBuilder("select e from hunttech_JobCandidate e where 1=1");
        if (nameQuery != null && !nameQuery.trim().isEmpty()) {
            jpql.append(" and lower(e.fullName) like :nameQuery");
        }
        if (cityQuery != null && !cityQuery.trim().isEmpty()) {
            jpql.append(" and lower(e.cityOfResidence.cityRuName) like :cityQuery");
        }
        jpql.append(" order by e.createTs desc");

        jobCandidatesDl.setQuery(jpql.toString());
        if (nameQuery != null && !nameQuery.trim().isEmpty()) {
            jobCandidatesDl.setParameter("nameQuery", "%" + nameQuery.trim().toLowerCase() + "%");
        }
        if (cityQuery != null && !cityQuery.trim().isEmpty()) {
            jobCandidatesDl.setParameter("cityQuery", "%" + cityQuery.trim().toLowerCase() + "%");
        }
        jobCandidatesDl.load();
    }

    @Subscribe("resetFilterBtn")
    public void onResetFilterBtnClick(Button.ClickEvent event) {
        filterNameField.setValue(null);
        filterCityField.setValue(null);
        jobCandidatesDl.setQuery("select e from hunttech_JobCandidate e order by e.createTs desc");
        jobCandidatesDl.load();
    }
}
