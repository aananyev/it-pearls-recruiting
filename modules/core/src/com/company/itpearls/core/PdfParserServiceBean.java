package com.company.itpearls.core;

import com.company.itpearls.entity.SkillTree;
import com.haulmont.cuba.core.global.DataManager;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
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

        for(SkillTree specialisation : competitions) {
            if(inputText.contains(specialisation.getSkillName())) {
                candidateSkills.add(specialisation);
            }
        }
        return competitions;
    }

    @Override
    public String pdf2txt(String fileName) throws IOException {
        String parsedText = "";

        if(fileName.contains("pdf")) {

            PDFParser parser = new PDFParser(new RandomAccessFile(new File(fileName), "r"));
            parser.parse();

            COSDocument cosDoc = parser.getDocument();
            PDFTextStripper pdfStripper = new PDFTextStripper();
            PDDocument pdDoc = new PDDocument(cosDoc);
            parsedText = pdfStripper.getText(pdDoc);
        }

        return parsedText;
    }
}