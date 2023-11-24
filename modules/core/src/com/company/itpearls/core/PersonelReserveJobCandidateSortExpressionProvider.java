package com.company.itpearls.core;

import com.company.itpearls.entity.PersonelReserve;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.core.app.DefaultJpqlSortExpressionProvider;

public class PersonelReserveJobCandidateSortExpressionProvider extends DefaultJpqlSortExpressionProvider {
    @Override
    public String getDatatypeSortExpression(MetaPropertyPath metaPropertyPath, boolean sortDirectionAsc) {
        if (metaPropertyPath.getMetaClass().getJavaClass().equals(PersonelReserve.class)
                && "jobCandidate.fullName".equals(metaPropertyPath.toPathString())) {
            return String.format("%s", metaPropertyPath.toString());
        }
        return String.format("%s", metaPropertyPath.toString());
    }
}
