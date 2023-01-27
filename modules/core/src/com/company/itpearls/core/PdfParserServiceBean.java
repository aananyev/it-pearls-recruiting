package com.company.itpearls.core;

import com.company.itpearls.entity.SkillTree;
import com.haulmont.cuba.core.global.DataManager;
import org.apache.fop.pdf.PDFDocument;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.io.RandomAccessReadMemoryMappedFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service(PdfParserService.NAME)
public class PdfParserServiceBean implements PdfParserService {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(PdfParserServiceBean.class);
    @Inject
    private DataManager dataManager;

    @Override
    public List<SkillTree> parseSkillTree(String inputText) {
        String QUERY_SKILL_TREE =
                "select e " +
                        "from itpearls_SkillTree e";

        List<SkillTree> candidateSkills = new ArrayList<>();

        List<SkillTree> competitions = dataManager.load(SkillTree.class)
                .query(QUERY_SKILL_TREE)
                .view("skillTree-view")
                .cacheable(true)
                .list();

        for (SkillTree specialisation : competitions) {
            try {
                if (!specialisation.getNotParsing()) {
                    if (inputText.toLowerCase().contains(specialisation.getSkillName().toLowerCase())) {
                        candidateSkills.add(specialisation);

                        if (specialisation.getSkillTree() != null &&
                                checkHiLevel(candidateSkills, specialisation.getSkillTree())) {
                            candidateSkills.add(specialisation.getSkillTree());
                        }
                    }
                }
            } catch (NullPointerException e) {
                log.error("Error", e);
            }
        }
        // удаляем дубликаты
/*        Set<SkillTree> st = new HashSet<>(candidateSkills);
        candidateSkills.clear();
        candidateSkills.addAll(st); */

        return candidateSkills;
    }

    private boolean checkHiLevel(List<SkillTree> candidateSkills, SkillTree skillTree) {
        for (SkillTree a : candidateSkills) {
            if (skillTree.getNotParsing() == false) {
                if (a.getSkillName().toLowerCase().equals(
                        skillTree.getSkillName().toLowerCase())) {
                    return false;
                }
            }
        }

        return true;
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
/*            PDFParser parser = new PDFParser(new RandomAccessFile(new File(fileName), "r"));
            parser.parse();

            COSDocument cosDoc = parser.getDocument();
            PDFTextStripper pdfStripper = new PDFTextStripper();
            PDDocument pdDoc = new PDDocument(cosDoc);
            parsedText = pdfStripper.getText(pdDoc); */
            return null;
        }
    }

    @Override
    public File getImageFromPDF(File file) throws IOException {
        //Loading an existing PDF document

        RandomAccessRead rad = new RandomAccessReadMemoryMappedFile(file);
        File tempJpg;

        PDFParser parser = new PDFParser(rad);
        PDDocument document = parser.parse();
//        PDDocument document = PDDocument.load(file);

        //Instantiating the PDFRenderer class
        PDFRenderer renderer = new PDFRenderer(document);

        //Rendering an image from the PDF document
        BufferedImage image = renderer.renderImage(0);

        //Writing the image to a file
        tempJpg = File.createTempFile("img", ".jpg");

        ImageIO.write(image, "JPEG", tempJpg);

        //Closing the document
        document.close();

        return tempJpg;
    }

    @Override
    public String getImageFromNamePDF(File file) throws IOException {
        //Loading an existing PDF document
        RandomAccessRead rad = new RandomAccessReadMemoryMappedFile(file);
        PDFParser parser = new PDFParser(rad);
        PDDocument document = parser.parse();
//        PDDocument document = PDDocument.load(file);

        //Instantiating the PDFRenderer class
        PDFRenderer renderer = new PDFRenderer(document);

        //Rendering an image from the PDF document
        BufferedImage image = renderer.renderImage(0);

        //Writing the image to a file
        File tempJpg = File.createTempFile("img", ".jpg");

        ImageIO.write(image, "JPEG", tempJpg);

        //Closing the document
        document.close();

        return tempJpg.getName();
    }
}