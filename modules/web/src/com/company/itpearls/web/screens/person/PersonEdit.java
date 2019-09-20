package com.company.itpearls.web.screens.person;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Person;

@UiController("itpearls_Person.edit")
@UiDescriptor("person-edit.xml")
@EditedEntityContainer("personDc")
@LoadDataBeforeShow
public class PersonEdit extends StandardEditor<Person> {
}