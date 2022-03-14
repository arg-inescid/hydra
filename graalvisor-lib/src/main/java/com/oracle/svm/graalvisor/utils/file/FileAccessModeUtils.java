package com.oracle.svm.graalvisor.utils.file;

import static com.oracle.svm.graalvisor.utils.file.Fcntl.*;

import com.oracle.svm.graalvisor.guestapi.file.FileAccessMode;

public class FileAccessModeUtils {
    public static final int DEFAULT_PERMISSIONS = 0666;

    public static int getFileAccessMode(FileAccessMode fileAccessMode) {
        switch (fileAccessMode) {
            case READ:
                return O_RDONLY();
            case READ_WRITE:
                return O_CREAT() | O_RDWR();
            case WRITE:
                return O_CREAT() | O_WRONLY();
            case WRITE_APPEND:
                return O_CREAT() | O_APPEND() | O_WRONLY();
            case READ_WRITE_APPEND:
                return O_CREAT() | O_APPEND() | O_RDWR();
        }
        return O_RDONLY();
    }
}
