package com.lambda_manager.callbacks.impl;

import com.lambda_manager.callbacks.OnProcessFinishCallback;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class CopyOutputLogFileCallback implements OnProcessFinishCallback {

    private final String sourceFilename;
    private final String destinationFilename;

    public CopyOutputLogFileCallback(String sourceFilename, String destinationFile) {
        this.sourceFilename = sourceFilename;
        this.destinationFilename = destinationFile;
    }

    @Override
    public void finish(int exitCode) {
        File sourceFile = new File(sourceFilename);
        try(FileInputStream fileInputStream = new FileInputStream(sourceFile);
            FileWriter fileWriter = new FileWriter(destinationFilename, true)) {
            byte[] data = new byte[(int) sourceFile.length()];
            //noinspection ResultOfMethodCallIgnored
            fileInputStream.read(data);
            fileWriter.write(new String(data, StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
