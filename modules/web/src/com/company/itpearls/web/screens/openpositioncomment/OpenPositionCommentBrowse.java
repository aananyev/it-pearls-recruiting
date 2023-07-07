package com.company.itpearls.web.screens.openpositioncomment;

import com.company.itpearls.core.StarsAndOtherService;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.DataGrid;
import com.haulmont.cuba.gui.components.HBoxLayout;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.OpenPositionComment;

import javax.inject.Inject;

@UiController("itpearls_OpenPositionComment.browse")
@UiDescriptor("open-position-comment-browse.xml")
@LookupComponent("openPositionCommentsTable")
@LoadDataBeforeShow
public class OpenPositionCommentBrowse extends StandardLookup<OpenPositionComment> {
    @Inject
    private StarsAndOtherService starsAndOtherService;
    @Inject
    private UiComponents uiComponents;

    @Install(to = "openPositionCommentsTable.rating", subject = "columnGenerator")
    private Component openPositionCommentsTableRatingColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPositionComment> event) {
        String labelText = event.getItem().getRating() != null
                ? starsAndOtherService.setStars(event.getItem().getRating() + 1) : "";

        HBoxLayout retBox = uiComponents.create(HBoxLayout.class);
        retBox.setWidthFull();
        retBox.setHeightFull();

        Label retLabel = uiComponents.create(Label.class);
        retLabel.setAlignment(Component.Alignment.MIDDLE_CENTER);
        retLabel.setValue(labelText);

        retBox.add(retLabel);

        return retBox;
    }
}