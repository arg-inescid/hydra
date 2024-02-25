#include <jni.h>
#include <unistd.h>
#include <signal.h>
#include <string.h>
#include <sys/types.h>
#ifdef LAZY_ISOLATION
#include "lazyisolation.h"
#endif
#include "svm-snapshot.h"
#include "org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface.h"

#define PIPE_READ_END  0
#define PIPE_WRITE_END 1

// Isolate and abi pointer array. These should be indexed using an svmid.
#define MAX_SVM_ID 16
graal_isolate_t* isolates[MAX_SVM_ID];
isolate_abi_t abis[MAX_SVM_ID];

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
    memset(isolates, 0, sizeof(graal_isolate_t*) * MAX_SVM_ID);
    memset(abis, 0, sizeof(isolate_abi_t) * MAX_SVM_ID);
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

JNIEXPORT long JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_svmAttachThread(
        JNIEnv *env, jobject thisObj, jint svmid) {
    graal_isolatethread_t* isolatethread;
    abis[svmid].graal_attach_thread(isolates[svmid], &isolatethread);
    return (long) isolatethread;
}

JNIEXPORT jstring JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_svmEntrypoint(
        JNIEnv *env, jobject thisObj, jint svmid, long isolatethread) {
    run_entrypoint(&(abis[svmid]), isolates[svmid], (graal_isolatethread_t*) isolatethread);
    return (*env)->NewStringUTF(env, "sandbox entrypoint should return :-)");
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_svmDetachThread(
        JNIEnv *env, jobject thisObj, jint svmid, long isolatethread) {
    abis[svmid].graal_detach_thread((graal_isolatethread_t*) isolatethread);
}

JNIEXPORT jstring JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_svmCheckpoint(
        JNIEnv *env, jobject thisObj, jint svmid, jstring func_path, jstring func_args, jstring meta_snap_path, jstring mem_snap_path) {
    const char* func_path_str = (*env)->GetStringUTFChars(env, func_path, 0);
    const char* func_args_str = (*env)->GetStringUTFChars(env, func_args, 0);
    const char* meta_snap_path_str = (*env)->GetStringUTFChars(env, meta_snap_path, 0);
    const char* mem_snap_path_str = (*env)->GetStringUTFChars(env, mem_snap_path, 0);
    checkpoint_svm(func_path_str, func_args_str, svmid, meta_snap_path_str, mem_snap_path_str, &abis[svmid], &isolates[svmid]);
    (*env)->ReleaseStringUTFChars(env, func_path, func_path_str);
    (*env)->ReleaseStringUTFChars(env, func_args, func_args_str);
    (*env)->ReleaseStringUTFChars(env, meta_snap_path, meta_snap_path_str);
    (*env)->ReleaseStringUTFChars(env, mem_snap_path, mem_snap_path_str);
    return (*env)->NewStringUTF(env, "sandbox checkpoint should return :-)");
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_svmRestore(
        JNIEnv *env, jobject thisObj, jint svmid, jstring meta_snap_path, jstring mem_snap_path) {
    const char* meta_snap_path_str = (*env)->GetStringUTFChars(env, meta_snap_path, 0);
    const char* mem_snap_path_str = (*env)->GetStringUTFChars(env, mem_snap_path, 0);
    restore_svm(meta_snap_path_str, mem_snap_path_str, &abis[svmid], &isolates[svmid]);
    (*env)->ReleaseStringUTFChars(env, mem_snap_path, mem_snap_path_str);
    (*env)->ReleaseStringUTFChars(env, meta_snap_path, meta_snap_path_str);
}