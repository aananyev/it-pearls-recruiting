package com.company.itpearls.web.screens.openpositionnews;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.OpenPositionNews;

@UiController("itpearls_OpenPositionNews.browse")
@UiDescriptor("open-position-news-browse.xml")
@LookupComponent("openPositionNewsTable")
@LoadDataBeforeShow
public class OpenPositionNewsBrowse extends StandardLookup<OpenPositionNews> {
}