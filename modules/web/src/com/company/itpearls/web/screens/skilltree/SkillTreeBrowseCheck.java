package com.company.itpearls.web.screens.skilltree;

import com.haulmont.cuba.gui.components.DataGrid;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.SkillTree;

import java.util.ArrayList;
import java.util.List;

@UiController("itpearls_SkillTreeCheck.browse")
@UiDescriptor("skill-tree-browse-check.xml")
@LookupComponent("skillTreesTable")
@LoadDataBeforeShow
public class SkillTreeBrowseCheck extends StandardLookup<SkillTree> {
    List<SkillTree> candidateCVSkills = new ArrayList<>();
    List<SkillTree> openPositionSkills = new ArrayList<>();

    public void setCandidateCVSkills(List<SkillTree> candidateCVSkills) {
        this.candidateCVSkills = candidateCVSkills;
    }

    public void setOpenPositionSkills(List<SkillTree> openPositionSkills) {
        this.openPositionSkills = openPositionSkills;
    }

    @Install(to = "skillTreesTable.cvSkills", subject = "columnGenerator")
    private Object skillTreesTableCvSkillsColumnGenerator(DataGrid.ColumnGeneratorEvent<SkillTree> event) {
        if(candidateCVSkills.size() != 0) {
            for (SkillTree s : candidateCVSkills) {
                if(s.equals(event.getItem())) {
                    return CubaIcon.PLUS_CIRCLE;
                }
            }

            return CubaIcon.MINUS_CIRCLE;
        } else {
            return null;
        }
    }

    @Install(to = "skillTreesTable.openPosition", subject = "columnGenerator")
    private Object skillTreesTableOpenPositionColumnGenerator(DataGrid.ColumnGeneratorEvent<SkillTree> event) {
        if(openPositionSkills.size() != 0) {
            for (SkillTree s : openPositionSkills) {
                if(s.equals(event.getItem())) {
                    return CubaIcon.PLUS_CIRCLE;
                }
            }
            return CubaIcon.MINUS_CIRCLE;
        } else {
            return null;
        }
    }


}