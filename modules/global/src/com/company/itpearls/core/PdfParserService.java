package com.company.itpearls.core;

import com.company.itpearls.entity.SkillTree;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface PdfParserService {
    String NAME = "itpearls_PdfParserService";

    List <SkillTree> parseSkillTree(String inputText);

    String pdf2txt(String fileName) throws IOException;

    File getImageFromPDF(File file) throws IOException;
}