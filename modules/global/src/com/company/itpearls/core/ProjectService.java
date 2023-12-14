package com.company.itpearls.core;

import com.company.itpearls.entity.Project;

public interface ProjectService {
    String NAME = "itpearls_ProjectService";

    Project getProjectDefault();

    Project createProjectDefault();
}