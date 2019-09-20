package com.company.itpearls.web.screens.skill;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Skill;

@UiController("itpearls_Skill.edit")
@UiDescriptor("skill-edit.xml")
@EditedEntityContainer("skillDc")
@LoadDataBeforeShow
public class SkillEdit extends StandardEditor<Skill> {
}