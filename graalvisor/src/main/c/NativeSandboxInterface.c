#define _GNU_SOURCE

#include <jni.h>
#include <unistd.h>
#include <signal.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <string.h>
#include <stdio.h>

#include "org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface.h"

#define PIPE_READ_END 0
#define PIPE_WRITE_END 1

void close_parent_fds(int childWrite, int parentRead)
{
    // TODO - we should try to get a sense for the used file descriptors.
    for (int fd = 3; fd < 1024; fd++)
    {
        if (fd != childWrite && fd != parentRead)
        {
            close(fd);
        }
    }
}

void reset_parent_signal_handlers()
{
    signal(SIGTERM, SIG_DFL);
    signal(SIGINT, SIG_DFL);
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_ginit(JNIEnv *env, jobject thisObj)
{
    setbuf(stdout, NULL);
    createMainCgroup(env, thisObj);
}

JNIEXPORT int JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_createNativeProcessSandbox(JNIEnv *env, jobject thisObj, jintArray childPipeFD, jintArray parentPipeFD)
{
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
    if (pid == 0)
    {
        // Sanitizing the child process.
        close_parent_fds(childWrite, parentRead);
        reset_parent_signal_handlers();
    }
    else
    {
        // Close the unnecessary pipe ends.
        close(childPipeFDptr[PIPE_WRITE_END]);
        close(parentPipeFDptr[PIPE_READ_END]);
    }
    return pid;
}

JNIEXPORT jint JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_getThreadId(JNIEnv *env, jclass thisObject)
{
    return gettid();
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_createMainCgroup(JNIEnv *env, jclass thisObject) {
    printf("Creating main cgroup\n");
    mkdir("/sys/fs/cgroup/user.slice/user-1000.slice/isolate", 0777);
    int fd = open("/sys/fs/cgroup/cgroup.subtree_control", O_WRONLY);
    write(fd, "+cpu +cpuset", 13);
    close(fd);
    fd = open("/sys/fs/cgroup/user.slice/cgroup.subtree_control", O_WRONLY);
    write(fd, "+cpu +cpuset", 13);
    close(fd);
    fd = open("/sys/fs/cgroup/user.slice/user-1000.slice/cgroup.subtree_control", O_WRONLY);
    write(fd, "+cpu +cpuset", 13);
    close(fd);
    fd = open("/sys/fs/cgroup/user.slice/user-1000.slice/isolate/cgroup.subtree_control", O_WRONLY);
    write(fd, "+cpu +cpuset", 13);
    close(fd);
    fd = open("/sys/fs/cgroup/user.slice/user-1000.slice/isolate/cpuset.cpus", O_WRONLY);
    write(fd, "0", 2);
    close(fd);
    fd = open("/sys/fs/cgroup/user.slice/user-1000.slice/isolate/cgroup.procs", O_WRONLY);
    int pid = getpid();
    char str[10];
    sprintf(str, "%d", pid);
    write(fd, str, strlen(str) + 1);
    close(fd);
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_deleteMainCgroup(JNIEnv *env, jclass thisObject) {
    printf("Deleting main cgroup\n");
    rmdir("/sys/fs/cgroup/user.slice/user-1000.slice/isolate");
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_createCgroup(JNIEnv *env, jclass thisObject, jstring isolateId)
{
    const char *isol = (*env)->GetStringUTFChars(env, isolateId, NULL);
    char cgroupPath[300];
    sprintf(cgroupPath, "/sys/fs/cgroup/user.slice/user-1000.slice/isolate/%s", isol);
    mkdir(cgroupPath, 0777);
    strcat(cgroupPath, "/cgroup.type");
    int fd = open(cgroupPath, O_WRONLY);
    write(fd, "threaded", 9);
    close(fd);
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_deleteCgroup(JNIEnv *env, jclass thisObject, jstring isolateId)
{
    const char *isol = (*env)->GetStringUTFChars(env, isolateId, NULL);
    char cgroupPath[300];
    sprintf(cgroupPath, "/sys/fs/cgroup/user.slice/user-1000.slice/isolate/%s", isol);
    rmdir(cgroupPath);
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_setCgroupQuota(JNIEnv *env, jclass thisObject, jstring isolateId, jint quota)
{
    const int period = 100000;
    const char *isol = (*env)->GetStringUTFChars(env, isolateId, NULL);
    char maxQuota[32];
    char cGroupMax[256];

    sprintf(maxQuota, "%d %d", quota, period);
    sprintf(cGroupMax, "/sys/fs/cgroup/user.slice/user-1000.slice/isolate/%s/cpu.max", isol);

    int maxF = open(cGroupMax, O_WRONLY);
    write(maxF, maxQuota, strlen(maxQuota) + 1);
    close(maxF);
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_insertThreadInCgroup(JNIEnv *env, jclass thisObject, jstring isolateId, jstring threadId)
{
    const char *isol = (*env)->GetStringUTFChars(env, isolateId, NULL);
    const char *t = (*env)->GetStringUTFChars(env, threadId, NULL);
    char cGroupThreads[300];

    sprintf(cGroupThreads, "/sys/fs/cgroup/user.slice/user-1000.slice/isolate/%s/cgroup.threads", isol);

    int fd = open(cGroupThreads, O_WRONLY);
    write(fd, t, strlen(t));
    close(fd);
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_removeThreadFromCgroup(JNIEnv *env, jclass thisObject, jstring threadId)
{
    const char *t = (*env)->GetStringUTFChars(env, threadId, NULL);
    char cGroupThreads[300];
     sprintf(cGroupThreads, "/sys/fs/cgroup/user.slice/user-1000.slice/isolate/cgroup.threads");
    int fd = open(cGroupThreads, O_WRONLY);
    int r = write(fd, t, strlen(t));
    close(fd);
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_enterNativeProcessSandbox(JNIEnv *env, jobject thisObj)
{
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_leaveNativeProcessSandbox(JNIEnv *env, jobject thisObj)
{
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_destroyNativeProcessSandbox(JNIEnv *env, jobject thisObj)
{
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_createNativeIsolateSandbox(JNIEnv *env, jobject thisObj)
{
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_enterNativeIsolateSandbox(JNIEnv *env, jobject thisObj)
{
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_leaveNativeIsolateSandbox(JNIEnv *env, jobject thisObj)
{
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_destroyNativeIsolateSandbox(JNIEnv *env, jobject thisObj)
{
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_createNativeRuntimeSandbox(JNIEnv *env, jobject thisObj)
{
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_enterNativeRuntimeSandbox(JNIEnv *env, jobject thisObj)
{
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_leaveNativeRuntimeSandbox(JNIEnv *env, jobject thisObj)
{
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_destroyNativeRuntimeSandbox(JNIEnv *env, jobject thisObj)
{
}