package com.company.itpearls.web.screens.skilltree;

import com.haulmont.cuba.gui.components.RichTextArea;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.SkillTree;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.inject.Inject;
import java.io.IOException;

@UiController("itpearls_SkillTree.edit")
@UiDescriptor("skill-tree-edit.xml")
@EditedEntityContainer("skillTreeDc")
@LoadDataBeforeShow
public class SkillTreeEdit extends StandardEditor<SkillTree> {
    @Inject
    private TextField<String> wikiPateField;
    @Inject
    private RichTextArea skillCommentRichTextArea;

    public void parseWikiToDescription() throws IOException {
        Document doc = Jsoup.connect(wikiPateField.getValue())
                .userAgent("Chrome/4.0.249.0 Safari/532.5")
                .referrer("http://www.google.com")
                .get();

        skillCommentRichTextArea.setValue(doc.html());
    }
}