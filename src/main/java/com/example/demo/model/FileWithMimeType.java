package com.example.demo.model;

import lombok.Data;

@Data
public class FileWithMimeType {
    private byte[] data;
    private String mimeType;

    public FileWithMimeType(byte[] data, String mimeType) {
        this.data = data;
        this.mimeType = mimeType;
    }
}
