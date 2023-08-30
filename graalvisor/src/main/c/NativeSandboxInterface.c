#include <jni.h>
#include <unistd.h>
#include <signal.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <string.h>
#include <stdio.h>

#include "org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface.h"

#define PIPE_READ_END  0
#define PIPE_WRITE_END 1

void close_parent_fds(int childWrite, int parentRead) {
    // TODO - we should try to get a sense for the used file descriptors.
    for (int fd = 3; fd < 1024; fd++) {
        if (fd != childWrite && fd != parentRead) {
            close(fd);
        }
    }
}

void reset_parent_signal_handlers() {
    signal(SIGTERM, SIG_DFL);
    signal(SIGINT, SIG_DFL);
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_ginit(JNIEnv *env, jobject thisObj) {
    setbuf(stdout, NULL);
}

JNIEXPORT int JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_createNativeProcessSandbox(JNIEnv *env, jobject thisObj, jintArray childPipeFD, jintArray parentPipeFD) {
    int parentRead, parentWrite, childRead, childWrite;

    // Preparing child pipe (where the child writes and the parent reads).
    jint *childPipeFDptr = (*env)->GetIntArrayElements(env, childPipeFD, 0);
    pipe(childPipeFDptr);
    childRead = childPipeFDptr[PIPE_READ_END];
    childWrite = childPipeFDptr[PIPE_WRITE_END];
    (*env)->ReleaseIntArrayElements(env, childPipeFD, childPipeFDptr, 0);

    // Preparing the parent pipe (where the parent writes and the child reads).
    jint *parentPipeFDptr = (*env)->GetIntArrayElements(env, parentPipeFD, 0);
    pipe(parentPipeFDptr);
    parentRead = parentPipeFDptr[PIPE_READ_END];
    parentWrite = parentPipeFDptr[PIPE_WRITE_END];
    (*env)->ReleaseIntArrayElements(env, parentPipeFD, parentPipeFDptr, 0);

    // Forking.
    int pid = fork();
    if (pid == 0) {
        // Sanitizing the child process.
        close_parent_fds(childWrite, parentRead);
        reset_parent_signal_handlers();
    } else {
        // Close the unnecessary pipe ends.
        close(childPipeFDptr[PIPE_WRITE_END]);
        close(parentPipeFDptr[PIPE_READ_END]);
    }
    return pid;
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_createCgroup
  (JNIEnv *env, jclass thisObject, jstring isolateId) {
    const char *isol = (*env)->GetStringUTFChars(env, isolateId, NULL);
    char path[300];
    strcpy(path, "/sys/fs/cgroup/isolate/");
    strcat(path, isol);
    mkdir(path, 0777);
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_deleteCgroup
  (JNIEnv *env, jclass thisObject, jstring isolateId) {
    const char *isol = (*env)->GetStringUTFChars(env, isolateId, NULL);
    char path[300];
    strcpy(path, "/sys/fs/cgroup/isolate/");
    strcat(path, isol);
    rmdir(path);
}

JNIEXPORT string JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_setCgroupWeight
  (JNIEnv *env, jclass thisObject, jstring isolateId, jint quota) {
    const int period = 100000;
    const int pid = getpid();
    char maxQuota[32];
    const char *isol = (*env)->GetStringUTFChars(env, isolateId, NULL);
    char cGroupPath[256];
    char cGroupMax[256];
    char cGroupProcs[256];

    strcpy(cGroupPath, "/sys/fs/cgroup/isolate/");
    strcat(cGroupPath, isol);
    strcat(cGroupMax, "/cpu.max");
    strcat(cGroupProcs, "/cpu.max");

    sprintf(maxQuota, "%d %d", quota, period);

    int maxF = open(cGroupMax, O_WRONLY);
    write(maxF, maxQuota, strlen(maxQuota) + 1);
    close(maxF);

    int procsF = open(cGroupMax, O_WRONLY);
    write(procsF, pid, sizeof(pid));
    close(procsF);

    return path;
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_enterNativeProcessSandbox(JNIEnv *env, jobject thisObj) {

}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_leaveNativeProcessSandbox(JNIEnv *env, jobject thisObj) {

}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_destroyNativeProcessSandbox(JNIEnv *env, jobject thisObj) {

}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_createNativeIsolateSandbox(JNIEnv *env, jobject thisObj) {

}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_enterNativeIsolateSandbox(JNIEnv *env, jobject thisObj) {

}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_leaveNativeIsolateSandbox(JNIEnv *env, jobject thisObj) {

}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_destroyNativeIsolateSandbox(JNIEnv *env, jobject thisObj) {

}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_createNativeRuntimeSandbox(JNIEnv *env, jobject thisObj) {

}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_enterNativeRuntimeSandbox(JNIEnv *env, jobject thisObj) {

}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_leaveNativeRuntimeSandbox(JNIEnv *env, jobject thisObj) {

}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_destroyNativeRuntimeSandbox(JNIEnv *env, jobject thisObj) {

}