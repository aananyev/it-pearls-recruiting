package com.company.itpearls.web.screens.fragments;

import com.company.itpearls.core.ParseCVService;
import com.company.itpearls.core.PdfParserService;
import com.company.itpearls.entity.CandidateCV;
import com.company.itpearls.entity.SkillTree;
import com.company.itpearls.web.StandartPrioritySkills;
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
import java.util.HashMap;
import java.util.List;

@UiController("itpearls_Skillsbar")
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
        if (skillText != null) {
            this.skillText = Jsoup.parse(skillText).text();

            return generateSkillLabels();
        } else {
            return false;
        }
    }

    public void setCaption(String textCaption) {
        skillsGroupBox.setCaption(textCaption);
    }

    private Boolean generateSkillLabels() {
        Integer counterOfPub = 0;
        Integer counterKeyOfPub = 0;

        if (skillText != null) {
            List<SkillTree> skillTrees = pdfParserService.parseSkillTree(skillText);
            HashMap<SkillTree, Integer> skillCounter = new HashMap<>();

            for (SkillTree skillTree : skillTrees) {
                skillCounter.put(skillTree, parseCVService.countMachesSkill(skillText, skillTree));
            }

            for (Integer i = 0; i < skillTrees.size(); i++) {
                if (!skillTrees.get(i).getNotParsing()) {
                    for (Integer j = i + 1; j < skillTrees.size(); j++) {
                        if (skillTrees.get(i).getSkillName().toLowerCase().equals(
                                skillTrees.get(j).getSkillName().toLowerCase())) {
                            skillTrees.remove(skillTrees.get(j));
                            break;
                        }
                    }
                }
            }

            if (skillTrees.size() > 0) {
                flagVisible = true;
            } else {
                flagVisible = false;
            }

            for (Integer i = StandartPrioritySkills.PROGRAMMING_LANGUAGE_INT;
                 i >= StandartPrioritySkills.DEFAULT_INT; i--) {
                for (SkillTree st : skillTrees) {
                    if (!st.getNotParsing()) {
                        if (st.getPrioritySkill() != null) {
                            if (st.getPrioritySkill().equals(i)) {
                                if (skillCounter.get(st) > 0) {
                                    Label labelSkill = uiComponents.create(Label.NAME);
                                    labelSkill.setHtmlEnabled(true);
                                    labelSkill.setDescriptionAsHtml(true);
                                    labelSkill.setDescriptionAsHtml(true);

                                    if (skillCounter.get(st) < 2) {
                                        String counter = new StringBuilder()
                                                .append(st.getSkillName())
                                                .append(" (")
                                                .append(skillCounter.get(st))
                                                .append(")")
                                                .toString();

                                        labelSkill.setValue(counter);
                                        labelSkill.setStyleName(getStyleForSkillPriority(st));
                                        labelSkill.setDescription(st.getComment());

                                        counterOfPub++;
                                        skillsFlowBox.add(labelSkill);
                                    } else {
                                        String counter = new StringBuilder()
                                                .append("<b>")
                                                .append(st.getSkillName())
                                                .append(" (")
                                                .append(skillCounter.get(st))
                                                .append(")</b>").toString();

                                        labelSkill.setValue(counter);
                                        labelSkill.setStyleName(getStyleForSkillPriority(st));
                                        labelSkill.setDescription(st.getComment());

                                        counterKeyOfPub++;
                                        keySkillsFlowBox.add(labelSkill);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (counterOfPub > 0) {
                skillsFlowBox.setVisible(true);
                skillsGroupBox.setVisible(true);
                skillFlowGroupBox.setVisible(true);
            }

            if (counterKeyOfPub > 0) {
                keySkillsFlowBox.setVisible(true);
                keySkillsFlowGroupBox.setVisible(true);
                skillsGroupBox.setVisible(true);
            }

            if (counterKeyOfPub == 0 && counterOfPub == 0) {
                skillsGroupBox.setVisible(false);
            } else {
                skillsGroupBox.setVisible(true);
            }

            return true;
        } else {
            return false;
        }
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