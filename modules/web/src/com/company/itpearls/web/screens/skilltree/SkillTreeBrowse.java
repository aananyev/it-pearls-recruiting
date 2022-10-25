package com.company.itpearls.web.screens.skilltree;

import com.company.itpearls.web.StandartPrioritySkills;
import com.haulmont.cuba.core.global.MessageTools;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.SkillTree;
import com.haulmont.cuba.gui.screen.LookupComponent;
import org.jsoup.Jsoup;

import javax.inject.Inject;

@UiController("itpearls_SkillTree.browse")
@UiDescriptor("skill-tree-browse.xml")
@LookupComponent("skillTreesTable")
@LoadDataBeforeShow
public class SkillTreeBrowse extends StandardLookup<SkillTree> {
    @Inject
    private LookupField columnSelector;
    @Inject
    private MessageTools messageTools;
    @Inject
    private GroupTable<SkillTree> skillTreesTable;
    @Inject
    private UiComponents uiComponents;

    @Subscribe
    protected void onInit(InitEvent event) {
//        initColumnSelector();
    }

    @Install(to = "skillTreesTable.wikiPage", subject = "columnGenerator")
    private Component skillTreesTableWikiPageColumnGenerator(SkillTree skillTree) {
        Link retLink = uiComponents.create(Link.NAME);

        if (skillTree.getWikiPage() != null) {
            retLink.setUrl(skillTree.getWikiPage());
//            retLink.setCaption(skillTree.getSkillName());
            retLink.setCaption(skillTree.getWikiPage());
            retLink.setTarget("_blank");
            retLink.setIcon("icons/chain.png");
        }

        return retLink;
    }

    @Install(to = "skillTreesTable.prioritySkill", subject = "columnGenerator")
    private Component skillTreesTablePrioritySkillColumnGenerator(SkillTree skillTree) {
        Label retLabel = uiComponents.create(Label.NAME);

        if (skillTree.getPrioritySkill() != null) {
            switch (skillTree.getPrioritySkill()) {
                case -1:
                    retLabel.setValue(StandartPrioritySkills.NOT_USED_SKILLS_STR);
                    retLabel.setStyleName(StandartPrioritySkills.NOT_USED_SKILLS_STYLE);
                    break;
                case 0:
                    retLabel.setValue(StandartPrioritySkills.DEFAULT_STR);
                    retLabel.setStyleName(StandartPrioritySkills.DEFAULT_STYLE);
                    break;
                case 1:
                    retLabel.setValue(StandartPrioritySkills.SUBJECT_AREA_STR);
                    retLabel.setStyleName(StandartPrioritySkills.SUBJECT_AREA_STYLE);
                    break;
                case 2:
                    retLabel.setValue(StandartPrioritySkills.FRAMEWORKS_STR);
                    retLabel.setStyleName(StandartPrioritySkills.FRAMEWORKS_STYLE);
                    break;
                case 3:
                    retLabel.setValue(StandartPrioritySkills.METHODOLOGY_STR);
                    retLabel.setStyleName(StandartPrioritySkills.METHODOLORY_STYLE);
                    break;
                case 4:
                    retLabel.setValue(StandartPrioritySkills.PROGRAMMING_LANGUAGE_STR);
                    retLabel.setStyleName(StandartPrioritySkills.PROGRAMMING_LANGUAGE_STYLE);
                    break;
                default:
                    retLabel.setValue(StandartPrioritySkills.NOT_USED_SKILLS_STR);
                    retLabel.setStyleName(StandartPrioritySkills.NOT_USED_SKILLS_STYLE);
                    break;
            }
        } else {
            retLabel.setValue("Not defined");
            retLabel.setStyleName(StandartPrioritySkills.NOT_USED_SKILLS_STYLE);
        }

        return retLabel;
    }

/*    private void initColumnSelector() {
        List<DataGrid.Column<SkillTree>> columns = skillTreesTable.getColumns();
        Map<String, String> columnsMap = columns.stream()
                .collect(Collectors.toMap(
                        column -> {
                            MetaPropertyPath propertyPath = column.getPropertyPath();
                            return propertyPath != null
                                    ? messageTools.getPropertyCaption(propertyPath.getMetaProperty())
                                    : column.getId();
                        },
                        DataGrid.Column::getId,
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new));
        columnSelector.setOptionsMap(columnsMap);

        columnSelector.setValue(columns.get(0).getId());
    } */


/*    @Install(to = "skillTreesTable.skillName", subject = "descriptionProvider")
//    private String skillTreesTableSkillNameDescriptionProvider(SkillTree skillTree) {
//        return skillTree.getComment() != null ? Jsoup.parse(skillTree.getComment()).text() : "";
//    }

    @Install(to = "skillTreesTable.isComment", subject = "columnGenerator")
    private Object skillTreesTableIsCommentColumnGenerator(DataGrid.ColumnGeneratorEvent<SkillTree> event) {
        if (event.getItem().getComment() != null && !event.getItem().equals("")) {
            return CubaIcon.FILE_TEXT;
        } else {
            return CubaIcon.FILE;
        }
    }

    @Install(to = "skillTreesTable.isComment", subject = "styleProvider")
    private String skillTreesTableIsCommentStyleProvider(SkillTree skillTree) {
        if (skillTree.getComment() != null && !skillTree.equals("")) {
            return "pic-center-large-green";
        } else {
            return "pic-center-large-red";
        }
    } */
}