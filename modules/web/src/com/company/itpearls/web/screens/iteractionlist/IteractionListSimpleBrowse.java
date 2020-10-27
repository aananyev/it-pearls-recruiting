package com.company.itpearls.web.screens.iteractionlist;

import com.company.itpearls.entity.JobCandidate;
import com.haulmont.cuba.gui.components.DataGrid;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.IteractionList;
import org.jsoup.Jsoup;

import javax.inject.Inject;

@UiController("itpearls_IteractionListSimple.browse")
@UiDescriptor("iteraction-list-simple-browse.xml")
@LookupComponent("iteractionListsTable")
@LoadDataBeforeShow
public class IteractionListSimpleBrowse extends StandardLookup<IteractionList> {
    @Inject
    private CollectionLoader<IteractionList> iteractionListsDl;
    @Inject
    private DataGrid<IteractionList> iteractionListsTable;

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
}