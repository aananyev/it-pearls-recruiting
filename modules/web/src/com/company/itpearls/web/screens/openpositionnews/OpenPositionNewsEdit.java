package com.company.itpearls.web.screens.openpositionnews;

import com.company.itpearls.entity.OpenPosition;
import com.haulmont.cuba.gui.components.DateField;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.OpenPositionNews;

import javax.inject.Inject;
import java.util.Date;

@UiController("itpearls_OpenPositionNews.edit")
@UiDescriptor("open-position-news-edit.xml")
@EditedEntityContainer("openPositionNewsDc")
@LoadDataBeforeShow
public class OpenPositionNewsEdit extends StandardEditor<OpenPositionNews> {
    @Inject
    private DateField<Date> dateNewsField;

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        dateNewsField.setValue(new Date());
    }

    @Install(to = "openPositionField", subject = "optionIconProvider")
    private String openPositionFieldOptionIconProvider(OpenPosition openPosition) {
        if (!openPosition.getOpenClose()) {
            return CubaIcon.PLUS_CIRCLE.source();
        } else {
            return CubaIcon.MINUS_CIRCLE.source();
        }
    }
}