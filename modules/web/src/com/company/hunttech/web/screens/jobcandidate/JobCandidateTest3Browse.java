package com.company.hunttech.web.screens.jobcandidate;

import com.company.hunttech.entity.JobCandidate;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.FileDescriptorResource;
import com.haulmont.cuba.gui.components.HBoxLayout;
import com.haulmont.cuba.gui.components.Image;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.VBoxLayout;
import com.haulmont.cuba.gui.model.CollectionChangeType;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.StandardLookup;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.Target;
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

    // Канбан строится по факту загрузки данных контейнера (BeforeShow срабатывает
    // до REFRESH-события, поэтому пустой список давал нулевые метрики и без карточек)
    @Subscribe(id = "jobCandidatesDc", target = Target.DATA_CONTAINER)
    public void onJobCandidatesDcCollectionChange(CollectionContainer.CollectionChangeEvent<JobCandidate> event) {
        if (event.getChangeType() == CollectionChangeType.REFRESH) {
            buildKanbanBoard();
        }
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
            int stage = i % 4;
            VBoxLayout card = createCandidateCard(candidate, stage);
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

        totalCountLabel.setHtmlEnabled(true);
        totalCountLabel.setValue(candidates.size() + " <span style='font-size: 11px; color: #27ae60; font-weight: normal; margin-left: 8px;'>+5.2% MTD</span>");

        newCountLabel.setHtmlEnabled(true);
        newCountLabel.setValue(newCnt + " <span style='font-size: 11px; color: #27ae60; font-weight: normal; margin-left: 8px;'>+18.1% MTD</span>");

        interviewCountLabel.setHtmlEnabled(true);
        interviewCountLabel.setValue(interviewCnt + " <span style='font-size: 11px; color: #e74c3c; font-weight: normal; margin-left: 8px;'>-2.3% MTD</span>");

        hiredCountLabel.setHtmlEnabled(true);
        hiredCountLabel.setValue(offerCnt + " <span style='font-size: 11px; color: #27ae60; font-weight: normal; margin-left: 8px;'>+9.5% MTD</span>");
    }

    private VBoxLayout createCandidateCard(JobCandidate candidate, int stage) {
        VBoxLayout card = uiComponents.create(VBoxLayout.class);
        card.setStyleName("edit-card");
        card.setWidthFull();
        card.setSpacing(true);

        // Шапка карточки: Фото + ФИО + Должность
        HBoxLayout header = uiComponents.create(HBoxLayout.class);
        header.setSpacing(true);
        header.setWidthFull();

        WebOvaFallbackImage img = uiComponents.create(WebOvaFallbackImage.class);
        img.setWidth("44px");
        img.setHeight("44px");
        img.setOvalWidth("44px");
        img.setOvalHeight("44px");
        img.setFallbackThemePath("icons/no-programmer.jpeg");
        img.setScaleMode(Image.ScaleMode.SCALE_DOWN);

        if (candidate.getFileImageFace() != null) {
            img.setSource(FileDescriptorResource.class).setFileDescriptor(candidate.getFileImageFace());
        }

        VBoxLayout nameBox = uiComponents.create(VBoxLayout.class);
        nameBox.setSpacing(false);

        Label<String> nameLbl = uiComponents.create(Label.NAME);
        String name = candidate.getFullName() != null ? candidate.getFullName() : "Без имени";
        nameLbl.setValue(name);
        nameLbl.setStyleName("bold");

        Label<String> posLbl = uiComponents.create(Label.NAME);
        posLbl.setHtmlEnabled(true);
        String posName = candidate.getPersonPosition() != null ? candidate.getPersonPosition().getPositionRuName() : "Разработчик";
        posLbl.setValue("<span style='background: rgba(0,123,255,0.15); color: #2b82c9; padding: 2px 6px; border-radius: 4px; font-size: 11px; font-weight: 600;'>" + posName + "</span>");

        nameBox.add(nameLbl);
        nameBox.add(posLbl);

        header.add(img);
        header.add(nameBox);
        header.expand(nameBox);

        // Инфо-строка: Рейтинг + Локация
        HBoxLayout infoRow = uiComponents.create(HBoxLayout.class);
        infoRow.setSpacing(true);
        infoRow.setWidthFull();

        Label<String> ratingLbl = uiComponents.create(Label.NAME);
        ratingLbl.setHtmlEnabled(true);
        double rating = 4.2 + (Math.abs(candidate.hashCode()) % 8) / 10.0;
        ratingLbl.setValue("<span style='color: #f39c12; font-weight: bold;'>★ " + String.format("%.1f", rating) + "</span>");

        Label<String> cityLbl = uiComponents.create(Label.NAME);
        cityLbl.setHtmlEnabled(true);
        String city = candidate.getCityOfResidence() != null ? candidate.getCityOfResidence().getCityRuName() : "Москва";
        cityLbl.setValue("<span style='color: #7f8c8d; font-size: 11px;'>📍 " + city + "</span>");

        infoRow.add(ratingLbl);
        infoRow.add(cityLbl);

        // Блок навыков / Теги
        Label<String> skillsLbl = uiComponents.create(Label.NAME);
        skillsLbl.setHtmlEnabled(true);
        skillsLbl.setValue("<div style='display: flex; gap: 4px; flex-wrap: wrap; margin-top: 4px;'>" +
                "<span style='background: #ecf0f1; color: #34495e; padding: 1px 5px; border-radius: 3px; font-size: 10px;'>Java</span>" +
                "<span style='background: #ecf0f1; color: #34495e; padding: 1px 5px; border-radius: 3px; font-size: 10px;'>Spring</span>" +
                "<span style='background: #ecf0f1; color: #34495e; padding: 1px 5px; border-radius: 3px; font-size: 10px;'>PostgreSQL</span>" +
                "</div>");

        // Нижние кнопки действий
        HBoxLayout btnRow = uiComponents.create(HBoxLayout.class);
        btnRow.setSpacing(true);
        btnRow.setWidthFull();

        com.haulmont.cuba.gui.components.Button profileBtn = uiComponents.create(com.haulmont.cuba.gui.components.Button.class);
        profileBtn.setCaption("Карточка");
        profileBtn.setStyleName("small primary");
        profileBtn.setIcon("font-icon:USER");

        com.haulmont.cuba.gui.components.Button msgBtn = uiComponents.create(com.haulmont.cuba.gui.components.Button.class);
        msgBtn.setCaption("Чат");
        msgBtn.setStyleName("small");
        msgBtn.setIcon("font-icon:COMMENTS");

        btnRow.add(profileBtn);
        btnRow.add(msgBtn);

        card.add(header);
        card.add(infoRow);
        card.add(skillsLbl);
        card.add(btnRow);
        return card;
    }
}
