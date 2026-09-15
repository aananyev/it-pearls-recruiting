package com.company.hunttech.web.screens.telegram;

import com.company.hunttech.entity.StdPictures;
import com.company.hunttech.web.util.FileDescriptorImageHelper;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@UiController("hunttech_TelegramPhotoSelectDialog")
@UiDescriptor("telegram-photo-select-dialog.xml")
public class TelegramPhotoSelectDialog extends Screen {

    private static final Logger log = LoggerFactory.getLogger(TelegramPhotoSelectDialog.class);

    @Inject
    private UiComponents uiComponents;
    @Inject
    private FlowBoxLayout photosFlowBox;
    @Inject
    private FileLoader fileLoader;
    @Inject
    private MessageBundle messageBundle;

    private List<FileDescriptor> photos = new ArrayList<>();
    private FileDescriptor selectedPhoto;
    private boolean selectionHandled;

    public void setPhotos(List<FileDescriptor> photos) {
        this.photos = photos != null ? photos : new ArrayList<>();
    }

    public FileDescriptor getSelectedPhoto() {
        return selectedPhoto;
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        populatePhotosGrid();
    }

    private void populatePhotosGrid() {
        photosFlowBox.removeAll();
        if (photos.isEmpty()) {
            log.warn("TelegramPhotoSelectDialog: no photos provided to display");
            Label<String> emptyLabel = uiComponents.create(Label.TYPE_STRING);
            emptyLabel.setValue(messageBundle.getMessage("msgNoPhotos"));
            emptyLabel.setStyleName("telegram-photo-empty-label");
            photosFlowBox.add(emptyLabel);
            return;
        }

        int index = 1;
        for (FileDescriptor fd : photos) {
            VBoxLayout card = uiComponents.create(VBoxLayout.class);
            card.setWidth("132px");
            card.setHeight("150px");
            card.setSpacing(false);
            card.setAlignment(Component.Alignment.MIDDLE_CENTER);
            card.setStyleName("telegram-photo-choice-card");

            Image image = uiComponents.create(Image.class);
            image.setWidth("120px");
            image.setHeight("120px");
            image.setScaleMode(Image.ScaleMode.CONTAIN);
            image.setStyleName("telegram-photo-choice-img");
            String description = messageBundle.formatMessage("msgPhotoChoiceHint", index);
            image.setDescription(description);

            // Безопасная инициализация ресурса с валидацией хранилища и fallback-изображением
            FileDescriptorImageHelper.setImageSource(image, fileLoader, fd, StdPictures.NO_CANDIDATE.getId());

            // Клик по изображению выбирает фото
            image.addClickListener(clickEvent -> selectAndClose(fd));

            // Клик по любой области карточки (отступы, подпись) также выбирает фото
            card.addLayoutClickListener(layoutClickEvent -> selectAndClose(fd));

            Label<String> numLabel = uiComponents.create(Label.TYPE_STRING);
            numLabel.setValue(messageBundle.formatMessage("msgPhotoNum", index));
            numLabel.setStyleName("telegram-photo-card-label");
            numLabel.setAlignment(Component.Alignment.MIDDLE_CENTER);

            card.add(image);
            card.add(numLabel);
            photosFlowBox.add(card);

            index++;
        }
    }

    private void selectAndClose(FileDescriptor fd) {
        if (selectionHandled) {
            return;
        }
        selectionHandled = true;
        log.info("Telegram photo selected by user: fdId={}, name={}",
                fd != null ? fd.getId() : null, fd != null ? fd.getName() : null);
        this.selectedPhoto = fd;
        close(StandardOutcome.SELECT);
    }

    public void onCancelButtonClick() {
        closeWithDefaultAction();
    }
}
