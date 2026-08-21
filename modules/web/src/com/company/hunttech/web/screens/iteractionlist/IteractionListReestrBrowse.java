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
    private GroupTable<IteractionList> iteractionListsTable;
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

        setupTableColumns();
        setupTableSelection();
        setupSidebarButtons();
    }

    private void setupTableColumns() {
        iteractionListsTable.addGeneratedColumn("avatar", item -> {
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

            if (item != null && item.getCandidate() != null && item.getCandidate().getFileImageFace() != null) {
                image.setSource(FileDescriptorResource.class).setFileDescriptor(item.getCandidate().getFileImageFace());
            } else {
                image.setSource(ThemeResource.class).setPath("icons/no-programmer.jpeg");
            }

            retBox.add(image);
            return retBox;
        });

        iteractionListsTable.addGeneratedColumn("candidate", item -> {
            JobCandidate c = item != null ? item.getCandidate() : null;
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

        iteractionListsTable.addGeneratedColumn("iteractionType", item -> {
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            String typeName = (item != null && item.getIteractionType() != null && item.getIteractionType().getIterationName() != null)
                    ? item.getIteractionType().getIterationName() : "Взаимодействие";
            lbl.setValue("<span style='background: rgba(99, 102, 241, 0.12); color: #4f46e5; border: 1px solid rgba(99, 102, 241, 0.25); padding: 2px 8px; border-radius: 4px; font-weight: 600; font-size: 11px; display: inline-block;'>" + typeName + "</span>");
            return lbl;
        });

        iteractionListsTable.addGeneratedColumn("ratingStars", item -> {
            HBoxLayout box = uiComponents.create(HBoxLayout.class);
            box.setWidthFull();
            box.setHeightFull();
            box.setAlignment(Component.Alignment.MIDDLE_CENTER);

            Label label = uiComponents.create(Label.class);
            label.setHtmlEnabled(true);
            label.setAlignment(Component.Alignment.MIDDLE_CENTER);

            Integer rating = item != null ? item.getRating() : null;
            if (rating != null && rating > 0) {
                label.setValue(starsAndOtherService.setStars(rating));
            } else {
                label.setValue("<span style='color: #cbd5e1;'>—</span>");
            }

            box.add(label);
            return box;
        });

        iteractionListsTable.addGeneratedColumn("vacancy", item -> {
            OpenPosition v = item != null ? item.getVacancy() : null;
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

        iteractionListsTable.addGeneratedColumn("vacancyStatus", item -> {
            HBoxLayout box = uiComponents.create(HBoxLayout.class);
            box.setWidthFull();
            box.setHeightFull();
            box.setAlignment(Component.Alignment.MIDDLE_CENTER);

            Label label = uiComponents.create(Label.class);
            label.setHtmlEnabled(true);
            label.setAlignment(Component.Alignment.MIDDLE_CENTER);

            OpenPosition vacancy = item != null ? item.getVacancy() : null;
            if (vacancy != null) {
                if (Boolean.TRUE.equals(vacancy.getOpenClose())) {
                    label.setValue("<span style='background: #fee2e2; color: #b91c1c; border: 1px solid #fecaca; padding: 2px 6px; border-radius: 4px; font-size: 10.5px; font-weight: 600;'>Закрыта</span>");
                } else {
                    label.setValue("<span style='background: #dcfce7; color: #15803d; border: 1px solid #bbf7d0; padding: 2px 6px; border-radius: 4px; font-size: 10.5px; font-weight: 600;'>Открыта</span>");
                }
            } else {
                label.setValue("<span style='color: #cbd5e1;'>—</span>");
            }

            box.add(label);
            return box;
        });

        iteractionListsTable.addGeneratedColumn("recrutier", item -> {
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            String recName = (item != null && item.getRecrutier() != null) ? item.getRecrutier().getInstanceName() : "-";
            lbl.setValue("<span style='font-size: 12px; color: #475569;'>👤 " + (recName != null ? recName : "-") + "</span>");
            return lbl;
        });

        iteractionListsTable.addGeneratedColumn("dateIteraction", item -> {
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            String d = (item != null && item.getDateIteraction() != null) ? DATE_FORMAT.format(item.getDateIteraction()) : "-";
            lbl.setValue("<span style='font-size: 11.5px; color: #64748b;'>📅 " + d + "</span>");
            return lbl;
        });
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
}
