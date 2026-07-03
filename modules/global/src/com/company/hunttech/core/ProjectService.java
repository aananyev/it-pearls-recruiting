package com.company.hunttech.core;

import com.company.hunttech.entity.Project;

public interface ProjectService {
    String NAME = "hunttech_ProjectService";

    Project getProjectDefault();

    Project createProjectDefault();
}