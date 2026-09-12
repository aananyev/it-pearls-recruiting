package com.company.hunttech.web.screens.skilltree;

import com.company.hunttech.web.StandartPrioritySkills;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.SkillTree;
import com.hunttech.hrm.gui.components.OvaFallbackImage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.gui.model.InstanceContainer;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

@UiController("hunttech_SkillTree.edit")
@UiDescriptor("skill-tree-edit.xml")
@EditedEntityContainer("skillTreeDc")
@LoadDataBeforeShow
public class SkillTreeEdit extends StandardEditor<SkillTree> {
    private static final Logger log = LoggerFactory.getLogger(SkillTreeEdit.class);

    @Inject
    private FileLoader fileLoader;
    @Inject
    private TextField<String> wikiPateField;
    @Inject
    private RichTextArea skillCommentRichTextArea;
    @Inject
    private Button parseWikiText;
    @Inject
    private Dialogs dialogs;
    @Inject
    private OvaFallbackImage skillPic;
    @Inject
    private FileUploadField fileImageSkillUpload;
    static String referer = "http://www.google.com";
    @Inject
    private TextField<String> skillNameField;
    @Inject
    private CheckBox notParsingCheckBox;
    @Inject
    private LookupField<Integer> skillPriorityField;
    @Inject
    private Button skillTreeMainNav;
    @Inject
    private Button skillTreeDescriptionNav;

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

        // Если картинку из Wiki получить не удалось — не затирать текущий
        // источник (fallback-аватар OvaFallbackImage), иначе src станет пустым.
        if (url == null) {
            return;
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

    @Subscribe(id = "skillTreeDc", target = Target.DATA_CONTAINER)
    public void onSkillTreeDcItemPropertyChange(InstanceContainer.ItemPropertyChangeEvent<SkillTree> event) {
        if ("logoImage".equals(event.getProperty())) {
            updateSkillLogoImage();
        } else if ("fileImageLogo".equals(event.getProperty())) {
            if (event.getValue() == null) {
                SkillTree skill = getEditedEntity();
                if (skill != null) {
                    skill.setLogoImage(null);
                }
            }
            updateSkillLogoImage();
        }
    }

    @Subscribe("fileImageSkillUpload")
    public void onFileImageSkillUploadFileUploadSucceed(FileUploadField.FileUploadSucceedEvent event) {
        SkillTree skill = getEditedEntity();
        com.haulmont.cuba.core.entity.FileDescriptor fd = fileImageSkillUpload.getFileDescriptor();
        if (skill != null && fd != null && fileLoader != null) {
            try (InputStream is = fileLoader.openStream(fd)) {
                if (is != null) {
                    byte[] bytes = IOUtils.toByteArray(is);
                    if (bytes != null && bytes.length > 0) {
                        skill.setLogoImage(bytes);
                    }
                }
            } catch (Exception ex) {
                log.warn("Не удалось синхронизировать файл логотипа в BLOB logoImage: {}", ex.getMessage());
            }
        }
        updateSkillLogoImage();
    }

    private void updateSkillLogoImage() {
        if (skillPic == null) {
            return;
        }
        SkillTree skill = getEditedEntity();
        if (skill == null) {
            skillPic.applyFallback();
            return;
        }
        byte[] logoBytes = skill.getLogoImage();
        if (logoBytes != null && logoBytes.length > 0) {
            skillPic.setSource(StreamResource.class)
                    .setStreamSupplier(() -> new ByteArrayInputStream(logoBytes));
        } else if (skill.getFileImageLogo() != null) {
            skillPic.setSource(FileDescriptorResource.class)
                    .setFileDescriptor(skill.getFileImageLogo());
        } else {
            skillPic.applyFallback();
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
        updateSkillLogoImage();
        String comment = skillCommentRichTextArea.getValue();
        if ((skillPic.getValueSource() == null || skillPic.getValueSource().getValue() == null) &&
                getEditedEntity().getLogoImage() == null &&
                getEditedEntity().getFileImageLogo() == null &&
                comment != null && !comment.trim().isEmpty()) {
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

    /**
     * Презентационная навигация: переводит фокус к первому полю «Основных данных»
     * и подсвечивает активный пункт sidebar. Entity, loaders и lifecycle не затрагиваются.
     */
    public void focusMainSection() {
        skillNameField.focus();
        setActiveNavigation(skillTreeMainNav, skillTreeDescriptionNav);
    }

    /**
     * Презентационная навигация: переводит фокус к редактору описания навыка
     * и подсвечивает активный пункт sidebar. Entity, loaders и lifecycle не затрагиваются.
     */
    public void focusDescriptionSection() {
        skillCommentRichTextArea.focus();
        setActiveNavigation(skillTreeDescriptionNav, skillTreeMainNav);
    }

    private void setActiveNavigation(Button activeButton, Button inactiveButton) {
        activeButton.addStyleName("label-nav-item-active");
        inactiveButton.removeStyleName("label-nav-item-active");
    }
}