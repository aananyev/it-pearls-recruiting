package com.company.itpearls.web.screens.candidatecv;

import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.FlowBoxLayout;
import com.haulmont.cuba.gui.components.Image;
import com.haulmont.cuba.gui.components.StreamResource;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.imageio.ImageIO;
import javax.inject.Inject;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

@UiController("itpearls_SelectRenderedImagesFromList")
@UiDescriptor("select-rendered-images-from-list.xml")
public class SelectRenderedImagesFromList extends Screen {
    @Inject
    private UiComponents uiComponents;
    @Inject
    private FlowBoxLayout imagesFlowBox;

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


    public void setRenderedImages(List<RenderedImage> images) {
        List <Image> scImages = new ArrayList<>();

        for (RenderedImage image : images) {
            Image screenImage = uiComponents.create(Image.class);
            screenImage.setWidth("150px");
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
                });

                imagesFlowBox.add(screenImage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void cancelButton() {
        closeWithDefaultAction();
    }
}