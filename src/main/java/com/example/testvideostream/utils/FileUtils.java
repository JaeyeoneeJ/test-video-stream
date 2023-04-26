package com.example.testvideostream.utils;

import java.io.File;

public class FileUtils {
    public static File checkFilePath (String outputDir) {
        File mediaDir = new File(outputDir);
        if (!mediaDir.exists()) {
            mediaDir.mkdirs();
        }
        return mediaDir;
    }
}