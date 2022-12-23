package com.company.itpearls.web.screens.jobcandidate;

import com.company.itpearls.entity.SkillTree;
import com.company.itpearls.web.SkillsFilterCandidateEvent;
import com.company.itpearls.web.StandartPrioritySkills;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Events;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.JobCandidate;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.security.global.UserSession;
import org.jsoup.Jsoup;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;

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
    private HashMap<SkillTree, GroupBoxLayout> skillGroupFilterGroupBoxLayouts = new HashMap<>();
    private HashMap<LinkButton, LinkButton> skillsPairAllToFilter = new HashMap<>();
    private HashMap<LinkButton, LinkButton> skillsPairFilterToAll = new HashMap<>();

    @Inject
    private UiComponents uiComponents;
    @Inject
    private ScrollBoxLayout allSkillsScrollBox;
    @Inject
    private ScrollBoxLayout filterSkillsScrollBox;
    @Inject
    private DataManager dataManager;
    @Inject
    private Events events;
    @Inject
    private UserSession userSession;

    @Order(15)
    @EventListener
    protected void onSkillSelected(SkillsFilterCandidateEvent event) {
        skillsPairAllToFilter.get((LinkButton) event.getSource()).setVisible(true);
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        skillTreeGroup = getSkillTreeGroup();

//        setSkillTreesGroupGroupBoxLayouts(skillGroupGroupBoxLayouts);
//        setSkillTreesGroupGroupBoxLayouts(skillGroupFilterGroupBoxLayouts);

//        addSkillsTreeGroupGroupBoxes(skillGroupGroupBoxLayouts, allSkillsScrollBox, true);
//        addSkillsTreeGroupGroupBoxes(skillGroupFilterGroupBoxLayouts, filterSkillsScrollBox, false);
        addSkillTreeGroupBoxes();
    }

    private GroupBoxLayout setSkillGroupBox(SkillTree skillTree) {
        GroupBoxLayout groupBoxLayout = uiComponents.create(GroupBoxLayout.class);

        groupBoxLayout.setStyleName("light");
        groupBoxLayout.setCollapsable(true);
        groupBoxLayout.setCaption(skillTree.getSkillName());
        groupBoxLayout.setDescription(skillTree.getComment() != null
                ? Jsoup.parse(skillTree.getComment()).text() : "");
        groupBoxLayout.setWidthFull();

        return groupBoxLayout;
    }

    private void addSkillTreeGroupBoxes() {
        for (SkillTree skillTree : skillTreeGroup) {
            // Левый
            GroupBoxLayout groupBoxLayoutLeft = setSkillGroupBox(skillTree);
            // Правый
            GroupBoxLayout groupBoxLayoutRight = setSkillGroupBox(skillTree);

            addSkillPairLabels(skillTree, groupBoxLayoutLeft, groupBoxLayoutRight);

            allSkillsScrollBox.add(groupBoxLayoutLeft);
            filterSkillsScrollBox.add(groupBoxLayoutRight);
        }
    }

    private FlowBoxLayout setFlowBoxLayout() {
        FlowBoxLayout flowBoxLayoutLeft = uiComponents.create(FlowBoxLayout.class);
        flowBoxLayoutLeft.setWidthAuto();
        flowBoxLayoutLeft.setSpacing(true);

        return flowBoxLayoutLeft;
    }

    private LinkButton setLinkButton(SkillTree st) {
        LinkButton label = uiComponents.create(LinkButton.class);
        label.setCaption(st.getSkillName());
        label.setStyleName(getStyleForSkillPriority(st));
        label.setVisible(true);

        return label;
    }

    private void addSkillPairLabels(SkillTree skillTreeGroup,
                                    GroupBoxLayout groupBoxLayoutLeft,
                                    GroupBoxLayout groupBoxLayoutRight) {
        List<SkillTree> skillTree = dataManager.load(SkillTree.class)
                .query(QUERY_SKILL_TREE_ITEM)
                .parameter("skillGroup", skillTreeGroup)
                .list();

        FlowBoxLayout flowBoxLayoutLeft = setFlowBoxLayout();
        FlowBoxLayout flowBoxLayoutRight = setFlowBoxLayout();

        for (SkillTree st : skillTree) {
            // левый
            LinkButton labelLeft = setLinkButton(st);
            LinkButton labelRight = setLinkButton(st);
            labelRight.setVisible(false);

            labelLeft.addClickListener(e -> {
                reverseVisible(e);
            });

            labelRight.addClickListener(e -> {
                reverseVisibleFilter(e);
            });

            skillsPairAllToFilter.put(labelLeft, labelRight);
            skillsPairFilterToAll.put(labelRight, labelLeft);

            flowBoxLayoutLeft.add(labelLeft);
            flowBoxLayoutRight.add(labelRight);
        }

        groupBoxLayoutLeft.add(flowBoxLayoutLeft);
        groupBoxLayoutRight.add(flowBoxLayoutRight);
    }

    private void reverseVisibleFilter(Button.ClickEvent e) {
        e.getSource().setVisible(!e.getSource().isVisible());
        skillsPairFilterToAll.get(e.getSource()).setVisible(
                !skillsPairFilterToAll.get(e.getSource()).isVisible());
    }

    private void setSkillTreesGroupGroupBoxLayouts(HashMap<SkillTree, GroupBoxLayout> skillBoxLayouts) {
        for (SkillTree skillTree : skillTreeGroup) {
            GroupBoxLayout groupBoxLayout = uiComponents.create(GroupBoxLayout.class);

            groupBoxLayout.setStyleName("light");
            groupBoxLayout.setCollapsable(true);
            groupBoxLayout.setCaption(skillTree.getSkillName());
            groupBoxLayout.setDescription(skillTree.getComment() != null
                    ? Jsoup.parse(skillTree.getComment()).text() : "");
            groupBoxLayout.setWidthFull();

            skillBoxLayouts.put(skillTree, groupBoxLayout);
        }
    }

    private void addSkillsTreeGroupGroupBoxes(HashMap<SkillTree, GroupBoxLayout> skillBoxLayouts,
                                              ScrollBoxLayout scrollBox,
                                              Boolean visible) {
        for (Map.Entry<SkillTree, GroupBoxLayout> groupBoxLayoutEntry : skillBoxLayouts.entrySet()) {
            addSkillLabels(groupBoxLayoutEntry, scrollBox, visible);
            scrollBox.add(groupBoxLayoutEntry.getValue());

        }
    }

    private void addSkillLabels(Map.Entry<SkillTree, GroupBoxLayout> groupBoxLayoutEntry,
                                ScrollBoxLayout scrollBoxLayout,
                                Boolean visible) {
        List<SkillTree> skillTree = dataManager.load(SkillTree.class)
                .query(QUERY_SKILL_TREE_ITEM)
                .parameter("skillGroup", groupBoxLayoutEntry.getKey())
                .list();

        FlowBoxLayout hBoxLayout = uiComponents.create(FlowBoxLayout.class);
        hBoxLayout.setWidthAuto();
        hBoxLayout.setSpacing(true);

        for (SkillTree st : skillTree) {
            LinkButton label = uiComponents.create(LinkButton.class);
            label.setCaption(st.getSkillName());
            label.setStyleName(getStyleForSkillPriority(st));
            label.setVisible(visible);

            label.addClickListener(e -> {
                reverseVisible(e);
            });

            hBoxLayout.add(label);

            if (visible) {
                skillsPairAllToFilter.put(label, null);

                for (Map.Entry<LinkButton, LinkButton> entry : skillsPairFilterToAll.entrySet()) {
                    if (entry.getKey().getCaption().equals(label.getCaption())) {
                        skillsPairFilterToAll.replace(entry.getKey(), label);
                    }
                }
            } else {
                skillsPairFilterToAll.put(label, null);

                for (Map.Entry<LinkButton, LinkButton> entry : skillsPairAllToFilter.entrySet()) {
                    if (entry.getKey().getCaption().equals(label.getCaption())) {
                        skillsPairAllToFilter.replace(entry.getKey(), label);
                    }
                }
            }
        }

        groupBoxLayoutEntry.getValue().add(hBoxLayout);

        if (skillTree.size() == 0) {
            groupBoxLayoutEntry.getValue().setVisible(false);
        } else {
            groupBoxLayoutEntry.getValue().setVisible(true);
        }
    }

    private void reverseVisible(Button.ClickEvent e) {
        e.getSource().setVisible(!e.getSource().isVisible());
        skillsPairAllToFilter.get(e.getSource()).setVisible(
                !skillsPairAllToFilter.get(e.getSource()).isVisible());
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

    private List<SkillTree> getSkillTreeGroup() {
        return dataManager.load(SkillTree.class)
                .query(QUERY_SKILL_TREE_GROUP)
                .list();
    }
}