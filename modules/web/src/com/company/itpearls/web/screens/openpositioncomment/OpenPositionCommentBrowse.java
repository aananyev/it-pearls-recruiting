package com.company.itpearls.web.screens.openpositioncomment;

import com.company.itpearls.core.StarsAndOtherService;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.DataGrid;
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

    @Install(to = "openPositionCommentsTable.rating", subject = "columnGenerator")
    private String openPositionCommentsTableRatingColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPositionComment> event) {
        return event.getItem().getRating() != null
                ? starsAndOtherService.setStars(event.getItem().getRating() + 1) : "";
    }
}