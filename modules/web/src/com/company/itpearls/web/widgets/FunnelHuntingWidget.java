package com.company.itpearls.web.widgets;

import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.addon.dashboard.web.annotation.WidgetParam;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.WindowParam;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.security.entity.User;

import javax.inject.Inject;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

@UiController("itpearls_FunnelHuntingWidget")
@UiDescriptor("funnel-hunting-widget.xml")
@DashboardWidget( name = "Funnel Hunting Widget" )
public class FunnelHuntingWidget extends ScreenFragment {
    @Inject
    private UiComponents uiComponents;

    @WidgetParam
    @WindowParam
    protected Date startDate;

    @WidgetParam
    @WindowParam
    protected Date endDate;

    private List<String> listIteractionForCheck = new ArrayList<String>();
    private List<User> reaearchers = new ArrayList<>();

    private String ITRKT_NEW_CONTACT = "Новый контакт";
    private String ITRKT_POPOSE_JOB = "Предложение работы";
    private String ITRKT_ASSIGN_ITPEARKS_INTERVIEW = "Назначено собеседование с рекрутером IT Pearls";
    private String ITRKT_PREPARE_ITPEARKS_INTERVIEW = "Прошел собеседование с рекрутером IT Pearls";
    private String ITRKT_ASSIGN_TECH_INTERVIEW = "Назначено техническое собеседование";
    private String ITRKT_PREPARE_TECH_INTERVIEW = "Прошел техническое собеседование";
    private String ITRKT_PREPARE_DIRECTOR_INTERVIEW = "Прошел собеседование с Директором";

    private String labelHeight = "15px";
    private String sizeColumn = "115px";

    @Inject
    private HBoxLayout boxWidgetTitle;
    @Inject
    private DataManager dataManager;
    @Inject
    private VBoxLayout researcherNameBox;
    @Inject
    private Label<String> widgetTitle;
    @Inject
    private Label<String> researcherTitle;
    @Inject
    private HBoxLayout researcherList;

    @Subscribe
    public void onAfterInit(AfterInitEvent event) {
        initListIteraction();

        setDeafaultTimeInterval();
        setWidgetTitle();
        getResearchersList();

        setIteractionTitle();
        setResearcherList();
    }

    private void setWidgetTitle() {
        String title = "Статистика по рекрутерам за: ";
        DateFormat df = new SimpleDateFormat("dd.MM.yyyy");

        widgetTitle.setValue( "<div style=\"text-transform: uppercase\"><b><u>" +
                title + df.format(startDate) + " - " +
                df.format(endDate) + "</u></b></div>" );
        widgetTitle.setStyleName( "widget-header" );
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
            // бокс вертикальный для набора статистики
            VBoxLayout vBox = uiComponents.create(VBoxLayout.class);
            vBox.setWidth( sizeColumn );
            vBox.setAlignment(Component.Alignment.BOTTOM_CENTER);
            vBox.setSpacing( false );

            boxWidgetTitle.add( vBox );
            boxWidgetTitle.expand( vBox );
            // бокс для фона элемента
            VBoxLayout titleBox = uiComponents.create( VBoxLayout.class );
            titleBox.setWidth( "100%" );
            titleBox.setHeightFull();
            titleBox.setSpacing( false );
            titleBox.setStyleName( "v-caption-label");

            vBox.add( titleBox );
            vBox.expand( titleBox );
            // заголовок

            Label<String> label = uiComponents.create(Label.TYPE_STRING);
            label.setWidth( "100%" );
            label.setSizeFull();
            label.setValue( a );
            label.setAlignment(Component.Alignment.BOTTOM_CENTER );
            label.setStyleName( "v-caption-label" );

            titleBox.add( label );
            titleBox.expand( label );

            Integer styleCount = 2;

            for( User user : reaearchers ) {
                HBoxLayout boxLayout = uiComponents.create(HBoxLayout.class);
                boxLayout.setWidth( "100%" );
                boxLayout.setStyleName( "v-caption-label" );
                boxLayout.setSpacing( false );

                if( styleCount == 2 )
                    styleCount = 1;
                else
                    styleCount = 2;

                vBox.add( boxLayout );

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

               labelCount.setValue( iteractionCount );
               labelCount.setStyleName( "widget-mountly-interview-table-" + styleCount.toString() );
               labelCount.setAlignment( Component.Alignment.MIDDLE_CENTER );
               labelCount.setWidth( "100%" );
               labelCount.setHeight( labelHeight );

               boxLayout.add( labelCount );
            }
        }
    }

    private void setResearcherList() {
        Integer styleCount = 2;

        for( User a : reaearchers ) {
            if( styleCount == 2 )
                styleCount = 1;
            else
                styleCount = 2;

            HBoxLayout boxLayout = uiComponents.create(HBoxLayout.class);
            boxLayout.setWidthFull();
            boxLayout.setAlignment(Component.Alignment.BOTTOM_LEFT);
            boxLayout.setStyleName( "widget-mountly-interview-table-" + styleCount.toString() );

            Label<String> label = uiComponents.create( Label.TYPE_STRING );
            label.setAlignment( Component.Alignment.MIDDLE_LEFT );
            label.setWidthFull();
            label.setStyleName( "widget-mountly-interview-table-" + styleCount.toString() );
            label.setValue( a.getName() );

            boxLayout.add( label );
            researcherNameBox.add( boxLayout );
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