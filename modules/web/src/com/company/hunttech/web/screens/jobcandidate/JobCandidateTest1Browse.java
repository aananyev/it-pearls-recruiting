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
import com.haulmont.cuba.gui.components.TextField;
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

@UiController("hunttech_JobCandidateTest1.browse")
@UiDescriptor("job-candidate-test1-browse.xml")
@LookupComponent("candidatesTable")
@LoadDataBeforeShow
public class JobCandidateTest1Browse extends StandardLookup<JobCandidate> {

    @Inject
    private GroupTable<JobCandidate> candidatesTable;
    @Inject
    private CollectionLoader<JobCandidate> jobCandidatesDl;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private Metadata metadata;
    @Inject
    private Notifications notifications;

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
    private Label<String> detailInteractionsInfo;
    @Inject
    private Button editCandidateBtn;
    @Inject
    private Button createInteractionBtn;

    @Inject
    private com.haulmont.cuba.security.global.UserSession userSession;

    public enum InteractionStatus {
        FREE("🟢 Свободен (> 1 мес)", "#27ae60", "rgba(39, 174, 96, 0.15)"),
        MY_CANDIDATE("🟡 В вашей работе (< 1 мес)", "#f39c12", "rgba(243, 156, 18, 0.15)"),
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

    private final java.text.SimpleDateFormat interactionDateFormat = new java.text.SimpleDateFormat("dd.MM.yyyy");

    @Inject
    private PopupButton actionsWithCandidateButton;

    private com.company.hunttech.entity.IteractionList getLastInteraction(JobCandidate candidate) {
        if (candidate == null || candidate.getIteractionList() == null || candidate.getIteractionList().isEmpty()) {
            return null;
        }
        com.company.hunttech.entity.IteractionList last = null;
        for (com.company.hunttech.entity.IteractionList item : candidate.getIteractionList()) {
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

    private InteractionStatus calculateInteractionStatus(JobCandidate candidate) {
        if (candidate == null) {
            return InteractionStatus.FREE;
        }
        com.company.hunttech.entity.IteractionList last = getLastInteraction(candidate);
        if (last == null) {
            return InteractionStatus.FREE;
        }

        java.util.Date date = last.getDateIteraction() != null ? last.getDateIteraction() : last.getCreateTs();
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

    @Inject
    private Button filterAllBtn;
    @Inject
    private Button filterFreeBtn;
    @Inject
    private Button filterMyBtn;
    @Inject
    private Button filterOtherBtn;

    private void updateFilterButtons(Button activeBtn) {
        filterAllBtn.setStyleName("secondary");
        filterFreeBtn.setStyleName("secondary");
        filterMyBtn.setStyleName("secondary");
        filterOtherBtn.setStyleName("secondary");
        activeBtn.setStyleName("primary");
    }

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
            if (candidate.getFileImageFace() != null) {
                avatarImg.setSource(FileDescriptorResource.class).setFileDescriptor(candidate.getFileImageFace());
            }
            return avatarImg;
        });

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

        candidatesTable.addGeneratedColumn("personPosition", candidate -> {
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            String pos = candidate.getPersonPosition() != null ? candidate.getPersonPosition().getPositionRuName() : "Специалист";
            lbl.setValue("<span style='background: rgba(43, 130, 201, 0.12); color: #2b82c9; padding: 2px 8px; border-radius: 4px; font-weight: 600; font-size: 11px; display: inline-block;'>" + pos + "</span>");
            return lbl;
        });

        candidatesTable.addGeneratedColumn("cityOfResidence", candidate -> {
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            String city = candidate.getCityOfResidence() != null ? candidate.getCityOfResidence().getCityRuName() : "Москва";
            lbl.setValue("<span style='font-size: 12px; color: #34495e;'>📍 " + city + "</span>");
            return lbl;
        });

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

        candidatesTable.addGeneratedColumn("lastInteractionStatus", candidate -> {
            Label<String> statusLbl = uiComponents.create(Label.NAME);
            statusLbl.setHtmlEnabled(true);
            InteractionStatus status = calculateInteractionStatus(candidate);
            com.company.hunttech.entity.IteractionList last = getLastInteraction(candidate);
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

    private void clearDetailPane() {
        detailFullName.setHtmlEnabled(true);
        detailFullName.setValue("<div style='text-align: center; font-size: 21px; font-weight: 700; color: #7f8c8d;'>Выберите кандидата</div>");
        detailPosition.setValue("");
        detailCity.setValue("");
        detailPhone.setValue("-");
        detailEmail.setValue("-");
        detailTelegram.setValue("-");
        detailCompany.setValue("-");
        detailInteractionsInfo.setValue("Выберите кандидата в таблице справа для просмотра истории.");
        detailPic.setSource(ThemeResource.class).setPath("icons/no-programmer.jpeg");
        editCandidateBtn.setEnabled(false);
        createInteractionBtn.setEnabled(false);
    }

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

        if (candidate.getFileImageFace() != null) {
            detailPic.setSource(FileDescriptorResource.class).setFileDescriptor(candidate.getFileImageFace());
        } else {
            detailPic.setSource(ThemeResource.class).setPath("icons/no-programmer.jpeg");
        }

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
}
