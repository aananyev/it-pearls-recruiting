package com.company.itpearls.web.screens.candidatecv;

import com.company.itpearls.entity.CandidateCV;
import com.company.itpearls.web.screens.SelectedCloseAction;
import com.haulmont.cuba.core.app.FileStorageService;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.core.global.FileStorageException;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.MessageBundle;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.gui.upload.FileUploadingAPI;
import com.haulmont.cuba.security.global.UserSession;

import javax.imageio.ImageIO;
import javax.inject.Inject;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

@UiController("itpearls_SelectRenderedImagesFromList")
@UiDescriptor("select-rendered-images-from-list.xml")
public class SelectRenderedImagesFromList extends Screen {
    @Inject
    private UiComponents uiComponents;
    @Inject
    private FlowBoxLayout imagesFlowBox;
    @Inject
    private Button okButton;

    private Image selectedImage = null;
    private HashMap<Image, InputStream> imageStream = new HashMap<>();
    @Inject
    private Metadata metadata;
    @Inject
    private FileStorageService fileStorageService;
    @Inject
    private FileUploadingAPI fileUploadingAPI;
    @Inject
    private UserSession userSession;
    @Inject
    private Notifications notifications;
    @Inject
    private MessageBundle messageBundle;
    @Inject
    private FileLoader fileLoader;
    @Inject
    private DataManager dataManager;
    private CandidateCV candidateCV;

    public HashMap<Image, InputStream> getImageStream() {
        return imageStream;
    }

    public InputStream getSelectedInputStream() {
        if (selectedImage != null) {
            return imageStream.get(selectedImage);
        } else {
            return null;
        }
    }

    private BufferedImage convertRenderedImage(RenderedImage img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }

        ColorModel cm = img.getColorModel();
        int width = img.getWidth();
        int height = img.getHeight();
        WritableRaster raster = cm.createCompatibleWritableRaster(width, height);
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        Hashtable properties = new Hashtable();
        String[] keys = img.getPropertyNames();

        if (keys != null) {
            for (int i = 0; i < keys.length; i++) {
                properties.put(keys[i], img.getProperty(keys[i]));
            }
        }

        BufferedImage result = new BufferedImage(cm, raster, isAlphaPremultiplied, properties);
        img.copyData(raster);
        return result;
    }

    private FileDescriptor selectedImageFileDescriptor;

    public FileDescriptor getSelectedImageFileDescriptor() {
        return selectedImageFileDescriptor;
    }

    public void setCandidateCV(CandidateCV candidateCV) {
        this.candidateCV = candidateCV;
    }

    public void setRenderedImages(List<RenderedImage> images) {
        List<Image> scImages = new ArrayList<>();

        for (RenderedImage image : images) {
            Image screenImage = uiComponents.create(Image.class);
            screenImage.setWidth("100px");
            screenImage.setHeightAuto();
            screenImage.setScaleMode(Image.ScaleMode.SCALE_DOWN);
            screenImage.setStyleName("image-candidate-deselect");
            scImages.add(screenImage);

            BufferedImage bufferedImage = convertRenderedImage(image);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            try {
                ImageIO.write(bufferedImage, "png", baos);
                byte[] bytes = baos.toByteArray();
                InputStream is = new ByteArrayInputStream(bytes);

                screenImage.setSource(StreamResource.class)
                        .setStreamSupplier(() -> is)
                        .setBufferSize(10240);

                screenImage.addClickListener(clickEvent -> {
                    for (Image im : scImages) {
                        im.setStyleName("image-candidate-deselect");
                    }

                    screenImage.setStyleName("image-candidate-select");
                    selectedImage = clickEvent.getSource();

                    Date createDate = new Date();
                    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy-hh.mm.ss");

                    selectedImageFileDescriptor = metadata.create(FileDescriptor.class);
                    selectedImageFileDescriptor.setSize((long) bytes.length);
                    selectedImageFileDescriptor.setCreateDate(createDate);
                    selectedImageFileDescriptor.setName(userSession.getUser()
                            + "-"
                            + sdf.format(createDate));

                    try {
                        fileLoader.saveStream(selectedImageFileDescriptor,
                                () -> new ByteArrayInputStream(bytes));
                    } catch (FileStorageException e) {
                        notifications.create(Notifications.NotificationType.ERROR)
                                .withCaption(messageBundle.getMessage("msgError"))
                                .withDescription(messageBundle.getMessage("msgErrorSaveFile"))
                                .show();

                        e.printStackTrace();
                    }

                    selectedImageFileDescriptor = dataManager.commit(selectedImageFileDescriptor);
                    okButton.setEnabled(true);

                    if (clickEvent.isDoubleClick()) {
                        okButtonInvoke();
                    }
                });

                imagesFlowBox.add(screenImage);
                imageStream.put(screenImage, is);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void cancelButton() {
        closeWithDefaultAction();
    }

    public Image getSelectedImage() {
        return selectedImage;
    }

    public void setSelectedImage(Image selectedImage) {
        this.selectedImage = selectedImage;
    }

    public void okButtonInvoke() {
        close(new SelectedCloseAction(CandidateCV.SelectedCloseActionType.SELECTED));
    }
}