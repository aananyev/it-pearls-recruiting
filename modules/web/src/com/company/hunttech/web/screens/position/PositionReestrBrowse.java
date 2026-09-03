package com.company.hunttech.web.screens.position;

import com.company.hunttech.entity.Position;
import com.haulmont.cuba.core.entity.KeyValueEntity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.gui.screen.LookupComponent;
import org.jsoup.Jsoup;

import javax.inject.Inject;
import java.util.*;

@UiController("hunttech_PositionReestr.browse")
@UiDescriptor("position-reestr-browse.xml")
@LookupComponent("positionsTable")
@LoadDataBeforeShow
public class PositionReestrBrowse extends StandardLookup<Position> {

    @Inject
    private DataManager dataManager;
    @Inject
    private Messages messages;
    @Inject
    private CollectionContainer<Position> positionsDc;
    @Inject
    private CollectionLoader<Position> positionsDl;
    @Inject
    private GroupTable<Position> positionsTable;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private ScreenBuilders screenBuilders;

    @Inject
    private Label<String> detailTitle;
    @Inject
    private Label<String> detailSubtitle;
    @Inject
    private Label<String> detailLocation;
    @Inject
    private Button openEditCardBtn;
    @Inject
    private Label<String> detailRuName;
    @Inject
    private Label<String> detailEnName;
    @Inject
    private Label<String> detailDescription;
    @Inject
    private Label<String> detailWhoIsThisGuy;

    @Inject
    private Button createPositionBtn;
    @Inject
    private Button editPositionToolbarBtn;
    @Inject
    private Button removePositionToolbarBtn;
    @Inject
    private Button refreshBtn;

    private static class PositionLobData {
        final String standartDescription;
        final String whoIsThisGuy;

        PositionLobData(String standartDescription, String whoIsThisGuy) {
            this.standartDescription = standartDescription;
            this.whoIsThisGuy = whoIsThisGuy;
        }
    }

    private Map<UUID, PositionLobData> lobDataCache = new HashMap<>();

    @Subscribe(id = "positionsDl", target = Target.DATA_LOADER)
    private void onPositionsDlPostLoad(CollectionLoader.PostLoadEvent<Position> event) {
        refreshLobCache(event.getLoadedEntities());
    }

    private void refreshLobCache(List<Position> positions) {
        List<UUID> ids = new ArrayList<>();
        for (Position p : positions) {
            if (p.getId() != null) {
                ids.add(p.getId());
            }
        }
        if (ids.isEmpty()) {
            lobDataCache.clear();
            return;
        }
        Map<UUID, PositionLobData> cache = new HashMap<>();
        for (KeyValueEntity row : dataManager.loadValues(
                "select e.id, e.standartDescription, e.whoIsThisGuy from hunttech_Position e where e.id in :ids")
                .properties("id", "standartDescription", "whoIsThisGuy")
                .parameter("ids", ids)
                .list()) {
            UUID id = row.getValue("id");
            String desc = row.getValue("standartDescription");
            String who = row.getValue("whoIsThisGuy");
            if (id != null) {
                cache.put(id, new PositionLobData(desc, who));
            }
        }
        lobDataCache = cache;
    }

    @Subscribe
    public void onInit(InitEvent event) {
        setupTableColumns();
        setupTableSelection();
        setupToolbarButtons();

        detailTitle.setAlignment(Component.Alignment.MIDDLE_CENTER);
        detailSubtitle.setAlignment(Component.Alignment.MIDDLE_CENTER);
        detailLocation.setAlignment(Component.Alignment.MIDDLE_CENTER);
    }

    private void setupTableColumns() {
        positionsTable.addGeneratedColumn("positionPicColumn", position -> {
            HBoxLayout retBox = uiComponents.create(HBoxLayout.class);
            retBox.setWidthFull();
            retBox.setHeightFull();
            retBox.setAlignment(Component.Alignment.MIDDLE_CENTER);

            Image image = uiComponents.create(Image.class);
            image.setScaleMode(Image.ScaleMode.SCALE_DOWN);
            image.setWidth("24px");
            image.setHeight("24px");
            image.setStyleName("circle-20px");
            image.setAlignment(Component.Alignment.MIDDLE_CENTER);
            image.setSource(ThemeResource.class).setPath("icons/dictionaries/position.png");

            retBox.add(image);
            return retBox;
        });

        positionsTable.addGeneratedColumn("hasDescription", position -> {
            Label<String> label = uiComponents.create(Label.TYPE_STRING);
            PositionLobData data = position != null && position.getId() != null ? lobDataCache.get(position.getId()) : null;
            boolean present = data != null && data.standartDescription != null && !data.standartDescription.trim().isEmpty();
            if (present) {
                label.setValue(messages.getMessage(getClass(), "msgHasDescription"));
                label.setStyleName("bold");
            } else {
                label.setValue(messages.getMessage(getClass(), "msgNoDescription"));
                label.setStyleName("edit-help");
            }
            return label;
        });

        positionsTable.addGeneratedColumn("hasWhoIs", position -> {
            Label<String> label = uiComponents.create(Label.TYPE_STRING);
            PositionLobData data = position != null && position.getId() != null ? lobDataCache.get(position.getId()) : null;
            boolean present = data != null && data.whoIsThisGuy != null && !data.whoIsThisGuy.trim().isEmpty();
            if (present) {
                label.setValue(messages.getMessage(getClass(), "msgHasWhoIs"));
                label.setStyleName("bold");
            } else {
                label.setValue(messages.getMessage(getClass(), "msgNoWhoIs"));
                label.setStyleName("edit-help");
            }
            return label;
        });
    }

    private void setupTableSelection() {
        positionsTable.addSelectionListener(event -> {
            Position singleSelected = positionsTable.getSingleSelected();
            if (singleSelected == null) {
                updateSidebar(null);
                openEditCardBtn.setEnabled(false);
                editPositionToolbarBtn.setEnabled(false);
                removePositionToolbarBtn.setEnabled(!event.getSelected().isEmpty());
            } else {
                updateSidebar(singleSelected);
                openEditCardBtn.setEnabled(true);
                editPositionToolbarBtn.setEnabled(true);
                removePositionToolbarBtn.setEnabled(true);
            }
        });
    }

    private void setupToolbarButtons() {
        createPositionBtn.addClickListener(e -> {
            screenBuilders.editor(positionsTable)
                    .newEntity()
                    .withOpenMode(OpenMode.NEW_TAB)
                    .show();
        });

        editPositionToolbarBtn.addClickListener(e -> openSelectedEditor());
        openEditCardBtn.addClickListener(e -> openSelectedEditor());

        removePositionToolbarBtn.addClickListener(e -> {
            Action removeAction = positionsTable.getAction("remove");
            if (removeAction != null) {
                removeAction.actionPerform(positionsTable);
            }
        });

        refreshBtn.addClickListener(e -> positionsDl.load());
    }

    private void openSelectedEditor() {
        Position selected = positionsTable.getSingleSelected();
        if (selected != null) {
            screenBuilders.editor(positionsTable)
                    .editEntity(selected)
                    .withOpenMode(OpenMode.NEW_TAB)
                    .show();
        }
    }

    private void updateSidebar(Position position) {
        if (position == null) {
            detailTitle.setValue(messages.getMessage(getClass(), "msgSelectPosition"));
            detailSubtitle.setValue("-");
            detailLocation.setValue(messages.getMessage(getClass(), "msgPositionHandbook"));
            detailRuName.setValue("-");
            detailEnName.setValue("-");
            detailDescription.setValue("-");
            detailWhoIsThisGuy.setValue("-");
            return;
        }

        String ruName = position.getPositionRuName() != null ? position.getPositionRuName() : messages.getMessage(getClass(), "msgNoName");
        String enName = position.getPositionEnName() != null ? position.getPositionEnName() : "-";

        detailTitle.setValue(ruName);
        detailSubtitle.setValue(enName);
        detailLocation.setValue(messages.getMessage(getClass(), "msgPositionHandbook"));

        detailRuName.setValue(ruName);
        detailEnName.setValue(enName);

        PositionLobData lobData = position.getId() != null ? lobDataCache.get(position.getId()) : null;

        if (lobData != null && lobData.standartDescription != null && !lobData.standartDescription.trim().isEmpty()) {
            String cleanText = Jsoup.parse(lobData.standartDescription).text();
            detailDescription.setValue(cleanText.length() > 300 ? cleanText.substring(0, 300) + "..." : cleanText);
        } else {
            detailDescription.setValue(messages.getMessage(getClass(), "msgDescriptionNotFilled"));
        }

        if (lobData != null && lobData.whoIsThisGuy != null && !lobData.whoIsThisGuy.trim().isEmpty()) {
            String cleanText = Jsoup.parse(lobData.whoIsThisGuy).text();
            detailWhoIsThisGuy.setValue(cleanText.length() > 300 ? cleanText.substring(0, 300) + "..." : cleanText);
        } else {
            detailWhoIsThisGuy.setValue(messages.getMessage(getClass(), "msgWhoIsThisGuyNotFilled"));
        }
    }
}
