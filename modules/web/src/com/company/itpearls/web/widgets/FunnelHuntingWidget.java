package com.company.itpearls.web.widgets;

import com.company.itpearls.entity.IteractionList;
import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.addon.dashboard.web.annotation.WidgetParam;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.WindowParam;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.Link;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@UiController("itpearls_FunnelHuntingWidget")
@UiDescriptor("funnel-hunting-widget.xml")
@DashboardWidget( name = "Funnel Hunting Widget" )
public class FunnelHuntingWidget extends ScreenFragment {
    @Inject
    private CollectionLoader<IteractionList> iteractioListDl;
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

    private String ITRKT_NEW_CONTACT = "Новый контакт";
    private String ITRKT_ASSIGN_ITPEARKS_INTERVIEW = "Назначено собеседование с рекрутером IT Pearls";
    private String ITRKT_PREPARE_ITPEARKS_INTERVIEW = "Прошел собеседование с рекрутером IT Pearls";
    private String ITRKT_ASSIGN_TECH_INTERVIEW = "Назначено техническое собеседование";
    private String ITRKT_PREPARE_TECH_INTERVIEW = "Прошел техническое собеседование";
    private String ITRKT_PREPARE_DIRECTOR_INTERVIEW = "Прошел собеседование с Директором";

    @Subscribe
    public void onAfterInit(AfterInitEvent event) {
        initListIteraction();

        researcherTitle.setValue( "Ресерчер" );

        setResearcherList();
    }

    private void setResearcherList() {
        for( String a : listIteractionForCheck ) {

            if( startDate == null || endDate == null ) {
                iteractioListDl.removeParameter( "startDate" );
                iteractioListDl.removeParameter( "endDate" );
            } else {
                iteractioListDl.setParameter( "startDate", startDate );
                iteractioListDl.setParameter( "endDate", endDate );
            }

            iteractioListDl.setParameter( "iteractionType", a );
            iteractioListDl.load();

            Label<String> label = uiComponents.create(Label.TYPE_STRING);
            label.setValue( a );
        }
    }

    private void initListIteraction() {
        listIteractionForCheck.add( ITRKT_NEW_CONTACT );
        listIteractionForCheck.add( ITRKT_ASSIGN_ITPEARKS_INTERVIEW );
        listIteractionForCheck.add( ITRKT_PREPARE_ITPEARKS_INTERVIEW );
        listIteractionForCheck.add( ITRKT_ASSIGN_TECH_INTERVIEW );
        listIteractionForCheck.add( ITRKT_PREPARE_TECH_INTERVIEW );
        listIteractionForCheck.add( ITRKT_PREPARE_DIRECTOR_INTERVIEW );
    }
}