package com.company.hunttech.web.screens.jobcandidate;

import com.company.hunttech.core.ParseCVService;
import com.company.hunttech.entity.CandidateCV;
import com.company.hunttech.entity.CandidateSkill;
import com.company.hunttech.entity.CandidateSkillPriority;
import com.company.hunttech.entity.Employee;
import com.company.hunttech.entity.ExtUser;
import com.company.hunttech.entity.IteractionList;
import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.entity.JobCandidateSignIcon;
import com.company.hunttech.entity.SignIcons;
import com.company.hunttech.entity.SkillTree;
import com.company.hunttech.service.AiExecutionResult;
import com.company.hunttech.service.SkillAnalysisResult;
import com.company.hunttech.service.SkillAnalysisService;
import com.company.hunttech.core.StarsAndOtherService;
import com.company.hunttech.web.screens.fragments.OnlyTextPersonPosition;
import com.company.hunttech.web.screens.fragments.OnlyTextPersonPositionLoadPdf;
import com.company.hunttech.web.screens.signicons.SignIconsBrowse;
import com.company.hunttech.web.util.AiOperationNotifier;
import com.company.hunttech.web.util.FileDescriptorImageHelper;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.Screens;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.Action;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.ContentMode;
import com.haulmont.cuba.gui.components.DialogAction;
import com.haulmont.cuba.gui.components.GroupTable;
import com.haulmont.cuba.gui.components.HBoxLayout;
import com.haulmont.cuba.gui.components.Image;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.PopupButton;
import com.haulmont.cuba.gui.components.Table;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.executors.BackgroundTask;
import com.haulmont.cuba.gui.executors.BackgroundWorker;
import com.haulmont.cuba.gui.executors.TaskLifeCycle;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.model.DataContext;
import com.haulmont.cuba.core.global.ViewBuilder;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.gui.screen.OpenMode;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.StandardLookup;
import com.haulmont.cuba.gui.screen.StandardOutcome;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.Target;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.security.global.UserSession;
import com.hunttech.hrm.web.components.WebOvaFallbackImage;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Контроллер экрана «Реестр кандидатов» (Split-View Halo).
 */
@UiController("hunttech_JobCandidateReestr.browse")
@UiDescriptor("job-candidate-reestr.xml")
@LookupComponent("candidatesTable")
@LoadDataBeforeShow
public class JobCandidateReestr extends StandardLookup<JobCandidate> {

    private static final Logger log = LoggerFactory.getLogger(JobCandidateReestr.class);

    private static final String QUERY_GET_JOB_CANDIDATE_SIGN_ICONS =
            "select e from hunttech_JobCandidateSignIcon e where e.jobCandidate = :jobCandidate order by e.createTs";

    /* =========================================================================
     * Инъекции компонентов UI и сервисов
     * ========================================================================= */

    @Inject
    private GroupTable<JobCandidate> candidatesTable;

    @Inject
    private CollectionLoader<JobCandidate> jobCandidatesDl;

    @Inject
    private CollectionContainer<SignIcons> signIconsDc;

    @Inject
    private CollectionLoader<SignIcons> signIconsDl;

    @Inject
    private ScreenBuilders screenBuilders;

    @Inject
    private Screens screens;

    @Inject
    private UiComponents uiComponents;
    @Inject
    private FileLoader fileLoader;

    @Inject
    private Metadata metadata;

    @Inject
    private com.haulmont.cuba.core.global.DataManager dataManager;

    @Inject
    private Notifications notifications;

    @Inject
    private Dialogs dialogs;

    @Inject
    private UserSession userSession;

    @Inject
    private SkillAnalysisService skillAnalysisService;

    @Inject
    private ParseCVService parseCVService;

    @Inject
    private BackgroundWorker backgroundWorker;

    @Inject
    private StarsAndOtherService starsAndOtherService;

    @Inject
    private DataContext dataContext;

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
    private Label<String> detailRating;
    @Inject
    private Label<String> detailVacancyStatus;
    @Inject
    private Label<String> detailNumber;
    @Inject
    private Label<String> detailReadiness;
    @Inject
    private Label<String> detailSentCandidates;
    @Inject
    private Button editCandidateBtn;
    @Inject
    private Button createInteractionBtn;

    @Inject
    private Button createCandidateBtn;
    @Inject
    private Button editCandidateToolbarBtn;
    @Inject
    private Button removeCandidateToolbarBtn;
    @Inject
    private PopupButton quickLoadCV;
    @Inject
    private PopupButton candidatesFilterPopupButton;
    @Inject
    private PopupButton signFilterButton;
    @Inject
    private PopupButton signIconsButton;
    @Inject
    private PopupButton actionsWithCandidateButton;

    private final java.text.SimpleDateFormat interactionDateFormat = new java.text.SimpleDateFormat("dd.MM.yyyy");

    /** Пакетно загруженные метки кандидатов (устранение N+1). */
    private Map<UUID, List<SignIcons>> signIconsByCandidateId = Collections.emptyMap();

    /** Пакетно загруженные навыки кандидатов (устранение N+1). */
    private Map<UUID, List<CandidateSkill>> skillsByCandidateId = Collections.emptyMap();

    /** Пакетно загруженное количество отправленных на сторону заказчика (signSendToClient=true). */
    private Map<UUID, Integer> sentCvCountByCandidateId = Collections.emptyMap();

    /** Пакетно загруженные Employee (в штате) кандидатов (устранение N+1). */
    private Map<UUID, Employee> employeeByCandidateId = Collections.emptyMap();

    /** Пакетно загруженные кандидаты с CV (устранение N+1 для индикатора ✓ Резюме). */
    private Set<UUID> candidatesWithCv = Collections.emptySet();

    /** Пакетно загруженные даты последнего взаимодействия (устранение N+1 для индикатора ✓ Активность). */
    private Map<UUID, Date> lastInteractionDateByCandidateId = Collections.emptyMap();

    /** Пакетно загруженные зарплатные ожидания (устранение N+1). */
    private Map<UUID, String> salaryByCandidateId = Collections.emptyMap();

    /** Пакетно загруженное последнее взаимодействие кандидата (устранение N+1 для рейтинга/статуса вакансии). */
    private Map<UUID, IteractionList> lastInteractionByCandidateId = Collections.emptyMap();

    public enum InteractionStatus {
        FREE("🟢 Свободен", "#27ae60", "rgba(39, 174, 96, 0.15)"),
        MY_CANDIDATE("🟡 В вашей работе", "#f39c12", "rgba(243, 156, 18, 0.15)"),
        OTHER_RECRUITER("🔴 В работе у другого", "#e74c3c", "rgba(231, 76, 60, 0.15)");

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
        // Используем кэш зарплатных ожиданий вместо N+1 запроса
        return salaryByCandidateId.get(candidate.getId());
    }

    /* =========================================================================
     * Пакетная загрузка связанных данных (Zero N+1)
     * ========================================================================= */

    @Subscribe(id = "jobCandidatesDl", target = Target.DATA_LOADER)
    public void onJobCandidatesDlPostLoad(CollectionLoader.PostLoadEvent<JobCandidate> event) {
        List<JobCandidate> candidates = event.getLoadedEntities();
        if (candidates == null || candidates.isEmpty()) {
            signIconsByCandidateId = Collections.emptyMap();
            skillsByCandidateId = Collections.emptyMap();
            return;
        }

        // Пакетная загрузка SignIcons для всех видимых кандидатов (1 SQL-запрос)
        List<JobCandidateSignIcon> allSignIcons = dataManager.load(JobCandidateSignIcon.class)
                .query("select e from hunttech_JobCandidateSignIcon e where e.jobCandidate in :candidates order by e.createTs asc")
                .parameter("candidates", candidates)
                .view("jobCandidateSignIcon-view")
                .list();

        Map<UUID, List<SignIcons>> signMap = new HashMap<>();
        for (JobCandidateSignIcon jcsi : allSignIcons) {
            if (jcsi.getJobCandidate() != null && jcsi.getSignIcon() != null) {
                signMap.computeIfAbsent(jcsi.getJobCandidate().getId(), k -> new ArrayList<>()).add(jcsi.getSignIcon());
            }
        }
        signIconsByCandidateId = signMap;

        // Пакетная загрузка CandidateSkills для всех видимых кандидатов (1 SQL-запрос)
        List<CandidateSkill> allSkills = dataManager.load(CandidateSkill.class)
                .query("select e from hunttech_CandidateSkill e where e.candidate in :candidates order by e.priority, e.skill.skillName")
                .parameter("candidates", candidates)
                .view("candidateSkill-view")
                .list();

        Map<UUID, List<CandidateSkill>> skillsMap = new HashMap<>();
        for (CandidateSkill cs : allSkills) {
            if (cs.getCandidate() != null) {
                skillsMap.computeIfAbsent(cs.getCandidate().getId(), k -> new ArrayList<>()).add(cs);
            }
        }
        skillsByCandidateId = skillsMap;

        // Пакетная загрузка счётчика отправленных на сторону заказчика (signSendToClient=true)
        List<com.haulmont.cuba.core.entity.KeyValueEntity> sentRows = dataManager.loadValues(
                "select e.candidate.id as candId, count(e) as cnt from hunttech_IteractionList e " +
                        "where e.candidate in :candidates and e.iteractionType.signSendToClient = true " +
                        "group by e.candidate.id")
                .parameter("candidates", candidates)
                .list();

        Map<UUID, Integer> sentMap = new HashMap<>();
        for (com.haulmont.cuba.core.entity.KeyValueEntity row : sentRows) {
            UUID candId = row.getValue("candId");
            Long cnt = row.getValue("cnt");
            if (candId != null && cnt != null) {
                sentMap.put(candId, cnt.intValue());
            }
        }
        sentCvCountByCandidateId = sentMap;

        // Пакетная загрузка Employee (в штате) для индикатора «В резерве» (1 SQL-запрос)
        List<Employee> allEmployees = dataManager.load(Employee.class)
                .query("select e from hunttech_Employee e where e.jobCandidate in :candidates")
                .parameter("candidates", candidates)
                .view(ViewBuilder.of(Employee.class)
                        .add("jobCandidate", "_minimal")
                        .add("workStatus", workStatusView -> workStatusView.add("inStaff"))
                        .build())
                .list();

        Map<UUID, Employee> empMap = new HashMap<>();
        for (Employee emp : allEmployees) {
            if (emp.getJobCandidate() != null && emp.getJobCandidate().getId() != null) {
                empMap.put(emp.getJobCandidate().getId(), emp);
            }
        }
        employeeByCandidateId = empMap;

        // Пакетная загрузка последнего взаимодействия (rating, vacancy, numberIteraction, addString для зарплатных ожиданий)
        List<IteractionList> lastInteractions = dataManager.load(IteractionList.class)
                .query("select e from hunttech_IteractionList e where e.candidate in :candidates " +
                        "order by e.candidate.id, e.dateIteraction desc, e.createTs desc")
                .parameter("candidates", candidates)
                .view("iteractionList-view")  // должен включать vacancy, rating, addString
                .list();

        Map<UUID, IteractionList> lastInterMap = new HashMap<>();
        for (IteractionList il : lastInteractions) {
            if (il.getCandidate() != null && il.getCandidate().getId() != null) {
                lastInterMap.putIfAbsent(il.getCandidate().getId(), il);
            }
        }
        // Переиспользуем lastInteractionByCandidateId (уже существует в классе) для кэша последнего взаимодействия
        lastInteractionByCandidateId = lastInterMap;

        // Пакетная загрузка зарплатных ожиданий (по interaction type "Зарплатные ожидания")
        List<IteractionList> salaryInteractions = dataManager.load(IteractionList.class)
                .query("select e from hunttech_IteractionList e " +
                        "where e.candidate in :candidates " +
                        "and e.iteractionType.iteractionTree.iteractionRuName like :name " +
                        "order by e.dateIteraction desc, e.createTs desc")
                .parameter("candidates", candidates)
                .parameter("name", "%Зарплатные ожидания%")
                .view("iteractionList-view")
                .maxResults(candidates.size()) // максимум 1 на кандидата
                .list();

        Map<UUID, String> salaryMap = new HashMap<>();
                for (IteractionList il : salaryInteractions) {
                    if (il.getCandidate() != null && il.getCandidate().getId() != null 
                            && il.getAddString() != null && !il.getAddString().trim().isEmpty()) {
                        salaryMap.putIfAbsent(il.getCandidate().getId(), il.getAddString().trim());
                    }
                }
                salaryByCandidateId = salaryMap;

                // Пакетная загрузка кандидатов с CV (1 SQL-запрос)
        List<com.haulmont.cuba.core.entity.KeyValueEntity> cvRows = dataManager.loadValues(
                "select e.candidate.id as candId from hunttech_CandidateCV e where e.candidate in :candidates group by e.candidate.id")
                .parameter("candidates", candidates)
                .list();

        Set<UUID> cvMap = new HashSet<>();
        for (com.haulmont.cuba.core.entity.KeyValueEntity row : cvRows) {
            UUID candId = row.getValue("candId");
            if (candId != null) {
                cvMap.add(candId);
            }
        }
        candidatesWithCv = cvMap;

        // Пакетная загрузка дат последнего взаимодействия (1 SQL-запрос)
        List<com.haulmont.cuba.core.entity.KeyValueEntity> interRows = dataManager.loadValues(
                "select e.candidate.id as candId, max(e.dateIteraction) as lastDate from hunttech_IteractionList e " +
                        "where e.candidate in :candidates group by e.candidate.id")
                .parameter("candidates", candidates)
                .list();

        Map<UUID, Date> interMap = new HashMap<>();
        for (com.haulmont.cuba.core.entity.KeyValueEntity row : interRows) {
            UUID candId = row.getValue("candId");
            Date lastDate = row.getValue("lastDate");
            if (candId != null && lastDate != null) {
                interMap.put(candId, lastDate);
            }
        }
        lastInteractionDateByCandidateId = interMap;
    }

    /* =========================================================================
     * Инициализация колонок и жизненного цикла
     * ========================================================================= */

    @Subscribe
    public void onInit(Screen.InitEvent event) {
        // Выравнивание заголовков профиля (под OvalFallbackImage) по центру
        detailFullName.setAlignment(Component.Alignment.MIDDLE_CENTER);
        detailPosition.setAlignment(Component.Alignment.MIDDLE_CENTER);
        detailCity.setAlignment(Component.Alignment.MIDDLE_CENTER);

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

        // Колонка 3: Кандидат (ФИО слева ячейки; справа — метки пользователя из пакетного кэша)
        candidatesTable.addGeneratedColumn("fullName", candidate -> {
            String name = candidate.getFullName() != null ? candidate.getFullName() : "Без имени";
            String sub = candidate.getTelegramName() != null ? "@" + candidate.getTelegramName() :
                    (candidate.getEmail() != null ? candidate.getEmail() : "");
            String textHtml = "<div style='text-align: left; white-space: normal; word-break: break-word;'><div style='font-weight: 600; color: #2c3e50; font-size: 13px;'>" + name + "</div>" +
                    (!sub.isEmpty() ? "<div style='font-size: 11px; color: #7f8c8d;'>" + sub + "</div>" : "") + "</div>";

            // Метки кандидата берутся из предварительно сформированной пакетной карты (без N+1)
            List<SignIcons> icons = signIconsByCandidateId.getOrDefault(candidate.getId(), Collections.emptyList());

            // Пустое состояние: только текст
            if (icons.isEmpty()) {
                Label<String> plain = uiComponents.create(Label.NAME);
                plain.setHtmlEnabled(true);
                plain.setWidth("100%");
                plain.setAlignment(Component.Alignment.MIDDLE_CENTER);
                plain.setValue(textHtml);
                return plain;
            }

            HBoxLayout box = uiComponents.create(HBoxLayout.NAME);
            box.setWidth("100%");
            box.setSpacing(true);

            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            lbl.setWidth("100%");
            lbl.setAlignment(Component.Alignment.MIDDLE_CENTER);
            lbl.setValue(textHtml);
            box.add(lbl);
            box.setExpandRatio(lbl, 1f);

            // Метки: до 4 иконок, при превышении — компактный «+N»
            int shown = 0;
            int total = icons.size();
            for (SignIcons icon : icons) {
                if (icon.getIconName() == null) {
                    continue;
                }
                if (shown >= 4) {
                    Label moreLabel = uiComponents.create(Label.NAME);
                    moreLabel.setHtmlEnabled(true);
                    moreLabel.setAlignment(Component.Alignment.MIDDLE_CENTER);
                    moreLabel.setValue("<span style='font-size: 10px; color: #7f8c8d; align-self: center;'>+" + (total - shown) + "</span>");
                    box.add(moreLabel);
                    break;
                }
                Label iconLabel = uiComponents.create(Label.class);
                iconLabel.setIcon(icon.getIconName());
                iconLabel.setAlignment(Component.Alignment.MIDDLE_CENTER);
                String desc = icon.getTitleDescription() != null
                        ? icon.getTitleDescription() : icon.getTitleRu();
                if (desc != null && !desc.trim().isEmpty()) {
                    iconLabel.setDescription(desc);
                }
                String color = icon.getIconColor();
                if (color != null && !color.trim().isEmpty()) {
                    color = color.trim();
                    injectColorCss(color);
                    iconLabel.setStyleName("pic-center-large-" + color);
                }
                box.add(iconLabel);
                shown++;
            }
            return box;
        });

        // Колонка 4: Должность
                candidatesTable.addGeneratedColumn("personPosition", candidate -> {
                    Label<String> lbl = uiComponents.create(Label.NAME);
                    lbl.setHtmlEnabled(true);
                    String pos = candidate.getPersonPosition() != null ? candidate.getPersonPosition().getPositionRuName() : "Специалист";
                    lbl.setValue("<span style='background: rgba(43, 130, 201, 0.12); color: #2b82c9; padding: 2px 8px; border-radius: 4px; font-weight: 600; font-size: 11px; display: inline-block; white-space: normal; word-break: break-word;'>" + pos + "</span>");
                    return lbl;
                });

                // Колонка 5: Город
                candidatesTable.addGeneratedColumn("cityOfResidence", candidate -> {
                    Label<String> lbl = uiComponents.create(Label.NAME);
                    lbl.setHtmlEnabled(true);
                    String city = candidate.getCityOfResidence() != null ? candidate.getCityOfResidence().getCityRuName() : "Москва";
                    lbl.setValue("<span style='font-size: 12px; color: #34495e; white-space: normal; word-break: break-word;'>📍 " + city + "</span>");
                    return lbl;
                });

                // Колонка 6: Компания
                candidatesTable.addGeneratedColumn("currentCompany", candidate -> {
                    Label<String> lbl = uiComponents.create(Label.NAME);
                    lbl.setHtmlEnabled(true);
                    String company = "-";
                    if (candidate.getCurrentCompany() != null) {
                        company = candidate.getCurrentCompany().getComanyName() != null ?
                                candidate.getCurrentCompany().getComanyName() : candidate.getCurrentCompany().getCompanyShortName();
                    }
                    lbl.setValue("<span style='font-size: 12px; color: #34495e; white-space: normal; word-break: break-word;'>" + (company != null ? company : "-") + "</span>");
                    return lbl;
                });

        // Колонка 7: Ключевые навыки (чипы из пакетной карты без N+1)
        candidatesTable.addGeneratedColumn("mainSkills", candidate -> {
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            try {
                List<CandidateSkill> skills = skillsByCandidateId.getOrDefault(candidate.getId(), Collections.emptyList());

                if (skills.isEmpty()) {
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

        // Колонка: Отправлено (счётчик кандидатов, отправленных на сторону заказчика)
        candidatesTable.addGeneratedColumn("sentCount", candidate -> {
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            Integer sentCount = sentCvCountByCandidateId.getOrDefault(candidate.getId(), 0);
            String color, bg;
            if (sentCount < 3) {
                color = "#16a34a";
                bg = "rgba(34, 197, 94, 0.12)";
            } else if (sentCount < 5) {
                color = "#ca8a04";
                bg = "rgba(250, 204, 21, 0.15)";
            } else {
                color = "#dc2626";
                bg = "rgba(239, 68, 68, 0.12)";
            }
            lbl.setValue(String.format(
                    "<span style='background: %s; color: %s; padding: 2px 10px; border-radius: 10px; font-weight: 700; font-size: 11px; white-space: nowrap; display: inline-block;'>%d</span>",
                    bg, color, sentCount));
            return lbl;
        });

        // Колонка 8: Статус взаимодействия
        candidatesTable.addGeneratedColumn("lastInteractionStatus", candidate -> {
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            InteractionStatus status = calculateInteractionStatus(candidate);
            lbl.setValue(String.format(
                    "<span style='background: %s; color: %s; padding: 2px 6px; border-radius: 4px; font-size: 10.5px; font-weight: 600; white-space: normal; word-break: break-word; display: inline-block;'>%s</span>",
                    status.getBgColor(), status.getColor(), status.getLabel()
            ));
            return lbl;
        });

        updateActionsState(candidatesTable.getSingleSelected());
        updateSignIconsState(candidatesTable.getSingleSelected());
    }

    @Subscribe
    public void onBeforeShow(Screen.BeforeShowEvent event) {
        initSignIconsDataContainer();
        injectAllSignIconColors();
        initSignFilterPopupButton();
        initSignIconsActions();
        updateSignIconsState(candidatesTable.getSingleSelected());
    }

    /**
     * Предварительная инъекция CSS-правил цветов всех меток пользователя.
     */
    private void injectAllSignIconColors() {
        if (signIconsDc == null) {
            return;
        }
        for (SignIcons icon : signIconsDc.getItems()) {
            if (icon != null && icon.getIconColor() != null && !icon.getIconColor().trim().isEmpty()) {
                injectColorCss(icon.getIconColor().trim());
            }
        }
    }

    private void initSignIconsDataContainer() {
        signIconsDl.setParameter("user", (ExtUser) userSession.getUser());
        signIconsDl.load();
    }

    @Subscribe(id = "signIconsDc", target = Target.DATA_CONTAINER)
    public void onSignIconsDcCollectionChange(CollectionContainer.CollectionChangeEvent<SignIcons> event) {
        initSignFilterPopupButton();
        initSignIconsActions();
        updateSignIconsState(candidatesTable.getSingleSelected());
    }

    /* =========================================================================
     * Выпадающий фильтр по меткам/значкам (signFilterButton)
     * ========================================================================= */

    private void initSignFilterPopupButton() {
        if (signFilterButton == null) return;
        signFilterButton.removeAllActions();

        final String separator = "⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯";

        for (SignIcons icons : signIconsDc.getItems()) {
            String actId = "signFilter_" + (icons.getId() != null ? icons.getId().toString().replace("-", "_") : icons.getTitleEnd());
            signFilterButton.addAction(new BaseAction(actId)
                    .withIcon(icons.getIconName())
                    .withCaption(icons.getTitleRu() != null ? icons.getTitleRu() : icons.getTitleEnd())
                    .withDescription(icons.getTitleDescription())
                    .withHandler(actionPerformedAction -> {
                        setSignFilter(icons);
                    }));
        }

        signFilterButton.addAction(new BaseAction("separator1Action")
                .withCaption(separator));

        signFilterButton.addAction(new BaseAction("removeFilterSignAction")
                .withIcon(CubaIcon.REMOVE_ACTION.source())
                .withCaption("Снять фильтр по меткам")
                .withDescription("Снять фильтрацию по значкам и показать всех кандидатов")
                .withHandler(actionPerformedAction -> {
                    removeSignFilterAction();
                }));

        signFilterButton.addAction(new BaseAction("separator3Action")
                .withCaption(separator));

        signFilterButton.addAction(new BaseAction("editSignIconsAction")
                .withCaption("Справочник меток...")
                .withDescription("Настройка справочника значков и меток")
                .withIcon(CubaIcon.FONTICONS.source())
                .withHandler(actionPerformedAction -> {
                    SignIconsBrowse screen = (SignIconsBrowse) screenBuilders.lookup(SignIcons.class, this)
                            .withOpenMode(OpenMode.DIALOG)
                            .build();
                    screen.addAfterCloseListener(closeEvent -> {
                        signIconsDl.load();
                        initSignFilterPopupButton();
                        initSignIconsActions();
                        jobCandidatesDl.load();
                    });
                    screen.show();
                }));
    }

    private void removeSignFilterAction() {
        jobCandidatesDl.removeParameter("signIcon");
        jobCandidatesDl.load();
        notifications.create(Notifications.NotificationType.TRAY)
                .withCaption("Фильтр по меткам снят")
                .show();
    }

    private void setSignFilter(SignIcons icons) {
        jobCandidatesDl.removeParameter("signIcon");
        jobCandidatesDl.setParameter("signIcon", icons);
        jobCandidatesDl.load();
        notifications.create(Notifications.NotificationType.TRAY)
                .withCaption("Фильтр по метке")
                .withDescription(icons.getTitleRu() != null ? icons.getTitleRu() : "")
                .show();
    }

    /* =========================================================================
     * Выпадающая кнопка присвоения меток кандидату (signIconsButton)
     * ========================================================================= */

    private void initSignIconsActions() {
        if (signIconsButton == null) return;

        signIconsButton.removeAllActions();

        for (SignIcons icon : signIconsDc.getItems()) {
            String actId = "sign_" + (icon.getId() != null ? icon.getId().toString().replace("-", "_") : icon.getTitleRu());
            signIconsButton.addAction(new BaseAction(actId)
                    .withIcon(icon.getIconName())
                    .withCaption(icon.getTitleRu() != null ? icon.getTitleRu() : "Метка")
                    .withDescription(icon.getTitleDescription())
                    .withHandler(e -> {
                        JobCandidate selected = candidatesTable.getSingleSelected();
                        if (selected != null) {
                            setSignIcons(icon, selected);
                        }
                    }));
        }

        signIconsButton.addAction(new BaseAction("removeSignAction")
                .withIcon(CubaIcon.REMOVE_ACTION.source())
                .withCaption("Снять метку")
                .withDescription("Снять присвоенную метку с выбранного кандидата")
                .withHandler(e -> {
                    JobCandidate selected = candidatesTable.getSingleSelected();
                    if (selected != null) {
                        removeSignAction(selected);
                    }
                }));

        signIconsButton.addAction(new BaseAction("editSignIconsAction")
                .withCaption("Редактирование значков")
                .withDescription("Настройка справочника значков и меток")
                .withIcon(CubaIcon.FONTICONS.source())
                .withHandler(e -> {
                    SignIconsBrowse screen = (SignIconsBrowse) screenBuilders.lookup(SignIcons.class, this)
                            .withOpenMode(OpenMode.DIALOG)
                            .build();
                    screen.addAfterCloseListener(closeEvent -> {
                        signIconsDl.load();
                        initSignFilterPopupButton();
                        initSignIconsActions();
                        jobCandidatesDl.load();
                    });
                    screen.show();
                }));
    }

    private void updateSignIconsState(JobCandidate selected) {
        if (signIconsButton == null) return;
        boolean hasSelected = selected != null;
        signIconsButton.setEnabled(hasSelected);
        if (hasSelected && signIconsButton.getAction("removeSignAction") != null) {
            List<SignIcons> assigned = signIconsByCandidateId.getOrDefault(selected.getId(), Collections.emptyList());
            signIconsButton.getAction("removeSignAction").setEnabled(!assigned.isEmpty());
        }
    }

    private void setSignIcons(SignIcons icon, JobCandidate jobCandidate) {
        if (jobCandidate == null || icon == null) return;
        List<JobCandidateSignIcon> list = dataManager.load(JobCandidateSignIcon.class)
                .query(QUERY_GET_JOB_CANDIDATE_SIGN_ICONS)
                .parameter("jobCandidate", jobCandidate)
                .view("jobCandidateSignIcon-view")
                .list();

        if (list.isEmpty()) {
            JobCandidateSignIcon jcsi = metadata.create(JobCandidateSignIcon.class);
            jcsi.setJobCandidate(jobCandidate);
            jcsi.setSignIcon(icon);
            if (userSession.getUser() instanceof ExtUser) {
                jcsi.setUser((ExtUser) userSession.getUser());
            }
            dataManager.commit(jcsi);
        } else {
            JobCandidateSignIcon jcsi = list.get(0);
            jcsi.setSignIcon(icon);
            dataManager.commit(jcsi);
        }

        jobCandidatesDl.load();
        candidatesTable.setSelected(jobCandidate);
        updateSignIconsState(jobCandidate);
        notifications.create(Notifications.NotificationType.TRAY)
                .withCaption("Метка присвоена")
                .withDescription(icon.getTitleRu() != null ? icon.getTitleRu() : "")
                .show();
    }

    /** Уже инъектированные цвета меток (дедупликация CSS-правил). */
    private static final Set<String> INJECTED_COLORS = new HashSet<>();

    private void injectColorCss(String color) {
        if (!INJECTED_COLORS.add(color)) {
            return;
        }
        com.vaadin.server.Page.Styles styles = com.vaadin.server.Page.getCurrent().getStyles();
        String style = String.format(
                ".pic-center-large-%s {" +
                        "color: #%s;" +
                        "text-align: center;" +
                        "font-size: large;" +
                        "}",
                color, color);
        styles.add(style);
    }

    private void removeSignAction(JobCandidate jobCandidate) {
        if (jobCandidate == null) return;
        List<JobCandidateSignIcon> list = dataManager.load(JobCandidateSignIcon.class)
                .query(QUERY_GET_JOB_CANDIDATE_SIGN_ICONS)
                .parameter("jobCandidate", jobCandidate)
                .view("jobCandidateSignIcon-view")
                .list();

        if (!list.isEmpty()) {
            for (JobCandidateSignIcon jcsi : list) {
                dataManager.remove(jcsi);
            }
        }

        jobCandidatesDl.load();
        candidatesTable.setSelected(jobCandidate);
        updateSignIconsState(jobCandidate);
        notifications.create(Notifications.NotificationType.TRAY)
                .withCaption("Метка снята")
                .show();
    }

    /* =========================================================================
     * Быстрая и умная загрузка резюме (quickLoadCV)
     * ========================================================================= */

    @Subscribe("quickLoadCV.smartLoad")
    public void onQuickLoadCVSmartLoad(Action.ActionPerformedEvent event) {
        openSmartCvUploadDialog();
    }

    private void openSmartCvUploadDialog() {
        SmartCvUploadScreen screen = screenBuilders.screen(this)
                .withScreenClass(SmartCvUploadScreen.class)
                .withOpenMode(OpenMode.DIALOG)
                .build();
        screen.addAfterCloseListener(closeEvent -> {
            if (closeEvent.closedWith(StandardOutcome.COMMIT)) {
                setCandidateScopeFilter("ALL", "Все кандидаты", "USERS");
                if (screen.getCreatedCandidate() != null) {
                    try {
                        candidatesTable.setSelected(screen.getCreatedCandidate());
                    } catch (Exception ignored) {
                    }
                }
            }
        });
        screen.show();
    }

    @Subscribe("quickLoadCV.loadFromPdf")
    public void onQuickLoadCVLoadFromPdf(Action.ActionPerformedEvent event) {
        OnlyTextPersonPositionLoadPdf screen = screenBuilders.screen(this)
                .withScreenClass(OnlyTextPersonPositionLoadPdf.class)
                .withOpenMode(OpenMode.NEW_TAB)
                .build();

        screen.addAfterCloseListener(afterCloseEvent -> {
            jobCandidatesDl.load();
        });

        screen.show();
    }

    @Subscribe("quickLoadCV.loadFromClipboard")
    public void onQuickLoadCVLoadFromClipboard(Action.ActionPerformedEvent event) {
        OnlyTextPersonPosition screenOnlytext = screenBuilders.screen(this)
                .withScreenClass(OnlyTextPersonPosition.class)
                .withOpenMode(OpenMode.NEW_TAB)
                .build();

        screenOnlytext.addAfterCloseListener(afterCloseEvent -> {
            if (Boolean.TRUE.equals(screenOnlytext.getCancel())) {
                return;
            }
            String textCV = screenOnlytext.getResultText();
            if (textCV != null && !textCV.trim().isEmpty()) {
                Screen jobCandidateEdit = screenBuilders.editor(JobCandidate.class, this)
                        .withOpenMode(OpenMode.NEW_TAB)
                        .withScreenClass(JobCandidateEdit.class)
                        .withAfterCloseListener(eventAfterClose -> {
                            jobCandidatesDl.load();
                        })
                        .withInitializer(e -> {
                            selectFirstNames(textCV, e);
                            selectMiddleNames(textCV, e);
                            selectSecondNames(textCV, e);

                            if (parseCVService != null) {
                                e.setEmail(parseCVService.parseEmail(textCV));
                                e.setPhone(parseCVService.parsePhone(textCV));
                                e.setBirdhDate(parseCVService.parseDate(textCV));
                                e.setCurrentCompany(parseCVService.parseCompany(textCV));
                                e.setCityOfResidence(parseCVService.parseCity(textCV));
                                e.setPersonPosition(screenOnlytext.getPersonPosition());
                                e.setTelegramName(parseCVService.parseTelegram(textCV));
                                e.setSkypeName(parseCVService.parseSkype(textCV));
                            }

                            CandidateCV candidateCV = metadata.create(CandidateCV.class);
                            candidateCV.setResumePosition(e.getPersonPosition());
                            candidateCV.setTextCV(textCV);
                            if (userSession.getUser() instanceof ExtUser) {
                                candidateCV.setOwner((ExtUser) userSession.getUser());
                            }
                            candidateCV.setCandidate(e);
                            candidateCV.setDatePost(new Date());

                            List<CandidateCV> candidateCVS = new ArrayList<>();
                            candidateCVS.add(candidateCV);
                            e.setCandidateCv(candidateCVS);
                        })
                        .build();
                jobCandidateEdit.show();
            }
        });

        screenOnlytext.show();
    }

    @Subscribe("quickLoadCV.loadFromWord")
    public void onQuickLoadCVLoadFromWord(Action.ActionPerformedEvent event) {
        onQuickLoadCVLoadFromClipboard(event);
    }

    private void selectFirstNames(String textCV, JobCandidate e) {
        if (parseCVService == null) return;
        List<String> namesList = parseCVService.getFirstNameList(textCV);
        if (namesList != null && !namesList.isEmpty()) {
            e.setFirstName(namesList.get(0));
        }
    }

    private void selectMiddleNames(String textCV, JobCandidate e) {
        if (parseCVService == null) return;
        List<String> namesList = parseCVService.getMiddleNameList(textCV);
        if (namesList != null && !namesList.isEmpty()) {
            e.setMiddleName(namesList.get(0));
        }
    }

    private void selectSecondNames(String textCV, JobCandidate e) {
        if (parseCVService == null) return;
        List<String> namesList = parseCVService.getSecondNameList(textCV);
        if (namesList != null && !namesList.isEmpty()) {
            e.setSecondName(namesList.get(0));
        }
    }

    /* =========================================================================
     * Выбор строки и обновление состояния интерфейса
     * ========================================================================= */

    @Subscribe("candidatesTable")
    public void onCandidatesTableSelection(Table.SelectionEvent<JobCandidate> event) {
        JobCandidate selected = candidatesTable.getSingleSelected();
        if (selected == null) {
            clearDetailPane();
        } else {
            populateDetailPane(selected);
        }
        updateActionsState(selected);
        updateSignIconsState(selected);
    }

    private void updateActionsState(JobCandidate selected) {
        boolean hasSelected = selected != null;
        if (editCandidateToolbarBtn != null) {
            editCandidateToolbarBtn.setEnabled(hasSelected);
        }
        if (removeCandidateToolbarBtn != null) {
            removeCandidateToolbarBtn.setEnabled(hasSelected);
        }
        if (actionsWithCandidateButton == null) return;
        actionsWithCandidateButton.setEnabled(true);
        if (actionsWithCandidateButton.getAction("editCandidateAction") != null) {
            actionsWithCandidateButton.getAction("editCandidateAction").setEnabled(hasSelected);
        }
        if (actionsWithCandidateButton.getAction("createInteractionAction") != null) {
            actionsWithCandidateButton.getAction("createInteractionAction").setEnabled(hasSelected);
        }
        if (actionsWithCandidateButton.getAction("scanSkillsAction") != null) {
            actionsWithCandidateButton.getAction("scanSkillsAction").setEnabled(hasSelected);
        }
        if (actionsWithCandidateButton.getAction("findSuitableAction") != null) {
            actionsWithCandidateButton.getAction("findSuitableAction").setEnabled(hasSelected);
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
        if (detailRating != null) {
            detailRating.setValue("-");
        }
        if (detailVacancyStatus != null) {
            detailVacancyStatus.setValue("-");
        }
        if (detailNumber != null) {
            detailNumber.setValue("-");
        }
        if (detailReadiness != null) {
            detailReadiness.setValue("-");
        }
        if (detailSentCandidates != null) {
            detailSentCandidates.setValue("-");
        }
        editCandidateBtn.setEnabled(false);
        createInteractionBtn.setEnabled(false);
    }

    private void populateDetailPane(JobCandidate candidate) {
        FileDescriptorImageHelper.setCandidateFace(detailPic, fileLoader, resolveCandidateFace(candidate));
        detailFullName.setValue("<div style='white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%;'>" + (candidate.getFullName() != null ? candidate.getFullName() : "Без имени") + "</div>");
        detailPosition.setValue("<div style='white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%;'>" + (candidate.getPersonPosition() != null ? candidate.getPersonPosition().getPositionRuName() : "Должность не указана") + "</div>");
        detailCity.setValue("<div style='white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%;'>" + (candidate.getCityOfResidence() != null ? "📍 " + candidate.getCityOfResidence().getCityRuName() : "") + "</div>");
        detailPhone.setValue("<div style='white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%;'>" + (candidate.getPhone() != null ? candidate.getPhone() : (candidate.getMobilePhone() != null ? candidate.getMobilePhone() : "—")) + "</div>");
        detailEmail.setValue("<div style='white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%;'>" + (candidate.getEmail() != null ? candidate.getEmail() : "—") + "</div>");
        detailTelegram.setValue("<div style='white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%;'>" + (candidate.getTelegramName() != null ? "@" + candidate.getTelegramName() : "—") + "</div>");
        if (candidate.getCurrentCompany() != null) {
            String company = candidate.getCurrentCompany().getComanyName() != null ?
                    candidate.getCurrentCompany().getComanyName() : candidate.getCurrentCompany().getCompanyShortName();
            detailCompany.setValue("<div style='white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%;'>" + (company != null ? company : "—") + "</div>");
        } else {
            detailCompany.setValue("<div style='white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%;'>—</div>");
        }

        String salary = resolveCandidateSalary(candidate);
        if (salary != null && !salary.isEmpty()) {
            detailSalaryCaption.setVisible(true);
            detailSalary.setValue("<div style='white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%;'>" + salary + "</div>");
            detailSalary.setVisible(true);
        } else {
            detailSalaryCaption.setVisible(false);
            detailSalary.setVisible(false);
        }

        // Рейтинг и статус вакансии из последнего взаимодействия
        updateRatingAndVacancyStatus(candidate);

        updateCandidateSkillsSidebar(candidate);
        updateReadinessRatingAndSent(candidate);
        editCandidateBtn.setEnabled(true);
        createInteractionBtn.setEnabled(true);
    }

    private void updateRatingAndVacancyStatus(JobCandidate candidate) {
        if (detailRating == null || detailVacancyStatus == null) {
            return;
        }
        try {
            // Используем кэш последнего взаимодействия вместо N+1 запроса
            IteractionList last = lastInteractionByCandidateId.get(candidate.getId());

            if (last != null) {

                // Рейтинг
                if (last.getRating() != null && last.getRating() > 0 && starsAndOtherService != null) {
                    detailRating.setValue("<div style='white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%;'>" + starsAndOtherService.setStars(last.getRating()) + "</div>");
                } else {
                    detailRating.setValue("<div style='white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%;'>Не оценен</div>");
                }

                // Статус вакансии
                if (last.getVacancy() != null) {
                    if (Boolean.TRUE.equals(last.getVacancy().getOpenClose())) {
                        detailVacancyStatus.setValue("<div style='white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%;'><span style='background: #fee2e2; color: #b91c1c; border: 1px solid #fecaca; padding: 2px 8px; border-radius: 10px; font-size: 11px; font-weight: 600;'>Закрыта</span></div>");
                    } else {
                        detailVacancyStatus.setValue("<div style='white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%;'><span style='background: #dcfce7; color: #15803d; border: 1px solid #bbf7d0; padding: 2px 8px; border-radius: 10px; font-size: 11px; font-weight: 600;'>Открыта</span></div>");
                    }
                } else {
                    detailVacancyStatus.setValue("<div style='white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%;'>Вакансия не указана</div>");
                }

                // Номер взаимодействия (целое число без точки и нулей)
                if (detailNumber != null && last.getNumberIteraction() != null) {
                    detailNumber.setValue("<div style='white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%;'>" + last.getNumberIteraction().toBigInteger().toString() + "</div>");
                } else if (detailNumber != null) {
                    detailNumber.setValue("<div style='white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%;'>—</div>");
                }
            } else {
                detailRating.setValue("<div style='white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%;'>Нет взаимодействий</div>");
                detailVacancyStatus.setValue("<div style='white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%;'>Нет взаимодействий</div>");
                if (detailNumber != null) {
                    detailNumber.setValue("-");
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to load interaction data for candidate {}", candidate != null ? candidate.getId() : null, ex);
            detailRating.setValue("Ошибка загрузки");
            detailVacancyStatus.setValue("Ошибка загрузки");
            if (detailNumber != null) {
                detailNumber.setValue("Ошибка загрузки");
            }
        }
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
            List<CandidateSkill> skills = skillsByCandidateId.getOrDefault(candidate.getId(), Collections.emptyList());

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
                    String priorityIcon = cs.getPriority() == CandidateSkillPriority.MAIN ? "★ " : "";
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

    /* =========================================================================
     * Быстрые фильтры и действия тулбара
     * ========================================================================= */

    @Subscribe("candidatesFilterPopupButton.filterAll")
    public void onCandidatesFilterPopupButtonFilterAll(Action.ActionPerformedEvent event) {
        setCandidateScopeFilter("ALL", "Все кандидаты", "USERS");
    }

    @Subscribe("candidatesFilterPopupButton.filterMyCandidates")
    public void onCandidatesFilterPopupButtonFilterMyCandidates(Action.ActionPerformedEvent event) {
        setCandidateScopeFilter("MY", "Мои кандидаты", "USER");
    }

    @Subscribe("candidatesFilterPopupButton.filterMyParticipation")
    public void onCandidatesFilterPopupButtonFilterMyParticipation(Action.ActionPerformedEvent event) {
        setCandidateScopeFilter("PARTICIPATION", "С моим участием", "font-icon:ADN");
    }

    private void setCandidateScopeFilter(String scope, String caption, String icon) {
        if (candidatesFilterPopupButton != null) {
            candidatesFilterPopupButton.setCaption(caption);
            candidatesFilterPopupButton.setIcon(icon);
            if ("ALL".equals(scope)) {
                candidatesFilterPopupButton.setStyleName("secondary candidate-btn candidate-filter-scope-btn");
            } else {
                candidatesFilterPopupButton.setStyleName("primary candidate-btn candidate-filter-scope-btn active");
            }
        }
        if ("MY".equals(scope)) {
            String currentLogin = userSession.getUser() != null ? userSession.getUser().getLogin() : "";
            jobCandidatesDl.setParameter("createdBy", currentLogin);
            jobCandidatesDl.removeParameter("recrutier");
            jobCandidatesDl.removeParameter("recrutierName");
        } else if ("PARTICIPATION".equals(scope)) {
            jobCandidatesDl.removeParameter("createdBy");
            jobCandidatesDl.setParameter("recrutier", userSession.getUser());
            String currentLogin = userSession.getUser() != null ? userSession.getUser().getLogin() : "";
            jobCandidatesDl.setParameter("recrutierName", currentLogin);
        } else {
            jobCandidatesDl.removeParameter("createdBy");
            jobCandidatesDl.removeParameter("recrutier");
            jobCandidatesDl.removeParameter("recrutierName");
        }
        jobCandidatesDl.load();
    }

    @Subscribe("createCandidateBtn")
    public void onCreateCandidateBtnClick(Button.ClickEvent event) {
        screenBuilders.editor(candidatesTable)
                .newEntity()
                .withOpenMode(OpenMode.DIALOG)
                .show();
    }

    @Subscribe("editCandidateToolbarBtn")
    public void onEditCandidateToolbarBtnClick(Button.ClickEvent event) {
        onEditCandidateBtnClick(null);
    }

    @Subscribe("removeCandidateToolbarBtn")
    public void onRemoveCandidateToolbarBtnClick(Button.ClickEvent event) {
        JobCandidate selected = candidatesTable.getSingleSelected();
        if (selected != null) {
            dialogs.createOptionDialog()
                    .withCaption("Подтверждение удаления")
                    .withMessage("Вы действительно хотите удалить кандидата " + (selected.getFullName() != null ? selected.getFullName() : "") + "?")
                    .withActions(
                            new DialogAction(DialogAction.Type.YES).withHandler(e -> {
                                dataManager.remove(selected);
                                jobCandidatesDl.load();
                                notifications.create(Notifications.NotificationType.TRAY)
                                        .withCaption("Кандидат удален")
                                        .show();
                            }),
                            new DialogAction(DialogAction.Type.NO)
                    )
                    .show();
        }
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

    @Subscribe("actionsWithCandidateButton.findSuitableAction")
    public void onActionsWithCandidateButtonFindSuitableAction(Action.ActionPerformedEvent event) {
        JobCandidate selected = candidatesTable.getSingleSelected();
        if (selected == null) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Выберите кандидата")
                    .withDescription("Для подбора подходящих вакансий выберите кандидата из таблицы")
                    .show();
            return;
        }
        FindSuitable findSuitable = screens.create(FindSuitable.class);
        findSuitable.setJobCandidate(selected);
        findSuitable.show();
    }

    @Subscribe("actionsWithCandidateButton.scanSkillsAction")
    public void onScanSkillsAction(Action.ActionPerformedEvent event) {
        scanCandidateSkills();
    }

    /**
     * AI-анализ навыков выбранного кандидата: берёт текст последнего резюме из БД,
     * вызывает SkillAnalysisService (основные/второстепенные/третьестепенные навыки),
     * сохраняет новые CandidateSkill и обновляет сайдбар навыков и колонку «Ключевые навыки».
     */
    private void scanCandidateSkills() {
        JobCandidate candidate = candidatesTable.getSingleSelected();
        if (candidate == null) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("ВНИМАНИЕ!")
                    .withDescription("Кандидат не выбран. Для анализа навыков выберите кандидата.")
                    .show();
            return;
        }

        String rawText = loadLastCvText(candidate.getId());
        if (rawText == null || rawText.trim().isEmpty()) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("ВНИМАНИЕ!")
                    .withDescription("Текст резюме пуст. Для анализа навыков необходимо заполнить текст резюме.")
                    .show();
            return;
        }

        String inputText = Jsoup.parse(rawText).text();
        if (inputText.trim().isEmpty()) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("ВНИМАНИЕ!")
                    .withDescription("Текст резюме после очистки HTML-разметки пуст.")
                    .show();
            return;
        }

        AiOperationNotifier.showStarted(notifications, "Запущен AI-анализ навыков резюме…", null);

        final JobCandidate candidateForScan = candidate;
        final String textForScan = inputText;
        final Screen progressDialog = AiOperationNotifier.showProgress(this, "Анализ навыков резюме…");

        BackgroundTask<Integer, SkillScanOutcome> task =
                new BackgroundTask<Integer, SkillScanOutcome>(240, this) {
                    @Override
                    public SkillScanOutcome run(TaskLifeCycle<Integer> taskLifeCycle) {
                        List<CandidateSkill> existingSkills = dataManager.load(CandidateSkill.class)
                                .query("select e from hunttech_CandidateSkill e where e.candidate.id = :candidateId")
                                .parameter("candidateId", candidateForScan.getId())
                                .view("candidateSkill-view")
                                .list();

                        Set<UUID> existingSkillIds = new HashSet<>();
                        for (CandidateSkill cs : existingSkills) {
                            if (cs.getSkill() != null) {
                                existingSkillIds.add(cs.getSkill().getId());
                            }
                        }

                        SkillAnalysisResult mainResult = skillAnalysisService.analyzeMain(textForScan);
                        SkillAnalysisResult secondaryResult = skillAnalysisService.analyzeSecondary(textForScan);
                        SkillAnalysisResult tertiaryResult = skillAnalysisService.analyzeTertiary(textForScan);

                        List<SkillTree> mainSkills = mainResult.getSkills();
                        List<SkillTree> secondarySkills = secondaryResult.getSkills();
                        List<SkillTree> tertiarySkills = tertiaryResult.getSkills();
                        SkillAnalysisResult allResult = null;

                        if (mainSkills == null) mainSkills = Collections.emptyList();
                        if (secondarySkills == null) secondarySkills = Collections.emptyList();
                        if (tertiarySkills == null) tertiarySkills = Collections.emptyList();

                        if (mainSkills.isEmpty() && secondarySkills.isEmpty() && tertiarySkills.isEmpty()) {
                            allResult = skillAnalysisService.analyzeAll(textForScan);
                            mainSkills = allResult.getSkills();
                            if (mainSkills == null) mainSkills = Collections.emptyList();
                        }

                        SkillAnalysisResult aiSourceResult = firstNonNull(mainResult, secondaryResult, tertiaryResult, allResult);
                        AiExecutionResult aiExecution = aiSourceResult == null ? null : aiSourceResult.getAiExecution();

                        List<CandidateSkill> toSave = new ArrayList<>();
                        Set<UUID> processedSkillIds = new HashSet<>(existingSkillIds);

                        for (SkillTree st : mainSkills) {
                            if (st != null && processedSkillIds.add(st.getId())) {
                                CandidateSkill cs = metadata.create(CandidateSkill.class);
                                cs.setCandidate(candidateForScan);
                                cs.setSkill(st);
                                cs.setPriority(CandidateSkillPriority.MAIN);
                                toSave.add(cs);
                            }
                        }

                        for (SkillTree st : secondarySkills) {
                            if (st != null && processedSkillIds.add(st.getId())) {
                                CandidateSkill cs = metadata.create(CandidateSkill.class);
                                cs.setCandidate(candidateForScan);
                                cs.setSkill(st);
                                cs.setPriority(CandidateSkillPriority.SECONDARY);
                                toSave.add(cs);
                            }
                        }

                        for (SkillTree st : tertiarySkills) {
                            if (st != null && processedSkillIds.add(st.getId())) {
                                CandidateSkill cs = metadata.create(CandidateSkill.class);
                                cs.setCandidate(candidateForScan);
                                cs.setSkill(st);
                                cs.setPriority(CandidateSkillPriority.TERTIARY);
                                toSave.add(cs);
                            }
                        }

                        int mainDetected = mainSkills.size();
                        int secondaryDetected = secondarySkills.size();
                        int tertiaryDetected = tertiarySkills.size();
                        int totalDetected = mainDetected + secondaryDetected + tertiaryDetected;
                        int savedCount = toSave.size();
                        int existingOrDuplicate = totalDetected - savedCount;

                        if (!toSave.isEmpty()) {
                            CommitContext commitContext = new CommitContext(toSave);
                            dataManager.commit(commitContext);
                        }

                        String statsDescription = String.format(
                                "Всего обнаружено навыков: <b>%d</b><br/>" +
                                "• Основных: <b>%d</b><br/>" +
                                "• Второстепенных: <b>%d</b><br/>" +
                                "• Третьестепенных: <b>%d</b><br/>" +
                                "──────────────────────<br/>" +
                                "✅ Сохранено новых: <b>%d</b>%s",
                                totalDetected,
                                mainDetected,
                                secondaryDetected,
                                tertiaryDetected,
                                savedCount,
                                (existingOrDuplicate > 0 ? "<br/>ℹ️ Уже присутствуют у кандидата: <b>" + existingOrDuplicate + "</b>" : "")
                        );

                        return new SkillScanOutcome(statsDescription, aiExecution);
                    }

                    @Override
                    public void done(SkillScanOutcome outcome) {
                        AiOperationNotifier.closeProgress(progressDialog);

                        String statsDescription = outcome.statsDescription;
                        AiExecutionResult aiExecution = outcome.aiExecution;
                        if (aiExecution != null) {
                            statsDescription = AiOperationNotifier.buildDescription(aiExecution, statsDescription);
                        }

                        notifications.create(Notifications.NotificationType.TRAY)
                                .withCaption("Статистика анализа навыков")
                                .withDescription(statsDescription)
                                .withContentMode(ContentMode.HTML)
                                .withHideDelayMs(5000)
                                .show();

                        jobCandidatesDl.load();
                        JobCandidate selected = candidatesTable.getSingleSelected();
                        if (selected != null && candidateForScan.getId() != null
                                && candidateForScan.getId().equals(selected.getId())) {
                            updateCandidateSkillsSidebar(selected);
                        }
                    }

                    @Override
                    public boolean handleException(Exception ex) {
                        AiOperationNotifier.closeProgress(progressDialog);
                        notifications.create(Notifications.NotificationType.ERROR)
                                .withCaption("Ошибка анализа навыков")
                                .withDescription("Не удалось выполнить анализ навыков: " + ex.getMessage())
                                .show();
                        return true;
                    }

                    @Override
                    public boolean handleTimeoutException() {
                        AiOperationNotifier.closeProgress(progressDialog);
                        notifications.create(Notifications.NotificationType.ERROR)
                                .withCaption("Ошибка анализа навыков")
                                .withDescription("Анализ навыков превысил допустимое время выполнения.")
                                .show();
                        return true;
                    }
                };
        backgroundWorker.handle(task).execute();
    }

    private String loadLastCvText(UUID candidateId) {
            if (candidateId == null) {
                return null;
            }
            return dataManager.loadValue(
                    "select e.textCV from hunttech_CandidateCV e " +
                            "where e.candidate.id = :candidateId " +
                            "order by e.datePost desc",
                    String.class)
                    .parameter("candidateId", candidateId)
                    .maxResults(1)
                    .optional()
                    .orElse(null);
        }

        /**
         * Обновляет блок «Готовность и рейтинг» в sidebar:
         * - индикаторы готовности (✓ Резюме / ✓ Активность / ✓ В резерве)
         * - звёзды рейтинга крупным шрифтом (20px) + цифра (14px)
         * - счётчик «Отправлено кандидатов» с раскраской (<3 зелёный, <5 жёлтый, >=5 красный)
         */
        private void updateReadinessRatingAndSent(JobCandidate candidate) {
            if (detailReadiness == null && detailRating == null && detailSentCandidates == null) {
                return;
            }
            try {
                // Используем кэши вместо N+1 запросов
                // 1. Индикаторы готовности
                if (detailReadiness != null) {
                    StringBuilder ind = new StringBuilder("<div style='display: flex; gap: 8px; font-size: 13px; flex-wrap: wrap;'>");
                
                    // ✓ Резюме (из кэша candidatesWithCv)
                    boolean hasCv = candidatesWithCv.contains(candidate.getId());
                    ind.append(hasCv
                            ? "<span style='color: #16a34a;'>✓ Резюме</span>"
                            : "<span style='color: #9ca3af;'>✕ Резюме</span>");
                
                    // ✓ Активность (из кэша lastInteractionDateByCandidateId)
                    boolean hasRecentInteraction = false;
                    Date lastDate = lastInteractionDateByCandidateId.get(candidate.getId());
                    if (lastDate != null) {
                        java.util.Calendar threshold = java.util.Calendar.getInstance();
                        threshold.setTime(lastDate);
                        threshold.add(java.util.Calendar.MONTH, 1);
                        if (java.util.Calendar.getInstance().before(threshold)) {
                            hasRecentInteraction = true;
                        }
                    }
                    ind.append(hasRecentInteraction
                            ? "<span style='color: #16a34a;'>✓ Активность</span>"
                            : "<span style='color: #9ca3af;'>✕ Активность</span>");
                
                    // ✓ В резерве (Employee с workStatus.inStaff=true из пакетного кэша)
                    boolean inReserve = false;
                    Employee emp = employeeByCandidateId.get(candidate.getId());
                    if (emp != null && emp.getWorkStatus() != null
                            && Boolean.TRUE.equals(emp.getWorkStatus().getInStaff())) {
                        inReserve = true;
                    }
                    ind.append(inReserve
                            ? "<span style='color: #16a34a;'>✓ В резерве</span>"
                            : "<span style='color: #9ca3af;'>✕ В резерве</span>");
                
                    ind.append("</div>");
                    detailReadiness.setValue(ind.toString());
                }

                // 2. Рейтинг: звёзды 20px + цифра 14px
                if (detailRating != null && starsAndOtherService != null) {
                    Integer rating = null;
                    // Используем кэш последнего взаимодействия вместо N+1
                    IteractionList lastInter = lastInteractionByCandidateId.get(candidate.getId());
                    if (lastInter != null && lastInter.getRating() != null && lastInter.getRating() > 0) {
                        rating = lastInter.getRating();
                    }
                    if (rating != null) {
                        String stars = starsAndOtherService.setStars(rating);
                        detailRating.setValue(String.format(
                                "<div style='display: flex; align-items: center; gap: 4px; font-size: 14px;'>" +
                                        "<span style='color: #f59e0b; font-size: 20px;'>%s</span>" +
                                        "<span style='font-weight: 700; color: #1f2937;'>%d</span>" +
                                        "</div>", stars, rating));
                    } else {
                        detailRating.setValue("<span style='color: #9ca3af; font-size: 14px;'>Не оценен</span>");
                    }
                }

                // 3. Счётчик отправленных на сторону заказчика с раскраской
                if (detailSentCandidates != null) {
                    Integer sentCount = sentCvCountByCandidateId.getOrDefault(candidate.getId(), 0);
                    String color, bg;
                    if (sentCount < 3) {
                        color = "#16a34a";
                        bg = "rgba(34, 197, 94, 0.12)";
                    } else if (sentCount < 5) {
                        color = "#ca8a04";
                        bg = "rgba(250, 204, 21, 0.15)";
                    } else {
                        color = "#dc2626";
                        bg = "rgba(239, 68, 68, 0.12)";
                    }
                    detailSentCandidates.setValue(String.format(
                            "<div style='display: flex; align-items: center; gap: 6px; font-size: 13px;'>" +
                                    "<span>Отправлено кандидатов:</span>" +
                                    "<span style='background: %s; color: %s; padding: 2px 10px; border-radius: 10px; font-weight: 700; font-size: 12px;'>%d</span>" +
                                    "</div>", bg, color, sentCount));
                }
            } catch (Exception ex) {
                log.warn("Failed to update readiness/rating/sent for candidate {}", candidate != null ? candidate.getId() : null, ex);
                if (detailReadiness != null) detailReadiness.setValue("Ошибка загрузки");
                if (detailRating != null) detailRating.setValue("Ошибка загрузки");
                if (detailSentCandidates != null) detailSentCandidates.setValue("Ошибка загрузки");
            }
        }

        private static class SkillScanOutcome {
        final String statsDescription;
        final AiExecutionResult aiExecution;

        SkillScanOutcome(String statsDescription, AiExecutionResult aiExecution) {
            this.statsDescription = statsDescription;
            this.aiExecution = aiExecution;
        }
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        if (values == null) return null;
        for (T v : values) {
            if (v != null) return v;
        }
        return null;
    }
}
