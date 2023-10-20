package com.oracle.svm.graalvisor.utils;

import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.Pointer;

import com.oracle.svm.core.SubstrateUtil;

public class StringUtils {

    private static ThreadLocal<byte[]> threadLocalBuffer = new ThreadLocal<>();

    public static String retrieveString(ObjectHandle handle) {
        String res = ObjectHandles.getGlobal().get(handle);
        ObjectHandles.getGlobal().destroy(handle);
        return res;
    }

    public static String copyString(CCharPointer cString) {
        int stringLength = (int) SubstrateUtil.strlen(cString).rawValue();
        byte[] bytes = threadLocalBuffer.get();
        if (bytes == null || bytes.length < stringLength) {
            bytes = new byte[stringLength];
            threadLocalBuffer.set(bytes);
        }

        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = ((Pointer) cString).readByte(i);
        }
        return new String(bytes, 0, stringLength);
    }
}
