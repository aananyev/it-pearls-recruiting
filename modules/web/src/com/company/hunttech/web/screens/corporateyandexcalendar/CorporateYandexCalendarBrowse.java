package com.company.hunttech.web.screens.corporateyandexcalendar;

import com.company.hunttech.entity.CorporateYandexCalendar;
import com.haulmont.cuba.gui.screen.*;

@UiController("hunttech_CorporateYandexCalendar.browse")
@UiDescriptor("corporate-yandex-calendar-browse.xml")
@LookupComponent("calendarsTable")
@LoadDataBeforeShow
public class CorporateYandexCalendarBrowse extends StandardLookup<CorporateYandexCalendar> {
}
