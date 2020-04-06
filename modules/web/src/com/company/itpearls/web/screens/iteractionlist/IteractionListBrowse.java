package com.company.itpearls.web.screens.iteractionlist;

import com.company.itpearls.BeanNotificationEvent;
import com.company.itpearls.UiNotificationEvent;
import com.company.itpearls.service.GetRoleService;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.IteractionList;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;
import org.junit.platform.commons.logging.LoggerFactory;
import org.springframework.context.event.EventListener;

import javax.inject.Inject;
import java.util.Collection;
import java.util.logging.Logger;

@UiController("itpearls_IteractionList.browse")
@UiDescriptor("iteraction-list-browse.xml")
@LookupComponent("iteractionListsTable")
@LoadDataBeforeShow
public class IteractionListBrowse extends StandardLookup<IteractionList> {
    @Inject
    private UserSession userSession;
    @Inject
    private CollectionLoader<IteractionList> iteractionListsDl;
    @Inject
    private CheckBox checkBoxShowOnlyMy;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private GroupTable<IteractionList> iteractionListsTable;
    @Inject
    private Button buttonCopy;
    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private Button buttonExcel;
    @Inject
    private GetRoleService getRoleService;
    @Inject
    private Notifications notifications;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        buttonExcel.setVisible( getRoleService.isUserRoles( userSession.getUser(), "Manager" ) );

        if( userSession.getUser().getGroup().getName().equals("Стажер") ) {
            iteractionListsDl.setParameter("userName", "%" + userSession.getUser().getLogin() + "%" );

            checkBoxShowOnlyMy.setValue( true );
            checkBoxShowOnlyMy.setEditable( false );
            iteractionListsDl.load();
        }
    }

    @Subscribe("checkBoxShowOnlyMy")
    public void onCheckBoxShowOnlyMyValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if(checkBoxShowOnlyMy.getValue()) {
           iteractionListsDl.setParameter("userName", "%" + userSession.getUser().getLogin() + "%");
        } else {
            iteractionListsDl.removeParameter("userName");
        }

        iteractionListsDl.load();
    }

    @Install(to = "iteractionListsTable", subject = "iconProvider")
    private String iteractionListsTableIconProvider(IteractionList iteractionList) {
        return iteractionList.getIteractionType().getPic();
    }

    public void onButtonCopyClick() {
        Screen screen = screenBuilders.editor(iteractionListsTable)
                .newEntity()
                .withInitializer( data -> {
                    if( iteractionListsTable.getSingleSelected() != null ) {

                        data.setCandidate( iteractionListsTable.getSingleSelected().getCandidate());
                        data.setVacancy( iteractionListsTable.getSingleSelected().getVacancy() );
                        data.setCompanyDepartment( iteractionListsTable.getSingleSelected().getCompanyDepartment() );
                        data.setProject( iteractionListsTable.getSingleSelected().getProject() );
                    }
                })
                .withScreenClass( IteractionListEdit.class )
                .withLaunchMode( OpenMode.THIS_TAB )
                .build();

                screen.show();
    }

    @Subscribe("iteractionListsTable")
    public void onIteractionListsTableSelection(Table.SelectionEvent<IteractionList> event) {
        if( iteractionListsTable.isFocusable() )
            buttonCopy.setEnabled( true );
        else
            buttonCopy.setEnabled( false );
        
    }

    /* @EventListener
    public void onUiNotificationEvent(UiNotificationEvent event) {
        notifications.create(Notifications.NotificationType.TRAY)
                .withDescription( event.getMessage() )
                .withCaption("INFO")
                .show();
    }

    // screens do not receive non-UI events!
    @EventListener
    public void onBeanNotificationEvent(BeanNotificationEvent event) {
        throw new IllegalStateException("Received " + event);
    }*/
}