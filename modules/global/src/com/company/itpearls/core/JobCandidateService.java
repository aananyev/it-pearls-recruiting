package com.company.itpearls.core;

import com.company.itpearls.entity.JobCandidate;

public interface JobCandidateService {
    String NAME = "itpearls_JobCandidateService";

    String getLastSalaryExpectations(JobCandidate jobCandidate);
}