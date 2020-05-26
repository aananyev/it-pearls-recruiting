package com.company.itpearls.web.widgets;

import com.company.itpearls.entity.Iteraction;
import com.company.itpearls.entity.IteractionList;
import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.addon.dashboard.web.annotation.WidgetParam;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.WindowParam;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.security.entity.User;
import javafx.scene.layout.VBox;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.function.Function;

@UiController("itpearls_FunnelHuntingWidget")
@UiDescriptor("funnel-hunting-widget.xml")
@DashboardWidget( name = "Funnel Hunting Widget" )
public class FunnelHuntingWidget extends ScreenFragment {
    @Inject
    private Label<String> researcherTitle;
    @Inject
    private UiComponents uiComponents;

    @WidgetParam
    @WindowParam
    protected Date startDate;

    @WidgetParam
    @WindowParam
    protected Date endDate;

    private List<String> listIteractionForCheck = new ArrayList<String>();
    private List<Label> researcherLabelList = new ArrayList<Label>();
    private List<User> reaearchers = new ArrayList<>();

    private String ITRKT_NEW_CONTACT = "Новый контакт";
    private String ITRKT_POPOSE_JOB = "Предложение работы";
    private String ITRKT_ASSIGN_ITPEARKS_INTERVIEW = "Назначено собеседование с рекрутером IT Pearls";
    private String ITRKT_PREPARE_ITPEARKS_INTERVIEW = "Прошел собеседование с рекрутером IT Pearls";
    private String ITRKT_ASSIGN_TECH_INTERVIEW = "Назначено техническое собеседование";
    private String ITRKT_PREPARE_TECH_INTERVIEW = "Прошел техническое собеседование";
    private String ITRKT_PREPARE_DIRECTOR_INTERVIEW = "Прошел собеседование с Директором";

    private String labelHeight = "15px";
    private String iteractionTitleHeight = "45px";
    private String sizeColumn = "120px";

    @Inject
    private HBoxLayout boxWidgetTitle;
    @Inject
    private DataManager dataManager;
    @Inject
    private VBoxLayout researcherNameBox;
    @Inject
    private Label<String> widgetTitle;

    @Subscribe
    public void onAfterInit(AfterInitEvent event) {
        initListIteraction();

        researcherTitle.setValue( "Ресерчер" );
        researcherTitle.setWidth(sizeColumn);
        researcherTitle.setHeight(iteractionTitleHeight);
        researcherTitle.setAlignment(Component.Alignment.BOTTOM_LEFT);
        researcherTitle.setSizeFull();

        setDeafaultTimeInterval();
        setWidgetTitle();
        getResearchersList();

        setIteractionTitle();
        setResearcherList();
    }

    private void setWidgetTitle() {
        String title = "Статистика по рекрутерам за: ";
        DateFormat df = new SimpleDateFormat("dd.MM.yyyy");

        widgetTitle.setValue( title + df.format(startDate) + " - " + df.format(endDate) );
    }

    private void getResearchersList() {
        LoadContext<User> loadContext = LoadContext.create(User.class)
                .setQuery(LoadContext.createQuery("select e from sec$User e where e.active=true and e.name not like \'Anonymous\' and e.name not like \'%Test%\' and e.name not like \'Administrator\' order by e.name"));
        reaearchers = dataManager.loadList(loadContext);
    }

    private void setDeafaultTimeInterval() {
        if( startDate == null || endDate == null ) {
            GregorianCalendar calendar = new GregorianCalendar();

            calendar.set(GregorianCalendar.DATE, 1);
            startDate = calendar.getTime();

            calendar.set(GregorianCalendar.DAY_OF_MONTH, calendar.getActualMaximum(GregorianCalendar.DAY_OF_MONTH));
            endDate = calendar.getTime();
        }
    }

    private void setIteractionTitle() {
        for( String a : listIteractionForCheck ) {

            String queryParameter = a;

            VBoxLayout    vBox = uiComponents.create(VBoxLayout.class);
            boxWidgetTitle.add( vBox );

            vBox.setWidth(sizeColumn);
            vBox.setAlignment(Component.Alignment.BOTTOM_CENTER);

            Label<String> label = uiComponents.create(Label.TYPE_STRING);
            vBox.add( label );
            vBox.setSpacing( true );

            label.setHeight( iteractionTitleHeight );
            label.setWidth( "100%" );
            label.setHeightFull();
            label.setWidthFull();
            label.setValue( a );
            label.setAlignment( Component.Alignment.BOTTOM_CENTER );
            label.setStyleName( "v-caption-label" );

            for( User user : reaearchers ) {
               String queryCounter = "select count(e) from itpearls_IteractionList e " +
                       "where e.dateIteraction between :startDate and :endDate and " +
                       "e.recrutier = :recrutier and " +
                       "e.iteractionType = (select f from itpearls_Iteraction f where f.iterationName like :iteractionName )";

               int  iteractionCount = dataManager.loadValue( queryCounter, Integer.class)
                       .parameter( "startDate", startDate )
                       .parameter( "endDate", endDate )
                       .parameter( "recrutier", user )
                       .parameter( "iteractionName", a )
                       .one();

               Label<Integer> labelCount = uiComponents.create(Label.TYPE_INTEGER);
               vBox.add( labelCount );

               labelCount.setValue( iteractionCount );
               labelCount.setAlignment( Component.Alignment.MIDDLE_CENTER );
               labelCount.setWidth( "100%" );
               labelCount.setHeight( labelHeight );
               labelCount.setHeightFull();
               labelCount.setWidthFull();
            }
        }
    }

    private void setResearcherList() {
        for( User a : reaearchers ) {
            Label<String> label = uiComponents.create( Label.TYPE_STRING );
            researcherNameBox.add( label );

            label.setAlignment( Component.Alignment.MIDDLE_LEFT);
            label.setHeight(labelHeight);
            label.setValue( a.getName() );

        }
    }

    private void initListIteraction() {
        listIteractionForCheck.add( ITRKT_NEW_CONTACT );
        listIteractionForCheck.add( ITRKT_POPOSE_JOB );
        listIteractionForCheck.add( ITRKT_ASSIGN_ITPEARKS_INTERVIEW );
        listIteractionForCheck.add( ITRKT_PREPARE_ITPEARKS_INTERVIEW );
        listIteractionForCheck.add( ITRKT_ASSIGN_TECH_INTERVIEW );
        listIteractionForCheck.add( ITRKT_PREPARE_TECH_INTERVIEW );
        listIteractionForCheck.add( ITRKT_PREPARE_DIRECTOR_INTERVIEW );
    }
}