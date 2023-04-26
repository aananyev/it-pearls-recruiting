package com.company.itpearls.web.screens.iteractionlist.iteractionlistbrowse;

import com.company.itpearls.core.StarsAndOtherService;
import com.company.itpearls.entity.JobCandidate;
import com.company.itpearls.entity.OpenPosition;
import com.company.itpearls.web.screens.iteractionlist.IteractionListEdit;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.DataGrid;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.icons.Icons;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.IteractionList;
import org.jsoup.Jsoup;

import javax.inject.Inject;
import java.math.BigDecimal;

@UiController("itpearls_IteractionListSimple.browse")
@UiDescriptor("iteraction-list-simple-browse.xml")
@LookupComponent("iteractionListsTable")
@LoadDataBeforeShow
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
    private CollectionContainer<IteractionList> iteractionListsDc;
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
        iteractionListsDl.setParameter("candidate", entity);
        iteractionListsDl.load();
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
        return Jsoup.parse(iteractionList.getComment() != null ? iteractionList.getComment() : "").text()
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
                                        "from itpearls_IteractionList e", BigDecimal.class)
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
        iteractionListsDl.setParameter("vacancy", this.openPosition);
        iteractionListsDl.load();
    }

    public void setOpenPosition(String openPosition) {
        this.openPositionStr = openPosition;
        iteractionListsDl.setParameter("vacancyStr", openPosition);
        iteractionListsDl.load();
    }

    @Install(to = "iteractionListsTable.commentColumn", subject = "columnGenerator")
    private Icons.Icon iteractionListsTableCommentColumnColumnGenerator(DataGrid.ColumnGeneratorEvent<IteractionList> event) {
        return (event.getItem().getComment() == null || event.getItem().getComment().equals("")) ?
                CubaIcon.FILE : CubaIcon.FILE_TEXT;
    }

    @Install(to = "iteractionListsTable.commentColumn", subject = "styleProvider")
    private String iteractionListsTableCommentColumnStyleProvider(IteractionList iteractionList) {
        return iteractionList.getComment() != null && !iteractionList.getComment().equals("") ? "pic-center-large-green" : "pic-center-large-red";
    }

    @Install(to = "iteractionListsTable.commentColumn", subject = "descriptionProvider")
    private String iteractionListsTableCommentColumnDescriptionProvider(IteractionList iteractionList) {
        if (iteractionList.getComment() != null) {
            return !iteractionList.getComment().equals("") ?
                    iteractionList.getComment() : null;
        } else {
            return null;
        }
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