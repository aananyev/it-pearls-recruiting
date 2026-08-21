package com.company.hunttech.web.screens.iteractionlist;

import com.company.hunttech.core.StarsAndOtherService;
import com.company.hunttech.entity.Iteraction;
import com.company.hunttech.entity.IteractionList;
import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.entity.OpenPosition;
import com.company.hunttech.service.GetRoleService;
import com.company.hunttech.web.StandartRoles;
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
import com.vaadin.ui.JavaScript;
import org.jsoup.Jsoup;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@UiController("hunttech_IteractionListReestr.browse")
@UiDescriptor("iteraction-list-reestr-browse.xml")
@LookupComponent("iteractionListsTable")
@LoadDataBeforeShow
public class IteractionListReestrBrowse extends StandardLookup<IteractionList> {

    private static final int DEFAULT_DATE_FILTER_DAYS = 90;
    private static final String QUERY_OUTSTAFFING_TYPES =
            "select e from hunttech_Iteraction e where e.outstaffingSign = true";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    @Inject
    private CollectionContainer<IteractionList> iteractionListsDc;
    @Inject
    private CollectionLoader<IteractionList> iteractionListsDl;
    @Inject
    private DataGrid<IteractionList> iteractionListsTable;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private DataManager dataManager;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private UserSession userSession;
    @Inject
    private GetRoleService getRoleService;
    @Inject
    private StarsAndOtherService starsAndOtherService;
    @Inject
    private Notifications notifications;

    @Inject
    private DateField<Date> dateFromField;
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
    private Label<String> detailNumber;
    @Inject
    private Label<String> detailDate;
    @Inject
    private Label<String> detailRating;
    @Inject
    private Label<String> detailVacancy;
    @Inject
    private Label<String> detailVacancyStatus;
    @Inject
    private Label<String> detailRecruiter;
    @Inject
    private Label<String> detailCommMethod;
    @Inject
    private Label<String> detailComment;

    private Set<UUID> outstaffingTypeIdsCache;
    private boolean suppressDateFromReload;

    @Subscribe
    public void onInit(InitEvent event) {
        iteractionListsDl.setMaxResults(100);
        suppressDateFromReload = true;
        try {
            dateFromField.setValue(getDefaultDateFrom());
            applyDateFromFilter();
            applyInternalProjectFilter();
        } finally {
            suppressDateFromReload = false;
        }

        setupTableSelection();
        setupSidebarButtons();
    }

    private Date getDefaultDateFrom() {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.add(GregorianCalendar.DAY_OF_MONTH, -DEFAULT_DATE_FILTER_DAYS);
        return calendar.getTime();
    }

    private void applyDateFromFilter() {
        Date dateFrom = dateFromField.getValue();
        if (dateFrom != null) {
            iteractionListsDl.setParameter("dateFrom", dateFrom);
        } else {
            iteractionListsDl.removeParameter("dateFrom");
        }
    }

    private void applyInternalProjectFilter() {
        if (getRoleService.isUserRoles(userSession.getUser(), StandartRoles.MANAGER) ||
                getRoleService.isUserRoles(userSession.getUser(), StandartRoles.ADMINISTRATOR)) {
            iteractionListsDl.removeParameter("internalProject");
        } else {
            iteractionListsDl.setParameter("internalProject", false);
        }
    }

    private Set<UUID> getOutstaffingTypeIds() {
        if (outstaffingTypeIdsCache == null) {
            outstaffingTypeIdsCache = dataManager.load(Iteraction.class)
                    .query(QUERY_OUTSTAFFING_TYPES)
                    .view("_minimal")
                    .cacheable(true)
                    .list()
                    .stream()
                    .map(Iteraction::getId)
                    .collect(Collectors.toSet());
        }
        return outstaffingTypeIdsCache;
    }

    @Subscribe("dateFromField")
    public void onDateFromFieldValueChange(HasValue.ValueChangeEvent<Date> event) {
        if (suppressDateFromReload) return;
        applyDateFromFilter();
        iteractionListsDl.load();
    }

    private void setupTableSelection() {
        iteractionListsTable.addSelectionListener(e -> {
            Set<IteractionList> selected = e.getSelected();
            if (selected != null && !selected.isEmpty()) {
                IteractionList single = selected.iterator().next();
                updateSidebarDetails(single);
            } else {
                clearSidebarDetails();
            }
        });
    }

    private void setupSidebarButtons() {
        openEditCardBtn.addClickListener(e -> {
            IteractionList selected = iteractionListsTable.getSingleSelected();
            if (selected != null) {
                screenBuilders.editor(iteractionListsTable)
                        .editEntity(selected)
                        .withOpenMode(OpenMode.DIALOG)
                        .show();
            }
        });

        openCandidateCardBtn.addClickListener(e -> {
            IteractionList selected = iteractionListsTable.getSingleSelected();
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
        iteractionListsDl.removeParameter("userName");
        iteractionListsDl.removeParameter("outstaffingTypeIds");
        filterPopupButton.setCaption("Все взаимодействия");
        iteractionListsDl.load();
    }

    @Subscribe("filterPopupButton.filterMyOnly")
    public void onFilterMyOnly(Action.ActionPerformedEvent event) {
        iteractionListsDl.setParameter("userName", "%" + userSession.getUser().getLogin() + "%");
        iteractionListsDl.removeParameter("outstaffingTypeIds");
        filterPopupButton.setCaption("Только мои взаимодействия");
        iteractionListsDl.load();
    }

    @Subscribe("filterPopupButton.filterOutstaffingOnly")
    public void onFilterOutstaffingOnly(Action.ActionPerformedEvent event) {
        Set<UUID> outstaffIds = getOutstaffingTypeIds();
        if (!outstaffIds.isEmpty()) {
            iteractionListsDl.setParameter("outstaffingTypeIds", outstaffIds);
        }
        filterPopupButton.setCaption("Только аутстаффинг");
        iteractionListsDl.load();
    }

    @Subscribe("actionsPopupButton.refreshAction")
    public void onRefreshAction(Action.ActionPerformedEvent event) {
        iteractionListsDl.load();
    }

    @Subscribe("actionsPopupButton.excelExportAction")
    public void onExcelExportAction(Action.ActionPerformedEvent event) {
        Action excel = iteractionListsTable.getAction("excel");
        if (excel != null) {
            excel.actionPerform(iteractionListsTable);
        }
    }

    @Subscribe("actionsPopupButton.copyCandidateSummaryAction")
    public void onCopyCandidateSummaryAction(Action.ActionPerformedEvent event) {
        IteractionList selected = iteractionListsTable.getSingleSelected();
        if (selected == null) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Выберите запись")
                    .show();
            return;
        }

        StringBuilder sb = new StringBuilder();
        if (selected.getCandidate() != null) {
            sb.append("Кандидат: ").append(selected.getCandidate().getFullName()).append("\n");
        }
        if (selected.getVacancy() != null) {
            sb.append("Вакансия: ").append(selected.getVacancy().getVacansyName()).append("\n");
        }
        if (selected.getIteractionType() != null) {
            sb.append("Тип: ").append(selected.getIteractionType().getIterationName()).append("\n");
        }
        if (selected.getComment() != null) {
            sb.append("Комментарий:\n").append(Jsoup.parse(selected.getComment()).text());
        }

        try {
            JavaScript.getCurrent().execute("navigator.clipboard.writeText(`" + sb.toString().replace("`", "\\`") + "`);");
            notifications.create(Notifications.NotificationType.TRAY)
                    .withCaption("Скопировано в буфер обмена")
                    .show();
        } catch (Exception ignored) {
        }
    }

    @Subscribe(id = "iteractionListsDl", target = Target.DATA_LOADER)
    private void onIteractionListsDlPostLoad(CollectionLoader.PostLoadEvent<IteractionList> event) {
        IteractionList current = iteractionListsTable.getSingleSelected();
        if (current != null) {
            updateSidebarDetails(current);
        } else if (!event.getLoadedEntities().isEmpty()) {
            iteractionListsTable.setSelected(event.getLoadedEntities().get(0));
        } else {
            clearSidebarDetails();
        }
    }

    private void updateSidebarDetails(IteractionList item) {
        openEditCardBtn.setEnabled(true);
        openCandidateCardBtn.setEnabled(item.getCandidate() != null);

        JobCandidate candidate = item.getCandidate();

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

        if (item.getIteractionType() != null) {
            detailSubtitle.setValue(item.getIteractionType().getIterationName());
        } else {
            detailSubtitle.setValue("-");
        }

        if (item.getVacancy() != null && item.getVacancy().getProjectName() != null) {
            detailLocation.setValue("Проект: " + item.getVacancy().getProjectName().getProjectName());
        } else if (candidate != null && candidate.getCityOfResidence() != null) {
            detailLocation.setValue(candidate.getCityOfResidence().getCityRuName());
        } else {
            detailLocation.setValue("-");
        }

        // Детали
        detailNumber.setValue(item.getNumberIteraction() != null ? String.valueOf(item.getNumberIteraction()) : "-");
        detailDate.setValue(item.getDateIteraction() != null ? DATE_FORMAT.format(item.getDateIteraction()) : "-");

        if (item.getRating() != null && item.getRating() > 0) {
            detailRating.setValue(starsAndOtherService.setStars(item.getRating()));
        } else {
            detailRating.setValue("<span style='color: #94a3b8;'>Не оценен</span>");
        }

        if (item.getVacancy() != null) {
            detailVacancy.setValue(item.getVacancy().getVacansyName());
            if (Boolean.TRUE.equals(item.getVacancy().getOpenClose())) {
                detailVacancyStatus.setValue("<span style='background: #fee2e2; color: #b91c1c; border: 1px solid #fecaca; padding: 2px 8px; border-radius: 10px; font-size: 11px; font-weight: 600;'>Закрыта</span>");
            } else {
                detailVacancyStatus.setValue("<span style='background: #dcfce7; color: #15803d; border: 1px solid #bbf7d0; padding: 2px 8px; border-radius: 10px; font-size: 11px; font-weight: 600;'>Открыта</span>");
            }
        } else {
            detailVacancy.setValue("-");
            detailVacancyStatus.setValue("-");
        }

        detailRecruiter.setValue(item.getRecrutier() != null ? item.getRecrutier().getInstanceName() : "-");
        detailCommMethod.setValue(item.getCommunicationMethod() != null ? item.getCommunicationMethod() : "-");

        // Комментарий
        if (item.getComment() != null && !item.getComment().trim().isEmpty()) {
            String plain = Jsoup.parse(item.getComment()).text();
            detailComment.setValue(plain.length() > 300 ? plain.substring(0, 300) + "..." : plain);
        } else {
            detailComment.setValue("<span style='color: #94a3b8;'>Комментарий отсутствует</span>");
        }
    }

    private void clearSidebarDetails() {
        openEditCardBtn.setEnabled(false);
        openCandidateCardBtn.setEnabled(false);
        logoPic.setSource(ThemeResource.class).setPath("icons/no-company.png");
        detailTitle.setValue("Выберите запись");
        detailSubtitle.setValue("-");
        detailLocation.setValue("-");
        detailNumber.setValue("-");
        detailDate.setValue("-");
        detailRating.setValue("-");
        detailVacancy.setValue("-");
        detailVacancyStatus.setValue("-");
        detailRecruiter.setValue("-");
        detailCommMethod.setValue("-");
        detailComment.setValue("<span style='color: #94a3b8;'>Запись не выбрана</span>");
    }

    @Install(to = "iteractionListsTable.ratingStars", subject = "columnGenerator")
    private Component iteractionListsTableRatingStarsColumnGenerator(DataGrid.ColumnGeneratorEvent<IteractionList> event) {
        HBoxLayout box = uiComponents.create(HBoxLayout.class);
        box.setWidthFull();
        box.setHeightFull();
        box.setAlignment(Component.Alignment.MIDDLE_CENTER);

        Label label = uiComponents.create(Label.class);
        label.setHtmlEnabled(true);
        label.setAlignment(Component.Alignment.MIDDLE_CENTER);

        Integer rating = event.getItem().getRating();
        if (rating != null && rating > 0) {
            label.setValue(starsAndOtherService.setStars(rating));
        } else {
            label.setValue("<span style='color: #cbd5e1;'>—</span>");
        }

        box.add(label);
        return box;
    }

    @Install(to = "iteractionListsTable.vacancyStatus", subject = "columnGenerator")
    private Component iteractionListsTableVacancyStatusColumnGenerator(DataGrid.ColumnGeneratorEvent<IteractionList> event) {
        HBoxLayout box = uiComponents.create(HBoxLayout.class);
        box.setWidthFull();
        box.setHeightFull();
        box.setAlignment(Component.Alignment.MIDDLE_CENTER);

        Label label = uiComponents.create(Label.class);
        label.setHtmlEnabled(true);
        label.setAlignment(Component.Alignment.MIDDLE_CENTER);

        OpenPosition vacancy = event.getItem().getVacancy();
        if (vacancy != null) {
            if (Boolean.TRUE.equals(vacancy.getOpenClose())) {
                label.setValue("<span style='background: #fee2e2; color: #b91c1c; border: 1px solid #fecaca; padding: 2px 8px; border-radius: 10px; font-size: 11px; font-weight: 600;'>Закрыта</span>");
            } else {
                label.setValue("<span style='background: #dcfce7; color: #15803d; border: 1px solid #bbf7d0; padding: 2px 8px; border-radius: 10px; font-size: 11px; font-weight: 600;'>Открыта</span>");
            }
        } else {
            label.setValue("<span style='color: #cbd5e1;'>—</span>");
        }

        box.add(label);
        return box;
    }
}
