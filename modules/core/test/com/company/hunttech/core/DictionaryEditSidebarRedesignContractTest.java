package com.company.hunttech.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Защищает редизайн простых Edit-форм «Справочников» (Specialisation, Ownershup,
 * Position) по общему контракту: sidebar с статичным ovalImage-логотипом 176×176,
 * label-навигация и неизменённые CUBA data/action-контракты.
 */
public class DictionaryEditSidebarRedesignContractTest {

    @Test
    public void specialisationEditHasSidebarNavigationAndPreservesBindings() throws IOException {
        String xml = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/specialisation/specialisation-edit.xml");
        String java = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/specialisation/SpecialisationEdit.java");

        // Контрактный sidebar: visual-блок, ovalImage 176×176, identity, навигация, hint.
        assertTrue(xml.contains("stylename=\"edit-sidebar\""));
        assertTrue(xml.contains("id=\"specialisationLogoImage\""));
        assertTrue(xml.contains("width=\"176px\""));
        assertTrue(xml.contains("<theme path=\"icons/dictionaries/specialisation.png\"/>"));
        assertTrue(xml.contains("stylename=\"label-navigation\""));
        assertTrue(xml.contains("id=\"specialisationNav\""));
        assertTrue(xml.contains("id=\"candidatesNav\""));
        assertTrue(xml.contains("stylename=\"edit-sidebar-hint\""));

        // Канон серии sidebar 2026-08-14: подпись типа записи удалена из identity
        // (остаётся только живое название по центру), как в ProjectEdit/гео-формах.
        assertFalse(xml.contains("stylename=\"edit-sidebar-subtitle\""));
        assertTrue(xml.contains("id=\"sidebarTitle\""));

        // SCSS-канон specialisation (dictionary-edit-forms.scss, 7 тем): отступы
        // контента sidebar 14/16/12 + border-right + тень, название по центру,
        // тонкий скроллбар при переполнении.
        String scss = readProjectFile(
                "modules/web/themes/hover/com.company.hunttech/dictionary-edit-forms.scss");
        assertTrue(scss.contains(".specialisation-editor .edit-sidebar {"));
        assertTrue(scss.contains("padding: 14px 16px 12px !important;"));
        assertTrue(scss.contains("border-right: 1px solid rgba(15, 23, 42, 0.78) !important;"));
        assertTrue(scss.contains("box-shadow: 5px 0 20px rgba(15, 23, 42, 0.18) !important;"));
        assertTrue(scss.contains(".specialisation-editor .edit-sidebar-title,"));
        assertTrue(scss.contains("text-align: center !important;"));
        assertTrue(scss.contains("scrollbar-width: thin;"));
        for (String theme : new String[]{"halo", "havana", "helium", "hover",
                "hunttech-modern", "hunttech-modern-light", "hunttech-modern-dark"}) {
            String t = readProjectFile("modules/web/themes/" + theme
                    + "/com.company.hunttech/dictionary-edit-forms.scss");
            assertTrue(theme + ": dictionary-edit-forms.scss не идентичен hover", scss.equals(t));
        }

        // Редизайн не должен менять bindings, loader и framework actions.
        assertTrue(xml.contains("id=\"specialisationDc\""));
        assertTrue(xml.contains("class=\"com.company.hunttech.entity.Specialisation\""));
        assertTrue(xml.contains("view extends=\"specialisation-view\""));
        assertTrue(xml.contains("id=\"specialisationCandidatesDc\" property=\"candidate\""));
        assertTrue(xml.contains("property=\"specRuName\""));
        assertTrue(xml.contains("id=\"specialisationCandidateTable\""));
        assertTrue(xml.contains("action=\"windowCommitAndClose\""));
        assertTrue(xml.contains("action=\"windowClose\""));

        // Навигация по вкладкам TabSheet.
        assertTrue(java.contains("initTabNavigation()"));
        assertTrue(java.contains("tabSheet.setSelectedTab(tabName)"));
        assertTrue(java.contains("addStyleName(ACTIVE_NAV_STYLE)"));
        assertTrue(java.contains("removeStyleName(ACTIVE_NAV_STYLE)"));
    }

    @Test
    public void ownershupEditHasSidebarAndPreservesBindings() throws IOException {
        String xml = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/ownershup/ownershup-edit.xml");
        String java = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/ownershup/OwnershupEdit.java");

        assertTrue(xml.contains("stylename=\"edit-sidebar\""));
        assertTrue(xml.contains("id=\"ownershupLogoImage\""));
        assertTrue(xml.contains("width=\"176px\""));
        assertTrue(xml.contains("<theme path=\"icons/dictionaries/ownershup.png\"/>"));
        assertTrue(xml.contains("id=\"mainNav\""));
        assertTrue(xml.contains("invoke=\"focusMainSection\""));
        assertTrue(xml.contains("stylename=\"edit-sidebar-hint\""));

        // Bindings и actions не изменены.
        assertTrue(xml.contains("id=\"ownershupDc\""));
        assertTrue(xml.contains("class=\"com.company.hunttech.entity.Ownershup\""));
        assertTrue(xml.contains("property=\"shortType\""));
        assertTrue(xml.contains("property=\"longType\""));
        assertTrue(xml.contains("action=\"windowCommitAndClose\""));
        assertTrue(xml.contains("action=\"windowClose\""));

        assertTrue(java.contains("focusMainSection()"));
        assertTrue(java.contains("addStyleName(\"label-nav-item-active\")"));
    }

    @Test
    public void positionEditHasSidebarNavigationAndPreservesBindings() throws IOException {
        String xml = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/position/position-edit.xml");
        String java = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/position/PositionEdit.java");

        assertTrue(xml.contains("stylename=\"edit-sidebar\""));
        assertTrue(xml.contains("id=\"positionLogoImage\""));
        assertTrue(xml.contains("width=\"176px\""));
        assertTrue(xml.contains("<theme path=\"icons/dictionaries/position.png\"/>"));
        assertTrue(xml.contains("id=\"mainNav\""));
        assertTrue(xml.contains("invoke=\"focusMainSection\""));
        assertTrue(xml.contains("id=\"descriptionNav\""));
        assertTrue(xml.contains("invoke=\"focusDescriptionSection\""));

        // Bindings, LOB-поля и Java-контракты контроллера не изменены.
        assertTrue(xml.contains("id=\"positionDc\""));
        assertTrue(xml.contains("class=\"com.company.hunttech.entity.Position\""));
        assertTrue(xml.contains("view extends=\"position-edit-view\""));
        assertTrue(xml.contains("property=\"standartDescription\""));
        assertTrue(xml.contains("property=\"whoIsThisGuy\""));
        assertTrue(xml.contains("id=\"textPositionName\""));
        assertTrue(xml.contains("property=\"positionRuName\""));
        assertTrue(xml.contains("property=\"positionEnName\""));
        assertTrue(xml.contains("id=\"standartDescriptionTextArea\""));
        assertTrue(xml.contains("id=\"whoIsThisGuyTextArea\""));
        assertTrue(xml.contains("action=\"windowCommitAndClose\""));
        assertTrue(xml.contains("action=\"windowClose\""));

        assertTrue(java.contains("textPositionName.setValue"));
        assertTrue(java.contains("focusMainSection()"));
        assertTrue(java.contains("focusDescriptionSection()"));
        assertTrue(java.contains("standartDescriptionTextArea.focus()"));
    }

    private String readProjectFile(String relativePath) throws IOException {
        return new String(
                Files.readAllBytes(projectRoot().resolve(relativePath)),
                StandardCharsets.UTF_8);
    }

    private Path projectRoot() {
        Path root = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("build.gradle"))) {
            root = root.getParent();
        }
        assertNotNull("Не найден корень проекта HRM HuntTech", root);
        return root;
    }
}
