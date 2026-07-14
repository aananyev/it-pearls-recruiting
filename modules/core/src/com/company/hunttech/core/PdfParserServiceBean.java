package com.company.hunttech.core;

import com.company.hunttech.entity.SkillTree;
import com.haulmont.cuba.core.entity.KeyValueEntity;
import com.haulmont.cuba.core.global.DataManager;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.io.RandomAccessReadMemoryMappedFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service(PdfParserService.NAME)
public class PdfParserServiceBean implements PdfParserService {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(PdfParserServiceBean.class);

    /**
     * Скалярная проекция не переносит в web-слой persistence-граф SkillTree.
     * Это исключает рост ObjectOutputStream при открытии JobCandidateEdit.
     */
    private static final String QUERY_SKILL_TREE =
            "select e.id, e.skillName, e.notParsing, e.prioritySkill, e.comment, "
                    + "parent.id, parent.skillName, parent.notParsing, parent.prioritySkill, parent.comment "
                    + "from hunttech_SkillTree e left join e.skillTree parent "
                    + "order by e.skillName desc";

    @Inject
    private DataManager dataManager;

    @Override
    public List<RenderedImage> getImagesFromPDF(PDDocument document) throws IOException {
        List<RenderedImage> images = new ArrayList<>();
        for (PDPage page : document.getPages()) {
            images.addAll(getImagesFromResources(page.getResources()));
        }

        return images;
    }

    @Override
    public List<RenderedImage> getImagesFromResources(PDResources resources) throws IOException {
        List<RenderedImage> images = new ArrayList<>();

        for (COSName xObjectName : resources.getXObjectNames()) {
            PDXObject xObject = resources.getXObject(xObjectName);

            if (xObject instanceof PDFormXObject) {
                images.addAll(getImagesFromResources(((PDFormXObject) xObject).getResources()));
            } else if (xObject instanceof PDImageXObject) {
                images.add(((PDImageXObject) xObject).getImage());
            }
        }

        return images;
    }

    @Override
    public List<SkillTree> parseSkillTree(String inputText) {
        if (inputText == null || inputText.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // Загружаем только поля, реально используемые анализатором и Skillsbar.
        List<KeyValueEntity> rows = dataManager.loadValues(QUERY_SKILL_TREE)
                .properties(
                        "skillId",
                        "skillName",
                        "notParsing",
                        "prioritySkill",
                        "comment",
                        "parentId",
                        "parentSkillName",
                        "parentNotParsing",
                        "parentPrioritySkill",
                        "parentComment")
                .list();

        List<SkillProjection> projections = new ArrayList<>(rows.size());
        for (KeyValueEntity row : rows) {
            projections.add(new SkillProjection(
                    row.getValue("skillId"),
                    row.getValue("skillName"),
                    row.getValue("notParsing"),
                    row.getValue("prioritySkill"),
                    row.getValue("comment"),
                    row.getValue("parentId"),
                    row.getValue("parentSkillName"),
                    row.getValue("parentNotParsing"),
                    row.getValue("parentPrioritySkill"),
                    row.getValue("parentComment")));
        }

        return buildSkillSnapshots(inputText, projections);
    }

    /**
     * Отбирает найденные навыки и создаёт компактные transient-снимки.
     * В снимки не попадают candidates, openPosition, candidateCV и другие связи сущности.
     */
    static List<SkillTree> buildSkillSnapshots(String inputText, List<SkillProjection> projections) {
        List<SkillTree> emptyResult = new ArrayList<>();
        if (inputText == null || inputText.trim().isEmpty() || projections == null || projections.isEmpty()) {
            return emptyResult;
        }

        String normalizedInput = inputText.toLowerCase(Locale.ROOT);
        Map<String, SkillTree> snapshots = new LinkedHashMap<>();
        Map<String, SkillTree> result = new LinkedHashMap<>();
        int invalidRecords = 0;

        for (SkillProjection projection : projections) {
            if (projection == null || isBlank(projection.skillName)) {
                invalidRecords++;
                continue;
            }
            if (Boolean.TRUE.equals(projection.notParsing)) {
                continue;
            }
            if (!normalizedInput.contains(projection.skillName.toLowerCase(Locale.ROOT))) {
                continue;
            }

            String skillKey = projectionKey(projection.skillId, projection.skillName);
            SkillTree skill = snapshots.computeIfAbsent(skillKey,
                    key -> createSnapshot(
                            projection.skillId,
                            projection.skillName,
                            projection.notParsing,
                            projection.prioritySkill,
                            projection.comment));
            result.putIfAbsent(skillKey, skill);

            // Родитель добавляется один раз и только если пригоден для парсинга.
            if (!isBlank(projection.parentSkillName)
                    && !Boolean.TRUE.equals(projection.parentNotParsing)) {
                String parentKey = projectionKey(projection.parentId, projection.parentSkillName);
                SkillTree parent = snapshots.computeIfAbsent(parentKey,
                        key -> createSnapshot(
                                projection.parentId,
                                projection.parentSkillName,
                                projection.parentNotParsing,
                                projection.parentPrioritySkill,
                                projection.parentComment));
                skill.setSkillTree(parent);
                result.putIfAbsent(parentKey, parent);
            }
        }

        if (invalidRecords > 0) {
            // Одна агрегированная запись вместо полного stack trace для каждой строки справочника.
            log.warn("Пропущено {} записей SkillTree без наименования при разборе резюме", invalidRecords);
        }

        return new ArrayList<>(result.values());
    }

    /** Создаёт снимок сущности только с полями, необходимыми вызывающему коду. */
    private static SkillTree createSnapshot(UUID id,
                                            String skillName,
                                            Boolean notParsing,
                                            Integer prioritySkill,
                                            String comment) {
        SkillTree snapshot = new SkillTree();
        if (id != null) {
            snapshot.setId(id);
        }
        snapshot.setSkillName(skillName);
        snapshot.setNotParsing(notParsing);
        snapshot.setPrioritySkill(prioritySkill);
        snapshot.setComment(comment);
        return snapshot;
    }

    private static String projectionKey(UUID id, String skillName) {
        if (id != null) {
            return id.toString();
        }
        return "name:" + skillName.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    static final class SkillProjection {
        final UUID skillId;
        final String skillName;
        final Boolean notParsing;
        final Integer prioritySkill;
        final String comment;
        final UUID parentId;
        final String parentSkillName;
        final Boolean parentNotParsing;
        final Integer parentPrioritySkill;
        final String parentComment;

        SkillProjection(UUID skillId,
                        String skillName,
                        Boolean notParsing,
                        Integer prioritySkill,
                        String comment,
                        UUID parentId,
                        String parentSkillName,
                        Boolean parentNotParsing,
                        Integer parentPrioritySkill,
                        String parentComment) {
            this.skillId = skillId;
            this.skillName = skillName;
            this.notParsing = notParsing;
            this.prioritySkill = prioritySkill;
            this.comment = comment;
            this.parentId = parentId;
            this.parentSkillName = parentSkillName;
            this.parentNotParsing = parentNotParsing;
            this.parentPrioritySkill = parentPrioritySkill;
            this.parentComment = parentComment;
        }
    }

    @Override
    public String pdf2txt(String fileName) throws IOException {
        String parsedText = "";

        if (fileName.contains("pdf")) {
            RandomAccessRead rad = new RandomAccessReadMemoryMappedFile(fileName);
            PDFParser parser = new PDFParser(rad);

            PDFTextStripper pdfStripper = new PDFTextStripper();
            PDDocument pdDoc = parser.parse();
            parsedText = pdfStripper.getText(pdDoc);

            return parsedText;
        } else {
            return null;
        }
    }

    @Override
    public File getImageFromPDF(File file) throws IOException {
        RandomAccessRead rad = new RandomAccessReadMemoryMappedFile(file);
        File tempJpg;

        PDFParser parser = new PDFParser(rad);
        PDDocument document = parser.parse();
        PDFRenderer renderer = new PDFRenderer(document);
        BufferedImage image = renderer.renderImage(0);
        tempJpg = File.createTempFile("img", ".jpg");
        ImageIO.write(image, "JPEG", tempJpg);
        document.close();

        return tempJpg;
    }

    @Override
    public String getImageFromNamePDF(File file) throws IOException {
        RandomAccessRead rad = new RandomAccessReadMemoryMappedFile(file);
        PDFParser parser = new PDFParser(rad);
        PDDocument document = parser.parse();
        PDFRenderer renderer = new PDFRenderer(document);
        BufferedImage image = renderer.renderImage(0);
        File tempJpg = File.createTempFile("img", ".jpg");
        ImageIO.write(image, "JPEG", tempJpg);
        document.close();

        return tempJpg.getName();
    }
}
