package com.company.itpearls.web.screens.recrutiestasks;

import com.company.itpearls.entity.IteractionList;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.core.global.ValueLoadContext;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.RecrutiesTasks;
import com.haulmont.cuba.gui.screen.LookupComponent;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Date;

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

    @Subscribe
    public void onInit(InitEvent event) {

        // если роль - ресерчер, то автоматически вставить себя
/*        Collection<String> s = userSessionSource.getUserSession().getRoles();
        // установить поле рекрутера
        if( s.contains( ROLE_RESEARCHER ) ) {
            // если ресерчер то ограничить просмотр других рекрутеров
            if( !s.contains( ROLE_MANAGER ))
                recrutiesTasksesDl.setParameter( "recrutier", userSessionSource.getUserSession().getUser() );
            else
                // менеджеру тоже всех показывать
                recrutiesTasksesDl.removeParameter( "recrutier" );
        } else {
            recrutiesTasksesDl.removeParameter( "recrutier" );
        }

        recrutiesTasksesDl.load(); */

        recrutiesTasksesDl.setParameter("allRecruters", true);
        recrutiesTasksesDl.load();

        // перечеркнем просроченные позиции
        recrutiesTasksesTable.setStyleProvider((recrutiesTask, property) -> {
            Date curDate = new Date();

            if (!recrutiesTask.getEndDate().after(curDate)) {
                return "recrutier-tasks-gray";
            } else {
                return "recrutier-tasks-normal";
            }
        });
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        checkBoxRemoveOld.setValue(true);
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
        } else {
            recrutiesTasksesDl.removeParameter("currentDate");
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

/*        ValueLoadContext valueLoadContext = ValueLoadContext.create()
                .setQuery(ValueLoadContext.createQuery(QUERY)
                        .setParameter("startDate", recrutiesTasks.getStartDate())
                        .setParameter("endDate", recrutiesTasks.getEndDate())
                        .setParameter("vacancy", recrutiesTasks.getOpenPosition()))
                .addProperty("count"); */

        Integer countOurInterview = dataManager.load(IteractionList.class)
                .view("iteractionList-view")
                .query(QUERY)
                .parameter("startDate", recrutiesTasks.getStartDate())
                .parameter("endDate", recrutiesTasks.getEndDate())
                .parameter("vacancy", recrutiesTasks.getOpenPosition())
                .parameter("recrutier", recrutiesTasks.getReacrutier())
                .list()
                .size();

        label.setValue(countOurInterview);
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

    @Install(to = "recrutiesTasksesTable.group", subject = "columnGenerator")
    private Component recrutiesTasksesTableGroupColumnGenerator(RecrutiesTasks recrutiesTasks) {
        String group = recrutiesTasks.getReacrutier().getGroup().getName();
        Label label = uiComponents.create(Label.NAME);
        label.setValue(group);
        return label;
    }
}