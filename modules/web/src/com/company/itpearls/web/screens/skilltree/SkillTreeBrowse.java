package com.company.itpearls.web.screens.skilltree;

import com.haulmont.cuba.gui.components.DataGrid;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.SkillTree;

@UiController("itpearls_SkillTree.browse")
@UiDescriptor("skill-tree-browse.xml")
@LookupComponent("skillTreesTable")
@LoadDataBeforeShow
public class SkillTreeBrowse extends StandardLookup<SkillTree> {
    @Install(to = "skillTreesTable", subject = "itemDescriptionProvider")
    private String skillTreesTableItemDescriptionProvider(SkillTree skillTree, String string) {
        return skillTree.getComment() != null ? skillTree.getComment() : "";
    }

    @Install(to = "skillTreesTable.isComment", subject = "columnGenerator")
    private Object skillTreesTableIsCommentColumnGenerator(DataGrid.ColumnGeneratorEvent<SkillTree> event) {
        if(event.getItem().getComment() != null && !event.getItem().equals("")) {
            return CubaIcon.PLUS_CIRCLE;
        } else {
            return CubaIcon.MINUS_CIRCLE;
        }
    }

    @Install(to = "skillTreesTable.isComment", subject = "styleProvider")
    private String skillTreesTableIsCommentStyleProvider(SkillTree skillTree) {
        if (skillTree.getComment() != null && !skillTree.equals("")) {
            return "pic-center-large-green";
        } else {
            return "pic-center-large-red";
        }
    }
}