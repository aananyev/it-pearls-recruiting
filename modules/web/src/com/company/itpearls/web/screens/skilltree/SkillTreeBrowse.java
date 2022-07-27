package com.company.itpearls.web.screens.skilltree;

import com.haulmont.cuba.core.global.MessageTools;
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

    @Subscribe
    protected void onInit(InitEvent event) {
//        initColumnSelector();
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