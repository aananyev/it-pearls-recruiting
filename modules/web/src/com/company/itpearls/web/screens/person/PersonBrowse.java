package com.company.itpearls.web.screens.person;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Person;

@UiController("itpearls_Person.browse")
@UiDescriptor("person-browse.xml")
@LookupComponent("personsTable")
@LoadDataBeforeShow
public class PersonBrowse extends StandardLookup<Person> {
}