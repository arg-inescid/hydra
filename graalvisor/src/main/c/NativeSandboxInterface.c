#include <jni.h>
#include <unistd.h>
#include <signal.h>
#include <sys/types.h>
#ifdef LAZY_ISOLATION
#include "lazyisolation.h"
#endif
#include "org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface.h"
#include "network-isolation.h"

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

JNIEXPORT jboolean JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_isLazyIsolationSupported(JNIEnv *env, jobject thisObj) {
#ifdef LAZY_ISOLATION
    return 1;
#else
    return 0;
#endif
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_ginit(JNIEnv *env, jobject thisObj) {
    setbuf(stdout, NULL);
#ifdef LAZY_ISOLATION
        initialize_seccomp();
#endif
}

JNIEXPORT int JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_createNativeProcessSandbox(JNIEnv *env, jobject thisObj, jintArray childPipeFD, jintArray parentPipeFD, jboolean lazyIsolation) {
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
#ifdef LAZY_ISOLATION
        if (lazyIsolation) {
            install_proc_filter(childPipeFDptr);
        }
#endif
    } else {
        // Close the unnecessary pipe ends.
        close(childPipeFDptr[PIPE_WRITE_END]);
        if (!lazyIsolation) {
            close(parentPipeFDptr[PIPE_READ_END]);
        }
#ifdef LAZY_ISOLATION
        if (lazyIsolation) {
            attach(pid, childPipeFDptr, parentPipeFDptr);
        }
#endif
    }
    return pid;
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_createNativeIsolateSandbox(JNIEnv *env, jobject thisObj, jboolean lazyIsolation) {
#ifdef LAZY_ISOLATION
    if (lazyIsolation) {
        install_thread_filter();
    }
#endif
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_createNativeRuntimeSandbox(JNIEnv *env, jobject thisObj, jboolean lazyIsolation) {
#ifdef LAZY_ISOLATION
    if (lazyIsolation) {
        install_thread_filter();
    }
#endif
}

JNIEXPORT jint JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_switchToDefaultNetworkNamespace(JNIEnv *env, jobject thisObj) {
    return switchToDefaultNetworkNamespace();
}

JNIEXPORT jint JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_switchNetworkNamespace(JNIEnv *env, jobject thisObj, jstring jName) {
    const char *ns_name = (*env)->GetStringUTFChars(env, jName, 0);
    int ret = switchNetworkNamespace(ns_name);
    (*env)->ReleaseStringUTFChars(env, jName, ns_name);
    return ret;
}

JNIEXPORT jint JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_createNetworkNamespace(JNIEnv *env, jobject thisObj, jstring jName, jint jThirdByte, jint jFourthByte) {
    const char *ns_name = (*env)->GetStringUTFChars(env, jName, 0);
    int thirdByte = (int) jThirdByte;
    int fourthByte = (int) jFourthByte;
    int ret = createNetworkNamespace(ns_name, thirdByte, fourthByte);
    (*env)->ReleaseStringUTFChars(env, jName, ns_name);
    return ret;
}

JNIEXPORT jint JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_deleteNetworkNamespace(JNIEnv *env, jobject thisObj, jstring jName) {
    const char *ns_name = (*env)->GetStringUTFChars(env, jName, 0);
    int ret = deleteNetworkNamespace(ns_name);
    (*env)->ReleaseStringUTFChars(env, jName, ns_name);
    return ret;
}

JNIEXPORT jint JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_enableVeths(JNIEnv *env, jobject thisObj, jstring jName) {
    const char *ns_name = (*env)->GetStringUTFChars(env, jName, 0);
    int ret = enableVeths(ns_name);
    (*env)->ReleaseStringUTFChars(env, jName, ns_name);
    return ret;
}

JNIEXPORT jint JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_disableVeths(JNIEnv *env, jobject thisObj, jstring jName) {
    const char *ns_name = (*env)->GetStringUTFChars(env, jName, 0);
    int ret = disableVeths(ns_name);
    (*env)->ReleaseStringUTFChars(env, jName, ns_name);
    return ret;
}
