package com.company.hunttech.web.screens.jobcandidate;

import com.company.hunttech.entity.CandidateCV;
import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.web.util.FileDescriptorImageHelper;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.Button;
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
import java.util.Comparator;

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
    private FileLoader fileLoader;

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
    private TextField<String> searchField;

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
            // Фото берётся из карточки кандидата, а при его отсутствии — из последнего резюме (CandidateCV)
            FileDescriptorImageHelper.setCandidateFace(avatarImg, fileLoader, resolveCandidateFace(candidate));
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

    /**
     * Возвращает фото кандидата: сначала из карточки (JobCandidate.fileImageFace),
     * при отсутствии — из последнего резюме с фото (CandidateCV.fileImageFace).
     */
    private FileDescriptor resolveCandidateFace(JobCandidate candidate) {
        if (candidate.getFileImageFace() != null) {
            return candidate.getFileImageFace();
        }
        if (candidate.getCandidateCv() != null) {
            return candidate.getCandidateCv().stream()
                    .filter(cv -> cv.getFileImageFace() != null)
                    .max(Comparator.comparing(CandidateCV::getCreateTs,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(CandidateCV::getFileImageFace)
                    .orElse(null);
        }
        return null;
    }

    private void clearDetailPane() {
        detailFullName.setValue("Выберите кандидата");
        detailPosition.setValue("");
        detailCity.setValue("");
        detailPhone.setValue("-");
        detailEmail.setValue("-");
        detailTelegram.setValue("-");
        detailCompany.setValue("-");
        detailInteractionsInfo.setValue("Выберите кандидата в таблице слева для просмотра истории.");
        detailPic.setSource(ThemeResource.class).setPath("icons/no-programmer.jpeg");
        editCandidateBtn.setEnabled(false);
        createInteractionBtn.setEnabled(false);
    }

    private void populateDetailPane(JobCandidate candidate) {
        String name = candidate.getFullName() != null ? candidate.getFullName() : "Без имени";
        detailFullName.setValue(name);

        String pos = candidate.getPersonPosition() != null ? candidate.getPersonPosition().getPositionRuName() : "";
        detailPosition.setValue(pos != null ? pos : "");

        String city = candidate.getCityOfResidence() != null ? candidate.getCityOfResidence().getCityRuName() : "";
        detailCity.setValue(city != null ? city : "");

        detailPhone.setValue(candidate.getPhone() != null ? candidate.getPhone() : "-");
        detailEmail.setValue(candidate.getEmail() != null ? candidate.getEmail() : "-");
        detailTelegram.setValue(candidate.getTelegramName() != null ? candidate.getTelegramName() : "-");

        String company = "-";
        if (candidate.getCurrentCompany() != null) {
            company = candidate.getCurrentCompany().getComanyName() != null ?
                    candidate.getCurrentCompany().getComanyName() : candidate.getCurrentCompany().getCompanyShortName();
        }
        detailCompany.setValue(company != null ? company : "-");

        // Фото: из карточки кандидата, при отсутствии — из последнего резюме (CandidateCV);
        // если файла нет в хранилище — автоматический fallback без битой картинки
        FileDescriptorImageHelper.setCandidateFace(detailPic, fileLoader, resolveCandidateFace(candidate));

        int interactionsCount = candidate.getIteractionList() != null ? candidate.getIteractionList().size() : 0;
        detailInteractionsInfo.setValue("Всего зарегистрировано взаимодействий: " + interactionsCount);

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
