package com.company.itpearls.core;

import com.company.itpearls.entity.SkillTree;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public interface PdfParserService {
    String NAME = "itpearls_PdfParserService";

    List<RenderedImage> getImagesFromPDF(PDDocument document) throws IOException;

    List<RenderedImage> getImagesFromResources(PDResources resources) throws IOException;

    List<SkillTree> parseSkillTree(String inputText);

    String pdf2txt(String fileName) throws IOException;

    File getImageFromPDF(File file) throws IOException;

    String getImageFromNamePDF(File file) throws IOException;
}