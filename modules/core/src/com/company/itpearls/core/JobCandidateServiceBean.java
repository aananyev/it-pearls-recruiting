package com.company.itpearls.core;

import com.company.itpearls.entity.IteractionList;
import com.company.itpearls.entity.JobCandidate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service(JobCandidateService.NAME)
public class JobCandidateServiceBean implements JobCandidateService {
    @Override
    public String getLastSalaryExpectations(JobCandidate jobCandidate) {
        List<IteractionList> iteractionListList = jobCandidate.getIteractionList().stream()
                .sorted((iteractionList1, iteractionList2)
                        -> iteractionList2.getNumberIteraction().compareTo(iteractionList1.getNumberIteraction()))
                .collect(Collectors.toList());

        for (IteractionList il : iteractionListList) {
            if (il.getIteractionType().getSignSalary() != null) {
                if (il.getIteractionType().getSignSalary()) {
                    return il.getAddString();
                }
            }
        }

        return null;
    }

}