package com.company.itpearls.web.screens.skill;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Skill;

@UiController("itpearls_Skill.browse")
@UiDescriptor("skill-browse.xml")
@LookupComponent("skillsTable")
@LoadDataBeforeShow
public class SkillBrowse extends StandardLookup<Skill> {
}