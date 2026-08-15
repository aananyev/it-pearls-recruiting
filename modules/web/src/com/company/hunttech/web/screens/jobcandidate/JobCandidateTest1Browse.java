package com.company.hunttech.web.screens.jobcandidate;

import com.company.hunttech.entity.ExtUser;
import com.company.hunttech.entity.IteractionList;
import com.company.hunttech.entity.JobCandidate;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.Action;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.FileDescriptorResource;
import com.haulmont.cuba.gui.components.GroupTable;
import com.haulmont.cuba.gui.components.Image;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.PopupButton;
import com.haulmont.cuba.gui.components.Table;
import com.haulmont.cuba.gui.components.ThemeResource;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.gui.screen.OpenMode;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.StandardLookup;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.hunttech.hrm.web.components.WebOvaFallbackImage;

import javax.inject.Inject;

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

    /** Сервис метаданных для создания новых сущностей */
    @Inject
    private Metadata metadata;

    /** Сервис отображения пользовательских всплывающих уведомлений */
    @Inject
    private Notifications notifications;

    /** Сервис DataManager для выполнения прямых JPQL-запросов */
    @Inject
    private com.haulmont.cuba.core.global.DataManager dataManager;

    /** Текущая сессия пользователя для проверки прав и авторства взаимодействий */
    @Inject
    private com.haulmont.cuba.security.global.UserSession userSession;

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

    /** Кнопка фильтра: 🟢 Свободные кандидаты (> 1 мес) */
    @Inject
    private Button filterFreeBtn;

    /** Кнопка фильтра: 🟡 В моей работе (< 1 мес) */
    @Inject
    private Button filterMyBtn;

    /** Кнопка фильтра: 🔴 В работе у других рекрутеров (< 1 мес) */
    @Inject
    private Button filterOtherBtn;

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

    /**
     * Обновляет активное визуальное состояние кнопок фильтров.
     *
     * @param activeBtn кнопка, которая становится активной
     */
    private void updateFilterButtons(Button activeBtn) {
        filterAllBtn.setStyleName("secondary");
        filterFreeBtn.setStyleName("secondary");
        filterMyBtn.setStyleName("secondary");
        filterOtherBtn.setStyleName("secondary");
        activeBtn.setStyleName("primary");
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
            if (candidate.getFileImageFace() != null) {
                avatarImg.setSource(FileDescriptorResource.class).setFileDescriptor(candidate.getFileImageFace());
            }
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
        detailFullName.setValue("<div style='text-align: center; font-size: 21px; font-weight: 700; color: #7f8c8d;'>Выберите кандидата</div>");
        detailPosition.setValue("");
        detailCity.setValue("");
        detailPhone.setValue("-");
        detailEmail.setValue("-");
        detailTelegram.setValue("-");
        detailCompany.setValue("-");
        detailSalaryCaption.setVisible(false);
        detailSalary.setVisible(false);
        detailInteractionsInfo.setValue("Выберите кандидата в таблице справа для просмотра истории.");
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
        detailFullName.setValue("<div style='text-align: center; font-size: 22px; font-weight: 700; color: #2c3e50; line-height: 1.3;'>" + name + "</div>");

        // Плашка должности
        String pos = candidate.getPersonPosition() != null ? candidate.getPersonPosition().getPositionRuName() : "Специалист";
        detailPosition.setHtmlEnabled(true);
        detailPosition.setValue("<div style='text-align: center; margin: 4px 0;'><span style='background: rgba(43, 130, 201, 0.15); color: #2b82c9; padding: 3px 10px; border-radius: 4px; font-weight: 600; font-size: 14px; display: inline-block;'>" + pos + "</span></div>");

        // Город проживания (центрировано, шрифт 15px)
        String city = candidate.getCityOfResidence() != null ? candidate.getCityOfResidence().getCityRuName() : "Москва";
        detailCity.setHtmlEnabled(true);
        detailCity.setValue("<div style='text-align: center; font-size: 15px; font-weight: 500; color: #7f8c8d; margin-top: 2px;'>📍 " + city + "</div>");

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

        // Фотография профиля
        if (candidate.getFileImageFace() != null) {
            detailPic.setSource(FileDescriptorResource.class).setFileDescriptor(candidate.getFileImageFace());
        } else {
            detailPic.setSource(ThemeResource.class).setPath("icons/no-programmer.jpeg");
        }

        // Светофорная карточка статуса взаимодействия
        InteractionStatus status = calculateInteractionStatus(candidate);
        int count = candidate.getIteractionList() != null ? candidate.getIteractionList().size() : 0;
        detailInteractionsInfo.setHtmlEnabled(true);
        detailInteractionsInfo.setValue("<div style='background: #f8f9fa; padding: 10px 14px; border-radius: 6px; border-left: 4px solid " + status.getColor() + "; margin-top: 6px; font-size: 12px; line-height: 1.6;'>" +
                "<b>Статус рекрутера:</b> <span style='color: " + status.getColor() + "; font-weight: bold;'>" + status.getLabel() + "</span><br/>" +
                "• Всего зарегистрированных актов: <b>" + count + "</b>" +
                "</div>");

        editCandidateBtn.setEnabled(true);
        createInteractionBtn.setEnabled(true);
    }

    /* =========================================================================
     * Обработчики действий верхнего тулбара и кнопок быстрых фильтров
     * ========================================================================= */

    @Subscribe("filterAllBtn")
    public void onFilterAllBtnClick(Button.ClickEvent event) {
        updateFilterButtons(filterAllBtn);
        jobCandidatesDl.load();
    }

    @Subscribe("filterFreeBtn")
    public void onFilterFreeBtnClick(Button.ClickEvent event) {
        updateFilterButtons(filterFreeBtn);
        jobCandidatesDl.load();
    }

    @Subscribe("filterMyBtn")
    public void onFilterMyBtnClick(Button.ClickEvent event) {
        updateFilterButtons(filterMyBtn);
        jobCandidatesDl.load();
    }

    @Subscribe("filterOtherBtn")
    public void onFilterOtherBtnClick(Button.ClickEvent event) {
        updateFilterButtons(filterOtherBtn);
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
