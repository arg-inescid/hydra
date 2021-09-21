package com.lambda_manager.utils;

import java.io.File;
import java.io.FileOutputStream;

public class FileUtils {

    public static void purgeDirectory(File dir) {

        if (!dir.exists()) {
            return;
        }

        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                purgeDirectory(file);
            }
            file.delete();
        }

        dir.delete();
    }

    public static void writeBytesToFile(File file, byte[] functionCode) throws Exception {
        if (file.getParentFile().mkdirs()) {
            if (file.createNewFile()) {
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(functionCode);
                fileOutputStream.close();
            } else {
                throw new Exception(String.format("Error writing file %s", file.getPath()));
            }
        }
    }
}
