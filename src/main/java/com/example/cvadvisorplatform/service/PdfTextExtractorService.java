package com.example.cvadvisorplatform.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PdfTextExtractorService {

    public String extractText(MultipartFile file)
            throws Exception {

        try (
                PDDocument document =
                        Loader.loadPDF(file.getBytes())
        ) {

            PDFTextStripper stripper =
                    new PDFTextStripper();

            return stripper.getText(document);
        }
    }
}