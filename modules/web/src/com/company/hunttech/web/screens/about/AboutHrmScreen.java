package com.company.hunttech.web.screens.about;

import com.company.hunttech.core.ApplicationSetupService;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.*;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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
    private Label<String> changesLabel;
    @Inject
    private TabSheet mainTabSheet;
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

        // Последние изменения (git log)
        loadRecentChanges();

        // Логотип из темы
        logoImage.setSource(ThemeResource.class).setPath("branding/HuntTech.png");
    }

    private void loadRecentChanges() {
        try {
            // Определяем рабочую директорию проекта
            String projectDir = System.getProperty("user.dir");
            if (projectDir == null || !projectDir.contains("hrm-antigravity")) {
                projectDir = "/Users/alekseyananyev/StudioProjects/hrm-antigravity";
            }

            // Получаем последние коммиты от текущей ветки до master
            ProcessBuilder pb = new ProcessBuilder("git", "log", "--oneline", "origin/master..HEAD", "--pretty=format:%h %s");
            pb.directory(new java.io.File(projectDir));
            pb.redirectErrorStream(true);

            Process process = pb.start();
            List<String> commits = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null && !line.trim().isEmpty()) {
                    commits.add(line.trim());
                }
            }

            process.waitFor();

            // Формируем HTML для отображения
            StringBuilder html = new StringBuilder();
            html.append("<b>Последние изменения в текущей ветке (относительно master):</b><br><br>");

            if (commits.isEmpty()) {
                html.append("Нет новых коммитов относительно master.");
            } else {
                html.append("<table style=\"width:100%; font-family:monospace; font-size:12px;\">");
                for (String commit : commits) {
                    String[] parts = commit.split(" ", 2);
                    String hash = parts.length > 0 ? parts[0] : "";
                    String message = parts.length > 1 ? parts[1] : commit;

                    // Определяем тип коммита для иконки/цвета
                    String icon = "📝";
                    String color = "#333";
                    if (message.startsWith("feat")) { icon = "✨"; color = "#2E7D32"; }
                    else if (message.startsWith("fix")) { icon = "🐛"; color = "#C62828"; }
                    else if (message.startsWith("docs")) { icon = "📚"; color = "#1565C0"; }
                    else if (message.startsWith("refactor")) { icon = "♻️"; color = "#6A1B9A"; }
                    else if (message.startsWith("test")) { icon = "✅"; color = "#EF6C00"; }
                    else if (message.startsWith("chore")) { icon = "🔧"; color = "#455A64"; }

                    html.append("<tr>")
                        .append("<td style=\"padding:4px 8px; color:#888;\">").append(hash).append("</td>")
                        .append("<td style=\"padding:4px 8px; color:").append(color).append(";\">")
                        .append(icon).append(" ").append(escapeHtml(message))
                        .append("</td></tr>");
                }
                html.append("</table>");
            }

            // Добавляем информацию о PR
            html.append("<br><hr><br>");
            html.append("<b>PR в GitHub:</b> #183 (WAITING_FOR_HERMES)<br>");
            html.append("<b>Ветка:</b> agent/antigravity-dev<br>");
            html.append("<b>Агенты:</b> Hermes-1 (CI/CD), Hermes-2, antigravity-dev (текущий)");

            changesLabel.setValue(html.toString());

        } catch (Exception e) {
            changesLabel.setValue("<span style=\"color:red;\">Ошибка загрузки изменений: " + escapeHtml(e.getMessage()) + "</span>");
        }
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&")
                   .replace("<", "<")
                   .replace(">", ">")
                   .replace("\"", "&#34;")
                   .replace("'", "'");
    }

    @Subscribe("closeBtn")
    protected void onCloseBtnClick(Button.ClickEvent event) {
        closeWithDefaultAction();
    }
}