package com.company.itpearls.web;

import com.haulmont.cuba.gui.model.BaseCollectionLoader;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.Sorter;
import com.haulmont.cuba.gui.model.SorterFactory;
import org.springframework.lang.Nullable;

public class PersonelReserveJobCandidateFullNameSorterFactory extends SorterFactory {

    @Override
    public Sorter createCollectionContainerSorter(CollectionContainer container,
                                                  @Nullable BaseCollectionLoader loader) {
        return new PersonelReserveJobCandidateFullNameCollectionContainerSorter(container, loader);
    }
}
