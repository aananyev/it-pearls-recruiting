package com.company.itpearls.web.screens.person;

import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Person;
import com.haulmont.cuba.gui.screen.LookupComponent;

import javax.inject.Inject;

@UiController("itpearls_Person.browse")
@UiDescriptor("person-browse.xml")
@LookupComponent("personsTable")
@LoadDataBeforeShow
public class PersonBrowse extends StandardLookup<Person> {
    @Inject
    private UiComponents uiComponents;

    @Install(to = "personsTable.personPicColumn", subject = "columnGenerator")
    private Component personsTablePersonPicColumnColumnGenerator(DataGrid.ColumnGeneratorEvent<Person> event) {
        HBoxLayout retBox = uiComponents.create(HBoxLayout.class);
        retBox.setWidthFull();
        retBox.setHeightFull();

        Image image = uiComponents.create(Image.class);
        image.setDescriptionAsHtml(true);
        image.setScaleMode(Image.ScaleMode.SCALE_DOWN);
        image.setWidth("20px");
        image.setHeight("20px");
        image.setStyleName("circle-20px");
        image.setAlignment(Component.Alignment.MIDDLE_CENTER);
        image.setDescription("<h4>"
                + event.getItem().getFirstName()
                + " "
                + event.getItem().getSecondName()
                + "</h4>");

        if (event.getItem().getFileImageFace() != null) {
            image.setSource(FileDescriptorResource.class)
                    .setFileDescriptor(event
                            .getItem()
                            .getFileImageFace());
        } else {
            image.setSource(ThemeResource.class).setPath("icons/no-programmer.jpeg");
        }

        retBox.add(image);
        return retBox;
    }
}