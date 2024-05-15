package com.company.itpearls.core;

import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.Query;
import com.haulmont.cuba.core.Transaction;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.reports.entity.Report;
import org.springframework.stereotype.Service;

import java.util.List;

@Service(ReportService.NAME)
public class ReportServiceBean implements ReportService {
    @Override
    public String getCVDefaultCode() {
        return "candidateCVdefault";
    }

    @Override
    public Report getDefaultReport() {
        return getReport(getCVDefaultCode()); // TO-DO надо что-то с этим делать. Куда-то в общие настройкк
    }

    @Override
    public Report getReport(String reportSystemCode) {
        StringBuilder sb = new StringBuilder();
        String QUERY_REPORT = "select p from report$Report p where p.code like '%s'";

        sb.append(String.format(QUERY_REPORT, reportSystemCode));

        Report report = null;

        try {
            report = queryOneResult(sb.toString());
        } catch (NullPointerException e) {
            try {
                report = queryOneResult(new StringBuilder()
                        .append(String.format(QUERY_REPORT, getCVDefaultCode()))
                        .toString());
            } catch (NullPointerException e1) {
                e.printStackTrace();
            }
        }

        if (report == null) {
            try {
                report = queryOneResult(new StringBuilder()
                        .append(String.format(QUERY_REPORT, getCVDefaultCode()))
                        .toString());
            } catch (NullPointerException e1) {
                e1.printStackTrace();
            }
        }

        return report;
    }

    @Override
    public List<Report> getReportList() {
        String QUERY_REPORT = "select p from report$Report p";
        Report report;

        return queryOneResult(QUERY_REPORT);
    }

    @Override
    public List<Report> getReportCVTemplateList() {
        String QUERY_REPORT = "select p from report$Report p where p.code like 'templateCV%'";

        return queryOneResult(QUERY_REPORT);
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