package com.company.itpearls.web.screens.jobcandidate;

import com.company.itpearls.entity.SkillTree;
import com.company.itpearls.web.StandartPrioritySkills;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.JobCandidate;
import com.haulmont.cuba.gui.screen.LookupComponent;
import org.jsoup.Jsoup;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@UiController("itpearls_SkillsFilterJobCandidate.browse")
@UiDescriptor("skills-filter-job-candidate-browse.xml")
@LookupComponent("jobCandidatesTable")
@LoadDataBeforeShow
public class SkillsFilterJobCandidateBrowse extends StandardLookup<JobCandidate> {
    private static final String QUERY_SKILL_TREE_GROUP
            = "select e from itpearls_SkillTree e where e.skillTree is null order by e.skillName";
    private static final String QUERY_SKILL_TREE_ITEM
            = "select e from itpearls_SkillTree e where e.skillTree = :skillGroup";
    private List<SkillTree> skillTreeGroup = new ArrayList<>();
    private HashMap<SkillTree, GroupBoxLayout> skillGroupGroupBoxLayouts = new HashMap<>();

    @Inject
    private DataManager dataManager;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private ScrollBoxLayout allSkillsScrollBox;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        skillTreeGroup = getSkillTreeGroup();
        skillGroupGroupBoxLayouts = setSkillTreesGroupGreoupBoxLayouts();
        addSkillsTreeGroupGroupBoxes();
    }

    private void addSkillsTreeGroupGroupBoxes() {
        for (Map.Entry<SkillTree, GroupBoxLayout> groupBoxLayoutEntry : skillGroupGroupBoxLayouts.entrySet()) {
            addSkillLabels(groupBoxLayoutEntry);
            allSkillsScrollBox.add(groupBoxLayoutEntry.getValue());
        }
    }

    private void addSkillLabels(Map.Entry<SkillTree, GroupBoxLayout> groupBoxLayoutEntry) {
        List<SkillTree> skillTree = dataManager.load(SkillTree.class)
                .query(QUERY_SKILL_TREE_ITEM)
                .parameter("skillGroup", groupBoxLayoutEntry.getKey())
                .list();

        FlowBoxLayout hBoxLayout = uiComponents.create(FlowBoxLayout.class);
        hBoxLayout.setWidthAuto();
        hBoxLayout.setSpacing(true);

        for (SkillTree st : skillTree) {
            Label<String> label = uiComponents.create(Label.class);
            label.setValue(st.getSkillName());
            label.setStyleName(getStyleForSkillPriority(st));
            hBoxLayout.add(label);
        }

        groupBoxLayoutEntry.getValue().add(hBoxLayout);

        if (skillTree.size() == 0) {
            groupBoxLayoutEntry.getValue().setVisible(false);
        } else {
            groupBoxLayoutEntry.getValue().setVisible(true);
        }
    }

    private String getStyleForSkillPriority(SkillTree st) {
        String retStr;

        if (st != null) {
            if (st.getPrioritySkill() != null) {
                switch (st.getPrioritySkill()) {
                    case -1:
//                    retLabel.setValue(StandartPrioritySkills.NOT_USED_SKILLS_STR);
                        retStr = StandartPrioritySkills.NOT_USED_SKILLS_STYLE;
                        break;
                    case 0:
//                    retLabel.setValue(StandartPrioritySkills.DEFAULT_STR);
                        retStr = StandartPrioritySkills.DEFAULT_STYLE;
                        break;
                    case 1:
//                    retLabel.setValue(StandartPrioritySkills.SUBJECT_AREA_STR);
                        retStr = StandartPrioritySkills.SUBJECT_AREA_STYLE;
                        break;
                    case 2:
//                    retLabel.setValue(StandartPrioritySkills.FRAMEWORKS_STR);
                        retStr = StandartPrioritySkills.FRAMEWORKS_STYLE;
                        break;
                    case 3:
//                    retLabel.setValue(StandartPrioritySkills.METHODOLOGY_STR);
                        retStr = StandartPrioritySkills.METHODOLORY_STYLE;
                        break;
                    case 4:
//                    retLabel.setValue(StandartPrioritySkills.PROGRAMMING_LANGUAGE_STR);
                        retStr = StandartPrioritySkills.PROGRAMMING_LANGUAGE_STYLE;
                        break;
                    default:
//                    retLabel.setValue(StandartPrioritySkills.NOT_USED_SKILLS_STR);
                        retStr = StandartPrioritySkills.NOT_USED_SKILLS_STYLE;
                        break;
                }
            } else {
                retStr = StandartPrioritySkills.NOT_USED_SKILLS_STYLE;
            }
        } else {
            retStr = StandartPrioritySkills.NOT_USED_SKILLS_STYLE;
        }

        return retStr;
    }

    private HashMap<SkillTree, GroupBoxLayout> setSkillTreesGroupGreoupBoxLayouts() {
        for (SkillTree skillTree : skillTreeGroup) {
            GroupBoxLayout groupBoxLayout = uiComponents.create(GroupBoxLayout.class);

            groupBoxLayout.setStyleName("light");
            groupBoxLayout.setCollapsable(true);
            groupBoxLayout.setCaption(skillTree.getSkillName());
            groupBoxLayout.setDescription(skillTree.getComment() != null
                    ? Jsoup.parse(skillTree.getComment()).text() : "");
            groupBoxLayout.setWidthFull();

            skillGroupGroupBoxLayouts.put(skillTree, groupBoxLayout);
        }

        return skillGroupGroupBoxLayouts;
    }

    private List<SkillTree> getSkillTreeGroup() {
        return dataManager.load(SkillTree.class)
                .query(QUERY_SKILL_TREE_GROUP)
                .list();
    }
}