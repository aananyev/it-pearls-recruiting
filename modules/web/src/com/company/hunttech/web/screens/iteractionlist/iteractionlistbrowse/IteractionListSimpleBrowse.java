package com.company.hunttech.web.screens.iteractionlist.iteractionlistbrowse;

import com.company.hunttech.core.StarsAndOtherService;
import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.entity.OpenPosition;
import com.company.hunttech.web.screens.iteractionlist.IteractionListEdit;
import com.haulmont.cuba.core.entity.KeyValueEntity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.ViewBuilder;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.DataGrid;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.icons.Icons;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.IteractionList;
import org.jsoup.Jsoup;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@UiController("hunttech_IteractionListSimple.browse")
@UiDescriptor("iteraction-list-simple-browse.xml")
@LookupComponent("iteractionListsTable")
public class IteractionListSimpleBrowse extends StandardLookup<IteractionList> {
    @Inject
    private Button copyLastIteractionButton;
    @Inject
    private CollectionLoader<IteractionList> iteractionListsDl;
    @Inject
    private DataGrid<IteractionList> iteractionListsTable;
    @Inject
    private StarsAndOtherService starsAndOtherService;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private DataManager dataManager;
    @Inject
    private Label<String> vacancyNameLabel;
    @Inject
    private Label<String> recrutierLabel;
    @Inject
    private Label<String> candidateLabel;
    @Inject
    private Label<String> candidatePositionLabel;
    @Inject
    private Label<String> candidatePositionEnLabel;

    private String openPositionStr;
    JobCandidate jobCandidate = null;
    private static final String QUERY_INTERACTIONS_WITH_COMMENT =
            "select e.id from hunttech_IteractionList e where e in :items and e.comment is not null and e.comment <> ''";
    private Set<UUID> interactionsWithComment = Collections.emptySet();
    private Map<UUID, String> commentTooltipCache = Collections.emptyMap();

    public void setJobCandidate(JobCandidate jobCandidate) {
        this.jobCandidate = jobCandidate;
    }

    public JobCandidate getJobCandidate() {
        return jobCandidate;
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        setButtonActions();
        setCandidateLabel(event);
    }

    @Subscribe(id = "iteractionListsDl", target = Target.DATA_LOADER)
    private void onIteractionListsDlPostLoad(CollectionLoader.PostLoadEvent<IteractionList> event) {
        // COMMENT_ is a LOB; cache only its presence for grid icons and load text lazily for tooltip hover.
        refreshCommentPresenceCache(event.getLoadedEntities());
    }

    private void setCandidateLabel(BeforeShowEvent event) {
        if (jobCandidate != null) {
            candidateLabel.setValue(jobCandidate.getFullName());
            if (jobCandidate.getPersonPosition() != null) {
                if (jobCandidate.getPersonPosition().getPositionRuName() != null) {
                    candidatePositionLabel.setValue(jobCandidate.getPersonPosition().getPositionRuName());
                }

                if (jobCandidate.getPersonPosition().getPositionEnName() != null) {
                    candidatePositionEnLabel.setValue(jobCandidate.getPersonPosition().getPositionEnName());
                }
            }
        }
    }

    private void setButtonActions() {
        copyLastIteractionButton.setEnabled(false);

        iteractionListsTable.addSelectionListener(e -> {
            if (e.getSelected() == null) {
                copyLastIteractionButton.setEnabled(false);
            } else {
                copyLastIteractionButton.setEnabled(true);

                if (iteractionListsTable.getSingleSelected() != null) {
                    if (iteractionListsTable.getSingleSelected().getRating() != null) {
                        recrutierLabel.setValue(iteractionListsTable
                                .getSingleSelected()
                                .getRecrutier()
                                .getName());
                        recrutierLabel.setVisible(true);
                    }
                }

                if (iteractionListsTable.getSingleSelected().getVacancy() != null) {
                    vacancyNameLabel.setValue(iteractionListsTable
                            .getSingleSelected()
                            .getVacancy()
                            .getVacansyName());
                    vacancyNameLabel.setVisible(true);
                }
            }
        });
    }

    @Install(to = "iteractionListsTable.rating", subject = "columnGenerator")
    private String iteractionListsTableRatingColumnGenerator(DataGrid.ColumnGeneratorEvent<IteractionList> event) {
        return event.getItem().getRating() != null ? starsAndOtherService.setStars(event.getItem().getRating() + 1) : "";
    }

    public void setSelectedCandidate(JobCandidate entity) {
        // Parent screens set candidate explicitly; avoiding @LoadDataBeforeShow prevents an accidental full-table load.
        iteractionListsDl.setParameter("candidate", entity);
        iteractionListsDl.load();
    }

    private void refreshCommentPresenceCache(List<IteractionList> items) {
        if (items.isEmpty()) {
            interactionsWithComment = Collections.emptySet();
            commentTooltipCache = Collections.emptyMap();
            return;
        }

        List<KeyValueEntity> rows = dataManager.loadValues(QUERY_INTERACTIONS_WITH_COMMENT)
                .properties("id")
                .parameter("items", items)
                .list();

        Set<UUID> ids = new HashSet<>();
        for (KeyValueEntity row : rows) {
            UUID id = row.getValue("id");
            if (id != null) {
                ids.add(id);
            }
        }
        interactionsWithComment = ids;
        commentTooltipCache = new HashMap<>();
    }

    private boolean hasComment(IteractionList iteractionList) {
        return iteractionList != null
                && iteractionList.getId() != null
                && interactionsWithComment.contains(iteractionList.getId());
    }

    private String getCommentTooltip(IteractionList iteractionList) {
        if (!hasComment(iteractionList)) {
            return null;
        }
        return commentTooltipCache.computeIfAbsent(iteractionList.getId(), id -> {
            // Tooltip text is loaded only for the hovered row, keeping the initial grid load LOB-free.
            IteractionList reloaded = dataManager.reload(iteractionList,
                    ViewBuilder.of(IteractionList.class).add("comment").build());
            return reloaded.getComment();
        });
    }

    private String getIcon(IteractionList item) {
        if (item.getIteractionType() != null) {
            return item.getIteractionType().getPic();
        } else {
            return "icons/one1d.png";
        }
    }

    private void addIconColumn() {
        DataGrid.Column iconColumn = iteractionListsTable.addGeneratedColumn("icon",
                new DataGrid.ColumnGenerator<IteractionList, String>() {
                    @Override
                    public String getValue(DataGrid.ColumnGeneratorEvent<IteractionList> event) {
                        return getIcon(event.getItem());
                    }

                    @Override
                    public Class<String> getType() {
                        return String.class;
                    }
                });

        iconColumn.setRenderer(iteractionListsTable.createRenderer(DataGrid.ImageRenderer.class));
    }

    @Subscribe
    public void onInit(InitEvent event) {
        addIconColumn();
    }

    @Install(to = "iteractionListsTable.iteractionType", subject = "descriptionProvider")
    private String iteractionListsTableIteractionTypeDescriptionProvider(IteractionList iteractionList) {
        String comment = getCommentTooltip(iteractionList);
        return Jsoup.parse(comment != null ? comment : "").text()
                + (iteractionList.getAddString() != null ? "\nДополнительно: " + iteractionList.getAddString() : "")
                + (iteractionList.getAddDate() != null ? "\nДата: " + iteractionList.getAddDate().toString() : "");
    }

    @Install(to = "iteractionListsTable.vacancy", subject = "descriptionProvider")
    private String iteractionListsTableVacancyDescriptionProvider(IteractionList iteractionList) {
        String retStr = "";

        if (iteractionList.getVacancy() != null) {
            try {
                retStr = iteractionList.getVacancy().getVacansyName() + "\n" +
                        iteractionList.getVacancy().getProjectName().getProjectName() + "\n" +
                        iteractionList.getVacancy().getProjectName().getProjectDepartment().getDepartamentRuName() + "\n" +
                        iteractionList.getVacancy().getProjectName().getProjectDepartment().getCompanyName().getComanyName();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }

        if (iteractionList.getAddDate() != null) {
            retStr = iteractionList.getAddDate() + "\n" + retStr;
        }

        if (iteractionList.getAddString() != null) {
            retStr = iteractionList.getAddString() + "\n" + retStr;
        }

        if (iteractionList.getAddInteger() != null) {
            retStr = iteractionList.getAddInteger() + "\n" + retStr;
        }

        return retStr;
    }

    public void createNewIteractionButton() {
        screenBuilders.editor(IteractionList.class, this)
                .newEntity()
                .withScreenClass(IteractionListEdit.class)
                .withInitializer(iteractionList1 -> {
                    iteractionList1.setCandidate((JobCandidate) iteractionListsDl.getParameter("candidate"));
                })
                .withAfterCloseListener(e -> {
                    iteractionListsDl.load();
                    iteractionListsTable.repaint();
                })
                .build()
                .show();
    }

    public void copyLastIteractionButton() {
        screenBuilders.editor(IteractionList.class, this)
                .newEntity()
                .withScreenClass(IteractionListEdit.class)
                .withInitializer(iteractionList1 -> {
                    if (iteractionListsTable.getSingleSelected() != null) {
                        iteractionList1.setCandidate(iteractionListsTable.getSingleSelected().getCandidate());
                        iteractionList1.setVacancy(iteractionListsTable.getSingleSelected().getVacancy());
                        iteractionList1.setNumberIteraction(dataManager.loadValue(
                                "select max(e.numberIteraction) " +
                                        "from hunttech_IteractionList e", BigDecimal.class)
                                .one().add(BigDecimal.ONE));
                    }
                })
                .withAfterCloseListener(e -> {
                    iteractionListsDl.load();
                    iteractionListsTable.repaint();
                })
                .build()
                .show();

        iteractionListsDl.load();
    }


    private OpenPosition openPosition;

    public void setOpenPosition(OpenPosition openPosition) {
        this.openPosition = openPosition;
        // Parent screens set vacancy explicitly; load only the scoped interaction history.
        iteractionListsDl.setParameter("vacancy", this.openPosition);
        iteractionListsDl.load();
    }

    public void setOpenPosition(String openPosition) {
        this.openPositionStr = openPosition;
        // String vacancy filter is scoped by caller and avoids opening the full interaction table.
        iteractionListsDl.setParameter("vacancyStr", openPosition);
        iteractionListsDl.load();
    }

    @Install(to = "iteractionListsTable.commentColumn", subject = "columnGenerator")
    private Icons.Icon iteractionListsTableCommentColumnColumnGenerator(DataGrid.ColumnGeneratorEvent<IteractionList> event) {
        return hasComment(event.getItem()) ? CubaIcon.FILE_TEXT : CubaIcon.FILE;
    }

    @Install(to = "iteractionListsTable.commentColumn", subject = "styleProvider")
    private String iteractionListsTableCommentColumnStyleProvider(IteractionList iteractionList) {
        return hasComment(iteractionList) ? "pic-center-large-green" : "pic-center-large-red";
    }

    @Install(to = "iteractionListsTable.commentColumn", subject = "descriptionProvider")
    private String iteractionListsTableCommentColumnDescriptionProvider(IteractionList iteractionList) {
        return getCommentTooltip(iteractionList);
    }

    @Install(to = "iteractionListsTable.currentOpenCloseColumn", subject = "columnGenerator")
    private Icons.Icon iteractionListsTableCurrentOpenCloseColumnColumnGenerator(
            DataGrid.ColumnGeneratorEvent<IteractionList> columnGeneratorEvent) {

        if (columnGeneratorEvent.getItem().getCurrentOpenClose() != null) {
            return columnGeneratorEvent.getItem().getCurrentOpenClose()
                    ? CubaIcon.MINUS_CIRCLE : CubaIcon.PLUS_CIRCLE;
        } else {
            if (columnGeneratorEvent.getItem() != null) {
                if (columnGeneratorEvent.getItem().getVacancy() != null) {
                    if (columnGeneratorEvent.getItem().getVacancy().getOpenClose() != null) {
                        return columnGeneratorEvent.getItem().getVacancy().getOpenClose() ?
                                CubaIcon.MINUS_CIRCLE : CubaIcon.PLUS_CIRCLE;
                    } else {
                        return CubaIcon.MINUS_CIRCLE;
                    }
                } else {
                    return CubaIcon.MINUS_CIRCLE;
                }
            } else {
                return CubaIcon.MINUS_CIRCLE;
            }
        }
    }

    @Install(to = "iteractionListsTable.currentOpenCloseColumn", subject = "styleProvider")
    private String iteractionListsTableCurrentOpenCloseColumnStyleProvider(
            IteractionList iteractionList) {

        if (iteractionList.getCurrentOpenClose() != null) {
            return iteractionList.getCurrentOpenClose()
                    ? "pic-center-large-red" : "pic-center-large-green";
        } else {
            if (iteractionList != null) {
                if (iteractionList.getVacancy() != null) {
                    if (iteractionList.getVacancy().getOpenClose() != null) {
                        return iteractionList.getVacancy().getOpenClose() ?
                                "pic-center-large-red" : "pic-center-large-green";
                    } else {
                        return "pic-center-large-red";
                    }
                } else {
                    return "pic-center-large-red";
                }
            } else {
                return "pic-center-large-red";
            }
        }
    }

    @Install(to = "iteractionListsTable.currentOpenCloseColumn", subject = "descriptionProvider")
    private String iteractionListsTableCurrentOpenCloseColumnDescriptionProvider(IteractionList iteractionList) {
        String retStr;
        final String OPENED_NOW = "Закрыта на текущий момент";
        final String CLOSED_NOW = "Открыта на текущий момент";

            if (iteractionList.getCurrentOpenClose() != null) {
                retStr = iteractionList.getCurrentOpenClose()
                        ? "Закрыта на момент создания взаимодействия" : "Открыта на момент создания взаимодействия";

                if (iteractionList.getVacancy().getOpenClose() != null) {
                    retStr += "\n\n" + (iteractionList.getVacancy().getOpenClose() ?
                            CLOSED_NOW : OPENED_NOW);

                }
            } else {
                if (iteractionList != null) {
                    if (iteractionList.getVacancy() != null) {
                        if (iteractionList.getVacancy().getOpenClose() != null) {
                            retStr = iteractionList.getVacancy().getOpenClose() ?
                                    CLOSED_NOW : OPENED_NOW;
                        } else {
                            retStr = CLOSED_NOW;
                        }
                    } else {
                        retStr = CLOSED_NOW;
                    }
                } else {
                    retStr = CLOSED_NOW;
                }
            }

            return retStr;
    }


}
