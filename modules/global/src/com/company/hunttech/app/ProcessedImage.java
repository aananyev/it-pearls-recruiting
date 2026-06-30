package com.company.hunttech.app;

import java.io.Serializable;

public class ProcessedImage implements Serializable {

    private final byte[] data;
    private final String name;
    private final String extension;
    private final boolean processed;

    public ProcessedImage(byte[] data, String name, String extension, boolean processed) {
        this.data = data;
        this.name = name;
        this.extension = extension;
        this.processed = processed;
    }

    public byte[] getData() {
        return data;
    }

    public String getName() {
        return name;
    }

    public String getExtension() {
        return extension;
    }

    public boolean isProcessed() {
        return processed;
    }
}
