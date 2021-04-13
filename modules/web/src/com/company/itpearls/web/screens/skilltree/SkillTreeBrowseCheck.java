package com.company.itpearls.web.screens.skilltree;

import com.haulmont.cuba.gui.components.DataGrid;
import com.haulmont.cuba.gui.components.HasValue;
import com.haulmont.cuba.gui.components.TreeDataGrid;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.SkillTree;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@UiController("itpearls_SkillTreeCheck.browse")
@UiDescriptor("skill-tree-browse-check.xml")
@LookupComponent("skillTreesTable")
@LoadDataBeforeShow
public class SkillTreeBrowseCheck extends StandardLookup<SkillTree> {
    List<SkillTree> candidateCVSkills = new ArrayList<>();
    List<SkillTree> openPositionSkills = new ArrayList<>();
    @Inject
    private TreeDataGrid<SkillTree> skillTreesTable;
    @Inject
    private CollectionContainer<SkillTree> skillTreesDc;
    @Inject
    private CollectionLoader<SkillTree> skillTreesDl;

    public void setCandidateCVSkills(List<SkillTree> candidateCVSkills) {
        this.candidateCVSkills = candidateCVSkills;
    }

    public void setOpenPositionSkills(List<SkillTree> openPositionSkills) {
        this.openPositionSkills = openPositionSkills;
    }

    @Install(to = "skillTreesTable.cvSkills", subject = "columnGenerator")
    private Object skillTreesTableCvSkillsColumnGenerator(DataGrid.ColumnGeneratorEvent<SkillTree> event) {
        if (candidateCVSkills.size() != 0) {
            for (SkillTree s : candidateCVSkills) {
                if (s.equals(event.getItem())) {
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
        if (openPositionSkills.size() != 0) {
            for (SkillTree s : openPositionSkills) {
                if (s.equals(event.getItem())) {
                    return CubaIcon.PLUS_CIRCLE;
                }
            }
            return CubaIcon.MINUS_CIRCLE;
        } else {
            return null;
        }
    }

    @Install(to = "skillTreesTable.cvSkills", subject = "styleProvider")
    private String skillTreesTableCvSkillsStyleProvider(SkillTree skillTree) {
        String style = "";

        if (checkSkills(candidateCVSkills, skillTree)) {
            style = "pic-center-large-green";
        } else {
            style = "pic-center-large-red";
        }

        return style;
    }

    @Install(to = "skillTreesTable.openPosition", subject = "styleProvider")
    private String skillTreesTableOpenPositionStyleProvider(SkillTree skillTree) {
        String style = "";

        if (checkSkills(openPositionSkills, skillTree)) {
            style = "pic-center-large-green";
        } else {
            style = "pic-center-large-red";
        }

        return style;
    }

    Boolean checkSkills(List<SkillTree> skillTrees, SkillTree s) {
        if (skillTrees.size() != 0) {
            for (SkillTree a : skillTrees) {
                if (s.equals(a)) {
                    return true;
                }

                return false;
            }
        }

        return null;
    }

    @Subscribe("removeBlankSkills")
    public void onRemoveBlankSkillsValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (event.getValue() != null) {
            if(event.getValue()) {
                skillTreesDl.setParameter("skillsFromJD", openPositionSkills);
                skillTreesDl.setParameter("skillsFromCV", candidateCVSkills);
            } else {
                skillTreesDl.removeParameter("skillsFromJD");
                skillTreesDl.removeParameter("skillsFromCV");
            }

            skillTreesDl.load();
        }
    }

    @Subscribe("skillsFromCVonly")
    public void onSkillsFromCVonlyValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if(event.getValue() != null) {
            if (event.getValue()) {
                if(openPositionSkills.size() != 0) {
                    skillTreesDl.setParameter("skillsFromJD", openPositionSkills);
                }
            } else {
                skillTreesDl.removeParameter("skillsFromJD");
            }

            skillTreesDl.load();
        }

    }




}