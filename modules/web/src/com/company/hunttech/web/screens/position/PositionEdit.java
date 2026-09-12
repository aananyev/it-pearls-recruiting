package com.company.hunttech.web.screens.position;

import com.company.hunttech.entity.Position;
import com.company.hunttech.gui.components.OvalImage;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.FileDescriptorResource;
import com.haulmont.cuba.gui.components.StreamResource;
import com.haulmont.cuba.gui.components.ThemeResource;
import com.haulmont.cuba.gui.model.InstanceContainer;
import com.haulmont.cuba.gui.screen.*;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

@UiController("hunttech_Position.edit")
@UiDescriptor("position-edit.xml")
@EditedEntityContainer("positionDc")
@LoadDataBeforeShow
public class PositionEdit extends StandardEditor<Position> {
    private static final Logger log = LoggerFactory.getLogger(PositionEdit.class);

    @Inject
    private OvalImage positionLogoImage;
    @Inject
    private FileLoader fileLoader;
    @Inject
    private Label<String> textPositionName;
    @Inject
    private TextField<String> positionEnNameField;
    @Inject
    private TextField<String> positionRuNameField;
    @Inject
    private RichTextArea standartDescriptionTextArea;
    @Inject
    private Button mainNav;
    @Inject
    private Button descriptionNav;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        setLabel();
    }

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        updatePositionLogoImage();
    }

    @Subscribe(id = "positionDc", target = Target.DATA_CONTAINER)
    public void onPositionDcItemPropertyChange(InstanceContainer.ItemPropertyChangeEvent<Position> event) {
        if ("filePositionIcon".equals(event.getProperty())) {
            Position position = getEditedEntity();
            if (position != null && event.getValue() == null) {
                position.setIconImage(null);
            }
            updatePositionLogoImage();
        } else if ("iconImage".equals(event.getProperty())) {
            updatePositionLogoImage();
        }
    }

    @Subscribe("positionIconUpload")
    public void onPositionIconUploadFileUploadSucceed(FileUploadField.FileUploadSucceedEvent event) {
        Position position = getEditedEntity();
        if (position == null) {
            updatePositionLogoImage();
            return;
        }
        FileDescriptor fd = position.getFilePositionIcon();
        if (fd != null) {
            try (InputStream is = fileLoader.openStream(fd)) {
                if (is != null) {
                    byte[] bytes = IOUtils.toByteArray(is);
                    if (bytes != null && bytes.length > 0) {
                        position.setIconImage(bytes);
                    }
                }
            } catch (Exception ex) {
                log.warn("Не удалось синхронизировать файл пиктограммы в BLOB iconImage: {}", ex.getMessage());
            }
        }
        updatePositionLogoImage();
    }

    private void updatePositionLogoImage() {
        if (positionLogoImage == null) {
            return;
        }
        Position position = getEditedEntity();
        if (position == null) {
            positionLogoImage.setSource(ThemeResource.class).setPath("icons/dictionaries/position.png");
            return;
        }
        byte[] iconBytes = position.getIconImage();
        if (iconBytes != null && iconBytes.length > 0) {
            positionLogoImage.setSource(StreamResource.class)
                    .setStreamSupplier(() -> new ByteArrayInputStream(iconBytes));
        } else if (position.getFilePositionIcon() != null) {
            positionLogoImage.setSource(FileDescriptorResource.class).setFileDescriptor(position.getFilePositionIcon());
        } else {
            positionLogoImage.setSource(ThemeResource.class).setPath("icons/dictionaries/position.png");
        }
    }

    @Subscribe("positionEnNameField")
    public void onPositionEnNameFieldTextChange(TextInputField.TextChangeEvent event) {
        String en = event.getText();
        String ru = positionRuNameField.getValue();
        updateLabelText(en, ru);
    }

    @Subscribe("positionRuNameField")
    public void onPositionRuNameFieldTextChange(TextInputField.TextChangeEvent event) {
        String ru = event.getText();
        String en = positionEnNameField.getValue();
        updateLabelText(en, ru);
    }

    private void setLabel() {
        String en = positionEnNameField.getValue();
        String ru = positionRuNameField.getValue();
        updateLabelText(en, ru);
    }

    private void updateLabelText(String en, String ru) {
        if (textPositionName == null) {
            return;
        }
        if (en != null && !en.trim().isEmpty() && ru != null && !ru.trim().isEmpty()) {
            textPositionName.setValue(en.trim() + " — " + ru.trim());
        } else if (ru != null && !ru.trim().isEmpty()) {
            textPositionName.setValue(ru.trim());
        } else if (en != null && !en.trim().isEmpty()) {
            textPositionName.setValue(en.trim());
        } else {
            textPositionName.setValue("");
        }
    }

    /**
     * Презентационная навигация: переводит фокус к русскому наименованию должности
     * и подсвечивает активный пункт sidebar.
     */
    public void focusMainSection() {
        positionRuNameField.focus();
        setActiveNavigation(mainNav);
    }

    /**
     * Презентационная навигация: переводит фокус к редактору общего описания
     * и подсвечивает активный пункт sidebar.
     */
    public void focusDescriptionSection() {
        standartDescriptionTextArea.focus();
        setActiveNavigation(descriptionNav);
    }

    private void setActiveNavigation(Button activeButton) {
        if (mainNav != null) {
            mainNav.removeStyleName("label-nav-item-active");
        }
        if (descriptionNav != null) {
            descriptionNav.removeStyleName("label-nav-item-active");
        }
        if (activeButton != null) {
            activeButton.addStyleName("label-nav-item-active");
        }
    }
}
