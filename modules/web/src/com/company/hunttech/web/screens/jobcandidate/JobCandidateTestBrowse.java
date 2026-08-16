package com.company.hunttech.web.screens.jobcandidate;

import com.company.hunttech.entity.CandidateCV;
import com.company.hunttech.entity.IteractionList;
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

import com.company.hunttech.entity.Iteraction;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Контроллер базового тестового экрана просмотра кандидатов «Split-View Halo».
 * <p>
 * Реализует просмотр кандидатов с левым сайдбаром в теме CUBA Halo:
 * <ul>
 *   <li>Центрированные ФИО (+30% шрифт), плашка должности и город проживания.</li>
 *   <li>Динамический поиск и отображение зарплатных ожиданий кандидата из связанных сущностей IteractionList.</li>
 *   <li>Стилизованные разделы сайдбара с линиями над и под заголовком (по аналогии с Edit-формами).</li>
 * </ul>
 *
 * @see JobCandidate
 * @see IteractionList
 */
@UiController("hunttech_JobCandidateTest.browse")
@UiDescriptor("job-candidate-test-browse.xml")
@LookupComponent("candidatesTable")
@LoadDataBeforeShow
public class JobCandidateTestBrowse extends StandardLookup<JobCandidate> {

    /* ==================================================================     * Инъекции компонентов UI и сервисов
     * ========================================================================= */

    /** Таблица реестра кандидатов */
    @Inject
    private GroupTable<JobCandidate> candidatesTable;

    /** Загрузчик данных реестра */
    @Inject
    private CollectionLoader<JobCandidate> jobCandidatesDl;

    /** Построитель диалоговых экранов */
    @Inject
    private ScreenBuilders screenBuilders;

    /** Фабрика UI-компонентов */
    @Inject
    private UiComponents uiComponents;
    @Inject
    private FileLoader fileLoader;

    /** Менеджер данных CUBA Platform для прямого поиска взаимодействий */
    @Inject
    private com.haulmont.cuba.core.global.DataManager dataManager;

    /** Текущая пользовательская сессия */
    @Inject
    private UserSession userSession;

    /** Форматтер даты */
    private final java.text.SimpleDateFormat interactionDateFormat = new java.text.SimpleDateFormat("dd.MM.yyyy");

    public enum InteractionStatus {
        FREE("🟢 Свободен (> 1 мес)", "#27ae60"),
        MY_CANDIDATE("🟡 В вашей работе (< 1 мес)", "#f39c12"),
        OTHER_RECRUITER("🔴 В работе у другого рекрутера", "#e74c3c");

        private final String label;
        private final String color;

        InteractionStatus(String label, String color) {
            this.label = label;
            this.color = color;
        }

        public String getLabel() {
            return label;
        }

        public String getColor() {
            return color;
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

    private final java.util.Map<String, String> userFullNameCache = new java.util.concurrent.ConcurrentHashMap<>();

    private String resolveUserFullName(String login) {
        if (login == null || login.trim().isEmpty()) {
            return "—";
        }
        return userFullNameCache.computeIfAbsent(login, l -> {
            try {
                com.haulmont.cuba.security.entity.User u = dataManager.load(com.haulmont.cuba.security.entity.User.class)
                        .query("select u from sec$User u where u.login = :login")
                        .parameter("login", l)
                        .view(com.haulmont.cuba.core.global.View.MINIMAL)
                        .optional()
                        .orElse(null);
                if (u != null && u.getName() != null && !u.getName().trim().isEmpty()) {
                    return u.getName().trim();
                }
            } catch (Exception ignored) {
            }
            return l;
        });
    }

    public static class CandidateRoleTeam {
        public String authorName;
        public String researcherName;
        public String recruiterName;
        public String coordinatorName;
        public boolean isFree;
        public Date lastInteractionDate;
        public int totalInteractions;
        public InteractionStatus status;
    }

    private CandidateRoleTeam calculateCandidateTeam(JobCandidate candidate) {
        CandidateRoleTeam team = new CandidateRoleTeam();
        if (candidate == null) {
            team.isFree = true;
            team.status = InteractionStatus.FREE;
            return team;
        }
        team.authorName = resolveUserFullName(candidate.getCreatedBy());
        List<IteractionList> list = candidate.getIteractionList();
        if (list == null || list.isEmpty()) {
            team.isFree = true;
            team.totalInteractions = 0;
            team.status = InteractionStatus.FREE;
            return team;
        }
        team.totalInteractions = list.size();
        List<IteractionList> sortedList = new ArrayList<>(list);
        sortedList.sort(Comparator.comparing(IteractionList::getDateIteraction, Comparator.nullsLast(Comparator.naturalOrder())));
        IteractionList last = sortedList.get(sortedList.size() - 1);
        team.lastInteractionDate = last.getDateIteraction();
        team.status = calculateInteractionStatus(candidate);
        team.isFree = (team.status == InteractionStatus.FREE);

        for (IteractionList il : sortedList) {
            Iteraction type = il.getIteractionType();
            String personName = null;
            if (il.getRecrutier() != null && il.getRecrutier().getName() != null && !il.getRecrutier().getName().trim().isEmpty()) {
                personName = il.getRecrutier().getName().trim();
            } else if (il.getCreatedBy() != null) {
                personName = resolveUserFullName(il.getCreatedBy());
            }
            if (personName == null) continue;
            if (type != null) {
                if (Boolean.TRUE.equals(type.getSignSendToClient())
                        || Boolean.TRUE.equals(type.getSignClientInterview())
                        || Boolean.TRUE.equals(type.getSignStartProject())) {
                    team.coordinatorName = personName;
                }
                if (Boolean.TRUE.equals(type.getSignOurInterview())
                        || Boolean.TRUE.equals(type.getSignFeedback())
                        || il.getRating() != null) {
                    team.recruiterName = personName;
                }
                if (Boolean.TRUE.equals(type.getSignOurInterviewAssigned())
                        || Boolean.TRUE.equals(type.getSignStartCase())) {
                    if (team.researcherName == null) {
                        team.researcherName = personName;
                    }
                }
            }
        }
        if (team.researcherName == null && !sortedList.isEmpty()) {
            IteractionList first = sortedList.get(0);
            if (first.getRecrutier() != null && first.getRecrutier().getName() != null) {
                team.researcherName = first.getRecrutier().getName().trim();
            } else if (first.getCreatedBy() != null) {
                team.researcherName = resolveUserFullName(first.getCreatedBy());
            }
        }
        if (team.researcherName == null) {
            team.researcherName = team.authorName;
        }
        return team;
    }

    /* ==================================================================     * Поля левого профильного сайдбара
     * ========================================================================= */

    /** Овальный фото-аватар кандидата */
    @Inject
    private WebOvaFallbackImage detailPic;

    /** Заголовок с ФИО выбранного кандидата */
    @Inject
    private Label<String> detailFullName;

    /** Подзаголовок с наименованием должности */
    @Inject
    private Label<String> detailPosition;

    /** Метка города проживания */
    @Inject
    private Label<String> detailCity;

    /** Номер телефона */
    @Inject
    private Label<String> detailPhone;

    /** Адрес электронной почты */
    @Inject
    private Label<String> detailEmail;

    /** Имя в Telegram */
    @Inject
    private Label<String> detailTelegram;

    /** Наименование текущей компании */
    @Inject
    private Label<String> detailCompany;

    /** Заголовок поля зарплатных ожиданий */
    @Inject
    private Label<String> detailSalaryCaption;

    /** Значение зарплатных ожиданий кандидата */
    @Inject
    private Label<String> detailSalary;

    /** Сводка по истории взаимодействий */
    @Inject
    private Label<String> detailInteractionsInfo;

    /** Метка цветных бейджей навыков кандидата */
    @Inject
    private Label<String> detailSkillsLabels;

    /** Кнопка открытия формы редактирования */
    @Inject
    private Button editCandidateBtn;

    /** Кнопка быстрого создания взаимодействия */
    @Inject
    private Button createInteractionBtn;

    /** Поле быстрого поиска кандидатов */
    @Inject
    private TextField<String> searchField;

    /* ==================================================================     * Бизнес-логика извлечения зарплатных ожиданий
     * ========================================================================= */

    /**
     * Извлекает зарплатные ожидания кандидата из сущности IteractionList.
     *
     * @param candidate кандидат
     * @return строка с суммой ожиданий либо null
     */
    private String getSalaryExpectations(JobCandidate candidate) {
        if (candidate == null) return null;
        
        // 1. Поиск во встроенной коллекции
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
        
        // 2. Резервный запрос в БД через DataManager
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

    /* ==================================================================     * Инициализация и обработчики событий
     * ========================================================================= */

    /**
     * Инициализация экрана: генератор аватара в первой колонке.
     */
    @Subscribe
    public void onInit(Screen.InitEvent event) {
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

    /**
     * Обработчик выбора строки в таблице: заполняет сайдбар.
     */
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

    /**
     * Сброс сайдбара в пустое состояние.
     */
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
        if (detailSkillsLabels != null) {
            detailSkillsLabels.setValue("<span style='color: #7f8c8d; font-size: 11px;'>Выберите кандидата для просмотра навыков</span>");
        }
        detailPic.setSource(ThemeResource.class).setPath("icons/no-programmer.jpeg");
        editCandidateBtn.setEnabled(false);
        createInteractionBtn.setEnabled(false);
    }

    /**
     * Заполнение сайдбара данными выбранного кандидата:
     * центрированная шапка (+30% шрифт), контакты, зарплата и счетчик взаимодействий.
     *
     * @param candidate выбранный кандидат
     */
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

        // Фото: из карточки кандидата, при отсутствии — из последнего резюме (CandidateCV);
        // если файла нет в хранилище — автоматический fallback без битой картинки
        FileDescriptorImageHelper.setCandidateFace(detailPic, fileLoader, resolveCandidateFace(candidate));

        // Светофорная карточка статуса взаимодействия и участников процесса
        CandidateRoleTeam team = calculateCandidateTeam(candidate);
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='background: #f8f9fa; padding: 10px 14px; border-radius: 6px; border-left: 4px solid ")
                .append(team.status.getColor()).append("; margin-top: 6px; font-size: 12px; line-height: 1.6;'>");

        sb.append("<div><b>Статус:</b> <span style='color: ").append(team.status.getColor())
                .append("; font-weight: bold;'>").append(team.status.getLabel()).append("</span></div>");

        if (team.isFree) {
            sb.append("<div style='margin-top: 5px;'>👤 <b>Автор карточки:</b> ")
                    .append(team.authorName != null ? team.authorName : "—").append("</div>");
            if (team.lastInteractionDate != null) {
                sb.append("<div style='color: #7f8c8d; font-size: 11px;'>Посл. активность: ")
                        .append(interactionDateFormat.format(team.lastInteractionDate)).append("</div>");
            }
        } else {
            sb.append("<div style='margin-top: 6px; border-top: 1px dashed #cbd5e1; padding-top: 5px; display: flex; flex-direction: column; gap: 3px;'>");
            if (team.researcherName != null) {
                sb.append("<div>🔍 <b>Ресерчер:</b> ").append(team.researcherName).append("</div>");
            }
            if (team.recruiterName != null) {
                sb.append("<div>👔 <b>Рекрутер:</b> ").append(team.recruiterName).append("</div>");
            }
            if (team.coordinatorName != null) {
                sb.append("<div>🎯 <b>Координатор:</b> ").append(team.coordinatorName).append("</div>");
            }
            if (team.lastInteractionDate != null) {
                sb.append("<div style='color: #7f8c8d; font-size: 11px; margin-top: 2px;'>Посл. активность: ")
                        .append(interactionDateFormat.format(team.lastInteractionDate)).append("</div>");
            }
            sb.append("</div>");
        }

        sb.append("<div style='color: #94a3b8; font-size: 10.5px; margin-top: 4px;'>Всего взаимодействий: ")
                .append(team.totalInteractions).append("</div>");
        sb.append("</div>");

        detailInteractionsInfo.setHtmlEnabled(true);
        detailInteractionsInfo.setValue(sb.toString());

        // Заполнение блока основных навыков кандидата
        updateCandidateSkillsSidebar(candidate);

        editCandidateBtn.setEnabled(true);
        createInteractionBtn.setEnabled(true);
    }

    /**
     * Заполняет сайдбар цветными бейджами основных навыков кандидата, распознанными AI.
     */
    private void updateCandidateSkillsSidebar(JobCandidate candidate) {
        if (detailSkillsLabels == null) {
            return;
        }
        if (candidate == null) {
            detailSkillsLabels.setValue("<span style='color: #7f8c8d; font-size: 11px;'>Навыки не определены</span>");
            return;
        }

        try {
            List<com.company.hunttech.entity.CandidateSkill> skills = dataManager.load(com.company.hunttech.entity.CandidateSkill.class)
                    .query("select e from hunttech_CandidateSkill e where e.candidate = :candidate order by e.priority, e.skill.skillName")
                    .parameter("candidate", candidate)
                    .view("candidateSkill-view")
                    .list();

            if (skills.isEmpty()) {
                detailSkillsLabels.setValue("<span style='color: #7f8c8d; font-size: 11px;'>Навыки не определены</span>");
                return;
            }

            StringBuilder sb = new StringBuilder("<div style='display: flex; flex-wrap: wrap; gap: 4px; padding: 2px 0;'>");
            String[] palette = new String[]{
                    "#2b82c9", "#27ae60", "#8e44ad", "#d35400", "#16a085", "#2c3e50", "#e67e22", "#2980b9"
            };
            for (com.company.hunttech.entity.CandidateSkill cs : skills) {
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
