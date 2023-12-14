package com.company.itpearls.core;

import com.company.itpearls.entity.Project;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service(ProjectService.NAME)
public class ProjectServiceBean implements ProjectService {
    @Inject
    private DataManager dataManager;

    final static String DEFAULT_PROJECT = "Default";
    @Inject
    private Metadata metadata;

    @Override
    public Project getProjectDefault() {
        Project project = null;

        try {
            project = dataManager
                    .loadValue("select e from itpearls_Project e where e.defaultProject = true",
                            Project.class)
                    .one();
        } catch (Exception e) {
            e.printStackTrace();
            project = createProjectDefault();
        } finally {
            return project;
        }
    }

    @Override
    public Project createProjectDefault() {
        Project project = metadata.create(Project.class);
        project.setDefaultProject(true);
        project.setProjectName(DEFAULT_PROJECT);
        project.setProjectDescription(DEFAULT_PROJECT);

        dataManager.commit(project);

        return project;
    }

}