#define _GNU_SOURCE

#include <jni.h>
#include <unistd.h>
#include <signal.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <string.h>
#include <stdio.h>
#include <time.h>
#include <errno.h>

#include "org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface.h"

#define PIPE_READ_END 0
#define PIPE_WRITE_END 1
//#define CGROUP_BASE_PATH "/sys/fs/cgroup/"
//#define USER_CGROUP_PATH "/sys/fs/cgroup/user.slice/"
//#define GV_CGROUP_FULL_PATH "/sys/fs/cgroup/user.slice/user-1000.slice/gv-cgroups"

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

    char cgroupPath[300];
    sprintf(cgroupPath, "/sys/fs/cgroup/user.slice/user-1000.slice/gv-cgroups");
    if (mkdir(cgroupPath, 0777) != 0) {
        printf("Failed to create gv-cgroups - path: %s\n", cgroupPath);
    };
    printf("Created gv-cgroups at %s\n", cgroupPath);

//    int fd = open("/sys/fs/cgroup/cgroup.subtree_control", O_WRONLY);
//    if (fd == -1) {
//        printf("Failed to open cgroup.subtree_control ERROR: %d\n", errno);
//    }
//    if (write(fd, "+cpu +cpuset", 13) != 0) {
//        printf("Failed to write to cgroup.subtree_control ERROR: %d\n", errno);
//    }
//    if (close(fd) != 0) {
//        printf("Failed to close cgroup.subtree_control ERROR: %d\n", errno);
//    }
//
//    fd = open("/sys/fs/cgroup/user.slice/cgroup.subtree_control", O_WRONLY);
//    if (fd == -1) {
//        printf("Failed to open user.slice/cgroup.subtree_control ERROR: %d\n", errno);
//    }
//    if (write(fd, "+cpu +cpuset", 13) != 0) {
//        printf("Failed to write to user.slice/cgroup.subtree_control ERROR: %d\n", errno);
//    }
//    if (close(fd) != 0) {
//        printf("Failed to close user.slice/cgroup.subtree_control ERROR: %d\n", errno);
//    }
//
//    fd = open("/sys/fs/cgroup/user.slice/user-1000.slice/cgroup.subtree_control", O_WRONLY);
//    if (fd == -1) {
//        printf("Failed to open user.slice/user-1000.slice/cgroup.subtree_control ERROR: %d\n", errno);
//    }
//    if (write(fd, "+cpu +cpuset", 13) != 0) {
//        printf("Failed to write to user.slice/user-1000.slice/cgroup.subtree_control ERROR: %d\n", errno);
//    }
//    if (close(fd) != 0) {
//        printf("Failed to close user.slice/user-1000.slice/cgroup.subtree_control ERROR: %d\n", errno);
//    }
//
    int fd = open("/sys/fs/cgroup/user.slice/user-1000.slice/gv-cgroups/cgroup.subtree_control", O_WRONLY);
    if (fd == -1) {
        printf("Failed to open user.slice/user-1000.slice/gv-cgroups/cgroup.subtree_control ERROR: %d\n", errno);
    }
    if (write(fd, "+cpu +cpuset", 13) != 0) {
        printf("Failed to write to user.slice/user-1000.slice/gv-cgroups/cgroup.subtree_control ERROR: %d\n", errno);
    }
    if (close(fd) != 0) {
        printf("Failed to close user.slice/user-1000.slice/gv-cgroups/cgroup.subtree_control ERROR: %d\n", errno);
    }
//
//    fd = open("/sys/fs/cgroup/user.slice/user-1000.slice/gv-cgroups/cpuset.cpus", O_WRONLY);
//    if (fd == -1) {
//        printf("Failed to open user.slice/user-1000.slice/gv-cgroups/cpuset.cpus\n");
//    }
//    if (write(fd, "0", 2) != 0) {
//        printf("Failed to write to user.slice/user-1000.slice/gv-cgroups/cpuset.cpus\n");
//    }
//    if (close(fd) != 0) {
//        printf("Failed to close user.slice/user-1000.slice/gv-cgroups/cpuset.cpus\n");
//    }

    strcat(cgroupPath, "/cgroup.procs");
    fd = open(cgroupPath, O_WRONLY);
    if (fd == -1) {
        printf("Failed to open %s ERROR: %d\n", cgroupPath, errno);
    }

    int pid = getpid();
    char str[10];
    sprintf(str, "%d", pid);
    if (write(fd, str, strlen(str) + 1) == -1) {
        printf("Failed to write to %s ERROR: %d\n", cgroupPath, errno);
    }
    if (close(fd) != 0) {
        printf("Failed to close %s ERROR: %d\n", cgroupPath, errno);
    }
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_deleteMainCgroup(JNIEnv *env, jclass thisObject) {
    printf("Deleting main cgroup\n");
    rmdir("/sys/fs/cgroup/user.slice/user-1000.slice/gv-cgroups/cgroup-*");
    rmdir("/sys/fs/cgroup/user.slice/user-1000.slice/gv-cgroups");
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_createCgroup(JNIEnv *env, jclass thisObject, jstring cgroupId)
{
    const char *cgroup = (*env)->GetStringUTFChars(env, cgroupId, NULL);
    char cgroupPath[300];
    sprintf(cgroupPath, "/sys/fs/cgroup/user.slice/user-1000.slice/gv-cgroups/%s", cgroup);
    if (mkdir(cgroupPath, 0777) != 0) {
        printf("Failed to create %s ERROR: %d\n", cgroupPath, errno);
    }

    strcat(cgroupPath, "/cgroup.type");
    int fd = open(cgroupPath, O_WRONLY);
    if (fd == -1)
    {
        printf("Failed to open %s ERROR: %d\n", cgroupPath, errno);
    }
    if (write(fd, "threaded", 9) == -1) {
        printf("Failed to write to %s ERROR: %d\n", cgroupPath, errno);
    }
    if (close(fd) != 0) {
        printf("Failed to close %s ERROR: %d\n", cgroupPath, errno);
    }
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
    if (maxF == -1)
    {
        printf("Failed to open %s ERRNO: %d \n", cGroupMax, errno);
    }
    if (write(maxF, maxQuota, strlen(maxQuota) + 1) == -1) {
        printf("Failed to write to %s ERRNO: %d \n", cGroupMax, errno);
    }
    if (close(maxF) != 0) {
        printf("Failed to close %s ERRNO: %d \n", cGroupMax, errno);
    }
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_insertThreadInCgroup(JNIEnv *env, jclass thisObject, jstring cgroupId, jstring threadId)
{
    const char *isol = (*env)->GetStringUTFChars(env, cgroupId, NULL);
    const char *t = (*env)->GetStringUTFChars(env, threadId, NULL);
    char cGroupThreads[300];

    sprintf(cGroupThreads, "/sys/fs/cgroup/user.slice/user-1000.slice/gv-cgroups/%s/cgroup.threads", isol);

    int fd = open(cGroupThreads, O_WRONLY);
    if (fd == -1)
    {
        printf("Failed to open %s ERROR: %d\n", cGroupThreads, errno);
    }
    if (write(fd, t, strlen(t) + 1) == -1) {
        printf("Failed to write to %s ERROR: %d\n", cGroupThreads, errno);
    }
    if (close(fd) != 0) {
        printf("Failed to close %s ERROR: %d\n", cGroupThreads, errno);
    }
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_removeThreadFromCgroup(JNIEnv *env, jclass thisObject, jstring threadId)
{
    const char *t = (*env)->GetStringUTFChars(env, threadId, NULL);
    char cGroupThreads[300];
    sprintf(cGroupThreads, "/sys/fs/cgroup/user.slice/user-1000.slice/gv-cgroups/cgroup.threads");

    int fd = open(cGroupThreads, O_WRONLY);
    if (fd == -1)
    {
        printf("Failed to open %s ERROR: %d\n", cGroupThreads, errno);
    }
    if (write(fd, t, strlen(t)) == -1) {
        printf("Failed to write to %s ERROR: %d\n", cGroupThreads, errno);
    }
    if (close(fd) != 0) {
        printf("Failed to close %s ERROR: %d\n", cGroupThreads, errno);
    }
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