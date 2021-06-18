package com.company.itpearls.web.screens.position;

import com.haulmont.cuba.gui.components.DataGrid;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.icons.Icons;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Position;
import org.jsoup.Jsoup;

@UiController("itpearls_Position.browse")
@UiDescriptor("position-browse.xml")
@LookupComponent("positionsTable")
@LoadDataBeforeShow
public class PositionBrowse extends StandardLookup<Position> {
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
}