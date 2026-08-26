package com.company.hunttech.web.screens.about;

import com.company.hunttech.core.ApplicationSetupService;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.*;

import javax.inject.Inject;

@UiController("hunttech_AboutHrmScreen")
@UiDescriptor("about-hrm-screen.xml")
public class AboutHrmScreen extends Screen {

    @Inject
    private Configuration configuration;
    @Inject
    private ApplicationSetupService applicationSetupService;
    @Inject
    private Label<String> versionLabel;
    @Inject
    private Label<String> buildDateLabel;
    @Inject
    private Label<String> descriptionLabel;
    @Inject
    private Label<String> authorLabel;
    @Inject
    private Image logoImage;

    @Subscribe
    protected void onInit(InitEvent event) {
        // Версия из build.gradle (cuba.artifact.version)
        String version = "0.315"; // актуальная версия из build.gradle

        versionLabel.setValue("Версия: " + version);

        // Дата сборки — показываем версию
        buildDateLabel.setValue("Сборка: " + version);

        // Описание HRM
        descriptionLabel.setValue(
                "HRM HuntTech — система управления рекрутингом и аутстаффингом " +
                "для IT-компаний: вакансии, кандидаты, проекты, компании, взаимодействия, " +
                "AI-автоматизация и аналитика."
        );

        // Автор
        authorLabel.setValue(
                "Разработчик: ООО «Ханттек» (hunttech.ru) | " +
                "Технологии: CUBA Platform 7.3, Vaadin 8, PostgreSQL"
        );

        // Логотип из темы
        logoImage.setSource(ThemeResource.class).setPath("branding/HuntTech.png");
    }

    @Subscribe("closeBtn")
    protected void onCloseBtnClick(Button.ClickEvent event) {
        closeWithDefaultAction();
    }
}