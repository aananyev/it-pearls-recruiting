package com.company.hunttech.web.screens.country;

import com.company.hunttech.entity.Country;
import com.company.hunttech.service.GeoBulkLoaderService;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.executors.BackgroundTask;
import com.haulmont.cuba.gui.executors.BackgroundTaskHandler;
import com.haulmont.cuba.gui.executors.BackgroundWorker;
import com.haulmont.cuba.gui.executors.TaskLifeCycle;
import com.haulmont.cuba.gui.screen.*;

import javax.inject.Inject;

@UiController("hunttech_Country.browse")
@UiDescriptor("country-browse.xml")
@LookupComponent("countriesTable")
@LoadDataBeforeShow
public class CountryBrowse extends StandardLookup<Country> {

    @Inject
    private Button loadAllCountriesBtn;

    @Inject
    private GeoBulkLoaderService geoBulkLoaderService;

    @Inject
    private Notifications notifications;

    @Inject
    private BackgroundWorker backgroundWorker;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CountryBrowse.class);

    @Subscribe("loadAllCountriesBtn")
    public void onLoadAllCountriesBtnClick(Button.ClickEvent event) {
        loadAllCountriesBtn.setEnabled(false);
        notifications.create(Notifications.NotificationType.HUMANIZED)
                .withCaption("Загрузка стран")
                .withDescription("Начинаем загрузку всех государств мира. Это может занять 1-3 минуты (скачивание флагов).")
                .show();

        BackgroundTask<Long, String> task = new BackgroundTask<Long, String>(30 * 60, this) {
            @Override
            public String run(TaskLifeCycle<Long> taskLifeCycle) {
                return geoBulkLoaderService.loadAllCountries();
            }

            @Override
            public void done(String summary) {
                loadAllCountriesBtn.setEnabled(true);
                notifications.create(Notifications.NotificationType.HUMANIZED)
                        .withCaption("Загрузка стран завершена")
                        .withDescription(summary)
                        .show();
                // Обновляем таблицу
                try {
                    getScreenData().loadAll();
                } catch (Exception e) {
                    log.warn("Не удалось обновить таблицу стран: {}", e.getMessage());
                }
            }

            @Override
            public void canceled() {
                loadAllCountriesBtn.setEnabled(true);
            }
        };

        try {
            BackgroundTaskHandler<String> handler = backgroundWorker.handle(task);
            handler.execute();
        } catch (Exception e) {
            loadAllCountriesBtn.setEnabled(true);
            log.error("Ошибка запуска фоновой загрузки всех стран", e);
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption("Ошибка загрузки стран")
                    .withDescription(e.getMessage())
                    .show();
        }
    }
}