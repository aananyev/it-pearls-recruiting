package com.company.itpearls.web.screens.skilltree;

import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.SkillTree;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.inject.Inject;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

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
    @Inject
    private Dialogs dialogs;
    @Inject
    private Image skillPic;
    @Inject
    private FileUploadField fileImageSkillUpload;
    static String referer = "http://www.google.com";
    @Inject
    private TextField<String> skillNameField;

    public void parseWikiToDescription() throws IOException {
        Document doc = Jsoup.connect(wikiPateField.getValue())
                .userAgent("Chrome/4.0.249.0 Safari/532.5")
                .referrer(referer)
                .get();

        Elements elements = doc.select("div#mw-content-text.mw-content-ltr");
//        Elements elements = doc.select("div[id=content]");

        skillCommentRichTextArea.setValue(elements.html());

        setLogo();
    }

    private void setLogo() {
        String urlString = "https://" + getPicFromWiki().substring(2);
        URL url = null;

        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        skillPic.setSource(UrlResource.class).setUrl(url);
    }

    private String getPicFromWiki() {
        Document document = Jsoup.parse(skillCommentRichTextArea.getValue());

        Element element = document.select("img").first();
        String imageUrl = element.attr("src");

        return imageUrl;
    }

    @Subscribe
    public void onInit(InitEvent event) {
        skillNameField.addTextChangeListener(e -> {
            String wikipedia = "https://ru.wikipedia.org/wiki/";
            String setUrl = wikipedia + e.getText().replace(" ", "_").trim();

            if(!setUrl.equals(getEditedEntity().getWikiPage())) {
                dialogs.createOptionDialog(Dialogs.MessageType.WARNING)
                        .withCaption("ВНИМАНИЕ")
                        .withMessage("Поле \"Ссылка на Wiki\" заполнено.\nВнести туда новые данные?")
                        .withActions(new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY).withHandler(f -> {
                                    wikiPateField.setValue(setUrl);
                                }),
                                new DialogAction((DialogAction.Type.NO)))
                        .show();
            }
        });
    }

    @Subscribe("fileImageSkillUpload")
    public void onFileImageSkillUploadFileUploadSucceed(FileUploadField.FileUploadSucceedEvent event) {
        try {
            FileDescriptorResource fileDescriptorResource = skillPic.createResource(FileDescriptorResource.class)
                    .setFileDescriptor(fileImageSkillUpload.getFileDescriptor());

            skillPic.setSource(fileDescriptorResource);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Subscribe("wikiPateField")
    public void onWikiPateFieldValueChange(HasValue.ValueChangeEvent<String> event) {
        if (event.getValue() == null || event.getValue() == "") {
            parseWikiText.setEnabled(false);
        } else {
            parseWikiText.setEnabled(true);
        }
    }

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        if (skillPic.getValueSource().getValue() == null &&
                skillCommentRichTextArea.getValue() != null) {
            setLogo();
        }
    }
}