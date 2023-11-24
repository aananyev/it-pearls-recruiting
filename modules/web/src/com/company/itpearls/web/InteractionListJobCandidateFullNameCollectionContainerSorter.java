package com.company.itpearls.web;

import com.company.itpearls.entity.IteractionList;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.Sort;
import com.haulmont.cuba.gui.model.BaseCollectionLoader;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.impl.CollectionContainerSorter;
import com.haulmont.cuba.gui.model.impl.EntityValuesComparator;
import com.sun.istack.Nullable;

import java.util.Comparator;
import java.util.Objects;

public class InteractionListJobCandidateFullNameCollectionContainerSorter  extends CollectionContainerSorter {
    public InteractionListJobCandidateFullNameCollectionContainerSorter(CollectionContainer container, @Nullable BaseCollectionLoader loader) {
        super(container, loader);
    }

    @Override
    protected Comparator<? extends Entity> createComparator(Sort sort, MetaClass metaClass) {
        MetaPropertyPath metaPropertyPath = Objects.requireNonNull(
                metaClass.getPropertyPath(sort.getOrders().get(0).getProperty()));

        if (metaPropertyPath.getMetaClass().getJavaClass().equals(IteractionList.class)
                && "candidate.fullName".equals(metaPropertyPath.toPathString())) {

            boolean isAsc = sort.getOrders().get(0).getDirection() == Sort.Direction.ASC;

            return Comparator.comparing(
                    (IteractionList e) -> e.getCandidate().getFullName() == null
                            ? null : String.valueOf(e.getCandidate().getFullName()),
                    EntityValuesComparator.asc(isAsc));
        }

        return super.createComparator(sort, metaClass);
    }
}
