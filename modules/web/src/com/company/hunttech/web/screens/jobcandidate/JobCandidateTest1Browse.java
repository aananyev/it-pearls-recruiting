package com.company.hunttech.web.screens.jobcandidate;

import com.company.hunttech.entity.CandidateCV;
import com.company.hunttech.entity.CandidateSkill;
import com.company.hunttech.entity.CandidateSkillPriority;
import com.company.hunttech.entity.ExtUser;
import com.company.hunttech.entity.Iteraction;
import com.company.hunttech.entity.IteractionList;
import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.entity.SkillTree;
import com.company.hunttech.service.SkillAnalysisService;
import com.company.hunttech.service.SkillAnalysisResult;
import com.company.hunttech.service.AiExecutionResult;
import com.company.hunttech.web.util.AiOperationNotifier;
import com.company.hunttech.web.util.FileDescriptorImageHelper;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.Action;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.ContentMode;
import com.haulmont.cuba.gui.components.GroupTable;
import com.haulmont.cuba.gui.components.Image;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.PopupButton;
import com.haulmont.cuba.gui.components.PopupView;
import com.haulmont.cuba.gui.components.Table;
import com.haulmont.cuba.gui.components.ThemeResource;
import com.haulmont.cuba.gui.components.VBoxLayout;
import com.haulmont.cuba.gui.executors.BackgroundTask;
import com.haulmont.cuba.gui.executors.BackgroundWorker;
import com.haulmont.cuba.gui.executors.TaskLifeCycle;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.company.hunttech.web.screens.candidatecv.CandidateCVSimpleBrowse;
import com.company.hunttech.web.screens.iteractionlist.iteractionlistbrowse.IteractionListSimpleBrowse;
import com.haulmont.cuba.gui.Screens;
import com.haulmont.cuba.gui.screen.OpenMode;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.StandardLookup;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.Target;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.security.global.UserSession;
import com.hunttech.hrm.web.components.WebOvaFallbackImage;
import org.jsoup.Jsoup;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Контроллер тестового экрана просмотра кандидатов «Split-View (Master-Detail)» (Эскиз 1).
 * <p>
 * Реализует двухпанельную архитектуру рабочего пространства рекрутера:
 * <ul>
 *   <li><b>Верхний тулбар:</b> заголовок экрана, стандартный Generic Filter CUBA Platform, кнопки создания и обновления.</li>
 *   <li><b>Левый профильный сайдбар (312px):</b> аватар, центрированные ФИО/должность/город (+30% шрифт),
 *       кнопки быстрых действий, реквизиты с динамическими зарплатными ожиданиями и лента статуса активности.</li>
 *   <li><b>Правая табличная область:</b> панель быстрых фильтров статусов рекрутера, выпадающая кнопка действий
 *       (PopupButton) и таблица GroupTable с нативной сортировкой CUBA Platform по клику на заголовках колонок.</li>
 * </ul>
 *
 * @see JobCandidate
 * @see IteractionList
 */
@UiController("hunttech_JobCandidateTest1.browse")
@UiDescriptor("job-candidate-test1-browse.xml")
@LookupComponent("candidatesTable")
@LoadDataBeforeShow
public class JobCandidateTest1Browse extends StandardLookup<JobCandidate> {

    /* =========================================================================
     * Инъекции компонентов UI и инфраструктурных сервисов CUBA Platform
     * ========================================================================= */

    /** Таблица реестра кандидатов с поддержкой группировки и сортировки */
    @Inject
    private GroupTable<JobCandidate> candidatesTable;

    /** Загрузчик коллекции кандидатов из базы данных */
    @Inject
    private CollectionLoader<JobCandidate> jobCandidatesDl;

    /** Построитель экранов для вызова диалоговых окон редактирования */
    @Inject
    private ScreenBuilders screenBuilders;

    /** Фабрика UI-компонентов CUBA Platform */
    @Inject
    private UiComponents uiComponents;
    @Inject
    private FileLoader fileLoader;

    /** Сервис метаданных для создания новых сущностей */
    @Inject
    private Metadata metadata;

    /** Сервис отображения пользовательских всплывающих уведомлений */
    @Inject
    private Notifications notifications;

    /** Сервис DataManager для выполнения прямых JPQL-запросов */
    @Inject
    private com.haulmont.cuba.core.global.DataManager dataManager;

    /** Сервис анализа навыков через AI */
    @Inject
    private SkillAnalysisService skillAnalysisService;

    /** Исполнитель фоновых задач CUBA */
    @Inject
    private BackgroundWorker backgroundWorker;

    /** Текущая сессия пользователя для проверки прав и авторства взаимодействий */
    @Inject
    private com.haulmont.cuba.security.global.UserSession userSession;

    /** Менеджер экранов CUBA для создания и отображения связанных форм */
    @Inject
    private Screens screens;

    /* =========================================================================
     * Элементы левого профильного сайдбара (Detail Pane)
     * ========================================================================= */

    /** Овальный фото-аватар кандидата с автоматическим фолбеком */
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

    /** Метка контактного номера телефона */
    @Inject
    private Label<String> detailPhone;

    /** Метка адреса электронной почты */
    @Inject
    private Label<String> detailEmail;

    /** Метка профиля в Telegram */
    @Inject
    private Label<String> detailTelegram;

    /** Метка текущей компании работодателя */
    @Inject
    private Label<String> detailCompany;

    /** Заголовок поля зарплатных ожиданий в таблице реквизитов */
    @Inject
    private Label<String> detailSalaryCaption;

    /** Значение зарплатных ожиданий кандидата */
    @Inject
    private Label<String> detailSalary;

    /** Блок отображения статуса последнего рекрутерского взаимодействия */
    @Inject
    private Label<String> detailInteractionsInfo;

    /** Метка цветных бейджей навыков кандидата */
    @Inject
    private Label<String> detailSkillsLabels;

    /** Кнопка открытия полной карточки редактирования кандидата */
    @Inject
    private Button editCandidateBtn;

    /** Кнопка создания нового взаимодействия с кандидатом */
    @Inject
    private Button createInteractionBtn;

    /* =========================================================================
     * Элементы панели фильтрации и действий над реестром
     * ========================================================================= */

    /** Кнопка фильтра: Все кандидаты */
    @Inject
    private Button filterAllBtn;

    /** Кнопка фильтра: Мои кандидаты */
    @Inject
    private Button filterMyCandidatesBtn;

    /** Кнопка фильтра: С моим участием */
    @Inject
    private Button filterMyParticipationBtn;

    /** Выпадающая кнопка действий над выбранным кандидатом */
    @Inject
    private PopupButton actionsWithCandidateButton;

    /** Форматтер даты для компактного вывода даты взаимодействий */
    private final java.text.SimpleDateFormat interactionDateFormat = new java.text.SimpleDateFormat("dd.MM.yyyy");

    /* =========================================================================
     * Перечисление статусов взаимодействия (светофорная бизнес-логика)
     * ========================================================================= */

    /**
     * Статусы закрепления кандидата за рекрутерами по правилу 1 календарного месяца.
     */
    public enum InteractionStatus {
        /** 🟢 Кандидат свободен: взаимодействий не было либо прошло более 1 месяца */
        FREE("🟢 Свободен (> 1 мес)", "#27ae60", "rgba(39, 174, 96, 0.15)"),

        /** 🟡 Кандидат в работе у текущего рекрутера: взаимодействие менее 1 месяца назад */
        MY_CANDIDATE("🟡 В вашей работе (< 1 мес)", "#f39c12", "rgba(243, 156, 18, 0.15)"),

        /** 🔴 Кандидат в работе у другого рекрутера: взаимодействие менее 1 месяца назад */
        OTHER_RECRUITER("🔴 В работе у другого (< 1 мес)", "#e74c3c", "rgba(231, 76, 60, 0.15)");

        private final String label;
        private final String color;
        private final String bgColor;

        InteractionStatus(String label, String color, String bgColor) {
            this.label = label;
            this.color = color;
            this.bgColor = bgColor;
        }

        public String getLabel() { return label; }
        public String getColor() { return color; }
        public String getBgColor() { return bgColor; }
    }

    /* =========================================================================
     * Вспомогательные методы бизнес-логики
     * ========================================================================= */

    /**
     * Извлекает зарплатные ожидания кандидата из связанной сущности IteractionList.
     * Поиск осуществляется по типу взаимодействия, содержащему «Зарплатные ожидания».
     *
     * @param candidate кандидат для анализа
     * @return строка с зарплатными ожиданиями либо null при их отсутствии
     */
    private String getSalaryExpectations(JobCandidate candidate) {
        if (candidate == null) return null;
        
        // 1. Попытка поиска во встроенной коллекции кандидата
        if (candidate.getIteractionList() != null) {
            for (IteractionList it : candidate.getIteractionList()) {
                if (it.getIteractionType() != null &&
                    it.getIteractionType().getIterationName() != null &&
                    it.getIteractionType().getIterationName().toLowerCase().contains("зарплатные ожидания")) {
                    if (it.getAddString() != null && !it.getAddString().trim().isEmpty()) {
                        return it.getAddString().trim();
                    }
                }
            }
        }
        
        // 2. Резервный запрос в БД через DataManager при ненагруженном view
        try {
            java.util.List<IteractionList> list = dataManager.load(IteractionList.class)
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

    /**
     * Находит самое последнее по дате взаимодействие по кандидату.
     *
     * @param candidate кандидат
     * @return последний объект IteractionList либо null
     */
    private IteractionList getLastInteraction(JobCandidate candidate) {
        if (candidate == null || candidate.getIteractionList() == null || candidate.getIteractionList().isEmpty()) {
            return null;
        }
        IteractionList last = null;
        for (IteractionList item : candidate.getIteractionList()) {
            if (last == null) {
                last = item;
            } else {
                java.util.Date d1 = item.getDateIteraction() != null ? item.getDateIteraction() : item.getCreateTs();
                java.util.Date d2 = last.getDateIteraction() != null ? last.getDateIteraction() : last.getCreateTs();
                if (d1 != null && (d2 == null || d1.after(d2))) {
                    last = item;
                }
            }
        }
        return last;
    }

    /**
     * Вычисляет статус взаимодействия кандидата по правилу 1 календарного месяца.
     *
     * @param candidate кандидат
     * @return вычисленный статус InteractionStatus (FREE, MY_CANDIDATE, OTHER_RECRUITER)
     */
    private InteractionStatus calculateInteractionStatus(JobCandidate candidate) {
        if (candidate == null) {
            return InteractionStatus.FREE;
        }
        IteractionList last = getLastInteraction(candidate);
        if (last == null) {
            return InteractionStatus.FREE;
        }

        java.util.Date date = last.getDateIteraction() != null ? last.getDateIteraction() : last.getCreateTs();
        if (date == null) {
            return InteractionStatus.FREE;
        }

        // Вычисляем порог: дата взаимодействия + 1 календарный месяц
        java.util.Calendar threshold = java.util.Calendar.getInstance();
        threshold.setTime(date);
        threshold.add(java.util.Calendar.MONTH, 1);

        java.util.Calendar now = java.util.Calendar.getInstance();

        if (now.after(threshold)) {
            // Прошло более 1 месяца -> кандидат свободен
            return InteractionStatus.FREE;
        } else {
            // Менее 1 месяца -> проверяем закрепленного рекрутера
            if (last.getRecrutier() != null && userSession.getUser() != null
                    && !last.getRecrutier().getId().equals(userSession.getUser().getId())) {
                return InteractionStatus.OTHER_RECRUITER;
            } else {
                return InteractionStatus.MY_CANDIDATE;
            }
        }
    }

    /** Кэш логин пользователя -> ФИО пользователя */
    private final java.util.Map<String, String> userFullNameCache = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Разрешает отображаемое ФИО пользователя по его логину.
     */
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

    private static final String GROUP_RESEARCH = "Ресерчинг";
    private static final String GROUP_RESEARCHER = "Ресерчер";
    private static final String GROUP_TRAINEE = "Стажер";
    private static final String GROUP_MANAGERS = "Менеджеры";
    private static final String GROUP_COORDINATION = "Координация";
    private static final String GROUP_LEADERSHIP = "Руководство";

    /**
     * Модель команды участников работы над кандидатом.
     */
    public static class CandidateRoleTeam {
        public String authorName;
        public java.util.Date createDate;
        public String researcherName;
        public String researcherAction;
        public String recruiterName;
        public String recruiterAction;
        public String coordinatorName;
        public String coordinatorAction;
        public boolean isFree;
        public java.util.Date lastInteractionDate;
        public String lastInteractionName;
        public String lastEmployeeName;
        public int totalInteractions;
        public InteractionStatus status;
    }

    private boolean isUserInGroup(com.company.hunttech.entity.ExtUser user, String... targetGroups) {
        if (user == null || user.getGroup() == null || user.getGroup().getName() == null) {
            return false;
        }
        String groupName = user.getGroup().getName().trim();
        for (String target : targetGroups) {
            if (groupName.equalsIgnoreCase(target) || groupName.toLowerCase().contains(target.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String getEmployeeName(IteractionList il) {
        if (il == null) return null;
        if (il.getRecrutier() != null && il.getRecrutier().getName() != null && !il.getRecrutier().getName().trim().isEmpty()) {
            return il.getRecrutier().getName().trim();
        }
        if (il.getCreatedBy() != null && !il.getCreatedBy().trim().isEmpty()) {
            return resolveUserFullName(il.getCreatedBy());
        }
        return null;
    }

    /**
     * Вычисляет роли участников (Автор, Ресерчер, Рекрутер, Координатор) по истории взаимодействий.
     */
    private CandidateRoleTeam calculateCandidateTeam(JobCandidate candidate) {
        CandidateRoleTeam team = new CandidateRoleTeam();
        if (candidate == null) {
            team.isFree = true;
            team.status = InteractionStatus.FREE;
            return team;
        }

        team.authorName = resolveUserFullName(candidate.getCreatedBy());
        team.createDate = candidate.getCreateTs();

        List<IteractionList> list = candidate.getIteractionList();
        if (list == null || list.isEmpty()) {
            team.isFree = true;
            team.totalInteractions = 0;
            team.status = InteractionStatus.FREE;
            team.researcherName = team.authorName;
            team.researcherAction = "Создание карточки кандидата";
            return team;
        }

        team.totalInteractions = list.size();

        List<IteractionList> sortedList = new ArrayList<>(list);
        sortedList.sort(Comparator.comparing(IteractionList::getDateIteraction, Comparator.nullsLast(Comparator.naturalOrder())));

        IteractionList last = sortedList.get(sortedList.size() - 1);
        team.lastInteractionDate = last.getDateIteraction();
        team.lastInteractionName = last.getIteractionType() != null ? last.getIteractionType().getIterationName() : "Взаимодействие";
        team.lastEmployeeName = getEmployeeName(last);
        team.status = calculateInteractionStatus(candidate);
        team.isFree = (team.status == InteractionStatus.FREE);

        for (IteractionList il : sortedList) {
            Iteraction type = il.getIteractionType();
            String personName = getEmployeeName(il);
            if (personName == null) continue;

            com.company.hunttech.entity.ExtUser recUser = il.getRecrutier();
            boolean isResearcherGroup = isUserInGroup(recUser, GROUP_RESEARCH, GROUP_RESEARCHER, GROUP_TRAINEE);
            boolean isManagerGroup = isUserInGroup(recUser, GROUP_MANAGERS, GROUP_COORDINATION, GROUP_LEADERSHIP);

            // 1. Координатор: общение с заказчиком, передача резюме, собеседование у клиента, выход в проект
            if (type != null) {
                if (Boolean.TRUE.equals(type.getSignSendToClient())
                        || Boolean.TRUE.equals(type.getSignClientInterview())
                        || Boolean.TRUE.equals(type.getSignStartProject())
                        || Boolean.TRUE.equals(type.getSignEndProject())) {
                    team.coordinatorName = personName;
                    team.coordinatorAction = type.getIterationName();
                } else if (isManagerGroup && team.coordinatorName == null) {
                    team.coordinatorName = personName;
                    team.coordinatorAction = type.getIterationName();
                }
            }

            // 2. Рекрутер: внутреннее собеседование, оценка навыков, выставление рейтинга
            if (type != null) {
                if (Boolean.TRUE.equals(type.getSignOurInterview())
                        || Boolean.TRUE.equals(type.getSignFeedback())
                        || il.getRating() != null) {
                    team.recruiterName = personName;
                    team.recruiterAction = il.getRating() != null ? ("Оценка " + il.getRating() + "★") : type.getIterationName();
                } else if (!isResearcherGroup && !isManagerGroup && team.recruiterName == null && !Boolean.TRUE.equals(type.getSignComment())) {
                    team.recruiterName = personName;
                    team.recruiterAction = type.getIterationName();
                }
            }

            // 3. Ресерчер: поиск кандидата, первый контакт, назначение собеседования
            if (type != null) {
                if (Boolean.TRUE.equals(type.getSignOurInterviewAssigned())
                        || Boolean.TRUE.equals(type.getSignStartCase())) {
                    if (team.researcherName == null) {
                        team.researcherName = personName;
                        team.researcherAction = type.getIterationName();
                    }
                } else if (isResearcherGroup && team.researcherName == null) {
                    team.researcherName = personName;
                    team.researcherAction = type.getIterationName();
                }
            }
        }

        if (team.researcherName == null && !sortedList.isEmpty()) {
            IteractionList first = sortedList.get(0);
            String firstPerson = getEmployeeName(first);
            if (firstPerson != null) {
                team.researcherName = firstPerson;
                team.researcherAction = first.getIteractionType() != null ? first.getIteractionType().getIterationName() : "Первичный контакт";
            }
        }
        if (team.researcherName == null) {
            team.researcherName = team.authorName;
            team.researcherAction = "Создатель карточки";
        }

        if (team.recruiterName == null && !team.isFree && team.lastEmployeeName != null) {
            if (!team.lastEmployeeName.equalsIgnoreCase(team.researcherName)) {
                team.recruiterName = team.lastEmployeeName;
                team.recruiterAction = team.lastInteractionName;
            }
        }

        return team;
    }

    /**
     * Обновляет активное визуальное состояние кнопок фильтров.
     *
     * @param activeBtn кнопка, которая становится активной
     */
    private void updateFilterButtons(Button activeBtn) {
        filterAllBtn.setStyleName("filter-pill-btn secondary");
        filterMyCandidatesBtn.setStyleName("filter-pill-btn secondary");
        filterMyParticipationBtn.setStyleName("filter-pill-btn secondary");
        activeBtn.setStyleName("filter-pill-btn primary active");
    }

    /* =========================================================================
     * Обработчики событий жизненного цикла экрана (Init / Selection)
     * ========================================================================= */

    /**
     * Инициализация экрана и регистрация генераторов колонок таблицы.
     * Колонки привязаны к реальным свойствам сущности для сохранения нативной сортировки.
     */
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
            // Фото берётся из карточки кандидата, а при его отсутствии — из последнего резюме (CandidateCV)
            FileDescriptorImageHelper.setCandidateFace(avatarImg, fileLoader, resolveCandidateFace(candidate));
            return avatarImg;
        });

        // Колонка 2: Кандидат (ФИО жирным + контакт подстрокой, сортировка по fullName)
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

        // Колонка 3: Должность (синий бейдж специальности, сортировка по personPosition)
        candidatesTable.addGeneratedColumn("personPosition", candidate -> {
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            String pos = candidate.getPersonPosition() != null ? candidate.getPersonPosition().getPositionRuName() : "Специалист";
            lbl.setValue("<span style='background: rgba(43, 130, 201, 0.12); color: #2b82c9; padding: 2px 8px; border-radius: 4px; font-weight: 600; font-size: 11px; display: inline-block;'>" + pos + "</span>");
            return lbl;
        });

        // Колонка 4: Город (маркер 📍, сортировка по cityOfResidence)
        candidatesTable.addGeneratedColumn("cityOfResidence", candidate -> {
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            String city = candidate.getCityOfResidence() != null ? candidate.getCityOfResidence().getCityRuName() : "Москва";
            lbl.setValue("<span style='font-size: 12px; color: #34495e;'>📍 " + city + "</span>");
            return lbl;
        });

        // Колонка 5: Компания (маркер 🏢, сортировка по currentCompany)
        candidatesTable.addGeneratedColumn("currentCompany", candidate -> {
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            String company = "-";
            if (candidate.getCurrentCompany() != null) {
                company = candidate.getCurrentCompany().getComanyName() != null ?
                        candidate.getCurrentCompany().getComanyName() : candidate.getCurrentCompany().getCompanyShortName();
            }
            lbl.setValue("<span style='font-size: 12px; color: #34495e;'>🏢 " + (company != null ? company : "-") + "</span>");
            return lbl;
        });

        // Колонка 6: Компактный светофорный статус с датой взаимодействия (135px)
        candidatesTable.addGeneratedColumn("lastInteractionStatus", candidate -> {
            Label<String> statusLbl = uiComponents.create(Label.NAME);
            statusLbl.setHtmlEnabled(true);
            InteractionStatus status = calculateInteractionStatus(candidate);
            IteractionList last = getLastInteraction(candidate);
            String dot = status == InteractionStatus.FREE ? "🟢" :
                    (status == InteractionStatus.MY_CANDIDATE ? "🟡" : "🔴");
            String dateText = "нет";
            if (last != null) {
                java.util.Date d = last.getDateIteraction() != null ? last.getDateIteraction() : last.getCreateTs();
                if (d != null) {
                    dateText = interactionDateFormat.format(d);
                }
            }
            statusLbl.setValue("<span style='background: " + status.getBgColor() + "; color: " + status.getColor() +
                    "; padding: 2px 7px; border-radius: 8px; font-weight: 600; font-size: 11px; white-space: nowrap; display: inline-block;'>" +
                    dot + " " + dateText + "</span>");
            statusLbl.setDescription(status.getLabel() + (last != null && last.getRecrutier() != null ? " (" + last.getRecrutier().getName() + ")" : ""));
            return statusLbl;
        });

        // Колонка 7: Ключевые навыки (3-4 чипа + PopupView со всеми навыками)
        candidatesTable.addGeneratedColumn("mainSkills", candidate -> {
            if (candidate == null || candidate.getId() == null) {
                return null;
            }

            List<CandidateSkill> skills = candidateSkillsCache.get(candidate.getId());
            if (skills == null) {
                try {
                    skills = dataManager.load(CandidateSkill.class)
                            .query("select e from hunttech_CandidateSkill e where e.candidate = :candidate order by e.priority, e.skill.skillName")
                            .parameter("candidate", candidate)
                            .view("candidateSkill-view")
                            .list();
                } catch (Exception ex) {
                    skills = Collections.emptyList();
                }
            }

            if (skills.isEmpty()) {
                Label<String> emptyLabel = uiComponents.create(Label.TYPE_STRING);
                emptyLabel.setValue("—");
                emptyLabel.setStyleName("text-muted");
                return emptyLabel;
            }

            List<CandidateSkill> mainSkills = new ArrayList<>();
            for (CandidateSkill cs : skills) {
                if (cs.getPriority() == CandidateSkillPriority.MAIN) {
                    mainSkills.add(cs);
                }
            }
            if (mainSkills.isEmpty()) {
                mainSkills.addAll(skills.subList(0, Math.min(3, skills.size())));
            }

            String[] palette = new String[]{
                    "#2b82c9", "#27ae60", "#8e44ad", "#d35400", "#16a085", "#2c3e50", "#e67e22", "#2980b9"
            };

            StringBuilder previewSb = new StringBuilder("<div style='display: flex; flex-wrap: nowrap; gap: 4px; align-items: center; overflow: hidden; cursor: pointer;'>");
            int countToShow = Math.min(4, mainSkills.size());
            for (int i = 0; i < countToShow; i++) {
                CandidateSkill cs = mainSkills.get(i);
                if (cs.getSkill() != null && cs.getSkill().getSkillName() != null) {
                    String name = cs.getSkill().getSkillName();
                    String color = palette[Math.abs(name.hashCode()) % palette.length];
                    String star = cs.getPriority() == CandidateSkillPriority.MAIN ? "★ " : "";
                    previewSb.append(String.format(
                            "<span style='background: %s18; color: %s; border: 1px solid %s44; " +
                            "padding: 1px 6px; border-radius: 10px; font-size: 10.5px; font-weight: 600; " +
                            "white-space: nowrap; line-height: 16px;'>%s%s</span>",
                            color, color, color, star, name
                    ));
                }
            }
            int remaining = skills.size() - countToShow;
            if (remaining > 0) {
                previewSb.append(String.format(
                        "<span style='background: #e2e8f0; color: #475569; border: 1px solid #cbd5e1; " +
                        "padding: 1px 5px; border-radius: 10px; font-size: 10px; font-weight: 600; white-space: nowrap;'>+%d</span>",
                        remaining
                ));
            }
            previewSb.append("</div>");

            PopupView popupView = uiComponents.create(PopupView.class);
            popupView.setMinimizedValue(previewSb.toString());
            popupView.setHideOnMouseOut(true);

            VBoxLayout popupContent = uiComponents.create(VBoxLayout.NAME);
            popupContent.setWidth("280px");
            popupContent.setSpacing(true);
            popupContent.setStyleName("card");

            Label<String> popupHeader = uiComponents.create(Label.TYPE_STRING);
            popupHeader.setStyleName("bold");
            popupHeader.setValue("Все навыки кандидата (" + skills.size() + "):");
            popupContent.add(popupHeader);

            StringBuilder allSkillsSb = new StringBuilder("<div style='display: flex; flex-wrap: wrap; gap: 4px; max-height: 220px; overflow-y: auto; padding: 4px 0;'>");
            StringBuilder plainTooltipSb = new StringBuilder("Все навыки кандидата (" + skills.size() + "):\n");
            for (CandidateSkill cs : skills) {
                if (cs.getSkill() != null && cs.getSkill().getSkillName() != null) {
                    String name = cs.getSkill().getSkillName();
                    String color = palette[Math.abs(name.hashCode()) % palette.length];
                    String star = cs.getPriority() == CandidateSkillPriority.MAIN ? "★ " : "";
                    allSkillsSb.append(String.format(
                            "<span style='background: %s18; color: %s; border: 1px solid %s44; " +
                            "padding: 2px 7px; border-radius: 12px; font-size: 11px; font-weight: 600; " +
                            "white-space: nowrap;'>%s%s</span>",
                            color, color, color, star, name
                    ));
                    plainTooltipSb.append(" • ").append(star).append(name).append("\n");
                }
            }
            allSkillsSb.append("</div>");

            Label<String> allSkillsLabel = uiComponents.create(Label.TYPE_STRING);
            allSkillsLabel.setHtmlEnabled(true);
            allSkillsLabel.setValue(allSkillsSb.toString());
            popupContent.add(allSkillsLabel);

            popupView.setPopupContent(popupContent);
            popupView.setDescription(plainTooltipSb.toString().trim());

            return popupView;
        });
    }

    private final java.util.Map<java.util.UUID, List<CandidateSkill>> candidateSkillsCache = new java.util.concurrent.ConcurrentHashMap<>();

    @Subscribe(id = "jobCandidatesDl", target = Target.DATA_LOADER)
    public void onJobCandidatesDlPostLoad(CollectionLoader.PostLoadEvent<JobCandidate> event) {
        candidateSkillsCache.clear();
        List<JobCandidate> candidates = event.getLoadedEntities();
        if (candidates != null && !candidates.isEmpty()) {
            try {
                List<CandidateSkill> allSkills = dataManager.load(CandidateSkill.class)
                        .query("select e from hunttech_CandidateSkill e where e.candidate in :candidates order by e.priority, e.skill.skillName")
                        .parameter("candidates", candidates)
                        .view("candidateSkill-view")
                        .list();
                for (CandidateSkill cs : allSkills) {
                    if (cs.getCandidate() != null && cs.getCandidate().getId() != null) {
                        candidateSkillsCache.computeIfAbsent(cs.getCandidate().getId(), k -> new ArrayList<>()).add(cs);
                    }
                }
            } catch (Exception ignored) {
            }
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
     * Обработчик выбора строки в таблице кандидатов:
     * синхронизирует левый сайдбар и разблокирует выпадающую кнопку действий.
     */
    @Subscribe("candidatesTable")
    public void onCandidatesTableSelection(Table.SelectionEvent<JobCandidate> event) {
        JobCandidate selected = candidatesTable.getSingleSelected();
        if (selected == null) {
            clearDetailPane();
            actionsWithCandidateButton.setEnabled(false);
        } else {
            populateDetailPane(selected);
            actionsWithCandidateButton.setEnabled(true);
        }
    }

    /* =========================================================================
     * Заполнение и очистка левого сайдбара
     * ========================================================================= */

    /**
     * Сбрасывает значения полей левого сайдбара в исходное неактивное состояние.
     */
    private void clearDetailPane() {
        detailFullName.setHtmlEnabled(true);
        detailFullName.setValue("<div style='text-align: center; font-size: 21px; font-weight: 700; color: #94a3b8;'>Выберите кандидата</div>");
        detailPosition.setHtmlEnabled(true);
        detailPosition.setValue("<div style='text-align: center; margin: 4px 0;'><span style='background: rgba(255, 255, 255, 0.08); color: #94a3b8; padding: 3px 10px; border-radius: 4px; font-weight: 500; font-size: 13px; display: inline-block;'>Должность</span></div>");
        detailCity.setHtmlEnabled(true);
        detailCity.setValue("<div style='text-align: center; font-size: 14px; font-weight: 500; color: #64748b; margin-top: 2px;'>📍 —</div>");
        detailPhone.setValue("-");
        detailEmail.setValue("-");
        detailTelegram.setValue("-");
        detailCompany.setValue("-");
        detailSalaryCaption.setVisible(false);
        detailSalary.setVisible(false);
        detailInteractionsInfo.setHtmlEnabled(true);
        detailInteractionsInfo.setValue("<span style='color: rgba(255, 255, 255, 0.5); font-size: 12px;'>Выберите кандидата в таблице справа для просмотра истории.</span>");
        if (detailSkillsLabels != null) {
            detailSkillsLabels.setValue("<span style='color: rgba(255, 255, 255, 0.5); font-size: 11px;'>Выберите кандидата для просмотра навыков</span>");
        }
        detailPic.setSource(ThemeResource.class).setPath("icons/no-programmer.jpeg");
        editCandidateBtn.setEnabled(false);
        createInteractionBtn.setEnabled(false);
    }

    /**
     * Заполняет левый сайдбар полной информацией о выбранном кандидате:
     * центрированная шапка (+30% шрифт), контакты, зарплатные ожидания и светофорная карточка.
     *
     * @param candidate выбранный кандидат
     */
    private void populateDetailPane(JobCandidate candidate) {
        // ФИО (центрировано, шрифт 22px)
        String name = candidate.getFullName() != null ? candidate.getFullName() : "Без имени";
        detailFullName.setHtmlEnabled(true);
        detailFullName.setValue("<div style='text-align: center; font-size: 22px; font-weight: 700; color: #f8fafc; line-height: 1.3; text-shadow: 0 1px 3px rgba(0,0,0,0.5);'>" + name + "</div>");

        // Плашка должности
        String pos = candidate.getPersonPosition() != null ? candidate.getPersonPosition().getPositionRuName() : "Специалист";
        detailPosition.setHtmlEnabled(true);
        detailPosition.setValue("<div style='text-align: center; margin: 4px 0;'><span style='background: rgba(59, 130, 246, 0.25); color: #93c5fd; padding: 3px 12px; border-radius: 12px; font-weight: 600; font-size: 13.5px; display: inline-block; border: 1px solid rgba(96, 165, 250, 0.35);'>" + pos + "</span></div>");

        // Город проживания (центрировано, шрифт 14px)
        String city = candidate.getCityOfResidence() != null ? candidate.getCityOfResidence().getCityRuName() : "Москва";
        detailCity.setHtmlEnabled(true);
        detailCity.setValue("<div style='text-align: center; font-size: 14px; font-weight: 500; color: #cbd5e1; margin-top: 2px;'>📍 " + city + "</div>");

        // Контакты
        detailPhone.setValue(candidate.getPhone() != null ? candidate.getPhone() : "-");
        detailEmail.setValue(candidate.getEmail() != null ? candidate.getEmail() : "-");
        detailTelegram.setValue(candidate.getTelegramName() != null ? candidate.getTelegramName() : "-");

        // Текущее место работы
        String company = "-";
        if (candidate.getCurrentCompany() != null) {
            company = candidate.getCurrentCompany().getComanyName() != null ?
                    candidate.getCurrentCompany().getComanyName() : candidate.getCurrentCompany().getCompanyShortName();
        }
        detailCompany.setValue(company != null ? company : "-");

        // Динамический вывод зарплатных ожиданий
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

        // Фотография профиля: из карточки кандидата, при отсутствии — из последнего резюме (CandidateCV);
        // если файла нет в хранилище — автоматический fallback без битой картинки
        FileDescriptorImageHelper.setCandidateFace(detailPic, fileLoader, resolveCandidateFace(candidate));

        // Формирование блока активности на стандартном фоне сайдбара без рамки
        CandidateRoleTeam team = calculateCandidateTeam(candidate);
        StringBuilder sb = new StringBuilder();
        sb.append("<div class='candidate-activity-sidebar' style='padding: 4px 2px; font-size: 12.5px; line-height: 1.55; color: rgba(248, 250, 252, 0.85);'>");

        sb.append("<div style='display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; padding-bottom: 6px; border-bottom: 1px solid rgba(255, 255, 255, 0.1);'>")
                .append("<span style='font-weight: 600; color: rgba(248, 250, 252, 0.75); font-size: 12px;'>Статус:</span> ")
                .append("<span style='color: ").append(team.status.getColor())
                .append("; font-weight: 700; background: ").append(team.status.getBgColor())
                .append("; padding: 2px 8px; border-radius: 4px; font-size: 11.5px;'>").append(team.status.getLabel()).append("</span>")
                .append("</div>");

        sb.append("<div style='display: flex; flex-direction: column; gap: 5px;'>");

        sb.append("<div>👤 <span style='font-weight: 600; color: rgba(248, 250, 252, 0.9);'>Автор:</span> <span style='color: rgba(248, 250, 252, 0.75);'>")
                .append(team.authorName != null ? team.authorName : "—").append("</span>");
        if (team.createDate != null) {
            sb.append(" <span style='color: rgba(255, 255, 255, 0.45); font-size: 11px;'>(").append(interactionDateFormat.format(team.createDate)).append(")</span>");
        }
        sb.append("</div>");

        if (team.researcherName != null) {
            sb.append("<div>🔍 <span style='font-weight: 600; color: rgba(248, 250, 252, 0.9);'>Ресерчер:</span> <span style='color: rgba(248, 250, 252, 0.75);'>")
                    .append(team.researcherName).append("</span>");
            if (team.researcherAction != null) {
                sb.append(" <span style='color: rgba(255, 255, 255, 0.55); font-size: 11px;'>• ").append(team.researcherAction).append("</span>");
            }
            sb.append("</div>");
        }

        if (team.recruiterName != null) {
            sb.append("<div>💼 <span style='font-weight: 600; color: rgba(248, 250, 252, 0.9);'>Рекрутер:</span> <span style='color: rgba(248, 250, 252, 0.75);'>")
                    .append(team.recruiterName).append("</span>");
            if (team.recruiterAction != null) {
                sb.append(" <span style='color: rgba(255, 255, 255, 0.55); font-size: 11px;'>• ").append(team.recruiterAction).append("</span>");
            }
            sb.append("</div>");
        }

        if (team.coordinatorName != null) {
            sb.append("<div>🤝 <span style='font-weight: 600; color: rgba(248, 250, 252, 0.9);'>Координатор:</span> <span style='color: rgba(248, 250, 252, 0.75);'>")
                    .append(team.coordinatorName).append("</span>");
            if (team.coordinatorAction != null) {
                sb.append(" <span style='color: rgba(255, 255, 255, 0.55); font-size: 11px;'>• ").append(team.coordinatorAction).append("</span>");
            }
            sb.append("</div>");
        }

        if (team.lastInteractionDate != null) {
            sb.append("<div style='color: rgba(248, 250, 252, 0.7); font-size: 11.5px; margin-top: 4px; padding-top: 5px; border-top: 1px dashed rgba(255, 255, 255, 0.12);'>")
                    .append("⏱️ <span style='font-weight: 600; color: rgba(248, 250, 252, 0.9);'>Посл. активность:</span> ").append(interactionDateFormat.format(team.lastInteractionDate));
            if (team.lastInteractionName != null) {
                sb.append(" — ").append(team.lastInteractionName);
            }
            if (team.lastEmployeeName != null) {
                sb.append(" (").append(team.lastEmployeeName).append(")");
            }
            sb.append("</div>");
        }

        sb.append("</div>");

        sb.append("<div style='color: rgba(255, 255, 255, 0.45); font-size: 11px; margin-top: 6px; text-align: right;'>Всего взаимодействий: ")
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

            List<CandidateSkill> mainSkills = new ArrayList<>();
            List<CandidateSkill> secondarySkills = new ArrayList<>();
            List<CandidateSkill> tertiarySkills = new ArrayList<>();

            for (CandidateSkill cs : skills) {
                if (cs.getPriority() == CandidateSkillPriority.MAIN) {
                    mainSkills.add(cs);
                } else if (cs.getPriority() == CandidateSkillPriority.SECONDARY) {
                    secondarySkills.add(cs);
                } else {
                    tertiarySkills.add(cs);
                }
            }

            String[] palette = new String[]{
                    "#2b82c9", "#27ae60", "#8e44ad", "#d35400", "#16a085", "#2c3e50", "#e67e22", "#2980b9"
            };

            StringBuilder sb = new StringBuilder("<div style='display: flex; flex-direction: column; gap: 8px; padding: 2px 0;'>");

            if (!mainSkills.isEmpty()) {
                sb.append("<div><div style='font-size: 10px; font-weight: 700; color: #1e293b; text-transform: uppercase; margin-bottom: 3px; letter-spacing: 0.5px;'>Основные:</div>");
                sb.append("<div style='display: flex; flex-wrap: wrap; gap: 4px;'>");
                for (CandidateSkill cs : mainSkills) {
                    if (cs.getSkill() != null && cs.getSkill().getSkillName() != null) {
                        String name = cs.getSkill().getSkillName();
                        String color = palette[Math.abs(name.hashCode()) % palette.length];
                        sb.append(String.format(
                                "<span style='background: %s18; color: %s; border: 1px solid %s44; " +
                                "padding: 2px 7px; border-radius: 12px; font-size: 11px; font-weight: 600; " +
                                "white-space: nowrap; display: inline-block;'>★ %s</span>",
                                color, color, color, name
                        ));
                    }
                }
                sb.append("</div></div>");
            }

            if (!secondarySkills.isEmpty()) {
                sb.append("<div><div style='font-size: 10px; font-weight: 700; color: #475569; text-transform: uppercase; margin-bottom: 3px; letter-spacing: 0.5px;'>Второстепенные:</div>");
                sb.append("<div style='display: flex; flex-wrap: wrap; gap: 4px;'>");
                for (CandidateSkill cs : secondarySkills) {
                    if (cs.getSkill() != null && cs.getSkill().getSkillName() != null) {
                        String name = cs.getSkill().getSkillName();
                        String color = palette[Math.abs(name.hashCode()) % palette.length];
                        sb.append(String.format(
                                "<span style='background: %s18; color: %s; border: 1px solid %s44; " +
                                "padding: 2px 7px; border-radius: 12px; font-size: 11px; font-weight: 600; " +
                                "white-space: nowrap; display: inline-block;'>%s</span>",
                                color, color, color, name
                        ));
                    }
                }
                sb.append("</div></div>");
            }

            if (!tertiarySkills.isEmpty()) {
                sb.append("<div><div style='font-size: 10px; font-weight: 700; color: #64748b; text-transform: uppercase; margin-bottom: 3px; letter-spacing: 0.5px;'>Прочее:</div>");
                sb.append("<div style='display: flex; flex-wrap: wrap; gap: 4px;'>");
                for (CandidateSkill cs : tertiarySkills) {
                    if (cs.getSkill() != null && cs.getSkill().getSkillName() != null) {
                        String name = cs.getSkill().getSkillName();
                        String color = palette[Math.abs(name.hashCode()) % palette.length];
                        sb.append(String.format(
                                "<span style='background: %s18; color: %s; border: 1px solid %s44; " +
                                "padding: 2px 7px; border-radius: 12px; font-size: 11px; font-weight: 600; " +
                                "white-space: nowrap; display: inline-block;'>%s</span>",
                                color, color, color, name
                        ));
                    }
                }
                sb.append("</div></div>");
            }

            sb.append("</div>");
            detailSkillsLabels.setValue(sb.toString());
        } catch (Exception ex) {
            detailSkillsLabels.setValue("<span style='color: #7f8c8d; font-size: 11px;'>Навыки не определены</span>");
        }
    }

    /* =========================================================================
     * Обработчики действий верхнего тулбара и кнопок быстрых фильтров
     * ========================================================================= */

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
        jobCandidatesDl.removeParameter("recrutier");
        jobCandidatesDl.removeParameter("recrutierName");
        String currentLogin = userSession.getUser() != null ? userSession.getUser().getLogin() : "";
        jobCandidatesDl.setParameter("createdBy", currentLogin);
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

    /* =========================================================================
     * Обработчики выпадающего меню действий над кандидатом (PopupButton)
     * ========================================================================= */

    /** Сканирование навыков по всем подчиненным резюме выбранного кандидата */
    @Subscribe("actionsWithCandidateButton.scanSkillsAction")
    public void onActionsScanSkills(Action.ActionPerformedEvent event) {
        scanCandidateSkills();
    }

    public void scanCandidateSkills() {
        JobCandidate candidate = candidatesTable.getSingleSelected();
        if (candidate == null) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("ВНИМАНИЕ!")
                    .withDescription("Для сканирования навыков выберите кандидата в таблице.")
                    .show();
            return;
        }

        List<CandidateCV> cvList = dataManager.load(CandidateCV.class)
                .query("select e from hunttech_CandidateCV e where e.candidate = :candidate order by e.datePost desc")
                .parameter("candidate", candidate)
                .view("candidateCV-browse-view")
                .list();

        List<String> cvTexts = new ArrayList<>();
        for (CandidateCV cv : cvList) {
            if (cv.getTextCV() != null && !cv.getTextCV().trim().isEmpty()) {
                String plain = Jsoup.parse(cv.getTextCV()).text();
                if (!plain.trim().isEmpty()) {
                    cvTexts.add(plain);
                }
            }
        }

        if (cvTexts.isEmpty()) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("ВНИМАНИЕ!")
                    .withDescription("У выбранного кандидата отсутствуют резюме с текстом для сканирования навыков.")
                    .show();
            return;
        }

        AiOperationNotifier.showStarted(notifications, "Запущен AI-анализ навыков по всем резюме кандидата…", null);
        final JobCandidate candidateForScan = candidate;
        final List<String> textsForScan = cvTexts;
        final Screen progressDialog = AiOperationNotifier.showProgress(this, "Анализ навыков по резюме кандидата…");

        BackgroundTask<Integer, SkillScanOutcome> task =
                new BackgroundTask<Integer, SkillScanOutcome>(240, this) {
                    @Override
                    public SkillScanOutcome run(TaskLifeCycle<Integer> taskLifeCycle) {
                        List<CandidateSkill> existingSkills = dataManager.load(CandidateSkill.class)
                                .query("select e from hunttech_CandidateSkill e where e.candidate = :candidate")
                                .parameter("candidate", candidateForScan)
                                .view("candidateSkill-view")
                                .list();

                        Set<UUID> existingSkillIds = new HashSet<>();
                        for (CandidateSkill cs : existingSkills) {
                            if (cs.getSkill() != null) {
                                existingSkillIds.add(cs.getSkill().getId());
                            }
                        }

                        List<CandidateSkill> toSave = new ArrayList<>();
                        Set<UUID> processedSkillIds = new HashSet<>(existingSkillIds);
                        List<SkillTree> allDetectedSkills = new ArrayList<>();
                        AiExecutionResult lastAiExecution = null;

                        int totalMain = 0;
                        int totalSecondary = 0;
                        int totalTertiary = 0;

                        for (String textForScan : textsForScan) {
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
                            if (aiSourceResult != null && aiSourceResult.getAiExecution() != null) {
                                lastAiExecution = aiSourceResult.getAiExecution();
                            }

                            for (SkillTree st : mainSkills) {
                                if (st != null) {
                                    allDetectedSkills.add(st);
                                    totalMain++;
                                    if (processedSkillIds.add(st.getId())) {
                                        CandidateSkill cs = metadata.create(CandidateSkill.class);
                                        cs.setCandidate(candidateForScan);
                                        cs.setSkill(st);
                                        cs.setPriority(CandidateSkillPriority.MAIN);
                                        toSave.add(cs);
                                    }
                                }
                            }

                            for (SkillTree st : secondarySkills) {
                                if (st != null) {
                                    allDetectedSkills.add(st);
                                    totalSecondary++;
                                    if (processedSkillIds.add(st.getId())) {
                                        CandidateSkill cs = metadata.create(CandidateSkill.class);
                                        cs.setCandidate(candidateForScan);
                                        cs.setSkill(st);
                                        cs.setPriority(CandidateSkillPriority.SECONDARY);
                                        toSave.add(cs);
                                    }
                                }
                            }

                            for (SkillTree st : tertiarySkills) {
                                if (st != null) {
                                    allDetectedSkills.add(st);
                                    totalTertiary++;
                                    if (processedSkillIds.add(st.getId())) {
                                        CandidateSkill cs = metadata.create(CandidateSkill.class);
                                        cs.setCandidate(candidateForScan);
                                        cs.setSkill(st);
                                        cs.setPriority(CandidateSkillPriority.TERTIARY);
                                        toSave.add(cs);
                                    }
                                }
                            }
                        }

                        int totalDetected = totalMain + totalSecondary + totalTertiary;
                        int savedCount = toSave.size();
                        int existingOrDuplicate = totalDetected - savedCount;

                        if (!toSave.isEmpty()) {
                            CommitContext commitContext = new CommitContext(toSave);
                            dataManager.commit(commitContext);
                        }

                        String statsDescription = String.format(
                                "Всего обнаружено навыков по %d резюме: <b>%d</b><br/>" +
                                "• Основных: <b>%d</b><br/>" +
                                "• Второстепенных: <b>%d</b><br/>" +
                                "• Прочих: <b>%d</b><br/>" +
                                "──────────────────────<br/>" +
                                "✅ Сохранено новых: <b>%d</b>%s",
                                textsForScan.size(),
                                totalDetected,
                                totalMain,
                                totalSecondary,
                                totalTertiary,
                                savedCount,
                                (existingOrDuplicate > 0 ? "<br/>ℹ️ Уже присутствуют у кандидата: <b>" + existingOrDuplicate + "</b>" : "")
                        );

                        return new SkillScanOutcome(statsDescription, lastAiExecution, allDetectedSkills);
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

                        candidateSkillsCache.remove(candidateForScan.getId());
                        try {
                            List<CandidateSkill> updatedSkills = dataManager.load(CandidateSkill.class)
                                    .query("select e from hunttech_CandidateSkill e where e.candidate = :candidate order by e.priority, e.skill.skillName")
                                    .parameter("candidate", candidateForScan)
                                    .view("candidateSkill-view")
                                    .list();
                            candidateSkillsCache.put(candidateForScan.getId(), updatedSkills);
                        } catch (Exception ignored) {
                        }

                        candidatesTable.repaint();
                        updateCandidateSkillsSidebar(candidateForScan);
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

    private static class SkillScanOutcome {
        final String statsDescription;
        final AiExecutionResult aiExecution;
        final List<SkillTree> allDetectedSkills;

        SkillScanOutcome(String statsDescription, AiExecutionResult aiExecution, List<SkillTree> allDetectedSkills) {
            this.statsDescription = statsDescription;
            this.aiExecution = aiExecution;
            this.allDetectedSkills = allDetectedSkills;
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

    /** Редактирование карточки выбранного кандидата */
    @Subscribe("actionsWithCandidateButton.editCandidateAction")
    public void onActionsEditCandidate(Action.ActionPerformedEvent event) {
        JobCandidate selected = candidatesTable.getSingleSelected();
        if (selected != null) {
            screenBuilders.editor(candidatesTable)
                    .editEntity(selected)
                    .withOpenMode(OpenMode.DIALOG)
                    .show();
        }
    }

    /** Создание нового взаимодействия с кандидатом */
    @Subscribe("actionsWithCandidateButton.createInteractionAction")
    public void onActionsCreateInteraction(Action.ActionPerformedEvent event) {
        JobCandidate selected = candidatesTable.getSingleSelected();
        if (selected != null) {
            IteractionList interaction = metadata.create(IteractionList.class);
            interaction.setCandidate(selected);
            if (userSession.getUser() instanceof ExtUser) {
                interaction.setRecrutier((ExtUser) userSession.getUser());
            }
            interaction.setDateIteraction(new java.util.Date());
            screenBuilders.editor(IteractionList.class, this)
                    .newEntity(interaction)
                    .withOpenMode(OpenMode.DIALOG)
                    .show();
        }
    }

    /** Отправка Email выбранному кандидату */
    @Subscribe("actionsWithCandidateButton.sendEmailAction")
    public void onActionsSendEmail(Action.ActionPerformedEvent event) {
        JobCandidate selected = candidatesTable.getSingleSelected();
        if (selected != null && selected.getEmail() != null && !selected.getEmail().isEmpty()) {
            notifications.create(Notifications.NotificationType.TRAY)
                    .withCaption("Отправка Email")
                    .withDescription("Подготовка письма для " + selected.getEmail())
                    .show();
        } else {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Email отсутствует")
                    .withDescription("У выбранного кандидата не указан адрес электронной почты.")
                    .show();
        }
    }

    /** Показать список резюме кандидата */
    @Subscribe("actionsWithCandidateButton.showCandidateCVListAction")
    public void onActionsShowCandidateCVList(Action.ActionPerformedEvent event) {
        JobCandidate selected = candidatesTable.getSingleSelected();
        if (selected != null) {
            CandidateCVSimpleBrowse screen = screens.create(CandidateCVSimpleBrowse.class);
            screen.setSelectedCandidate(selected);
            screen.setJobCandidate(selected);
            screens.show(screen);
        }
    }

    /** Показать список взаимодействий кандидата */
    @Subscribe("actionsWithCandidateButton.showIteractionListAction")
    public void onActionsShowIteractionList(Action.ActionPerformedEvent event) {
        JobCandidate selected = candidatesTable.getSingleSelected();
        if (selected != null) {
            IteractionListSimpleBrowse screen = screens.create(IteractionListSimpleBrowse.class);
            screen.setSelectedCandidate(selected);
            screen.setJobCandidate(selected);
            screens.show(screen);
        }
    }

    /** Добавление кандидата в кадровый резерв */
    @Subscribe("actionsWithCandidateButton.addPersonalReserveAction")
    public void onActionsAddPersonalReserve(Action.ActionPerformedEvent event) {
        JobCandidate selected = candidatesTable.getSingleSelected();
        if (selected != null) {
            notifications.create(Notifications.NotificationType.HUMANIZED)
                    .withCaption("Кадровый резерв")
                    .withDescription("Кандидат " + selected.getFullName() + " добавлен в кадровый резерв.")
                    .show();
        }
    }
}
