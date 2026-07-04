package com.company.hunttech.web.screens.project;

import com.company.hunttech.entity.Project;
import com.haulmont.cuba.core.app.DataService;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.KeyValueEntity;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.ValueLoadContext;
import com.haulmont.cuba.security.entity.Group;
import com.haulmont.cuba.web.testsupport.TestContainer;
import com.haulmont.cuba.web.testsupport.TestEntityFactory;
import com.haulmont.cuba.web.testsupport.TestEntityState;
import com.haulmont.cuba.web.testsupport.TestUiEnvironment;
import com.haulmont.cuba.web.testsupport.proxy.DataServiceProxy;
import com.haulmont.cuba.web.testsupport.proxy.TestServiceProxy;
import mockit.Delegate;
import mockit.Expectations;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public final class ProjectPerfTestSupport {

    public static final String ADMIN_LOGIN = "admin";
    public static final String PROJECT_ENTITY = "hunttech_Project";

    private ProjectPerfTestSupport() {
    }

    public static Metrics registerDataServiceDelegate(DataService dataService, TestUiEnvironment environment) {
        initializeUserSession(environment);
        TestContainer testContainer = environment.getContainer();
        DataServiceProxy defaultProxy = new DataServiceProxy(testContainer);
        Metrics metrics = new Metrics();

        new Expectations() {{
            dataService.load((LoadContext<? extends Entity>) any);
            result = new Delegate() {
                @SuppressWarnings("unchecked")
                Entity load(LoadContext<? extends Entity> loadContext) {
                    metrics.loadCalls.incrementAndGet();
                    if (isProject(loadContext)) {
                        metrics.projectLoadCalls.incrementAndGet();
                    }
                    return defaultProxy.load(loadContext);
                }
            };
            minTimes = 0;

            dataService.loadList((LoadContext<? extends Entity>) any);
            result = new Delegate() {
                @SuppressWarnings("unchecked")
                List<Entity> loadList(LoadContext<? extends Entity> loadContext) {
                    metrics.loadListCalls.incrementAndGet();
                    if (isProject(loadContext)) {
                        metrics.projectLoadListCalls.incrementAndGet();
                    }
                    return (List<Entity>) defaultProxy.loadList((LoadContext<Entity>) loadContext);
                }
            };
            minTimes = 0;

            dataService.getCount((LoadContext<? extends Entity>) any);
            result = new Delegate() {
                long getCount(LoadContext<? extends Entity> loadContext) {
                    metrics.getCountCalls.incrementAndGet();
                    if (isProject(loadContext)) {
                        metrics.projectGetCountCalls.incrementAndGet();
                    }
                    return defaultProxy.getCount(loadContext);
                }
            };
            minTimes = 0;

            dataService.loadValues((ValueLoadContext) any);
            result = new Delegate() {
                List<KeyValueEntity> loadValues(ValueLoadContext loadContext) {
                    metrics.loadValuesCalls.incrementAndGet();
                    return defaultProxy.loadValues(loadContext);
                }
            };
            minTimes = 0;

            dataService.commit((CommitContext) any);
            result = new Delegate() {
                Set<Entity> commit(CommitContext commitContext) {
                    return defaultProxy.commit(commitContext);
                }
            };
            minTimes = 0;
        }};

        TestServiceProxy.mock(DataService.class, dataService);
        return metrics;
    }

    public static void clearServiceMocks() {
        TestServiceProxy.clear();
    }

    public static void initializeUserSession(TestUiEnvironment environment) {
        Group group = environment.getContainer()
                .getEntityFactory(Group.class, TestEntityState.NEW)
                .create("name", "Administrators");
        environment.getUserSession().getUser().setGroup(group);
    }

    public static Project createProjectForEdit(TestUiEnvironment environment) {
        TestEntityFactory<Project> factory =
                environment.getContainer().getEntityFactory(Project.class, TestEntityState.NEW);
        Project project = factory.create(
                "projectName", "Perf Project",
                "projectIsClosed", false,
                "defaultProject", false);
        return project;
    }

    private static boolean isProject(LoadContext<? extends Entity> loadContext) {
        return PROJECT_ENTITY.equals(loadContext.getEntityMetaClass());
    }

    public static final class Metrics {
        private final AtomicInteger loadCalls = new AtomicInteger();
        private final AtomicInteger loadListCalls = new AtomicInteger();
        private final AtomicInteger getCountCalls = new AtomicInteger();
        private final AtomicInteger loadValuesCalls = new AtomicInteger();
        private final AtomicInteger projectLoadCalls = new AtomicInteger();
        private final AtomicInteger projectLoadListCalls = new AtomicInteger();
        private final AtomicInteger projectGetCountCalls = new AtomicInteger();

        public int getLoadCalls() {
            return loadCalls.get();
        }

        public int getLoadListCalls() {
            return loadListCalls.get();
        }

        public int getGetCountCalls() {
            return getCountCalls.get();
        }

        public int getLoadValuesCalls() {
            return loadValuesCalls.get();
        }

        public int getProjectLoadCalls() {
            return projectLoadCalls.get();
        }

        public int getProjectLoadListCalls() {
            return projectLoadListCalls.get();
        }

        public int getProjectGetCountCalls() {
            return projectGetCountCalls.get();
        }

        public void reset() {
            loadCalls.set(0);
            loadListCalls.set(0);
            getCountCalls.set(0);
            loadValuesCalls.set(0);
            projectLoadCalls.set(0);
            projectLoadListCalls.set(0);
            projectGetCountCalls.set(0);
        }
    }
}
