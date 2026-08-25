package com.company.hunttech.web.screens.candidatecv;

import com.company.hunttech.entity.CandidateCV;
import com.company.hunttech.entity.JobCandidate;
import com.hunttech.hrm.gui.components.OvaFallbackImage;
import com.company.hunttech.web.screens.jobcandidate.SmartCvUploadScreen;
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
    private GroupTable<CandidateCV> candidateCVsTable;
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
        setupTableColumns();
        setupTableSelection();
        setupSidebarButtons();

        // Выравнивание заголовков профиля (под OvalFallbackImage) по центру
        detailTitle.setAlignment(Component.Alignment.MIDDLE_CENTER);
        detailSubtitle.setAlignment(Component.Alignment.MIDDLE_CENTER);
        detailLocation.setAlignment(Component.Alignment.MIDDLE_CENTER);

        // Выравнивание заголовков сайдбара по центру
        detailToVacancy.setAlignment(Component.Alignment.MIDDLE_CENTER);
        detailProject.setAlignment(Component.Alignment.MIDDLE_CENTER);
        detailOwner.setAlignment(Component.Alignment.MIDDLE_CENTER);
    }

    private void setupTableColumns() {
        candidateCVsTable.addGeneratedColumn("avatar", cv -> {
            HBoxLayout retBox = uiComponents.create(HBoxLayout.class);
            retBox.setWidthFull();
            retBox.setHeightFull();
            retBox.setAlignment(Component.Alignment.MIDDLE_CENTER);

            Image image = uiComponents.create(Image.class);
            image.setScaleMode(Image.ScaleMode.SCALE_DOWN);
            image.setWidth("24px");
            image.setHeight("24px");
            image.setStyleName("circle-20px");
            image.setAlignment(Component.Alignment.MIDDLE_CENTER);

            if (cv != null && cv.getCandidate() != null && cv.getCandidate().getFileImageFace() != null) {
                image.setSource(FileDescriptorResource.class).setFileDescriptor(cv.getCandidate().getFileImageFace());
            } else {
                image.setSource(ThemeResource.class).setPath("icons/no-programmer.jpeg");
            }

            retBox.add(image);
            return retBox;
        });

        candidateCVsTable.addGeneratedColumn("candidate", cv -> {
            JobCandidate c = cv != null ? cv.getCandidate() : null;
            String name = (c != null && c.getFullName() != null) ? c.getFullName() : "Без имени";
            StringBuilder sub = new StringBuilder();
            if (c != null) {
                if (c.getCityOfResidence() != null && c.getCityOfResidence().getCityRuName() != null) {
                    sub.append("📍 ").append(c.getCityOfResidence().getCityRuName());
                }
                if (c.getTelegramName() != null && !c.getTelegramName().trim().isEmpty()) {
                    if (sub.length() > 0) sub.append(" • ");
                    sub.append("@").append(c.getTelegramName().trim());
                } else if (c.getEmail() != null && !c.getEmail().trim().isEmpty()) {
                    if (sub.length() > 0) sub.append(" • ");
                    sub.append(c.getEmail().trim());
                }
            }
            String textHtml = "<div style='text-align: left;'><div style='font-weight: 600; color: #2c3e50; font-size: 13px;'>" + name + "</div>" +
                    (sub.length() > 0 ? "<div style='font-size: 11px; color: #7f8c8d;'>" + sub.toString() + "</div>" : "") + "</div>";
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            lbl.setWidth("100%");
            lbl.setValue(textHtml);
            return lbl;
        });

        candidateCVsTable.addGeneratedColumn("resumePosition", cv -> {
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            lbl.setWidthFull();
            String pos = (cv != null && cv.getResumePosition() != null) ? cv.getResumePosition().getPositionRuName() :
                    (cv != null && cv.getCandidate() != null && cv.getCandidate().getPersonPosition() != null ?
                            cv.getCandidate().getPersonPosition().getPositionRuName() : "Специалист");
            lbl.setValue("<span style='background: rgba(43, 130, 201, 0.12); color: #2b82c9; padding: 3px 8px; border-radius: 4px; font-weight: 600; font-size: 11px; display: inline-block; white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%;'>" + (pos != null ? pos : "Специалист") + "</span>");
            return lbl;
        });

        candidateCVsTable.addGeneratedColumn("toVacancy", cv -> {
            com.company.hunttech.entity.OpenPosition v = cv != null ? cv.getToVacancy() : null;
            if (v == null) {
                Label<String> plain = uiComponents.create(Label.NAME);
                plain.setHtmlEnabled(true);
                plain.setValue("<span style='color: #94a3b8; font-size: 11px;'>—</span>");
                return plain;
            }
            String vName = v.getVacansyName() != null ? v.getVacansyName() : "Вакансия";
            String pName = v.getProjectName() != null ? v.getProjectName().getProjectName() : "";
            String textHtml = "<div style='text-align: left;'><div style='font-weight: 600; color: #1e293b; font-size: 12.5px;'>" + vName + "</div>" +
                    (!pName.isEmpty() ? "<div style='font-size: 11px; color: #64748b;'>📁 " + pName + "</div>" : "") + "</div>";
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            lbl.setWidth("100%");
            lbl.setValue(textHtml);
            return lbl;
        });

        candidateCVsTable.addGeneratedColumn("cvReady", cv -> {
            HBoxLayout box = uiComponents.create(HBoxLayout.class);
            box.setWidthFull();
            box.setHeightFull();
            box.setAlignment(Component.Alignment.MIDDLE_CENTER);

            Label label = uiComponents.create(Label.class);
            label.setHtmlEnabled(true);
            label.setAlignment(Component.Alignment.MIDDLE_CENTER);

            boolean hasText = cv != null && cv.getId() != null && cvsWithText.contains(cv.getId());
            boolean hasLetter = cv != null && cv.getId() != null && cvsWithLetter.contains(cv.getId());

            if (hasText || hasLetter) {
                label.setValue("<span style='background: #dcfce7; color: #15803d; border: 1px solid #bbf7d0; padding: 2px 8px; border-radius: 10px; font-size: 11px; font-weight: 600;'>✓ Готово</span>");
            } else {
                label.setValue("<span style='background: #fef9c3; color: #a16207; border: 1px solid #fef08a; padding: 2px 8px; border-radius: 10px; font-size: 11px; font-weight: 600;'>✎ Черновик</span>");
            }

            box.add(label);
            return box;
        });

        candidateCVsTable.addGeneratedColumn("originalFileColumn", cv -> {
            HBoxLayout box = uiComponents.create(HBoxLayout.class);
            box.setWidthFull();
            box.setHeightFull();
            box.setAlignment(Component.Alignment.MIDDLE_CENTER);

            Label label = uiComponents.create(Label.class);
            label.setHtmlEnabled(true);
            label.setAlignment(Component.Alignment.MIDDLE_CENTER);

            if (cv != null && (cv.getOriginalFileCV() != null || (cv.getLinkOriginalCv() != null && !cv.getLinkOriginalCv().trim().isEmpty()))) {
                label.setValue("<span style='background: #ecfdf5; color: #059669; border: 1px solid #a7f3d0; padding: 2px 6px; border-radius: 4px; font-size: 10.5px; font-weight: 600;'>📄 Да</span>");
            } else {
                label.setValue("<span style='color: #cbd5e1;'>—</span>");
            }

            box.add(label);
            return box;
        });

        candidateCVsTable.addGeneratedColumn("huntTechFileColumn", cv -> {
            HBoxLayout box = uiComponents.create(HBoxLayout.class);
            box.setWidthFull();
            box.setHeightFull();
            box.setAlignment(Component.Alignment.MIDDLE_CENTER);

            Label label = uiComponents.create(Label.class);
            label.setHtmlEnabled(true);
            label.setAlignment(Component.Alignment.MIDDLE_CENTER);

            if (cv != null && (cv.getFileCV() != null || (cv.getLinkHuntTechCV() != null && !cv.getLinkHuntTechCV().trim().isEmpty()))) {
                label.setValue("<span style='background: #eff6ff; color: #2563eb; border: 1px solid #bfdbfe; padding: 2px 6px; border-radius: 4px; font-size: 10.5px; font-weight: 600;'>⚡ Да</span>");
            } else {
                label.setValue("<span style='color: #cbd5e1;'>—</span>");
            }

            box.add(label);
            return box;
        });

        candidateCVsTable.addGeneratedColumn("owner", cv -> {
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            lbl.setWidthFull();
            String ownerName = (cv != null && cv.getOwner() != null) ? cv.getOwner().getInstanceName() : "-";
            lbl.setValue("<div style='font-size: 12px; color: #475569; white-space: normal; word-break: break-word; line-height: 1.35;'>👤 " + (ownerName != null ? ownerName : "-") + "</div>");
            return lbl;
        });

        candidateCVsTable.addGeneratedColumn("datePost", cv -> {
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            String d = (cv != null && cv.getDatePost() != null) ? DATE_FORMAT.format(cv.getDatePost()) : "-";
            lbl.setValue("<span style='font-size: 11.5px; color: #64748b;'>📅 " + d + "</span>");
            return lbl;
        });
    }

    @Subscribe("smartUploadBtn")
    public void onSmartUploadBtnClick(Button.ClickEvent event) {
        openSmartCvUploadDialog();
    }

    private void openSmartCvUploadDialog() {
        SmartCvUploadScreen screen = screenBuilders.screen(this)
                .withScreenClass(SmartCvUploadScreen.class)
                .withOpenMode(OpenMode.DIALOG)
                .build();
        screen.addAfterCloseListener(closeEvent -> {
            if (closeEvent.closedWith(StandardOutcome.COMMIT)) {
                candidateCVsDl.load();
                if (screen.getCreatedCv() != null) {
                    try {
                        candidateCVsTable.setSelected(screen.getCreatedCv());
                    } catch (Exception ignored) {
                    }
                }
            }
        });
        screen.show();
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
            String vacName = cv.getToVacancy().getVacansyName();
            detailToVacancy.setValue("<div style='white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%;'>" + (vacName != null ? vacName : "—") + "</div>");
            if (cv.getToVacancy().getProjectName() != null) {
                String projName = cv.getToVacancy().getProjectName().getProjectName();
                detailProject.setValue("<div style='white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%;'>" + (projName != null ? projName : "—") + "</div>");
            } else {
                detailProject.setValue("—");
            }
        } else {
            detailToVacancy.setValue("Не привязано к вакансии");
            detailProject.setValue("—");
        }

        String ownerName = cv.getOwner() != null ? cv.getOwner().getInstanceName() : (cv.getCreatedBy() != null ? cv.getCreatedBy() : "—");
        detailOwner.setValue("<div style='white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%;'>" + ownerName + "</div>");
        detailDatePost.setValue("<div style='white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%;'>" + (cv.getDatePost() != null ? DATE_FORMAT.format(cv.getDatePost()) : "—") + "</div>");

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
}
