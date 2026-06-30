package com.company.hunttech.app;

public interface ImageProcessingService {
    String NAME = "hunttech_ImageProcessingService";

    ProcessedImage process(byte[] data, String fileName);
}
