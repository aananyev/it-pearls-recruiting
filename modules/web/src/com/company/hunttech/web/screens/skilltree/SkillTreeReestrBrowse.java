package com.company.hunttech.web.screens.skilltree;

import com.company.hunttech.entity.SkillTree;
import com.hunttech.hrm.gui.components.OvaFallbackImage;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.FileDescriptorResource;
import com.haulmont.cuba.gui.components.FlowBoxLayout;
import com.haulmont.cuba.gui.components.ThemeResource;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.gui.screen.LookupComponent;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;

/**
 * Реестр компетенций (SkillTreeReestrBrowse) — двухпанельный экран по контракту HRM реестров:
 * слева сайдбар 312px с деталями выбранного узла, справа TreeTable иерархии SkillTree.
 * Все геттеры, используемые при заполнении сайдбара, задекларированы во view
 * skillTree-reestr-browse-view (Data View Integrity).
 */
@UiController("hunttech_SkillTreeReestr.browse")
@UiDescriptor("skill-tree-reestr-browse.xml")
@LookupComponent("skillTreesTreeTable")
@LoadDataBeforeShow
public class SkillTreeReestrBrowse extends StandardLookup<SkillTree> {

    @Inject
    private CollectionContainer<SkillTree> skillTreesDc;
    @Inject
    private CollectionLoader<SkillTree> skillTreesDl;
    @Inject
    private Metadata metadata;
    @Inject
    private TreeDataGrid<SkillTree> skillTreesTreeTable;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private ScreenBuilders screenBuilders;

    @Inject
    private OvaFallbackImage skillLogoPic;
    @Inject
    private OvaFallbackImage detailLogoImage;
    @Inject
    private Label<String> detailSkillName;
    @Inject
    private Label<String> detailSpecialisation;
    @Inject
    private Label<String> detailParentSkill;
    @Inject
    private Label<String> detailPriority;
    @Inject
    private Button openEditCardBtn;
    @Inject
    private Button createChildBtn;
    @Inject
    private Label<String> detailSkillNameVal;
    @Inject
    private Label<String> detailSpecialisationVal;
    @Inject
    private Label<String> detailParentSkillVal;
    @Inject
    private Label<String> detailPriorityVal;
    @Inject
    private Label<String> detailWikiPageVal;
    @Inject
    private Label<String> detailStyleHighlightingVal;
    @Inject
    private Label<String> detailNotParsingVal;
    @Inject
    private Label<String> detailComment;
    @Inject
    private FlowBoxLayout childrenSkillsChips;

    @Subscribe
    public void onInit(InitEvent event) {
        detailSkillName.setAlignment(Component.Alignment.MIDDLE_CENTER);
        detailSpecialisation.setAlignment(Component.Alignment.MIDDLE_CENTER);
        detailParentSkill.setAlignment(Component.Alignment.MIDDLE_CENTER);
        detailPriority.setAlignment(Component.Alignment.MIDDLE_CENTER);

        setupTableColumns();
        setupTableSelection();
        setupSidebarButtons();
    }

    private void setupTableColumns() {
        // Превью логотипа навыка в колонке «Логотип» (по эталону CityReestrBrowse).
        skillTreesTreeTable.addGeneratedColumn("skillLogoColumn", event -> {
            SkillTree skill = event.getItem();
            HBoxLayout box = uiComponents.create(HBoxLayout.class);
            box.setWidthFull();
            box.setHeightFull();
            box.setAlignment(Component.Alignment.MIDDLE_CENTER);

            Image image = uiComponents.create(Image.class);
            image.setScaleMode(Image.ScaleMode.SCALE_DOWN);
            image.setWidth("24px");
            image.setHeight("24px");
            image.setStyleName("icon-no-border-20px");
            image.setAlignment(Component.Alignment.MIDDLE_CENTER);

            if (skill != null && skill.getFileImageLogo() != null) {
                image.setSource(FileDescriptorResource.class).setFileDescriptor(skill.getFileImageLogo());
            } else {
                image.setSource(ThemeResource.class).setPath("icons/no-programmer.jpeg");
            }

            box.add(image);
            return box;
        });
    }

    private void setupTableSelection() {
        skillTreesTreeTable.addSelectionListener(e -> {
            Set<SkillTree> selected = e.getSelected();
            if (selected != null && !selected.isEmpty()) {
                updateSidebarDetails(selected.iterator().next());
            } else {
                clearSidebarDetails();
            }
        });
    }

    private void setupSidebarButtons() {
        openEditCardBtn.addClickListener(e -> {
            SkillTree selected = skillTreesTreeTable.getSingleSelected();
            if (selected != null) {
                screenBuilders.editor(skillTreesTreeTable)
                        .editEntity(selected)
                        .withOpenMode(OpenMode.DIALOG)
                        .show();
            }
        });
        createChildBtn.addClickListener(e -> {
            SkillTree parent = skillTreesTreeTable.getSingleSelected();
            if (parent != null) {
                SkillTree child = metadata.create(SkillTree.class);
                child.setSkillTree(parent);
                // withContainer(skillTreesDc): коммит редактора сам добавит узел в коллекцию
                // и обновит дерево + сайдбар (стандартный механизм CUBA ScreenBuilders).
                screenBuilders.editor(SkillTree.class, this)
                        .newEntity(child)
                        .withContainer(skillTreesDc)
                        .withOpenMode(OpenMode.DIALOG)
                        .show();
            }
        });
    }

    @Subscribe("actionsPopupButton.refreshAction")
    public void onRefreshAction(Action.ActionPerformedEvent event) {
        skillTreesDl.load();
    }

    @Subscribe("actionsPopupButton.excelExportAction")
    public void onExcelExportAction(Action.ActionPerformedEvent event) {
        Action excel = skillTreesTreeTable.getAction("excel");
        if (excel != null) {
            excel.actionPerform(skillTreesTreeTable);
        }
    }

    @Subscribe("actionsPopupButton.expandAllAction")
    public void onExpandAllAction(Action.ActionPerformedEvent event) {
        skillTreesTreeTable.expandAll();
    }

    @Subscribe("actionsPopupButton.collapseAllAction")
    public void onCollapseAllAction(Action.ActionPerformedEvent event) {
        skillTreesTreeTable.collapseAll();
    }

    @Subscribe(id = "skillTreesDl", target = Target.DATA_LOADER)
    private void onSkillTreesDlPostLoad(CollectionLoader.PostLoadEvent<SkillTree> event) {
        SkillTree current = skillTreesTreeTable.getSingleSelected();
        if (current != null) {
            updateSidebarDetails(current);
        } else if (!event.getLoadedEntities().isEmpty()) {
            skillTreesTreeTable.setSelected(event.getLoadedEntities().get(0));
        } else {
            clearSidebarDetails();
        }
    }

    private void updateSidebarDetails(SkillTree skill) {
        openEditCardBtn.setEnabled(true);
        createChildBtn.setEnabled(true);

        // Логотип в шапке профиля и в секции «ЛОГОТИП НАВЫКА»
        if (skill.getFileImageLogo() != null) {
            skillLogoPic.setSource(FileDescriptorResource.class).setFileDescriptor(skill.getFileImageLogo());
            detailLogoImage.setSource(FileDescriptorResource.class).setFileDescriptor(skill.getFileImageLogo());
        } else {
            skillLogoPic.applyFallback();
            detailLogoImage.applyFallback();
        }

        String skillName = nvl(skill.getSkillName(), "Без названия");
        String parentName = skill.getSkillTree() != null ? nvl(skill.getSkillTree().getSkillName(), "-") : "-";
        String specName = skill.getSpecialisation() != null
                ? nvl(skill.getSpecialisation().getSpecRuName(), "-") : "-";
        String priority = skill.getPrioritySkill() != null ? String.valueOf(skill.getPrioritySkill()) : "-";

        // 4 уровня типографики идентичности
        detailSkillName.setValue(escapeHtml(skillName));
        detailSpecialisation.setValue(escapeHtml(specName));
        detailParentSkill.setValue(parentName.equals("-") ? "Корневая компетенция" : "Родитель: " + escapeHtml(parentName));
        detailPriority.setValue(priority.equals("-") ? "-" : "★ Приоритет: " + priority);

        // Реквизиты
        detailSkillNameVal.setValue(escapeHtml(skillName));
        detailSpecialisationVal.setValue(escapeHtml(specName));
        detailParentSkillVal.setValue(escapeHtml(parentName));
        detailPriorityVal.setValue(priority);
        detailWikiPageVal.setValue(safeWikiLinkHtml(skill.getWikiPage()));
        detailStyleHighlightingVal.setValue(nvl(skill.getStyleHighlighting(), "—"));
        detailNotParsingVal.setValue(Boolean.TRUE.equals(skill.getNotParsing()) ? "Да" : "Нет");

        // Описание навыка
        detailComment.setValue(skill.getComment() != null && !skill.getComment().trim().isEmpty()
                ? escapeHtml(skill.getComment().trim()) : "—");

        renderChildrenChips(skill);
    }

    /** Чипы дочерних навыков выбранного узла (из уже загруженной коллекции — без доп. запросов). */
    private void renderChildrenChips(SkillTree skill) {
        childrenSkillsChips.removeAll();
        List<SkillTree> children = skillTreesDc.getItems().stream()
                .filter(s -> s.getSkillTree() != null && s.getSkillTree().getId().equals(skill.getId()))
                .sorted((a, b) -> nullSafeCompare(a.getSkillName(), b.getSkillName()))
                .collect(java.util.stream.Collectors.toList());
        if (children.isEmpty()) {
            Label<String> empty = uiComponents.create(Label.TYPE_STRING);
            empty.setValue("<span style='color: #94a3b8; font-size: 11px;'>Дочерних навыков нет</span>");
            empty.setHtmlEnabled(true);
            childrenSkillsChips.add(empty);
            return;
        }
        for (SkillTree child : children) {
            Label<String> chip = uiComponents.create(Label.TYPE_STRING);
            chip.setValue("<span style='background: rgba(43, 130, 201, 0.12); color: #2b82c9; " +
                    "padding: 3px 8px; border-radius: 4px; font-weight: 600; font-size: 11px; " +
                    "display: inline-block; white-space: normal; word-break: break-word; line-height: 1.35;'>" +
                    escapeHtml(nvl(child.getSkillName(), "Без названия")) + "</span>");
            chip.setHtmlEnabled(true);
            childrenSkillsChips.add(chip);
        }
    }

    /** Ссылка на Wiki только по http/https (защита от javascript:-XSS), target=_blank + rel=noopener. */
    private String safeWikiLinkHtml(String wikiPage) {
        if (wikiPage == null || wikiPage.trim().isEmpty()) {
            return "—";
        }
        String url = wikiPage.trim();
        String lower = url.toLowerCase();
        if (!(lower.startsWith("http://") || lower.startsWith("https://"))) {
            return "<span style='color: #94a3b8; font-size: 11px;'>"
                    + escapeHtml(url) + "</span>";
        }
        return "<a href=\"" + escapeHtml(url) + "\" target=\"_blank\" rel=\"noopener noreferrer\">📖 Открыть Wiki</a>";
    }

    private void clearSidebarDetails() {
        openEditCardBtn.setEnabled(false);
        createChildBtn.setEnabled(false);
        skillLogoPic.applyFallback();
        detailLogoImage.applyFallback();
        detailSkillName.setValue("Выберите компетенцию");
        detailSpecialisation.setValue("—");
        detailParentSkill.setValue("—");
        detailPriority.setValue("—");
        detailSkillNameVal.setValue("—");
        detailSpecialisationVal.setValue("—");
        detailParentSkillVal.setValue("—");
        detailPriorityVal.setValue("—");
        detailWikiPageVal.setValue("—");
        detailStyleHighlightingVal.setValue("—");
        detailNotParsingVal.setValue("—");
        detailComment.setValue("—");
        childrenSkillsChips.removeAll();
    }

    private String nvl(String value, String fallback) {
        return value != null && !value.trim().isEmpty() ? value.trim() : fallback;
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private int nullSafeCompare(String a, String b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        return a.compareToIgnoreCase(b);
    }
}
