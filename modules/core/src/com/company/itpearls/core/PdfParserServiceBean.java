package com.company.itpearls.core;

import com.company.itpearls.entity.SkillTree;
import com.haulmont.cuba.core.global.DataManager;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service(PdfParserService.NAME)
public class PdfParserServiceBean implements PdfParserService {

    @Inject
    private DataManager dataManager;

    @Override
    public List<SkillTree> parseSkillTree(String inputText) {
        List<SkillTree> candidateSkills = new ArrayList<>();

        List<SkillTree> competitions = dataManager.load(SkillTree.class)
                .view("skillTree-view")
                .list();

        for (SkillTree specialisation : competitions) {
            try {
                if (inputText.toLowerCase().contains(specialisation.getSkillName().toLowerCase())) {
                    candidateSkills.add(specialisation);

                    if (specialisation.getSkillTree() != null &&
                            checkHiLevel(candidateSkills, specialisation.getSkillTree())) {
                        candidateSkills.add(specialisation.getSkillTree());
                    }
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }

        return candidateSkills;
    }

    private boolean checkHiLevel(List<SkillTree> candidateSkills, SkillTree skillTree) {
        for (SkillTree a : candidateSkills) {
            if(a.getSkillName().toLowerCase().equals(skillTree.getSkillName().toLowerCase())) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String pdf2txt(String fileName) throws IOException {
        String parsedText = "";

        if (fileName.contains("pdf")) {

            PDFParser parser = new PDFParser(new RandomAccessFile(new File(fileName), "r"));
            parser.parse();

            COSDocument cosDoc = parser.getDocument();
            PDFTextStripper pdfStripper = new PDFTextStripper();
            PDDocument pdDoc = new PDDocument(cosDoc);
            parsedText = pdfStripper.getText(pdDoc);
        }

        return parsedText;
    }

    @Override
    public File getImageFromPDF(File file) throws IOException {
        //Loading an existing PDF document
        PDDocument document = PDDocument.load(file);

        //Instantiating the PDFRenderer class
        PDFRenderer renderer = new PDFRenderer(document);

        //Rendering an image from the PDF document
        BufferedImage image = renderer.renderImage(0);

        //Writing the image to a file
        File tempJpg = File.createTempFile("img", ".jpg");

        ImageIO.write (image, "JPEG", tempJpg);

        //Closing the document
        document.close();
        return tempJpg;
    }

    @Override
    public String getImageFromNamePDF(File file) throws IOException {
        //Loading an existing PDF document
        PDDocument document = PDDocument.load(file);

        //Instantiating the PDFRenderer class
        PDFRenderer renderer = new PDFRenderer(document);

        //Rendering an image from the PDF document
        BufferedImage image = renderer.renderImage(0);

        //Writing the image to a file
        File tempJpg = File.createTempFile("img", ".jpg");

        ImageIO.write (image, "JPEG", tempJpg);

        //Closing the document
        document.close();

        return tempJpg.getName();
    }
}