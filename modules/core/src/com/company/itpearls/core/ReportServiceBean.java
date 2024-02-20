package com.company.itpearls.core;

import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.Query;
import com.haulmont.cuba.core.Transaction;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.reports.entity.Report;
import org.springframework.stereotype.Service;

@Service(ReportService.NAME)
public class ReportServiceBean implements ReportService {
    @Override
    public Report getReport(String reportSystemCode) {
        StringBuilder sb = new StringBuilder();
        String QUERY_REPORT = "select p from report$Report p where p.code = '%s'";

        sb.append(String.format(QUERY_REPORT, reportSystemCode));

        return queryOneResult(sb.toString());
    }

    private static <T> T queryOneResult(String queryStr) {
        Object retObj = null;

        try {
            Persistence persistence = AppBeans.get(Persistence.class);
            try (Transaction tx = persistence.createTransaction()) {
                // get EntityManager for the current transaction
                EntityManager em = persistence.getEntityManager();
                Query query = em.createQuery(queryStr);

                retObj = (T) query.getFirstResult();
                tx.commit();
            }

        } finally {
            return (T) retObj;
        }
    }

}