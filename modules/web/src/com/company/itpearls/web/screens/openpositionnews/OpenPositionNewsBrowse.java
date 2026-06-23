package com.company.itpearls.web.screens.openpositionnews;

import com.company.itpearls.entity.OpenPositionNews;
import com.haulmont.cuba.core.entity.KeyValueEntity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;

import javax.inject.Inject;
import java.util.*;

@UiController("itpearls_OpenPositionNews.browse")
@UiDescriptor("open-position-news-browse.xml")
@com.haulmont.cuba.gui.screen.LookupComponent("openPositionNewsDataGrid")
@LoadDataBeforeShow
public class OpenPositionNewsBrowse extends StandardLookup<OpenPositionNews> {
    @Inject
    private DataGrid<OpenPositionNews> openPositionNewsDataGrid;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private DataManager dataManager;
    @Inject
    private CollectionLoader<OpenPositionNews> openPositionNewsCollectionDl;

    private Map<UUID, String> commentCache = Collections.emptyMap();

    @Subscribe(id = "openPositionNewsCollectionDl", target = Target.DATA_LOADER)
    private void onOpenPositionNewsCollectionDlPostLoad(CollectionLoader.PostLoadEvent<OpenPositionNews> event) {
        refreshCommentCache(event.getLoadedEntities());
    }

    private void refreshCommentCache(List<OpenPositionNews> newsList) {
        List<UUID> ids = new ArrayList<>();
        for (OpenPositionNews news : newsList) {
            if (news.getId() != null) {
                ids.add(news.getId());
            }
        }
        if (ids.isEmpty()) {
            commentCache = Collections.emptyMap();
            return;
        }
        Map<UUID, String> cache = new HashMap<>();
        for (KeyValueEntity row : dataManager.loadValues(
                "select e.id, e.comment from itpearls_OpenPositionNews e where e.id in :ids")
                .properties("id", "comment")
                .parameter("ids", ids)
                .list()) {
            UUID id = row.getValue("id");
            if (id != null) {
                cache.put(id, row.getValue("comment"));
            }
        }
        commentCache = cache;
    }

    private String getCachedComment(OpenPositionNews news) {
        return news != null && news.getId() != null ? commentCache.get(news.getId()) : null;
    }

    @Subscribe
    public void onInit(InitEvent event) {
        setOpenPositionNewsDetailsGenerator();
    }

    private void setOpenPositionNewsDetailsGenerator() {
        openPositionNewsDataGrid.setItemClickAction(new BaseAction("itemClickAction")
                .withHandler(actionPerformedEvent -> openPositionNewsDataGrid
                        .setDetailsVisible(openPositionNewsDataGrid.getSingleSelected(), true)));
    }

    @Install(to = "openPositionNewsDataGrid", subject = "detailsGenerator")
    private Component openPositionNewsDataGridDetailsGenerator(OpenPositionNews entity) {
        VBoxLayout mainLayout = uiComponents.create(VBoxLayout.NAME);
        mainLayout.setWidth("100%");
        mainLayout.setMargin(true);

        HBoxLayout headerBox = uiComponents.create(HBoxLayout.NAME);
        headerBox.setWidth("100%");

        Label infoLabel = uiComponents.create(Label.NAME);
        infoLabel.setHtmlEnabled(true);
        infoLabel.setStyleName("h1");
        infoLabel.setValue("News:");

        Component closeButton = createCloseButton(entity);
        headerBox.add(infoLabel);
        headerBox.add(closeButton);
        headerBox.expand(infoLabel);

        Component content = getContent(entity);

        mainLayout.add(headerBox);
        mainLayout.add(content);
        mainLayout.expand(content);

        return mainLayout;
    }

    private Component getContent(OpenPositionNews entity) {
        Label<String> content = uiComponents.create(Label.TYPE_STRING);
        content.setHtmlEnabled(true);
        StringBuilder sb = new StringBuilder();

        sb.append(entity.getDateNews())
                .append("  ")
                .append(entity.getAuthor().getName())
                .append("<tr>")
                .append("<tr>")
                .append(getCachedComment(entity));

        content.setValue(sb.toString());
        return content;
    }

    private Component createCloseButton(OpenPositionNews entity) {
        Button closeButton = uiComponents.create(Button.class);
        closeButton.setIcon("icons/close.png");
        BaseAction closeAction = new BaseAction("closeAction")
                .withHandler(actionPerformedEvent ->
                        openPositionNewsDataGrid.setDetailsVisible(entity, false))
                .withCaption("");
        closeButton.setAction(closeAction);
        return closeButton;
    }

    @Install(to = "openPositionNewsDataGrid.openPosition", subject = "descriptionProvider")
    private String openPositionNewsDataGridOpenPositionDescriptionProvider(OpenPositionNews openPositionNews) {
        return openPositionNews.getOpenPosition().getVacansyName();
    }

    @Install(to = "openPositionNewsDataGrid.comment", subject = "columnGenerator")
    private String openPositionNewsDataGridCommentColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPositionNews> event) {
        String comment = getCachedComment(event.getItem());
        if (comment == null || comment.isEmpty()) {
            return "";
        }
        return comment.length() > 120 ? comment.substring(0, 120) + "…" : comment;
    }

    @Install(to = "openPositionNewsDataGrid.comment", subject = "descriptionProvider")
    private String openPositionNewsDataGridCommentDescriptionProvider(OpenPositionNews openPositionNews) {
        return getCachedComment(openPositionNews);
    }
}
