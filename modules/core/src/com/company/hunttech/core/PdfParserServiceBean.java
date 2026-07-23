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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service(PdfParserService.NAME)
public class PdfParserServiceBean implements PdfParserService {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(PdfParserServiceBean.class);

    /**
     * Скалярная проекция используется только для поиска совпадений и не переносит
     * в web-слой persistence-граф всего справочника SkillTree.
     */
    private static final String QUERY_SKILL_TREE_PROJECTION =
            "select e.id, e.skillName, e.notParsing, parent.id, parent.skillName, parent.notParsing "
                    + "from hunttech_SkillTree e left join e.skillTree parent "
                    + "order by e.skillName desc";

    /** Загружает реальные сущности только для уже найденных идентификаторов. */
    private static final String QUERY_SKILL_TREE_BY_IDS =
            "select e from hunttech_SkillTree e where e.id in :skillIds";

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

        // Сначала отбираем совпадения по компактной скалярной проекции.
        List<KeyValueEntity> rows = dataManager.loadValues(QUERY_SKILL_TREE_PROJECTION)
                .properties(
                        "skillId",
                        "skillName",
                        "notParsing",
                        "parentId",
                        "parentSkillName",
                        "parentNotParsing")
                .list();

        List<SkillProjection> projections = new ArrayList<>(rows.size());
        for (KeyValueEntity row : rows) {
            projections.add(new SkillProjection(
                    row.getValue("skillId"),
                    row.getValue("skillName"),
                    row.getValue("notParsing"),
                    row.getValue("parentId"),
                    row.getValue("parentSkillName"),
                    row.getValue("parentNotParsing")));
        }

        List<UUID> selectedIds = selectSkillIds(inputText, projections);
        if (selectedIds.isEmpty()) {
            return new ArrayList<>();
        }

        // Для вызывающего кода возвращаем штатные сущности, а не искусственные
        // объекты с существующими ID. Узкий view исключает обратные коллекции.
        List<SkillTree> loadedSkills = dataManager.load(SkillTree.class)
                .query(QUERY_SKILL_TREE_BY_IDS)
                .parameter("skillIds", selectedIds)
                .view("skillTree-edit-view")
                .list();

        Map<UUID, SkillTree> skillsById = new HashMap<>();
        for (SkillTree skill : loadedSkills) {
            skillsById.put(skill.getId(), skill);
        }

        // Восстанавливаем детерминированный порядок: найденный навык, затем родитель.
        return selectedIds.stream()
                .map(skillsById::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Отбирает идентификаторы навыков без создания Entity-объектов.
     * Значение notParsing должно быть явно false: так сохраняется прежнее
     * поведение, при котором null-запись не участвовала в распознавании.
     */
    static List<UUID> selectSkillIds(String inputText, List<SkillProjection> projections) {
        if (inputText == null || inputText.trim().isEmpty()
                || projections == null || projections.isEmpty()) {
            return new ArrayList<>();
        }

        String normalizedInput = inputText.toLowerCase(Locale.ROOT);
        LinkedHashSet<UUID> result = new LinkedHashSet<>();
        Set<String> selectedNames = new HashSet<>();
        int invalidRecords = 0;
        int undefinedParsingFlags = 0;

        for (SkillProjection projection : projections) {
            if (projection == null || projection.skillId == null || isBlank(projection.skillName)) {
                invalidRecords++;
                continue;
            }

            if (projection.notParsing == null) {
                undefinedParsingFlags++;
                continue;
            }
            if (!Boolean.FALSE.equals(projection.notParsing)) {
                continue;
            }

            String normalizedSkillName = normalizeName(projection.skillName);
            if (!normalizedInput.contains(normalizedSkillName)) {
                continue;
            }

            result.add(projection.skillId);
            selectedNames.add(normalizedSkillName);

            // Родитель добавляется после найденного навыка, только один раз и
            // только при явно разрешённом флаге парсинга.
            if (projection.parentId != null
                    && !isBlank(projection.parentSkillName)
                    && Boolean.FALSE.equals(projection.parentNotParsing)) {
                String normalizedParentName = normalizeName(projection.parentSkillName);
                if (selectedNames.add(normalizedParentName)) {
                    result.add(projection.parentId);
                }
            }
        }

        if (invalidRecords > 0) {
            log.warn("Пропущено {} некорректных записей SkillTree без ID или наименования", invalidRecords);
        }
        if (undefinedParsingFlags > 0) {
            log.warn("Пропущено {} записей SkillTree с неопределённым флагом notParsing", undefinedParsingFlags);
        }

        return new ArrayList<>(result);
    }

    private static String normalizeName(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    static final class SkillProjection {
        final UUID skillId;
        final String skillName;
        final Boolean notParsing;
        final UUID parentId;
        final String parentSkillName;
        final Boolean parentNotParsing;

        SkillProjection(UUID skillId,
                        String skillName,
                        Boolean notParsing,
                        UUID parentId,
                        String parentSkillName,
                        Boolean parentNotParsing) {
            this.skillId = skillId;
            this.skillName = skillName;
            this.notParsing = notParsing;
            this.parentId = parentId;
            this.parentSkillName = parentSkillName;
            this.parentNotParsing = parentNotParsing;
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
