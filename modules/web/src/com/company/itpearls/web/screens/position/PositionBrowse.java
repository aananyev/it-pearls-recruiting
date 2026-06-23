package com.company.itpearls.web.screens.position;

import com.haulmont.cuba.core.entity.KeyValueEntity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.core.global.ViewBuilder;
import com.haulmont.cuba.gui.components.DataGrid;
import com.haulmont.cuba.gui.components.HasValue;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.components.TextInputField;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.icons.Icons;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Position;
import org.jsoup.Jsoup;

import javax.inject.Inject;
import java.util.*;

@UiController("itpearls_Position.browse")
@UiDescriptor("position-browse.xml")
@LookupComponent("positionsTable")
@LoadDataBeforeShow
public class PositionBrowse extends StandardLookup<Position> {
    @Inject
    private DataManager dataManager;
    @Inject
    private CollectionLoader<Position> positionsDl;

    private Map<UUID, String> standartDescriptionCache = Collections.emptyMap();
    private Map<UUID, String> whoIsThisGuyCache = Collections.emptyMap();

    @Subscribe(id = "positionsDl", target = Target.DATA_LOADER)
    private void onPositionsDlPostLoad(CollectionLoader.PostLoadEvent<Position> event) {
        refreshLobCaches(event.getLoadedEntities());
    }

    private void refreshLobCaches(List<Position> positions) {
        List<UUID> ids = new ArrayList<>();
        for (Position position : positions) {
            if (position.getId() != null) {
                ids.add(position.getId());
            }
        }
        if (ids.isEmpty()) {
            standartDescriptionCache = Collections.emptyMap();
            whoIsThisGuyCache = Collections.emptyMap();
            return;
        }
        Map<UUID, String> descriptions = new HashMap<>();
        Map<UUID, String> whoIs = new HashMap<>();
        for (KeyValueEntity row : dataManager.loadValues(
                "select e.id, e.standartDescription, e.whoIsThisGuy from itpearls_Position e where e.id in :ids")
                .properties("id", "standartDescription", "whoIsThisGuy")
                .parameter("ids", ids)
                .list()) {
            UUID id = row.getValue("id");
            if (id != null) {
                descriptions.put(id, row.getValue("standartDescription"));
                whoIs.put(id, row.getValue("whoIsThisGuy"));
            }
        }
        standartDescriptionCache = descriptions;
        whoIsThisGuyCache = whoIs;
    }

    private String getCachedStandartDescription(Position position) {
        return position != null && position.getId() != null
                ? standartDescriptionCache.get(position.getId()) : null;
    }

    private String getCachedWhoIsThisGuy(Position position) {
        return position != null && position.getId() != null
                ? whoIsThisGuyCache.get(position.getId()) : null;
    }

    @Install(to = "positionsTable.standartDescriptionIcon", subject = "columnGenerator")
    private Icons.Icon positionsTableStandartDescriptionIconColumnGenerator(DataGrid.ColumnGeneratorEvent<Position> event) {
        if (getCachedStandartDescription(event.getItem()) == null) {
            return CubaIcon.FILE;
        } else
            return CubaIcon.FILE_TEXT;
    }

    @Install(to = "positionsTable.standartDescriptionIcon", subject = "styleProvider")
    private String positionsTableStandartDescriptionIconStyleProvider(Position position) {
        if (getCachedStandartDescription(position) == null) {
            return "open-position-pic-center-large-red";
        } else {
            return "open-position-pic-center-large-green";
        }
    }

    @Install(to = "positionsTable.standartDescriptionIcon", subject = "descriptionProvider")
    private String positionsTableStandartDescriptionIconDescriptionProvider(Position position) {
        String description = getCachedStandartDescription(position);
        if (description != null) {
            return Jsoup.parse(description).text();
        } else {
            return null;
        }
    }

    @Install(to = "positionsTable.whoThisGuyIcon", subject = "columnGenerator")
    private Icons.Icon positionsTableWhoThisGuyIconColumnGenerator(DataGrid.ColumnGeneratorEvent<Position> event) {
        if (getCachedWhoIsThisGuy(event.getItem()) == null) {
            return CubaIcon.FILE;
        } else
            return CubaIcon.FILE_TEXT;
    }

    @Install(to = "positionsTable.whoThisGuyIcon", subject = "styleProvider")
    private String positionsTableWhoThisGuyIconStyleProvider(Position position) {
        if (getCachedWhoIsThisGuy(position) == null) {
            return "open-position-pic-center-large-red";
        } else {
            return "open-position-pic-center-large-green";
        }
    }

    @Install(to = "positionsTable.whoThisGuyIcon", subject = "descriptionProvider")
    private String positionsTableWhoThisGuyIconDescriptionProvider(Position position) {
        String description = getCachedWhoIsThisGuy(position);
        if (description != null) {
            return Jsoup.parse(description).text();
        } else {
            return null;
        }
    }
}