#define _GNU_SOURCE

#include <jni.h>
#include <unistd.h>
#include <signal.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <string.h>
#include <stdio.h>
#include <time.h>

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
    mkdir("/sys/fs/cgroup/user.slice/user-1000.slice/gv-cgroups", 0777);
    int fd = open("/sys/fs/cgroup/cgroup.subtree_control", O_WRONLY);
    write(fd, "+cpu +cpuset", 13);
    close(fd);
    fd = open("/sys/fs/cgroup/user.slice/cgroup.subtree_control", O_WRONLY);
    write(fd, "+cpu +cpuset", 13);
    close(fd);
    fd = open("/sys/fs/cgroup/user.slice/user-1000.slice/cgroup.subtree_control", O_WRONLY);
    write(fd, "+cpu +cpuset", 13);
    close(fd);
    fd = open("/sys/fs/cgroup/user.slice/user-1000.slice/gv-cgroups/cgroup.subtree_control", O_WRONLY);
    write(fd, "+cpu +cpuset", 13);
    close(fd);
    fd = open("/sys/fs/cgroup/user.slice/user-1000.slice/gv-cgroups/cpuset.cpus", O_WRONLY);
    write(fd, "0", 2);
    close(fd);
    fd = open("/sys/fs/cgroup/user.slice/user-1000.slice/gv-cgroups/cgroup.procs", O_WRONLY);
    int pid = getpid();
    char str[10];
    sprintf(str, "%d", pid);
    write(fd, str, strlen(str) + 1);
    close(fd);
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_deleteMainCgroup(JNIEnv *env, jclass thisObject) {
    printf("Deleting main cgroup\n");
//    rmdir("/sys/fs/cgroup/user.slice/user-1000.slice/gv-cgroups/cgroup-*"); TODO - should I add this if shutdown hook is working?
    rmdir("/sys/fs/cgroup/user.slice/user-1000.slice/gv-cgroups");
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_createCgroup(JNIEnv *env, jclass thisObject, jstring cgroupId)
{
    printf("(C) Creating cgroup\n");
    clock_t t = clock();
    const char *isol = (*env)->GetStringUTFChars(env, cgroupId, NULL);
    char cgroupPath[300];
    sprintf(cgroupPath, "/sys/fs/cgroup/user.slice/user-1000.slice/gv-cgroups/%s", isol);
    int success = mkdir(cgroupPath, 0777);
    if (success != 0)
    {
        printf("(C) Failed to create cgroup\n");
        return;
    }
    strcat(cgroupPath, "/cgroup.type");
    int fd = open(cgroupPath, O_WRONLY);
    if (fd == -1)
    {
        printf("(C) Failed to open cgroup.type\n");
        return;
    }
    ssize_t success2 = write(fd, "threaded", 9);
    if (success2 == -1)
    {
        printf("(C) Failed to write to cgroup.type\n");
        return;
    }
    int success = close(fd);
    if (success != 0)
    {
        printf("(C) Failed to close cgroup.type\n");
        return;
    }
    t = clock() - t;
    double time_taken = ((double)t) / CLOCKS_PER_SEC;
    printf("(C) Created cgroup in %f miliseconds\n", time_taken*1000);
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_deleteCgroup(JNIEnv *env, jclass thisObject, jstring cgroupId)
{
    const char *isol = (*env)->GetStringUTFChars(env, cgroupId, NULL);
    char cgroupPath[300];
    sprintf(cgroupPath, "/sys/fs/cgroup/user.slice/user-1000.slice/gv-cgroups/%s", isol);
    rmdir(cgroupPath);
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_setCgroupQuota(JNIEnv *env, jclass thisObject, jstring cgroupId, jint quota)
{
    const int period = 100000;
    const char *isol = (*env)->GetStringUTFChars(env, cgroupId, NULL);
    char maxQuota[32];
    char cGroupMax[256];

    sprintf(maxQuota, "%d %d", quota, period);
    sprintf(cGroupMax, "/sys/fs/cgroup/user.slice/user-1000.slice/gv-cgroups/%s/cpu.max", isol);

    int maxF = open(cGroupMax, O_WRONLY);
    write(maxF, maxQuota, strlen(maxQuota) + 1);
    close(maxF);
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_insertThreadInCgroup(JNIEnv *env, jclass thisObject, jstring cgroupId, jstring threadId)
{
    const char *isol = (*env)->GetStringUTFChars(env, cgroupId, NULL);
    const char *t = (*env)->GetStringUTFChars(env, threadId, NULL);
    char cGroupThreads[300];

    sprintf(cGroupThreads, "/sys/fs/cgroup/user.slice/user-1000.slice/gv-cgroups/%s/cgroup.threads", isol);

    int fd = open(cGroupThreads, O_WRONLY);
    write(fd, t, strlen(t));
    close(fd);
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_removeThreadFromCgroup(JNIEnv *env, jclass thisObject, jstring threadId)
{
    const char *t = (*env)->GetStringUTFChars(env, threadId, NULL);
    char cGroupThreads[300];
    sprintf(cGroupThreads, "/sys/fs/cgroup/user.slice/user-1000.slice/gv-cgroups/cgroup.threads");
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