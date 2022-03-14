package com.oracle.svm.graalvisor.utils.file;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CConstant;

import com.oracle.svm.core.posix.headers.PosixDirectives;

@CContext(PosixDirectives.class)
class Fcntl {

    @CConstant
    public static native int O_RDONLY();

    @CConstant
    public static native int O_APPEND();

    @CConstant
    public static native int O_RDWR();

    @CConstant
    public static native int O_WRONLY();

    @CConstant
    public static native int O_CREAT();

}