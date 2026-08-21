package com.company.hunttech.web.screens.candidatecv;

import com.company.hunttech.entity.CandidateCV;
import com.company.hunttech.entity.JobCandidate;
import com.hunttech.hrm.gui.components.OvaFallbackImage;
import com.haulmont.cuba.core.entity.KeyValueEntity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.security.global.UserSession;
import org.jsoup.Jsoup;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@UiController("hunttech_CandidateCVReestr.browse")
@UiDescriptor("candidate-cv-reestr-browse.xml")
@LookupComponent("candidateCVsTable")
@LoadDataBeforeShow
public class CandidateCVReestrBrowse extends StandardLookup<CandidateCV> {

    private static final String QUERY_CVS_WITH_LETTER =
            "select e.id from hunttech_CandidateCV e where e in :candidateCVs and e.letter is not null";
    private static final String QUERY_CVS_WITH_TEXT =
            "select e.id from hunttech_CandidateCV e where e in :candidateCVs and e.textCV is not null";
    private static final String QUERY_CV_LETTERS_BY_IDS =
            "select e.id, e.letter from hunttech_CandidateCV e where e.id in :ids";

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy");
    private static final String GROUP_INTERN = "Стажер";

    @Inject
    private CollectionContainer<CandidateCV> candidateCVsDc;
    @Inject
    private CollectionLoader<CandidateCV> candidateCVsDl;
    @Inject
    private DataGrid<CandidateCV> candidateCVsTable;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private DataManager dataManager;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private UserSession userSession;
    @Inject
    private Notifications notifications;

    @Inject
    private PopupButton filterPopupButton;

    @Inject
    private OvaFallbackImage logoPic;
    @Inject
    private Label<String> detailTitle;
    @Inject
    private Label<String> detailSubtitle;
    @Inject
    private Label<String> detailLocation;
    @Inject
    private Button openEditCardBtn;
    @Inject
    private Button openCandidateCardBtn;
    @Inject
    private Label<String> detailCvReady;
    @Inject
    private Label<String> detailToVacancy;
    @Inject
    private Label<String> detailProject;
    @Inject
    private Label<String> detailOwner;
    @Inject
    private Label<String> detailDatePost;
    @Inject
    private Label<String> detailOriginalFile;
    @Inject
    private Label<String> detailHuntTechFile;
    @Inject
    private Label<String> detailLetter;

    private Set<UUID> cvsWithLetter = Collections.emptySet();
    private Set<UUID> cvsWithText = Collections.emptySet();
    private Map<UUID, String> cvLettersCache = Collections.emptyMap();

    @Subscribe
    public void onInit(InitEvent event) {
        candidateCVsDl.setMaxResults(100);
        setupTableSelection();
        setupSidebarButtons();
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        if (userSession.getUser().getGroup() != null &&
                GROUP_INTERN.equals(userSession.getUser().getGroup().getName())) {
            candidateCVsDl.setParameter("userName", "%" + userSession.getUser().getLogin() + "%");
            filterPopupButton.setCaption("Только мои резюме");
        }
    }

    private void setupTableSelection() {
        candidateCVsTable.addSelectionListener(e -> {
            Set<CandidateCV> selected = e.getSelected();
            if (selected != null && !selected.isEmpty()) {
                CandidateCV single = selected.iterator().next();
                updateSidebarDetails(single);
            } else {
                clearSidebarDetails();
            }
        });
    }

    private void setupSidebarButtons() {
        openEditCardBtn.addClickListener(e -> {
            CandidateCV selected = candidateCVsTable.getSingleSelected();
            if (selected != null) {
                screenBuilders.editor(candidateCVsTable)
                        .editEntity(selected)
                        .withOpenMode(OpenMode.DIALOG)
                        .show();
            }
        });

        openCandidateCardBtn.addClickListener(e -> {
            CandidateCV selected = candidateCVsTable.getSingleSelected();
            if (selected != null && selected.getCandidate() != null) {
                screenBuilders.editor(JobCandidate.class, this)
                        .editEntity(selected.getCandidate())
                        .withOpenMode(OpenMode.NEW_TAB)
                        .show();
            }
        });
    }

    @Subscribe("filterPopupButton.filterAll")
    public void onFilterAll(Action.ActionPerformedEvent event) {
        candidateCVsDl.removeParameter("userName");
        filterPopupButton.setCaption("Все резюме");
        candidateCVsDl.load();
    }

    @Subscribe("filterPopupButton.filterMyOnly")
    public void onFilterMyOnly(Action.ActionPerformedEvent event) {
        candidateCVsDl.setParameter("userName", "%" + userSession.getUser().getLogin() + "%");
        filterPopupButton.setCaption("Только мои резюме");
        candidateCVsDl.load();
    }

    @Subscribe("filterPopupButton.filterWithVacancyOnly")
    public void onFilterWithVacancyOnly(Action.ActionPerformedEvent event) {
        candidateCVsDl.removeParameter("userName");
        filterPopupButton.setCaption("С привязкой к вакансии");
        candidateCVsDl.load();
    }

    @Subscribe("actionsPopupButton.refreshAction")
    public void onRefreshAction(Action.ActionPerformedEvent event) {
        candidateCVsDl.load();
    }

    @Subscribe("actionsPopupButton.excelExportAction")
    public void onExcelExportAction(Action.ActionPerformedEvent event) {
        Action excel = candidateCVsTable.getAction("excel");
        if (excel != null) {
            excel.actionPerform(candidateCVsTable);
        }
    }

    @Subscribe(id = "candidateCVsDl", target = Target.DATA_LOADER)
    private void onCandidateCVsDlPostLoad(CollectionLoader.PostLoadEvent<CandidateCV> event) {
        refreshCaches(event.getLoadedEntities());

        CandidateCV current = candidateCVsTable.getSingleSelected();
        if (current != null) {
            updateSidebarDetails(current);
        } else if (!event.getLoadedEntities().isEmpty()) {
            candidateCVsTable.setSelected(event.getLoadedEntities().get(0));
        } else {
            clearSidebarDetails();
        }
    }

    private void refreshCaches(List<CandidateCV> candidateCVs) {
        if (candidateCVs.isEmpty()) {
            cvsWithLetter = Collections.emptySet();
            cvsWithText = Collections.emptySet();
            cvLettersCache = Collections.emptyMap();
            return;
        }

        cvsWithLetter = loadCvIdSet(QUERY_CVS_WITH_LETTER, candidateCVs);
        cvsWithText = loadCvIdSet(QUERY_CVS_WITH_TEXT, candidateCVs);

        List<UUID> ids = candidateCVs.stream()
                .map(CandidateCV::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<KeyValueEntity> rows = dataManager.loadValues(QUERY_CV_LETTERS_BY_IDS)
                .properties("id", "letter")
                .parameter("ids", ids)
                .list();
        Map<UUID, String> letters = new HashMap<>();
        for (KeyValueEntity r : rows) {
            UUID id = r.getValue("id");
            String let = r.getValue("letter");
            if (id != null && let != null) {
                letters.put(id, let);
            }
        }
        cvLettersCache = letters;
    }

    private Set<UUID> loadCvIdSet(String queryString, List<CandidateCV> candidateCVs) {
        List<KeyValueEntity> rows = dataManager.loadValues(queryString)
                .properties("id")
                .parameter("candidateCVs", candidateCVs)
                .list();
        Set<UUID> result = new HashSet<>();
        for (KeyValueEntity row : rows) {
            UUID id = row.getValue("id");
            if (id != null) {
                result.add(id);
            }
        }
        return result;
    }

    private void updateSidebarDetails(CandidateCV cv) {
        openEditCardBtn.setEnabled(true);
        openCandidateCardBtn.setEnabled(cv.getCandidate() != null);

        JobCandidate candidate = cv.getCandidate();

        // Аватар кандидата
        if (candidate != null && candidate.getFileImageFace() != null) {
            logoPic.setSource(FileDescriptorResource.class).setFileDescriptor(candidate.getFileImageFace());
        } else {
            logoPic.setSource(ThemeResource.class).setPath("icons/no-company.png");
        }

        // Заголовки
        if (candidate != null && candidate.getFullName() != null) {
            detailTitle.setValue(candidate.getFullName());
        } else {
            detailTitle.setValue("Кандидат не указан");
        }

        if (cv.getResumePosition() != null) {
            detailSubtitle.setValue(cv.getResumePosition().getPositionRuName());
        } else if (candidate != null && candidate.getPersonPosition() != null) {
            detailSubtitle.setValue(candidate.getPersonPosition().getPositionRuName());
        } else {
            detailSubtitle.setValue("-");
        }

        if (candidate != null && candidate.getCityOfResidence() != null) {
            detailLocation.setValue(candidate.getCityOfResidence().getCityRuName());
        } else {
            detailLocation.setValue("-");
        }

        // Готовность
        boolean hasText = cv.getId() != null && cvsWithText.contains(cv.getId());
        boolean hasLetter = cv.getId() != null && cvsWithLetter.contains(cv.getId());
        if (hasText || hasLetter) {
            detailCvReady.setValue("<span style='background: #dcfce7; color: #15803d; border: 1px solid #bbf7d0; padding: 2px 8px; border-radius: 10px; font-size: 11px; font-weight: 600;'>✓ Готово</span>");
        } else {
            detailCvReady.setValue("<span style='background: #fef9c3; color: #a16207; border: 1px solid #fef08a; padding: 2px 8px; border-radius: 10px; font-size: 11px; font-weight: 600;'>Черновик</span>");
        }

        if (cv.getToVacancy() != null) {
            detailToVacancy.setValue(cv.getToVacancy().getVacansyName());
            if (cv.getToVacancy().getProjectName() != null) {
                detailProject.setValue(cv.getToVacancy().getProjectName().getProjectName());
            } else {
                detailProject.setValue("-");
            }
        } else {
            detailToVacancy.setValue("Не привязано к вакансии");
            detailProject.setValue("-");
        }

        detailOwner.setValue(cv.getOwner() != null ? cv.getOwner().getInstanceName() : (cv.getCreatedBy() != null ? cv.getCreatedBy() : "-"));
        detailDatePost.setValue(cv.getDatePost() != null ? DATE_FORMAT.format(cv.getDatePost()) : "-");

        // Документы
        if (cv.getOriginalFileCV() != null) {
            detailOriginalFile.setValue("<span style='color: #16a34a; font-weight: 600;'>📄 Файл прикреплен (" + cv.getOriginalFileCV().getName() + ")</span>");
        } else if (cv.getLinkOriginalCv() != null && !cv.getLinkOriginalCv().isEmpty()) {
            detailOriginalFile.setValue("<span style='color: #2563eb;'>🔗 Ссылка на оригинал</span>");
        } else {
            detailOriginalFile.setValue("<span style='color: #94a3b8;'>Оригинал отсутствует</span>");
        }

        if (cv.getFileCV() != null) {
            detailHuntTechFile.setValue("<span style='color: #16a34a; font-weight: 600;'>📄 HuntTech резюме (" + cv.getFileCV().getName() + ")</span>");
        } else if (cv.getLinkHuntTechCV() != null && !cv.getLinkHuntTechCV().isEmpty()) {
            detailHuntTechFile.setValue("<span style='color: #2563eb;'>🔗 Ссылка HuntTech</span>");
        } else {
            detailHuntTechFile.setValue("<span style='color: #94a3b8;'>HuntTech версия не создана</span>");
        }

        // Сопроводительное письмо
        String letterText = cv.getId() != null ? cvLettersCache.get(cv.getId()) : null;
        if (letterText != null && !letterText.trim().isEmpty()) {
            String plain = Jsoup.parse(letterText).text();
            detailLetter.setValue(plain.length() > 300 ? plain.substring(0, 300) + "..." : plain);
        } else {
            detailLetter.setValue("<span style='color: #94a3b8;'>Сопроводительное письмо отсутствует</span>");
        }
    }

    private void clearSidebarDetails() {
        openEditCardBtn.setEnabled(false);
        openCandidateCardBtn.setEnabled(false);
        logoPic.setSource(ThemeResource.class).setPath("icons/no-company.png");
        detailTitle.setValue("Выберите резюме");
        detailSubtitle.setValue("-");
        detailLocation.setValue("-");
        detailCvReady.setValue("-");
        detailToVacancy.setValue("-");
        detailProject.setValue("-");
        detailOwner.setValue("-");
        detailDatePost.setValue("-");
        detailOriginalFile.setValue("-");
        detailHuntTechFile.setValue("-");
        detailLetter.setValue("<span style='color: #94a3b8;'>Резюме не выбрано</span>");
    }

    @Install(to = "candidateCVsTable.cvReady", subject = "columnGenerator")
    private Component candidateCVsTableCvReadyColumnGenerator(DataGrid.ColumnGeneratorEvent<CandidateCV> event) {
        HBoxLayout box = uiComponents.create(HBoxLayout.class);
        box.setWidthFull();
        box.setHeightFull();
        box.setAlignment(Component.Alignment.MIDDLE_CENTER);

        Label label = uiComponents.create(Label.class);
        label.setHtmlEnabled(true);
        label.setAlignment(Component.Alignment.MIDDLE_CENTER);

        CandidateCV cv = event.getItem();
        boolean hasText = cv != null && cv.getId() != null && cvsWithText.contains(cv.getId());
        boolean hasLetter = cv != null && cv.getId() != null && cvsWithLetter.contains(cv.getId());

        if (hasText || hasLetter) {
            label.setValue("<span style='background: #dcfce7; color: #15803d; border: 1px solid #bbf7d0; padding: 2px 8px; border-radius: 10px; font-size: 11px; font-weight: 600;'>Готово</span>");
        } else {
            label.setValue("<span style='background: #fef9c3; color: #a16207; border: 1px solid #fef08a; padding: 2px 8px; border-radius: 10px; font-size: 11px; font-weight: 600;'>Черновик</span>");
        }

        box.add(label);
        return box;
    }

    @Install(to = "candidateCVsTable.originalFileColumn", subject = "columnGenerator")
    private Component candidateCVsTableOriginalFileColumnGenerator(DataGrid.ColumnGeneratorEvent<CandidateCV> event) {
        HBoxLayout box = uiComponents.create(HBoxLayout.class);
        box.setWidthFull();
        box.setHeightFull();
        box.setAlignment(Component.Alignment.MIDDLE_CENTER);

        Label label = uiComponents.create(Label.class);
        label.setHtmlEnabled(true);
        label.setAlignment(Component.Alignment.MIDDLE_CENTER);

        CandidateCV cv = event.getItem();
        if (cv.getOriginalFileCV() != null || (cv.getLinkOriginalCv() != null && !cv.getLinkOriginalCv().isEmpty())) {
            label.setValue("<span style='color: #16a34a; font-weight: 700;'>📄 Да</span>");
        } else {
            label.setValue("<span style='color: #cbd5e1;'>—</span>");
        }

        box.add(label);
        return box;
    }

    @Install(to = "candidateCVsTable.huntTechFileColumn", subject = "columnGenerator")
    private Component candidateCVsTableHuntTechFileColumnGenerator(DataGrid.ColumnGeneratorEvent<CandidateCV> event) {
        HBoxLayout box = uiComponents.create(HBoxLayout.class);
        box.setWidthFull();
        box.setHeightFull();
        box.setAlignment(Component.Alignment.MIDDLE_CENTER);

        Label label = uiComponents.create(Label.class);
        label.setHtmlEnabled(true);
        label.setAlignment(Component.Alignment.MIDDLE_CENTER);

        CandidateCV cv = event.getItem();
        if (cv.getFileCV() != null || (cv.getLinkHuntTechCV() != null && !cv.getLinkHuntTechCV().isEmpty())) {
            label.setValue("<span style='color: #2563eb; font-weight: 700;'>📄 Да</span>");
        } else {
            label.setValue("<span style='color: #cbd5e1;'>—</span>");
        }

        box.add(label);
        return box;
    }
}
