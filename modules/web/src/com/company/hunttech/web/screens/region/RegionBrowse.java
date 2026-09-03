package com.company.hunttech.web.screens.region;

import com.company.hunttech.entity.Region;
import com.company.hunttech.service.GeoBulkLoaderService;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.executors.BackgroundTask;
import com.haulmont.cuba.gui.executors.BackgroundWorker;
import com.haulmont.cuba.gui.executors.TaskLifeCycle;
import com.haulmont.cuba.gui.screen.*;

import javax.inject.Inject;

@UiController("hunttech_Region.browse")
@UiDescriptor("region-browse.xml")
@LookupComponent("regionsTable")
@LoadDataBeforeShow
public class RegionBrowse extends StandardLookup<Region> {

    @Inject
    private Button loadRegionsRussiaBtn;

    @Inject
    private GeoBulkLoaderService geoBulkLoaderService;

    @Inject
    private Notifications notifications;

    @Inject
    private BackgroundWorker backgroundWorker;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RegionBrowse.class);

    @Subscribe("loadRegionsRussiaBtn")
    public void onLoadRegionsRussiaBtnClick(Button.ClickEvent event) {
        loadRegionsRussiaBtn.setEnabled(false);
        notifications.create(Notifications.NotificationType.HUMANIZED)
                .withCaption("Загрузка регионов России")
                .withDescription("Начинаем загрузку всех субъектов РФ. Это может занять 1-3 минуты (скачивание гербов).")
                .show();

        BackgroundTask<Long, String> task = new BackgroundTask<Long, String>(30 * 60, this) {
            @Override
            public String run(TaskLifeCycle<Long> taskLifeCycle) {
                return geoBulkLoaderService.loadAllRegionsForRussia();
            }

            @Override
            public void done(String summary) {
                loadRegionsRussiaBtn.setEnabled(true);
                notifications.create(Notifications.NotificationType.HUMANIZED)
                        .withCaption("Загрузка регионов завершена")
                        .withDescription(summary)
                        .show();
                try {
                    getScreenData().loadAll();
                } catch (Exception e) {
                    log.warn("Не удалось обновить таблицу регионов: {}", e.getMessage());
                }
            }

            @Override
            public void canceled() {
                loadRegionsRussiaBtn.setEnabled(true);
            }
        };

        try {
            backgroundWorker.handle(task).execute();
        } catch (Exception e) {
            loadRegionsRussiaBtn.setEnabled(true);
            log.error("Ошибка запуска фоновой загрузки регионов", e);
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption("Ошибка загрузки регионов")
                    .withDescription(e.getMessage())
                    .show();
        }
    }
}