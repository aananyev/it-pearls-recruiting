package com.company.itpearls.core;

import com.haulmont.reports.entity.Report;

import java.util.List;

public interface ReportService {
    String NAME = "itpearls_ReportService";

    Report getReport(String reportSystemCode);

    List<Report> getReportList();

    List<Report> getReportCVTemplateList();
}