package com.company.itpearls.web.screens.skilltree;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.SkillTree;

@UiController("itpearls_SkillTree.browse")
@UiDescriptor("skill-tree-browse.xml")
@LookupComponent("skillTreesTable")
@LoadDataBeforeShow
public class SkillTreeBrowse extends StandardLookup<SkillTree> {
}