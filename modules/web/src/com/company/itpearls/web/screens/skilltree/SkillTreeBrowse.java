package com.company.itpearls.web.screens.skilltree;

import com.company.itpearls.entity.JobCandidate;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.data.value.ContainerValueSource;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.SkillTree;
import com.haulmont.cuba.gui.screen.LookupComponent;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.inject.Inject;
import java.net.MalformedURLException;
import java.net.URL;

@UiController("itpearls_SkillTree.browse")
@UiDescriptor("skill-tree-browse.xml")
@LookupComponent("skillTreesTable")
@LoadDataBeforeShow
public class SkillTreeBrowse extends StandardLookup<SkillTree> {
    @Inject
    private TreeDataGrid<SkillTree> skillTreesTable;
    @Inject
    private UiComponents uiComponents;
/*    @Install(to = "skillTreesTable", subject = "itemDescriptionProvider")
    private String skillTreesTableItemDescriptionProvider(SkillTree skillTree, String string) {
        return skillTree.getComment() != null ? skillTree.getComment() : "";
    }*/

    @Install(to = "skillTreesTable.skillName", subject = "descriptionProvider")
    private String skillTreesTableSkillNameDescriptionProvider(SkillTree skillTree) {
        return skillTree.getComment() != null ? Jsoup.parse(skillTree.getComment()).text() : "";
    }

    @Install(to = "skillTreesTable.isComment", subject = "columnGenerator")
    private Object skillTreesTableIsCommentColumnGenerator(DataGrid.ColumnGeneratorEvent<SkillTree> event) {
        if (event.getItem().getComment() != null && !event.getItem().equals("")) {
            return CubaIcon.FILE_TEXT;
        } else {
            return CubaIcon.FILE;
        }
    }

    @Install(to = "skillTreesTable.isComment", subject = "styleProvider")
    private String skillTreesTableIsCommentStyleProvider(SkillTree skillTree) {
        if (skillTree.getComment() != null && !skillTree.equals("")) {
            return "pic-center-large-green";
        } else {
            return "pic-center-large-red";
        }
    }
}