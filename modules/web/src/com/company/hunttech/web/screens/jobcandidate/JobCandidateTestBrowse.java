package com.company.hunttech.web.screens.jobcandidate;

import com.company.hunttech.entity.JobCandidate;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.FileDescriptorResource;
import com.haulmont.cuba.gui.components.GroupTable;
import com.haulmont.cuba.gui.components.Image;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.Table;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.components.ThemeResource;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.gui.screen.OpenMode;
import com.haulmont.cuba.gui.screen.StandardLookup;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.hunttech.hrm.web.components.WebOvaFallbackImage;

import javax.inject.Inject;

@UiController("hunttech_JobCandidateTest.browse")
@UiDescriptor("job-candidate-test-browse.xml")
@LookupComponent("candidatesTable")
@LoadDataBeforeShow
public class JobCandidateTestBrowse extends StandardLookup<JobCandidate> {

    @Inject
    private GroupTable<JobCandidate> candidatesTable;
    @Inject
    private CollectionLoader<JobCandidate> jobCandidatesDl;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private UiComponents uiComponents;

    @Inject
    private WebOvaFallbackImage detailPic;
    @Inject
    private Label<String> detailFullName;
    @Inject
    private Label<String> detailPosition;
    @Inject
    private Label<String> detailCity;
    @Inject
    private Label<String> detailPhone;
    @Inject
    private Label<String> detailEmail;
    @Inject
    private Label<String> detailTelegram;
    @Inject
    private Label<String> detailCompany;
    @Inject
    private Label<String> detailInteractionsInfo;
    @Inject
    private Button editCandidateBtn;
    @Inject
    private Button createInteractionBtn;
    @Inject
    private com.haulmont.cuba.core.global.DataManager dataManager;
    @Inject
    private Label<String> detailSalaryCaption;
    @Inject
    private Label<String> detailSalary;
    @Inject
    private TextField<String> searchField;

    private String getSalaryExpectations(JobCandidate candidate) {
        if (candidate == null) return null;
        if (candidate.getIteractionList() != null) {
            for (com.company.hunttech.entity.IteractionList it : candidate.getIteractionList()) {
                if (it.getIteractionType() != null &&
                    it.getIteractionType().getIterationName() != null &&
                    it.getIteractionType().getIterationName().toLowerCase().contains("зарплатные ожидания")) {
                    if (it.getAddString() != null && !it.getAddString().trim().isEmpty()) {
                        return it.getAddString().trim();
                    }
                }
            }
        }
        try {
            java.util.List<com.company.hunttech.entity.IteractionList> list = dataManager.load(com.company.hunttech.entity.IteractionList.class)
                    .query("select e from hunttech_IteractionList e where e.iteractionType.iterationName like :name and e.candidate = :cand order by e.createTs desc")
                    .parameter("name", "%Зарплатные ожидания%")
                    .parameter("cand", candidate)
                    .view("iteractionList-view")
                    .list();
            if (!list.isEmpty() && list.get(0).getAddString() != null && !list.get(0).getAddString().trim().isEmpty()) {
                return list.get(0).getAddString().trim();
            }
        } catch (Exception ignored) {}
        return null;
    }

    @Subscribe
    public void onInit(Screen.InitEvent event) {
        // Генератор фото-аватара в первой колонке таблицы
        candidatesTable.addGeneratedColumn("avatar", candidate -> {
            WebOvaFallbackImage avatarImg = uiComponents.create(WebOvaFallbackImage.class);
            avatarImg.setWidth("36px");
            avatarImg.setHeight("36px");
            avatarImg.setOvalWidth("36px");
            avatarImg.setOvalHeight("36px");
            avatarImg.setFallbackThemePath("icons/no-programmer.jpeg");
            avatarImg.setScaleMode(Image.ScaleMode.SCALE_DOWN);
            if (candidate.getFileImageFace() != null) {
                avatarImg.setSource(FileDescriptorResource.class).setFileDescriptor(candidate.getFileImageFace());
            }
            return avatarImg;
        });
    }

    @Subscribe("candidatesTable")
    public void onCandidatesTableSelection(Table.SelectionEvent<JobCandidate> event) {
        JobCandidate selected = candidatesTable.getSingleSelected();
        if (selected == null) {
            clearDetailPane();
        } else {
            populateDetailPane(selected);
        }
    }

    private void clearDetailPane() {
        detailFullName.setHtmlEnabled(true);
        detailFullName.setValue("<div style='text-align: center; font-size: 21px; font-weight: 700; color: #7f8c8d;'>Выберите кандидата</div>");
        detailPosition.setValue("");
        detailCity.setValue("");
        detailPhone.setValue("-");
        detailEmail.setValue("-");
        detailTelegram.setValue("-");
        detailCompany.setValue("-");
        detailSalaryCaption.setVisible(false);
        detailSalary.setVisible(false);
        detailInteractionsInfo.setValue("Выберите кандидата в таблице для просмотра истории.");
        detailPic.setSource(ThemeResource.class).setPath("icons/no-programmer.jpeg");
        editCandidateBtn.setEnabled(false);
        createInteractionBtn.setEnabled(false);
    }

    private void populateDetailPane(JobCandidate candidate) {
        String name = candidate.getFullName() != null ? candidate.getFullName() : "Без имени";
        detailFullName.setHtmlEnabled(true);
        detailFullName.setValue("<div style='text-align: center; font-size: 22px; font-weight: 700; color: #2c3e50; line-height: 1.3;'>" + name + "</div>");

        String pos = candidate.getPersonPosition() != null ? candidate.getPersonPosition().getPositionRuName() : "Специалист";
        detailPosition.setHtmlEnabled(true);
        detailPosition.setValue("<div style='text-align: center; margin: 4px 0;'><span style='background: rgba(43, 130, 201, 0.15); color: #2b82c9; padding: 3px 10px; border-radius: 4px; font-weight: 600; font-size: 14px; display: inline-block;'>" + pos + "</span></div>");

        String city = candidate.getCityOfResidence() != null ? candidate.getCityOfResidence().getCityRuName() : "Москва";
        detailCity.setHtmlEnabled(true);
        detailCity.setValue("<div style='text-align: center; font-size: 15px; font-weight: 500; color: #7f8c8d; margin-top: 2px;'>📍 " + city + "</div>");

        detailPhone.setValue(candidate.getPhone() != null ? candidate.getPhone() : "-");
        detailEmail.setValue(candidate.getEmail() != null ? candidate.getEmail() : "-");
        detailTelegram.setValue(candidate.getTelegramName() != null ? candidate.getTelegramName() : "-");

        String company = "-";
        if (candidate.getCurrentCompany() != null) {
            company = candidate.getCurrentCompany().getComanyName() != null ?
                    candidate.getCurrentCompany().getComanyName() : candidate.getCurrentCompany().getCompanyShortName();
        }
        detailCompany.setValue(company != null ? company : "-");

        String salary = getSalaryExpectations(candidate);
        if (salary != null && !salary.isEmpty()) {
            detailSalaryCaption.setVisible(true);
            detailSalary.setVisible(true);
            detailSalary.setHtmlEnabled(true);
            detailSalary.setValue("<span style='color: #27ae60; font-weight: 600;'>" + salary + "</span>");
        } else {
            detailSalaryCaption.setVisible(false);
            detailSalary.setVisible(false);
        }

        if (candidate.getFileImageFace() != null) {
            detailPic.setSource(FileDescriptorResource.class).setFileDescriptor(candidate.getFileImageFace());
        } else {
            detailPic.setSource(ThemeResource.class).setPath("icons/no-programmer.jpeg");
        }

        int interactionsCount = candidate.getIteractionList() != null ? candidate.getIteractionList().size() : 0;
        detailInteractionsInfo.setHtmlEnabled(true);
        detailInteractionsInfo.setValue("<div style='background: #f8f9fa; padding: 10px 14px; border-radius: 6px; border-left: 4px solid #2980b9; margin-top: 6px; font-size: 12px; line-height: 1.6;'>" +
                "• Всего зарегистрировано взаимодействий: <b>" + interactionsCount + "</b>" +
                "</div>");

        editCandidateBtn.setEnabled(true);
        createInteractionBtn.setEnabled(true);
    }

    @Subscribe("editCandidateBtn")
    public void onEditCandidateBtnClick(Button.ClickEvent event) {
        JobCandidate selected = candidatesTable.getSingleSelected();
        if (selected != null) {
            screenBuilders.editor(candidatesTable)
                    .editEntity(selected)
                    .withOpenMode(OpenMode.DIALOG)
                    .show();
        }
    }

    @Subscribe("createCandidateBtn")
    public void onCreateCandidateBtnClick(Button.ClickEvent event) {
        screenBuilders.editor(candidatesTable)
                .newEntity()
                .withOpenMode(OpenMode.DIALOG)
                .show();
    }

    @Subscribe("refreshBtn")
    public void onRefreshBtnClick(Button.ClickEvent event) {
        jobCandidatesDl.load();
    }

    @Subscribe("searchButton")
    public void onSearchButtonClick(Button.ClickEvent event) {
        String queryText = searchField.getValue();
        if (queryText == null || queryText.trim().isEmpty()) {
            jobCandidatesDl.setQuery("select e from hunttech_JobCandidate e order by e.createTs desc");
        } else {
            jobCandidatesDl.setQuery("select e from hunttech_JobCandidate e where lower(e.fullName) like :queryText order by e.createTs desc");
            jobCandidatesDl.setParameter("queryText", "%" + queryText.trim().toLowerCase() + "%");
        }
        jobCandidatesDl.load();
    }
}
