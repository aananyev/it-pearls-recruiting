package com.company.itpearls.web.screens.iteractionlist;

import com.company.itpearls.core.StarsAndOtherService;
import com.company.itpearls.entity.JobCandidate;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.DataGrid;
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
    private Button createNewIteractionButton;
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

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        setButtonActions();
    }

    private void setButtonActions() {
        copyLastIteractionButton.setEnabled(false);

        iteractionListsTable.addSelectionListener(e -> {
            if(e.getSelected() == null) {
                copyLastIteractionButton.setEnabled(false);
            } else {
                copyLastIteractionButton.setEnabled(true);
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
        return item.getIteractionType().getPic();
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
        return Jsoup.parse(iteractionList.getComment() != null ? iteractionList.getComment() : "").text();
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
                .build()
                .show();
    }

    public void copyLastIteractionButton() {
        screenBuilders.editor(IteractionList.class, this)
                .newEntity()
                .withScreenClass(IteractionListEdit.class)
                .withInitializer(iteractionList1 -> {
                    iteractionList1.setCandidate(iteractionListsTable.getSingleSelected().getCandidate());

                    BigDecimal maxNumberIteraction = BigDecimal.ZERO;
                    IteractionList lastIteraction = null;

                    for (IteractionList list : iteractionListsDc.getItems()){
                        if (maxNumberIteraction.compareTo(list.getNumberIteraction()) < 0) {
                            maxNumberIteraction = list.getNumberIteraction();
                            lastIteraction = list;
                        }
                    }

                    if (lastIteraction != null) {
                        iteractionList1.setVacancy(lastIteraction.getVacancy());
                        iteractionList1.setNumberIteraction(dataManager.loadValue(
                                "select max(e.numberIteraction) " +
                                        "from itpearls_IteractionList e", BigDecimal.class)
                                .one().add(BigDecimal.ONE));
                    }
                })
                .build()
                .show();
    }
}