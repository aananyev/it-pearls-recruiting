package com.company.hunttech.web.screens.filetype;

import com.company.hunttech.entity.FileType;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.screen.*;

import javax.inject.Inject;

@UiController("hunttech_FileType.edit")
@UiDescriptor("file-type-edit.xml")
@EditedEntityContainer("fileTypeDc")
@LoadDataBeforeShow
public class FileTypeEdit extends StandardEditor<FileType> {

    @Inject
    private TextField<String> nameFileTypeField;
    @Inject
    private Button mainNav;

    /**
     * Презентационная навигация: переводит фокус к первому полю «Основных данных»
     * и подсвечивает активный пункт sidebar. Entity, loaders и lifecycle не затрагиваются.
     */
    public void focusMainSection() {
        nameFileTypeField.focus();
        setActiveNavigation(mainNav);
    }

    private void setActiveNavigation(Button activeButton) {
        activeButton.addStyleName("label-nav-item-active");
    }
}
