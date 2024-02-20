package com.company.itpearls.core;

import com.haulmont.reports.entity.Report;

public interface ReportService {
    String NAME = "itpearls_ReportService";

    Report getReport(String reportSystemCode);
}