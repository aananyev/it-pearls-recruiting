package com.company.itpearls.web.screens.recrutiestasks;

import com.company.itpearls.entity.ExtUser;
import com.company.itpearls.entity.IteractionList;
import com.company.itpearls.entity.OpenPositionNews;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.core.global.ValueLoadContext;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.RecrutiesTasks;
import com.haulmont.cuba.gui.screen.LookupComponent;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@UiController("itpearls_RecrutiesTasks.browse")
@UiDescriptor("recruties-tasks-browse.xml")
@LookupComponent("recrutiesTasksesTable")
@LoadDataBeforeShow
public class RecrutiesTasksBrowse extends StandardLookup<RecrutiesTasks> {
    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private CollectionLoader<RecrutiesTasks> recrutiesTasksesDl;
    @Inject
    private CheckBox checkBoxRemoveOld;
    @Inject
    private GroupTable<RecrutiesTasks> recrutiesTasksesTable;

    private String ROLE_RESEARCHER = "Researcher";
    private String ROLE_MANAGER = "Manager";
    @Inject
    private UiComponents uiComponents;
    @Inject
    private DataManager dataManager;
    @Inject
    private Button unsubscribeButton;
    @Inject
    private Dialogs dialogs;

    @Subscribe
    public void onInit(InitEvent event) {

        // перечеркнем просроченные позиции
        recrutiesTasksesTable.setStyleProvider((recrutiesTask, property) -> {
            Date curDate = new Date();

            if (!recrutiesTask.getEndDate().after(curDate)) {
                return "recrutier-tasks-gray";
            } else {
                return "recrutier-tasks-normal";
            }
        });

        setButtonsRecrutiesTasksesTable();
    }

    private void setButtonsRecrutiesTasksesTable() {
        recrutiesTasksesTable.addSelectionListener(e -> {
            if (recrutiesTasksesTable.getSingleSelected() != null) {
                unsubscribeButton.setEnabled(true);
                unsubscribeButton.setCaption(recrutiesTasksesTable.getSingleSelected().getClosed() ? "Подписаться" : "Отписаться");
            } else {
                unsubscribeButton.setEnabled(false);
                unsubscribeButton.setCaption("Отписаться");
            }
        });
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        checkBoxRemoveOld.setValue(true);

        recrutiesTasksesDl.setParameter("allRecruters", true);
        recrutiesTasksesDl.setParameter("closed", false);
        recrutiesTasksesDl.load();
    }

    @Subscribe("allReacrutersCheckBox")
    public void onAllReacrutersCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (!event.getValue()) {
            recrutiesTasksesDl.setParameter("allRecruters", true);
        } else {
            recrutiesTasksesDl.removeParameter("allRecruters");
        }

        recrutiesTasksesDl.load();
    }

    @Subscribe("checkBoxRemoveOld")
    public void onCheckBoxRemoveOldValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        recrutiesTasksesDl.removeParameter("allRecruters");

        if (checkBoxRemoveOld.getValue()) {
            Date curDate = new Date();

            recrutiesTasksesDl.setParameter("currentDate", curDate);
            recrutiesTasksesDl.setParameter("closed", false);
        } else {
            recrutiesTasksesDl.removeParameter("currentDate");
            recrutiesTasksesDl.removeParameter("closed");
        }

        recrutiesTasksesDl.load();
    }

    @Install(to = "recrutiesTasksesTable.factForPeriod", subject = "columnGenerator")
    private Component recrutiesTasksesTableFactForPeriodColumnGenerator(RecrutiesTasks recrutiesTasks) {
        Label label = uiComponents.create(Label.NAME);
        String QUERY = "select e from itpearls_IteractionList e " +
                "where e.dateIteraction between :startDate and :endDate " +
                "and e.vacancy = :vacancy " +
                "and e.recrutier = :recrutier " +
                "and e.iteractionType.signOurInterview = true";

        List<IteractionList> iteractionLists = dataManager.load(IteractionList.class)
                .view("iteractionList-view")
                .query(QUERY)
                .parameter("startDate", recrutiesTasks.getStartDate())
                .parameter("endDate", recrutiesTasks.getEndDate())
                .parameter("vacancy", recrutiesTasks.getOpenPosition())
                .parameter("recrutier", recrutiesTasks.getReacrutier())
                .list();

        Integer countOurInterview = iteractionLists.size();

        String countDdscription = "";
        for (IteractionList e : iteractionLists) {
            countDdscription = countDdscription + e.getCandidate().getFullName() + "\n";
        }

        label.setValue(countOurInterview);
        label.setDescription(countDdscription);

        if (countOurInterview != 0) {
            if (recrutiesTasks.getPlanForPeriod() != null) {
                if (countOurInterview >= recrutiesTasks.getPlanForPeriod()) {
                    label.setStyleName("label_button_green");
                } else {
                    label.setStyleName("label_button_red");
                }
            } else {
                label.setStyleName("label_button_green");
            }
        } else {
            label.setStyleName("label_button_red");
        }

        return label;
    }

    @Install(to = "recrutiesTasksesTable", subject = "iconProvider")
    private String recrutiesTasksesTableIconProvider(RecrutiesTasks recrutiesTasks) {
        return (!recrutiesTasks.getOpenPosition().getOpenClose() ? "icons/ok.png" : "icons/close.png");
    }

/*    @Install(to = "recrutiesTasksesTable.group", subject = "columnGenerator")
    private Component recrutiesTasksesTableGroupColumnGenerator(RecrutiesTasks recrutiesTasks) {
        String group = recrutiesTasks.getReacrutier().getGroup().getName();
        Label label = uiComponents.create(Label.NAME);
        label.setValue(group);
        return label;
    } */



    public void unsubscribeFromVacancy() {
        dialogs.createOptionDialog(Dialogs.MessageType.WARNING)
                .withType(Dialogs.MessageType.WARNING)
                .withMessage("Отписаться от вакансии "
                        + recrutiesTasksesTable.getSingleSelected().getOpenPosition().getVacansyName())
                .withActions(
                        new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY).withHandler(e -> {
                            RecrutiesTasks recrutiesTasks = recrutiesTasksesTable.getSingleSelected();
                            recrutiesTasks.setClosed(!recrutiesTasksesTable.getSingleSelected().getClosed());

                            unsubscribeButton.setCaption(recrutiesTasks.getClosed() ? "Подписаться" : "Отписаться");
                            dataManager.commit(recrutiesTasks);

                            OpenPositionNews openPositionNews = new OpenPositionNews();
                            openPositionNews.setPriorityNews(false);
                            openPositionNews.setOpenPosition(recrutiesTasks.getOpenPosition());
                            openPositionNews.setDateNews(new Date());
                            openPositionNews.setAuthor((ExtUser) userSessionSource.getUserSession().getUser());
                            openPositionNews.setSubject(recrutiesTasks.getReacrutier().getName()
                                    + " отписан от вакансии");
                            dataManager.commit(openPositionNews);

                            recrutiesTasksesDl.load();
                        }),
                        new DialogAction(DialogAction.Type.NO)
                )
                .show();

    }

    public Component projectLogoColumnGenerator(Entity entity) {
        HBoxLayout retHBox = uiComponents.create(HBoxLayout.class);
        retHBox.setWidthFull();
        retHBox.setHeightAuto();

        Image image = uiComponents.create(Image.class);
        image.setDescriptionAsHtml(true);
        image.setScaleMode(Image.ScaleMode.SCALE_DOWN);
        image.setWidth("50px");
        image.setHeight("50px");
        image.setStyleName("icon-no-border-50px");
        image.setAlignment(Component.Alignment.MIDDLE_CENTER);

        if (((RecrutiesTasks)entity).getOpenPosition().getProjectName() != null) {
            if (((RecrutiesTasks)entity).getOpenPosition().getProjectName().getProjectDescription() != null) {
                image.setDescription("<h4>"
                        + ((RecrutiesTasks)entity).getOpenPosition().getProjectName().getProjectName()
                        + "</h4><br><br>"
                        + ((RecrutiesTasks)entity).getOpenPosition().getProjectName().getProjectDescription());
            }

            if (((RecrutiesTasks)entity).getOpenPosition().getProjectName().getProjectLogo() != null) {
                image.setSource(FileDescriptorResource.class)
                        .setFileDescriptor(((RecrutiesTasks)entity).getOpenPosition()
                                .getProjectName()
                                .getProjectLogo());
            } else {
                image.setSource(ThemeResource.class).setPath("icons/no-company.png");
            }
        }

        retHBox.add(image);
        return retHBox;
    }
}