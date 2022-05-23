package com.company.itpearls.web.screens.openpositionnews;

import com.company.itpearls.entity.OpenPosition;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.components.DateField;
import com.haulmont.cuba.gui.components.LookupPickerField;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.model.DataContext;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.OpenPositionNews;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.util.Date;

@UiController("itpearls_OpenPositionNews.edit")
@UiDescriptor("open-position-news-edit.xml")
@EditedEntityContainer("openPositionNewsDc")
@LoadDataBeforeShow
public class OpenPositionNewsEdit extends StandardEditor<OpenPositionNews> {
    @Inject
    private DateField<Date> dateNewsField;
    @Inject
    private LookupPickerField<User> authorLookupPickerField;
    @Inject
    private UserSession userSession;
    @Inject
    private LookupPickerField<OpenPosition> openPositionField;

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        dateNewsField.setValue(new Date());
        authorLookupPickerField.setValue(userSession.getUser());
    }

    @Install(to = "openPositionField", subject = "optionIconProvider")
    private String openPositionFieldOptionIconProvider(OpenPosition openPosition) {
        if (!openPosition.getOpenClose()) {
            return CubaIcon.PLUS_CIRCLE.source();
        } else {
            return CubaIcon.MINUS_CIRCLE.source();
        }
    }

/*    public void setOpenPosition(OpenPosition openPosition) {
        openPositionField.setValue(openPosition);
    } */
}