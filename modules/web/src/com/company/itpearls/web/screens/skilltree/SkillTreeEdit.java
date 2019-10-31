package com.company.itpearls.web.screens.skilltree;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.SkillTree;

@UiController("itpearls_SkillTree.edit")
@UiDescriptor("skill-tree-edit.xml")
@EditedEntityContainer("skillTreeDc")
@LoadDataBeforeShow
public class SkillTreeEdit extends StandardEditor<SkillTree> {
}