package com.company.hunttech.web.screens.fragments;

import com.company.hunttech.core.ParseCVService;
import com.company.hunttech.core.PdfParserService;
import com.company.hunttech.entity.CandidateCV;
import com.company.hunttech.entity.SkillTree;
import com.company.hunttech.web.StandartPrioritySkills;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.DataGrid;
import com.haulmont.cuba.gui.components.FlowBoxLayout;
import com.haulmont.cuba.gui.components.GroupBoxLayout;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import org.jsoup.Jsoup;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@UiController("hunttech_Skillsbar")
@UiDescriptor("skillsBar.xml")
public class Skillsbar extends ScreenFragment {
    private String skillText;
    @Inject
    private PdfParserService pdfParserService;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private FlowBoxLayout skillsFlowBox;
    @Inject
    private GroupBoxLayout skillsGroupBox;
    private boolean flagVisible;
    @Inject
    private ParseCVService parseCVService;
    @Inject
    private FlowBoxLayout keySkillsFlowBox;
    @Inject
    private GroupBoxLayout keySkillsFlowGroupBox;
    @Inject
    private GroupBoxLayout skillFlowGroupBox;

    @Inject
    private DataManager dataManager;

    /**
     * Parse a CV text and return a list of skill data — safe to call
     * from a background thread (no UI access).
     */
    public List<SkillLabelData> analyzeSkills(String cvText) {
        if (cvText == null) {
            return Collections.emptyList();
        }
        String plainText = Jsoup.parse(cvText).text();
        List<SkillTree> skillTrees = pdfParserService.parseSkillTree(plainText);
        HashMap<SkillTree, Integer> skillCounter = new HashMap<>();

        for (SkillTree skillTree : skillTrees) {
            skillCounter.put(skillTree, parseCVService.countMachesSkill(plainText, skillTree));
        }

        // Deduplicate
        for (int i = 0; i < skillTrees.size(); i++) {
            if (!skillTrees.get(i).getNotParsing()) {
                for (int j = i + 1; j < skillTrees.size(); j++) {
                    if (skillTrees.get(i).getSkillName().equalsIgnoreCase(
                            skillTrees.get(j).getSkillName())) {
                        skillTrees.remove(j);
                        break;
                    }
                }
            }
        }

        if (skillTrees.isEmpty()) {
            return Collections.emptyList();
        }

        List<SkillLabelData> result = new ArrayList<>();

        for (int i = StandartPrioritySkills.PROGRAMMING_LANGUAGE_INT;
             i >= StandartPrioritySkills.DEFAULT_INT; i--) {
            for (SkillTree st : skillTrees) {
                if (!st.getNotParsing() && st.getPrioritySkill() != null
                        && st.getPrioritySkill().equals(i)) {
                    Integer count = skillCounter.get(st);
                    if (count != null && count > 0) {
                        boolean isKey = count >= 2;
                        result.add(new SkillLabelData(
                                st.getSkillName(),
                                count,
                                getStyleForSkillPriority(st),
                                st.getComment(),
                                isKey
                        ));
                    }
                }
            }
        }

        return result;
    }

    /**
     * Render skill labels from previously computed data into the fragment's
     * UI containers.  Must be called on the UI thread.
     *
     * @return true if any skills were rendered
     */
    public boolean renderSkillLabels(List<SkillLabelData> skills) {
        if (skills == null || skills.isEmpty()) {
            skillsGroupBox.setVisible(false);
            skillFlowGroupBox.setVisible(false);
            keySkillsFlowGroupBox.setVisible(false);
            return false;
        }

        int counterOfPub = 0;
        int counterKeyOfPub = 0;

        for (SkillLabelData data : skills) {
            Label labelSkill = uiComponents.create(Label.NAME);
            labelSkill.setHtmlEnabled(true);
            labelSkill.setDescriptionAsHtml(true);

            if (data.isKeySkill) {
                labelSkill.setValue("<b>" + data.skillName + " (" + data.counter + ")</b>");
                labelSkill.setStyleName(data.styleName);
                labelSkill.setDescription(data.description);
                counterKeyOfPub++;
                keySkillsFlowBox.add(labelSkill);
            } else {
                labelSkill.setValue(data.skillName + " (" + data.counter + ")");
                labelSkill.setStyleName(data.styleName);
                labelSkill.setDescription(data.description);
                counterOfPub++;
                skillsFlowBox.add(labelSkill);
            }
        }

        skillsFlowBox.setVisible(counterOfPub > 0);
        keySkillsFlowBox.setVisible(counterKeyOfPub > 0);
        skillsGroupBox.setVisible(counterOfPub > 0 || counterKeyOfPub > 0);
        skillFlowGroupBox.setVisible(counterOfPub > 0);
        keySkillsFlowGroupBox.setVisible(counterKeyOfPub > 0);

        return counterOfPub > 0 || counterKeyOfPub > 0;
    }

    public void setSkillText(String skillText) {
        if (skillText != null) {
            this.skillText = Jsoup.parse(skillText).text();
        } else {
            this.skillText = null;
        }
    }

    @Subscribe
    public void onAfterInit(AfterInitEvent event) {
        skillsGroupBox.setVisible(flagVisible);
    }

    @Subscribe
    public void onAttach(AttachEvent event) {
        skillsGroupBox.setVisible(flagVisible);
    }

    public Boolean generateSkillLabels(String skillText) {
        if (skillText == null) {
            return false;
        }
        this.skillText = Jsoup.parse(skillText).text();
        List<SkillLabelData> skills = analyzeSkills(skillText);
        return renderSkillLabels(skills);
    }

    public void setCaption(String textCaption) {
        skillsGroupBox.setCaption(textCaption);
    }

    private String getStyleForSkillPriority(SkillTree st) {
        String retStr;

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

        return retStr;
    }
}