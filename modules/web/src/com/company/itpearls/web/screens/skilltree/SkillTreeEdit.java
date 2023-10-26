package com.company.itpearls.web.screens.skilltree;

import com.company.itpearls.web.StandartPrioritySkills;
import com.haulmont.cuba.core.global.PersistenceHelper;
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
import java.util.LinkedHashMap;
import java.util.Map;

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
    @Inject
    private CheckBox notParsingCheckBox;
    @Inject
    private LookupField<Integer> skillPriorityField;

    public void parseWikiToDescription() throws IOException {
        Document doc = Jsoup.connect(wikiPateField.getValue())
                .userAgent("Chrome/4.0.249.0 Safari/532.5")
                .referrer(referer)
                .get();

        Elements elements = doc.select("div#mw-content-text.mw-content-ltr");

        skillCommentRichTextArea.setValue(elements.html());

        setLogo();
    }

    private void setLogo() {
        String urlString = "";
        URL url = null;


        try {
            urlString = "https://" + getPicFromWiki().substring(2);
        } catch (NullPointerException | StringIndexOutOfBoundsException e) {
            e.printStackTrace();
        }

        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        skillPic.setSource(UrlResource.class).setUrl(url);
    }

    private String getPicFromWiki() {
        Document document = Jsoup.parse(skillCommentRichTextArea.getValue());
        String imageUrl = "";

        Element element = document.select("img").first();

        try {
            imageUrl = element.attr("src");
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

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

        setOptionsSkillPriorityField();
    }

    private void setOptionsSkillPriorityField() {
        Map<String, Integer> map = new LinkedHashMap<>();

        map.put(StandartPrioritySkills.NOT_USED_SKILLS_STR,
                StandartPrioritySkills.NOT_USED_SKILLS_INT);
        map.put(StandartPrioritySkills.DEFAULT_STR,
                StandartPrioritySkills.DEFAULT_INT);
        map.put(StandartPrioritySkills.SUBJECT_AREA_STR,
                StandartPrioritySkills.SUBJECT_AREA_INT);
        map.put(StandartPrioritySkills.FRAMEWORKS_STR,
                StandartPrioritySkills.FRAMEWORKS_INT);
        map.put(StandartPrioritySkills.METHODOLOGY_STR,
                StandartPrioritySkills.METHODOLORY_INT);
        map.put(StandartPrioritySkills.PROGRAMMING_LANGUAGE_STR,
                StandartPrioritySkills.PROGRAMMING_LANGUAGE_INT);

        skillPriorityField.setOptionsMap(map);
    }

    @Install(to = "skillPriorityField", subject = "optionCaptionProvider")
    private String skillPriorityFieldOptionCaptionProvider(Integer integer) {
        String retStr = "";

        switch (integer) {
            case -1:
                retStr = StandartPrioritySkills.NOT_USED_SKILLS_STYLE;
                break;
            case 0:
                retStr = StandartPrioritySkills.DEFAULT_STYLE;
                break;
            case 1:
                retStr = StandartPrioritySkills.SUBJECT_AREA_STYLE;
                break;
            case 2:
                retStr = StandartPrioritySkills.FRAMEWORKS_STYLE;
                break;
            case 3:
                retStr = StandartPrioritySkills.METHODOLORY_STYLE;
                break;
            case 4:
                retStr = StandartPrioritySkills.PROGRAMMING_LANGUAGE_STYLE;
                break;
            default:
                retStr = StandartPrioritySkills.NOT_USED_SKILLS_STYLE;
                break;
        }

        return retStr;
    }


    @Install(to = "skillPriorityField", subject = "optionStyleProvider")
    private String skillPriorityFieldOptionStyleProvider(Integer integer) {
        String retStr = "";

        switch (integer) {
            case -1:
                retStr = StandartPrioritySkills.NOT_USED_SKILLS_STYLE;
                break;
            case 0:
                retStr = StandartPrioritySkills.DEFAULT_STYLE;
                break;
            case 1:
                retStr = StandartPrioritySkills.SUBJECT_AREA_STYLE;
                break;
            case 2:
                retStr = StandartPrioritySkills.FRAMEWORKS_STYLE;
                break;
            case 3:
                retStr = StandartPrioritySkills.METHODOLORY_STYLE;
                break;
            case 4:
                retStr = StandartPrioritySkills.PROGRAMMING_LANGUAGE_STYLE;
                break;
            default:
                retStr = StandartPrioritySkills.NOT_USED_SKILLS_STYLE;
                break;
        }

        return retStr;
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
        if (event.getValue() != null) {
            if (event.getValue().equals("")) {
                parseWikiText.setEnabled(true);
            } else {
                parseWikiText.setEnabled(false);
            }
        } else {
            parseWikiText.setVisible(false);
        }
    }

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        if (skillPic.getValueSource().getValue() == null &&
                skillCommentRichTextArea.getValue() != null) {
            setLogo();
        }
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            notParsingCheckBox.setValue(false);
        } else {
            if (notParsingCheckBox.getValue() == null) {
                notParsingCheckBox.setValue(false);
            }
        }
    }
}