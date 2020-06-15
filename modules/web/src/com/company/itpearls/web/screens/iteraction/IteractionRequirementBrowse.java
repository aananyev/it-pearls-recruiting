package com.company.itpearls.web.screens.iteraction;

import com.company.itpearls.entity.ItearctionRequirements;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.gui.components.DataGrid;
import com.haulmont.cuba.gui.components.Table;
import com.haulmont.cuba.gui.components.TreeDataGrid;
import com.haulmont.cuba.gui.components.TreeTable;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Iteraction;

import javax.inject.Inject;
import java.util.List;

@UiController("itpearls_IteractionRequirement.browse")
@UiDescriptor("iteraction-requirement-browse.xml")
@LookupComponent("iteractionsTable")
@LoadDataBeforeShow
public class IteractionRequirementBrowse extends StandardLookup<Iteraction> {
    @Inject
    private CollectionLoader<ItearctionRequirements> iteractionRequirementDl;
    @Inject
    private CollectionContainer<ItearctionRequirements> iteractionRequirementDc;
    @Inject
    private CollectionContainer<Iteraction> iteractionsDc;
    @Inject
    private Metadata metadata;
    @Inject
    private DataManager dataManager;
    @Inject
    private TreeDataGrid<Iteraction> iteractionsTable;
    @Inject
    private DataGrid<ItearctionRequirements> iteractionRequirementsTable;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        setRequirement();
    }

    private void setRequirement() {
        Iteraction iteraction = iteractionsTable.getSingleSelected();

        if( iteraction != null ) {
            iteractionRequirementDl.setParameter("iteraction", iteraction);
            iteractionRequirementDl.load();

            List<ItearctionRequirements> iteractionsList = iteractionRequirementDc.getItems();

            if (iteractionsList.size() == 0) {
                for (Iteraction a : iteractionsDc.getItems()) {
                    if(a.getIteractionTree() != null ) {
                        ItearctionRequirements itearctionRequirements = metadata.create(ItearctionRequirements.class);

                        itearctionRequirements.setRequirement(false);
                        itearctionRequirements.setIteraction(iteraction);
                        itearctionRequirements.setIteractionRequirement(a);

                        dataManager.commit(itearctionRequirements);
                    }
                }
            }
        }
    }

    @Subscribe("iteractionsTable")
    public void onIteractionsTableSelection(DataGrid.SelectionEvent<Iteraction> event) {
        setRequirement();
    }
}