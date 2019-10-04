package com.company.itpearls.web.screens.iteractionlist;

import com.company.itpearls.entity.JobCandidate;
import com.haulmont.cuba.gui.model.CollectionChangeType;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.InstanceContainer;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.IteractionList;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.persistence.internal.sessions.DirectCollectionChangeRecord;

import javax.inject.Inject;
import javax.validation.constraints.Null;
import java.math.BigDecimal;

@UiController("itpearls_IteractionList.edit")
@UiDescriptor("iteraction-list-edit.xml")
@EditedEntityContainer("iteractionListDc")
@LoadDataBeforeShow
public class IteractionListEdit extends StandardEditor<IteractionList> {
    @Inject
    private InstanceContainer<IteractionList> iteractionListDc;

    @Subscribe(id = "iteractionListDc", target = Target.DATA_CONTAINER)

    private void onIteractionListDcItemChange(InstanceContainer.ItemChangeEvent<IteractionList> event) {
// проверить пустое ли поле
        if( getEditedEntity().getNumberIteraction() == 0 )
            getMaxNumberrIteration();
    }

    protected void getMaxNumberrIteration(){
        Integer maxIterationNumber = 0;

//        maxIterationNumber = getEditedEntity().getNumberIteraction().

        getEditedEntity().setNumberIteraction(maxIterationNumber+10000);
    }
    
}