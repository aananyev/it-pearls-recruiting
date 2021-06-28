package com.company.itpearls.web.screens.iteraction;

import com.haulmont.cuba.gui.components.DataGrid;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Iteraction;

@UiController("itpearls_Iteraction._tree.browse")
@UiDescriptor("iteraction-tree-browse.xml")
@LookupComponent("iteractionsTable")
@LoadDataBeforeShow
public class IteractionTreeBrowse extends StandardLookup<Iteraction> {
    @Install(to = "iteractionTreeTable.needSendEmail", subject = "columnGenerator")
    private Object iteractionTreeTableNeedSendEmailColumnGenerator(DataGrid.ColumnGeneratorEvent<Iteraction> event) {
        if (event.getItem().getNeedSendLetter() != null) {
            if (event.getItem().getNeedSendLetter()) {
                return CubaIcon.PLUS_CIRCLE;
            } else {
                return CubaIcon.MINUS_CIRCLE;
            }
        } else {
            return CubaIcon.MINUS_CIRCLE;
        }
    }

    @Install(to = "iteractionTreeTable.needSendMemo", subject = "columnGenerator")
    private Object iteractionTreeTableNeedSendMemoColumnGenerator(DataGrid.ColumnGeneratorEvent<Iteraction> event) {
        if (event.getItem().getNeedSendMemo() != null) {
            if (event.getItem().getNeedSendMemo()) {
                return CubaIcon.PLUS_CIRCLE;
            } else {
                return CubaIcon.MINUS_CIRCLE;
            }
        } else {
            return CubaIcon.MINUS_CIRCLE;
        }
    }

    @Install(to = "iteractionTreeTable.notification", subject = "columnGenerator")
    private Object iteractionTreeTableNotificationColumnGenerator(DataGrid.ColumnGeneratorEvent<Iteraction> event) {
        if (event.getItem().getNotificationType() != null) {
            if (event.getItem().getNotificationType() == 6) {
                return CubaIcon.PLUS_CIRCLE;
            } else {
                return CubaIcon.MINUS_CIRCLE;
            }
        } else {
            return CubaIcon.MINUS_CIRCLE;
        }
    }

    @Install(to = "iteractionTreeTable.notification", subject = "styleProvider")
    private String iteractionTreeTableNotificationStyleProvider(Iteraction iteraction) {
        String style = "";

        if(iteraction.getNotificationType() != null) {
            if (iteraction.getNotificationType() == 6) {
                style = "open-position-pic-center-large-green";
            } else {
                style = "open-position-pic-center-large-red";
            }
        } else {
            style = "open-position-pic-center-large-red";
        }

        return style;
    }



    @Install(to = "iteractionTreeTable.needSendEmail", subject = "styleProvider")
    private String iteractionTreeTableNeedSendEmailStyleProvider(Iteraction iteraction) {
        String style = "";

        if(iteraction.getNeedSendLetter() != null) {
            if (!iteraction.getNeedSendLetter().equals("")) {
                style = "open-position-pic-center-large-green";
            } else {
                style = "open-position-pic-center-large-red";
            }
        } else {
            style = "open-position-pic-center-large-red";
        }

        return style;
    }

    @Install(to = "iteractionTreeTable.needSendMemo", subject = "styleProvider")
    private String iteractionTreeTableNeedSendMemoStyleProvider(Iteraction iteraction) {
        String style = "";

        if(iteraction.getNeedSendMemo() != null) {
            if (!iteraction.getNeedSendMemo().equals("")) {
                style = "open-position-pic-center-large-green";
            } else {
                style = "open-position-pic-center-large-red";
            }
        } else {
            style = "open-position-pic-center-large-red";
        }

        return style;
    }
}