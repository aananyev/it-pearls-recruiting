package com.company.itpearls.core;

import java.util.Map;

public interface StandartMapsService {
    String NAME = "itpearls_StandartMapsService";

    Map<String, Integer> setPriorityMap();

    Map<String, Integer> setPaymentsTypeMap();

    Map<String, Integer> setResearcherSalaryMap();

    Map<String, Integer> setRecruterSalaryMap();

    Map<String, Integer> setRemoteWorkMap();

    Map<String, Integer> setOnlyOpenedPositionMap();
}