package com.company.itpearls.web.screens.position;

import com.company.itpearls.entity.JobCandidate;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.data.value.ContainerValueSource;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.icons.Icons;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Position;
import com.haulmont.cuba.gui.screen.LookupComponent;
import org.jsoup.Jsoup;

import javax.inject.Inject;

@UiController("itpearls_Position.browse")
@UiDescriptor("position-browse.xml")
@LookupComponent("positionsTable")
@LoadDataBeforeShow
public class PositionBrowse extends StandardLookup<Position> {
    @Inject
    private UiComponents uiComponents;
    @Inject
    private DataGrid<Position> positionsTable;

    @Subscribe
    public void onInit(InitEvent event) {
        logoColumnRenderer();
    }

    @Install(to = "positionsTable.standartDescriptionIcon", subject = "columnGenerator")
    private Icons.Icon positionsTableStandartDescriptionIconColumnGenerator(DataGrid.ColumnGeneratorEvent<Position> event) {
        if(event.getItem().getStandartDescription() == null) {
            return CubaIcon.FILE;
        } else
            return CubaIcon.FILE_TEXT;
    }

    @Install(to = "positionsTable.standartDescriptionIcon", subject = "styleProvider")
    private String positionsTableStandartDescriptionIconStyleProvider(Position position) {
        if(position.getStandartDescription() == null) {
            return "open-position-pic-center-large-red";
        } else {
            return "open-position-pic-center-large-green";
        }
    }

    @Install(to = "positionsTable.standartDescriptionIcon", subject = "descriptionProvider")
    private String positionsTableStandartDescriptionIconDescriptionProvider(Position position) {
        if(position.getStandartDescription() != null) {
            return Jsoup.parse(position.getStandartDescription()).text();
        } else {
            return null;
        }
    }

    @Install(to = "positionsTable.whoThisGuyIcon", subject = "columnGenerator")
    private Icons.Icon positionsTableWhoThisGuyIconColumnGenerator(DataGrid.ColumnGeneratorEvent<Position> event) {
        if(event.getItem().getWhoIsThisGuy() == null) {
            return CubaIcon.FILE;
        } else
            return CubaIcon.FILE_TEXT;
    }

    @Install(to = "positionsTable.whoThisGuyIcon", subject = "styleProvider")
    private String positionsTableWhoThisGuyIconStyleProvider(Position position) {
        if(position.getWhoIsThisGuy() == null) {
            return "open-position-pic-center-large-red";
        } else {
            return "open-position-pic-center-large-green";
        }
    }

    @Install(to = "positionsTable.whoThisGuyIcon", subject = "descriptionProvider")
    private String positionsTableWhoThisGuyIconDescriptionProvider(Position position) {
        if(position.getWhoIsThisGuy() != null) {
            return Jsoup.parse(position.getWhoIsThisGuy()).text();
        } else {
            return null;
        }
    }

    private void logoColumnRenderer() {
        positionsTable.addGeneratedColumn("logoColumn", entity -> {
            HBoxLayout hBox = uiComponents.create(HBoxLayout.class);
            Image image = uiComponents.create(Image.NAME);

            if (entity.getItem().getLogo() != null) {
                try {
                    image.setValueSource(new ContainerValueSource<Position, FileDescriptor>(entity.getContainer(),
                            "logo"));

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                image.setSource(ThemeResource.class).setPath("icons/no-programmer.jpeg");
            }

            image.setWidth("30px");
            image.setStyleName("circle-30px-noborder");

            image.setScaleMode(Image.ScaleMode.CONTAIN);
            image.setAlignment(Component.Alignment.MIDDLE_CENTER);

            hBox.setWidthFull();
            hBox.setHeightFull();
            hBox.add(image);

            return hBox;
        });
    }
}