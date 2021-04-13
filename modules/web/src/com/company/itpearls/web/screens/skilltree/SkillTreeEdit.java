package com.company.itpearls.web.screens.skilltree;

import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.HasValue;
import com.haulmont.cuba.gui.components.RichTextArea;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.SkillTree;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

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
    @Inject
    private Button parseWikiText;

    public void parseWikiToDescription() throws IOException {
        Document doc = Jsoup.connect(wikiPateField.getValue())
                .userAgent("Chrome/4.0.249.0 Safari/532.5")
                .referrer("http://www.google.com")
                .get();

        Elements elements = doc.select("div#mw-content-text.mw-content-ltr");

        skillCommentRichTextArea.setValue(elements.html());
    }

    @Subscribe("skillNameField")
    public void onSkillNameFieldValueChange(HasValue.ValueChangeEvent<String> event) {
        wikiPateField.setValue("https://ru.wikipedia.org/wiki/" + event.getValue());
    }

    @Subscribe("wikiPateField")
    public void onWikiPateFieldValueChange(HasValue.ValueChangeEvent<String> event) {
        if(event.getValue() == null || event.getValue() == "") {
            parseWikiText.setEnabled(false);
        } else {
            parseWikiText.setEnabled(true);
        }

    }


}