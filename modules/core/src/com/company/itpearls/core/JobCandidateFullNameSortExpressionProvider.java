package com.company.itpearls.core;

import com.company.itpearls.entity.JobCandidate;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.core.app.DefaultJpqlSortExpressionProvider;

public class JobCandidateFullNameSortExpressionProvider extends DefaultJpqlSortExpressionProvider {
    @Override
    public String getDatatypeSortExpression(MetaPropertyPath metaPropertyPath, boolean sortDirectionAsc) {
        if (metaPropertyPath.getMetaClass().getJavaClass().equals(JobCandidate.class)
                && "fullName".equals(metaPropertyPath.toPathString())) {
//            return String.format("CAST({E}.%s STRING)", metaPropertyPath.toString());
            String s = metaPropertyPath.toString();
            return String.format("%s", metaPropertyPath.toString());
        }
        return String.format("%s", metaPropertyPath.toString());
    }
}
