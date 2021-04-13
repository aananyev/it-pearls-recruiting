package com.company.itpearls.web.screens.skilltree;

import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.SkillTree;
import com.haulmont.cuba.gui.screen.LookupComponent;
import org.jsoup.Jsoup;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@UiController("itpearls_SkillTreeCheck.browse")
@UiDescriptor("skill-tree-browse-check.xml")
@LookupComponent("skillTreesTable")
@LoadDataBeforeShow
public class SkillTreeBrowseCheck extends StandardLookup<SkillTree> {
    List<SkillTree> candidateCVSkills = new ArrayList<>();
    List<SkillTree> openPositionSkills = new ArrayList<>();
    String title = "";

    @Inject
    private TreeDataGrid<SkillTree> skillTreesTable;
    @Inject
    private CollectionContainer<SkillTree> skillTreesDc;
    @Inject
    private CollectionLoader<SkillTree> skillTreesDl;
    @Inject
    private CheckBox removeBlankSkills;
    @Inject
    private Label<String> header;
    @Inject
    private Label<String> percent;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        if (title != null) {
            header.setValue(title);
        }

        if (candidateCVSkills.size() != 0 && openPositionSkills.size() != 0) {
            int percentInt = (openPositionSkills.size() > 0 ?
                    (candidateCVSkills.size() *100 / openPositionSkills.size()) : 0);
            percent.setValue("Процент релевантности: " + percentInt + "%");
        }
    }


    public void setCandidateCVSkills(List<SkillTree> candidateCVSkills) {
        this.candidateCVSkills = candidateCVSkills;
    }

    public void setOpenPositionSkills(List<SkillTree> openPositionSkills) {
        this.openPositionSkills = openPositionSkills;
    }

    public void setTitle(String title) {
        this.title = title;
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
        if (candidateCVSkills.size() != 0) {
            for (SkillTree s : candidateCVSkills) {
                if (s.equals(skillTree)) {
                    return "pic-center-large-green";
                }
            }

        }

        return "pic-center-large-red";
    }

    @Install(to = "skillTreesTable.openPosition", subject = "styleProvider")
    private String skillTreesTableOpenPositionStyleProvider(SkillTree skillTree) {
        if (openPositionSkills.size() != 0) {
            for (SkillTree s : openPositionSkills) {
                if (s.equals(skillTree)) {
                    return "pic-center-large-green";
                }
            }

        }

        return "pic-center-large-red";
    }

    @Install(to = "skillTreesTable", subject = "rowDescriptionProvider")
    private String skillTreesTableRowDescriptionProvider(SkillTree skillTree) {
        return skillTree.getComment() != null ? Jsoup.parse(skillTree.getComment()).text() : "";
    }

    Boolean checkSkills(List<SkillTree> skillTrees, SkillTree s) {
        if (skillTrees.size() != 0) {
            for (SkillTree a : skillTrees) {
                if (s.getSkillName().equals(a.getSkillName())) {
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
            if (event.getValue()) {
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
        if (event.getValue() != null) {
            if (event.getValue()) {
                if (openPositionSkills.size() != 0) {
                    skillTreesDl.setParameter("skillsFromJD", openPositionSkills);
                }
            } else {
                skillTreesDl.removeParameter("skillsFromJD");
            }

            skillTreesDl.load();
        }
    }

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        removeBlankSkills.setValue(true);
    }
}