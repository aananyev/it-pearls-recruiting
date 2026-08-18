package com.company.hunttech.web.screens.jobcandidate;

import com.company.hunttech.entity.CandidateCV;
import com.company.hunttech.entity.CandidateSkill;
import com.company.hunttech.entity.IteractionList;
import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.web.util.FileDescriptorImageHelper;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.Action;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.GroupTable;
import com.haulmont.cuba.gui.components.Image;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.PopupButton;
import com.haulmont.cuba.gui.components.Table;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.gui.screen.OpenMode;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.StandardLookup;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.security.global.UserSession;
import com.hunttech.hrm.web.components.WebOvaFallbackImage;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Контроллер базового тестового экрана просмотра кандидатов «Split-View Halo».
 */
@UiController("hunttech_JobCandidateTest.browse")
@UiDescriptor("job-candidate-test-browse.xml")
@LookupComponent("candidatesTable")
@LoadDataBeforeShow
public class JobCandidateTestBrowse extends StandardLookup<JobCandidate> {

    /* =========================================================================
     * Инъекции компонентов UI и сервисов
     * ========================================================================= */

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
    private com.haulmont.cuba.core.global.DataManager dataManager;

    @Inject
    private Notifications notifications;

    @Inject
    private UserSession userSession;

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
    private Label<String> detailSalaryCaption;
    @Inject
    private Label<String> detailSalary;
    @Inject
    private Label<String> detailInteractionsInfo;
    @Inject
    private Label<String> detailSkillsLabels;
    @Inject
    private Button editCandidateBtn;
    @Inject
    private Button createInteractionBtn;

    @Inject
    private Button filterAllBtn;
    @Inject
    private Button filterMyCandidatesBtn;
    @Inject
    private Button filterMyParticipationBtn;
    @Inject
    private PopupButton actionsWithCandidateButton;

    private final java.text.SimpleDateFormat interactionDateFormat = new java.text.SimpleDateFormat("dd.MM.yyyy");

    public enum InteractionStatus {
        FREE("🟢 Свободен (> 1 мес)", "#27ae60", "rgba(39, 174, 96, 0.15)"),
        MY_CANDIDATE("🟡 В вашей работе (< 1 мес)", "#f39c12", "rgba(243, 156, 18, 0.15)"),
        OTHER_RECRUITER("🔴 В работе у другого рекрутера", "#e74c3c", "rgba(231, 76, 60, 0.15)");

        private final String label;
        private final String color;
        private final String bgColor;

        InteractionStatus(String label, String color, String bgColor) {
            this.label = label;
            this.color = color;
            this.bgColor = bgColor;
        }

        public String getLabel() {
            return label;
        }

        public String getColor() {
            return color;
        }

        public String getBgColor() {
            return bgColor;
        }
    }

    private InteractionStatus calculateInteractionStatus(JobCandidate candidate) {
        if (candidate == null) {
            return InteractionStatus.FREE;
        }
        List<IteractionList> list = candidate.getIteractionList();
        if (list == null || list.isEmpty()) {
            return InteractionStatus.FREE;
        }
        IteractionList last = list.stream()
                .filter(i -> i.getDateIteraction() != null || i.getCreateTs() != null)
                .max(Comparator.comparing(i -> i.getDateIteraction() != null ? i.getDateIteraction() : i.getCreateTs(),
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
        if (last == null) {
            return InteractionStatus.FREE;
        }
        Date date = last.getDateIteraction() != null ? last.getDateIteraction() : last.getCreateTs();
        if (date == null) {
            return InteractionStatus.FREE;
        }
        java.util.Calendar threshold = java.util.Calendar.getInstance();
        threshold.setTime(date);
        threshold.add(java.util.Calendar.MONTH, 1);
        java.util.Calendar now = java.util.Calendar.getInstance();
        if (now.after(threshold)) {
            return InteractionStatus.FREE;
        } else {
            if (last.getRecrutier() != null && userSession.getUser() != null
                    && !last.getRecrutier().getId().equals(userSession.getUser().getId())) {
                return InteractionStatus.OTHER_RECRUITER;
            } else {
                return InteractionStatus.MY_CANDIDATE;
            }
        }
    }

    private FileDescriptor resolveCandidateFace(JobCandidate candidate) {
        if (candidate == null) {
            return null;
        }
        if (candidate.getFileImageFace() != null) {
            return candidate.getFileImageFace();
        }
        if (candidate.getCandidateCv() != null && !candidate.getCandidateCv().isEmpty()) {
            return candidate.getCandidateCv().stream()
                    .filter(cv -> cv.getFileImageFace() != null)
                    .max(Comparator.comparing(CandidateCV::getCreateTs, Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(CandidateCV::getFileImageFace)
                    .orElse(null);
        }
        return null;
    }

    private String resolveCandidateSalary(JobCandidate candidate) {
        if (candidate == null) {
            return null;
        }
        try {
            List<IteractionList> list = dataManager.load(IteractionList.class)
                    .query("select e from hunttech_IteractionList e where e.iteractionType.iteractionTree.iteractionRuName like :name and e.candidate = :cand order by e.dateIteraction desc, e.createTs desc")
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
        // Колонка 1: Миниатюра фото кандидата (36px oval)
        candidatesTable.addGeneratedColumn("avatar", candidate -> {
            WebOvaFallbackImage avatarImg = uiComponents.create(WebOvaFallbackImage.class);
            avatarImg.setWidth("36px");
            avatarImg.setHeight("36px");
            avatarImg.setOvalWidth("36px");
            avatarImg.setOvalHeight("36px");
            avatarImg.setFallbackThemePath("icons/no-programmer.jpeg");
            avatarImg.setScaleMode(Image.ScaleMode.SCALE_DOWN);
            FileDescriptorImageHelper.setCandidateFace(avatarImg, fileLoader, resolveCandidateFace(candidate));
            return avatarImg;
        });

        // Колонка 2: Кандидат (ФИО + контакт)
        candidatesTable.addGeneratedColumn("fullName", candidate -> {
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            String name = candidate.getFullName() != null ? candidate.getFullName() : "Без имени";
            String sub = candidate.getTelegramName() != null ? "@" + candidate.getTelegramName() :
                    (candidate.getEmail() != null ? candidate.getEmail() : "");
            lbl.setValue("<div><div style='font-weight: 600; color: #2c3e50; font-size: 13px;'>" + name + "</div>" +
                    (!sub.isEmpty() ? "<div style='font-size: 11px; color: #7f8c8d;'>" + sub + "</div>" : "") + "</div>");
            return lbl;
        });

        // Колонка 3: Должность
        candidatesTable.addGeneratedColumn("personPosition", candidate -> {
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            String pos = candidate.getPersonPosition() != null ? candidate.getPersonPosition().getPositionRuName() : "Специалист";
            lbl.setValue("<span style='background: rgba(43, 130, 201, 0.12); color: #2b82c9; padding: 2px 8px; border-radius: 4px; font-weight: 600; font-size: 11px; display: inline-block;'>" + pos + "</span>");
            return lbl;
        });

        // Колонка 4: Город
        candidatesTable.addGeneratedColumn("cityOfResidence", candidate -> {
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            String city = candidate.getCityOfResidence() != null ? candidate.getCityOfResidence().getCityRuName() : "Москва";
            lbl.setValue("<span style='font-size: 12px; color: #34495e;'>📍 " + city + "</span>");
            return lbl;
        });

        // Колонка 5: Компания
        candidatesTable.addGeneratedColumn("currentCompany", candidate -> {
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            String company = "-";
            if (candidate.getCurrentCompany() != null) {
                company = candidate.getCurrentCompany().getComanyName() != null ?
                        candidate.getCurrentCompany().getComanyName() : candidate.getCurrentCompany().getCompanyShortName();
            }
            lbl.setValue("<span style='font-size: 12px; color: #34495e;'>" + (company != null ? company : "-") + "</span>");
            return lbl;
        });

        // Колонка 6: Ключевые навыки (чипы)
        candidatesTable.addGeneratedColumn("mainSkills", candidate -> {
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            try {
                List<CandidateSkill> skills = dataManager.load(CandidateSkill.class)
                        .query("select e from hunttech_CandidateSkill e where e.candidate = :candidate order by e.priority, e.skill.skillName")
                        .parameter("candidate", candidate)
                        .view("candidateSkill-view")
                        .list();

                if (skills == null || skills.isEmpty()) {
                    lbl.setValue("<span style='color: #a0aec0; font-size: 11px;'>—</span>");
                    return lbl;
                }

                StringBuilder sb = new StringBuilder("<div style='display: flex; gap: 4px; flex-wrap: wrap;'>");
                String[] palette = new String[]{"#38bdf8", "#4ade80", "#c084fc", "#fb923c", "#2dd4bf", "#f472b6", "#facc15", "#60a5fa"};
                int count = 0;
                for (CandidateSkill cs : skills) {
                    if (cs.getSkill() != null && cs.getSkill().getSkillName() != null) {
                        if (count >= 3) {
                            sb.append("<span style='font-size: 10px; color: #7f8c8d; align-self: center;'>+").append(skills.size() - 3).append("</span>");
                            break;
                        }
                        String sName = cs.getSkill().getSkillName();
                        String color = palette[Math.abs(sName.hashCode()) % palette.length];
                        sb.append(String.format("<span style='background: %s18; color: %s; border: 1px solid %s44; padding: 1px 6px; border-radius: 10px; font-size: 10.5px; font-weight: 600; white-space: nowrap;'>%s</span>",
                                color, color, color, sName));
                        count++;
                    }
                }
                sb.append("</div>");
                lbl.setValue(sb.toString());
            } catch (Exception ex) {
                lbl.setValue("<span style='color: #a0aec0; font-size: 11px;'>—</span>");
            }
            return lbl;
        });

        // Колонка 7: Статус взаимодействия
        candidatesTable.addGeneratedColumn("lastInteractionStatus", candidate -> {
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            InteractionStatus status = calculateInteractionStatus(candidate);
            lbl.setValue(String.format(
                    "<span style='background: %s; color: %s; padding: 2px 6px; border-radius: 4px; font-size: 10.5px; font-weight: 600; white-space: nowrap; display: inline-block;'>%s</span>",
                    status.getBgColor(), status.getColor(), status.getLabel()
            ));
            return lbl;
        });

        updateActionsState(candidatesTable.getSingleSelected());
    }

    @Subscribe("candidatesTable")
    public void onCandidatesTableSelection(Table.SelectionEvent<JobCandidate> event) {
        JobCandidate selected = candidatesTable.getSingleSelected();
        if (selected == null) {
            clearDetailPane();
        } else {
            populateDetailPane(selected);
        }
        updateActionsState(selected);
    }

    private void updateActionsState(JobCandidate selected) {
        if (actionsWithCandidateButton == null) return;
        actionsWithCandidateButton.setEnabled(true);
        boolean hasSelected = selected != null;
        if (actionsWithCandidateButton.getAction("editCandidateAction") != null) {
            actionsWithCandidateButton.getAction("editCandidateAction").setEnabled(hasSelected);
        }
        if (actionsWithCandidateButton.getAction("createInteractionAction") != null) {
            actionsWithCandidateButton.getAction("createInteractionAction").setEnabled(hasSelected);
        }
        if (actionsWithCandidateButton.getAction("scanSkillsAction") != null) {
            actionsWithCandidateButton.getAction("scanSkillsAction").setEnabled(hasSelected);
        }
        if (actionsWithCandidateButton.getAction("showCandidateCVListAction") != null) {
            actionsWithCandidateButton.getAction("showCandidateCVListAction").setEnabled(hasSelected);
        }
        if (actionsWithCandidateButton.getAction("showIteractionListAction") != null) {
            actionsWithCandidateButton.getAction("showIteractionListAction").setEnabled(hasSelected);
        }
        if (actionsWithCandidateButton.getAction("sendEmailAction") != null) {
            actionsWithCandidateButton.getAction("sendEmailAction").setEnabled(hasSelected);
        }
        if (actionsWithCandidateButton.getAction("addPersonalReserveAction") != null) {
            actionsWithCandidateButton.getAction("addPersonalReserveAction").setEnabled(hasSelected);
        }
        if (actionsWithCandidateButton.getAction("refreshAction") != null) {
            actionsWithCandidateButton.getAction("refreshAction").setEnabled(true);
        }
    }

    private void clearDetailPane() {
        detailFullName.setValue("Выберите кандидата");
        detailPosition.setValue("");
        detailCity.setValue("");
        detailPhone.setValue("-");
        detailEmail.setValue("-");
        detailTelegram.setValue("-");
        detailCompany.setValue("-");
        detailSalaryCaption.setVisible(false);
        detailSalary.setVisible(false);
        detailInteractionsInfo.setValue("Выберите кандидата в таблице для просмотра истории.");
        if (detailSkillsLabels != null) {
            detailSkillsLabels.setValue("<span style='color: #7f8c8d; font-size: 11px;'>Навыки не определены</span>");
        }
        editCandidateBtn.setEnabled(false);
        createInteractionBtn.setEnabled(false);
    }

    private void populateDetailPane(JobCandidate candidate) {
        FileDescriptorImageHelper.setCandidateFace(detailPic, fileLoader, resolveCandidateFace(candidate));
        detailFullName.setValue(candidate.getFullName() != null ? candidate.getFullName() : "Без имени");
        detailPosition.setValue(candidate.getPersonPosition() != null ? candidate.getPersonPosition().getPositionRuName() : "Должность не указана");
        detailCity.setValue(candidate.getCityOfResidence() != null ? "📍 " + candidate.getCityOfResidence().getCityRuName() : "");
        detailPhone.setValue(candidate.getPhone() != null ? candidate.getPhone() : (candidate.getMobilePhone() != null ? candidate.getMobilePhone() : "-"));
        detailEmail.setValue(candidate.getEmail() != null ? candidate.getEmail() : "-");
        detailTelegram.setValue(candidate.getTelegramName() != null ? "@" + candidate.getTelegramName() : "-");
        if (candidate.getCurrentCompany() != null) {
            String company = candidate.getCurrentCompany().getComanyName() != null ?
                    candidate.getCurrentCompany().getComanyName() : candidate.getCurrentCompany().getCompanyShortName();
            detailCompany.setValue(company != null ? company : "-");
        } else {
            detailCompany.setValue("-");
        }

        String salary = resolveCandidateSalary(candidate);
        if (salary != null && !salary.isEmpty()) {
            detailSalaryCaption.setVisible(true);
            detailSalary.setValue(salary);
            detailSalary.setVisible(true);
        } else {
            detailSalaryCaption.setVisible(false);
            detailSalary.setVisible(false);
        }

        updateCandidateSkillsSidebar(candidate);
        editCandidateBtn.setEnabled(true);
        createInteractionBtn.setEnabled(true);
    }

    private void updateCandidateSkillsSidebar(JobCandidate candidate) {
        if (detailSkillsLabels == null) {
            return;
        }
        if (candidate == null) {
            detailSkillsLabels.setValue("<span style='color: #7f8c8d; font-size: 11px;'>Навыки не определены</span>");
            return;
        }
        try {
            List<CandidateSkill> skills = dataManager.load(CandidateSkill.class)
                    .query("select e from hunttech_CandidateSkill e where e.candidate = :candidate order by e.priority, e.skill.skillName")
                    .parameter("candidate", candidate)
                    .view("candidateSkill-view")
                    .list();

            if (skills.isEmpty()) {
                detailSkillsLabels.setValue("<span style='color: #7f8c8d; font-size: 11px;'>Навыки не определены</span>");
                return;
            }

            StringBuilder sb = new StringBuilder("<div style='display: flex; flex-wrap: wrap; gap: 4px; padding: 2px 0;'>");
            String[] palette = new String[]{"#38bdf8", "#4ade80", "#c084fc", "#fb923c", "#2dd4bf", "#f472b6", "#facc15", "#60a5fa"};
            for (CandidateSkill cs : skills) {
                if (cs.getSkill() != null && cs.getSkill().getSkillName() != null) {
                    String skillName = cs.getSkill().getSkillName();
                    String color = palette[Math.abs(skillName.hashCode()) % palette.length];
                    String priorityIcon = cs.getPriority() == com.company.hunttech.entity.CandidateSkillPriority.MAIN ? "★ " : "";
                    sb.append(String.format(
                            "<span style='background: %s18; color: %s; border: 1px solid %s44; " +
                            "padding: 2px 7px; border-radius: 12px; font-size: 11px; font-weight: 600; " +
                            "white-space: nowrap; display: inline-block;'>%s%s</span>",
                            color, color, color, priorityIcon, skillName
                    ));
                }
            }
            sb.append("</div>");
            detailSkillsLabels.setValue(sb.toString());
        } catch (Exception ex) {
            detailSkillsLabels.setValue("<span style='color: #7f8c8d; font-size: 11px;'>Навыки не определены</span>");
        }
    }

    @Subscribe("filterAllBtn")
    public void onFilterAllBtnClick(Button.ClickEvent event) {
        updateFilterButtons(filterAllBtn);
        jobCandidatesDl.removeParameter("createdBy");
        jobCandidatesDl.removeParameter("recrutier");
        jobCandidatesDl.removeParameter("recrutierName");
        jobCandidatesDl.load();
    }

    @Subscribe("filterMyCandidatesBtn")
    public void onFilterMyCandidatesBtnClick(Button.ClickEvent event) {
        updateFilterButtons(filterMyCandidatesBtn);
        String currentLogin = userSession.getUser() != null ? userSession.getUser().getLogin() : "";
        jobCandidatesDl.setParameter("createdBy", currentLogin);
        jobCandidatesDl.removeParameter("recrutier");
        jobCandidatesDl.removeParameter("recrutierName");
        jobCandidatesDl.load();
    }

    @Subscribe("filterMyParticipationBtn")
    public void onFilterMyParticipationBtnClick(Button.ClickEvent event) {
        updateFilterButtons(filterMyParticipationBtn);
        jobCandidatesDl.removeParameter("createdBy");
        jobCandidatesDl.setParameter("recrutier", userSession.getUser());
        String currentLogin = userSession.getUser() != null ? userSession.getUser().getLogin() : "";
        jobCandidatesDl.setParameter("recrutierName", currentLogin);
        jobCandidatesDl.load();
    }

    private void updateFilterButtons(Button activeBtn) {
        filterAllBtn.setStyleName("secondary filter-pill-btn");
        filterMyCandidatesBtn.setStyleName("secondary filter-pill-btn");
        filterMyParticipationBtn.setStyleName("secondary filter-pill-btn");
        activeBtn.setStyleName("primary filter-pill-btn active");
    }

    @Subscribe("createCandidateBtn")
    public void onCreateCandidateBtnClick(Button.ClickEvent event) {
        screenBuilders.editor(candidatesTable)
                .newEntity()
                .withOpenMode(OpenMode.DIALOG)
                .show();
    }

    @Subscribe("actionsWithCandidateButton.refreshAction")
    public void onActionsWithCandidateButtonRefreshAction(Action.ActionPerformedEvent event) {
        jobCandidatesDl.load();
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
}
