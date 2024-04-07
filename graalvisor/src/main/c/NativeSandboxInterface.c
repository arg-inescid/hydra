#include <jni.h>
#include <unistd.h>
#include <signal.h>
#include <string.h>
#include <sys/types.h>
#ifdef LAZY_ISOLATION
#include "lazyisolation.h"
#endif
#ifdef SVM_SNAPSHOT
#include "svm-snapshot.h"
#endif
#include "org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface.h"
#include "network-isolation.h"

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

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_ginit(JNIEnv *env, jobject thisObj) {
    setbuf(stdout, NULL);
    memset(isolates, 0, sizeof(graal_isolate_t*) * MAX_SVM_ID);
    memset(abis, 0, sizeof(isolate_abi_t) * MAX_SVM_ID);
    // Note: for some reason that we should track down, the first call to dup2 takes 10s of ms.
    // We do it now to avoid further latency later.
    int dummy = dup2(0, 1023);
    if (dummy >= 0) {
        close(dummy);
    }
#ifdef LAZY_ISOLATION
        initialize_seccomp();
#endif
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
#ifdef LAZY_ISOLATION
        install_proc_filter(childPipeFDptr);
#endif
    } else {
        // Close the unnecessary pipe ends.
        close(childPipeFDptr[PIPE_WRITE_END]);
#ifdef LAZY_ISOLATION
        attach(pid, childPipeFDptr, parentPipeFDptr);
#else
        close(parentPipeFDptr[PIPE_READ_END]);
#endif
    }
    return pid;
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_createNativeIsolateSandbox(JNIEnv *env, jobject thisObj) {
#ifdef LAZY_ISOLATION
    install_thread_filter();
#endif
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_createNativeRuntimeSandbox(JNIEnv *env, jobject thisObj) {
#ifdef LAZY_ISOLATION
    install_thread_filter();
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

JNIEXPORT jint JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_createNetworkNamespace(JNIEnv *env, jobject thisObj, jstring jName, jint jThirdByte, jint jSecondByte) {
    const char *ns_name = (*env)->GetStringUTFChars(env, jName, 0);
    int thirdByte = (int) jThirdByte;
    int secondByte = (int) jSecondByte;
    int ret = createNetworkNamespace(ns_name, thirdByte, secondByte);
    (*env)->ReleaseStringUTFChars(env, jName, ns_name);
    return ret;
}

JNIEXPORT jint JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_deleteNetworkNamespace(JNIEnv *env, jobject thisObj, jstring jName) {
    const char *ns_name = (*env)->GetStringUTFChars(env, jName, 0);
    int ret = deleteNetworkNamespace(ns_name);
    (*env)->ReleaseStringUTFChars(env, jName, ns_name);
    return ret;
}

JNIEXPORT jint JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_enableVeths(JNIEnv *env, jobject thisObj, jstring jName, jint jThirdByte, jint jSecondByte) {
    const char *ns_name = (*env)->GetStringUTFChars(env, jName, 0);
    int thirdByte = (int) jThirdByte;
    int secondByte = (int) jSecondByte;
    int ret = enableVeths(ns_name, thirdByte, secondByte);
    (*env)->ReleaseStringUTFChars(env, jName, ns_name);
    return ret;
}

JNIEXPORT jint JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_disableVeths(JNIEnv *env, jobject thisObj, jstring jName) {
    const char *ns_name = (*env)->GetStringUTFChars(env, jName, 0);
    int ret = disableVeths(ns_name);
    (*env)->ReleaseStringUTFChars(env, jName, ns_name);
    return ret;
}

JNIEXPORT long JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_svmAttachThread(
        JNIEnv *env, jobject thisObj, jint svmid) {
    graal_isolatethread_t* isolatethread;
    abis[svmid].graal_attach_thread(isolates[svmid], &isolatethread);
    return (long) isolatethread;
}

JNIEXPORT jstring JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_svmEntrypoint(
        JNIEnv *env, jobject thisObj, jint svmid, long isolatethread, jstring fin) {
    char fout[256];
    const char* fin_str = (*env)->GetStringUTFChars(env, fin, 0);
    run_entrypoint(&(abis[svmid]), isolates[svmid], (graal_isolatethread_t*) isolatethread, 1, 1, fin_str, fout, 256);
    (*env)->ReleaseStringUTFChars(env, fin, fin_str);
    return (*env)->NewStringUTF(env, fout);
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_svmDetachThread(
        JNIEnv *env, jobject thisObj, jint svmid, long isolatethread) {
    abis[svmid].graal_detach_thread((graal_isolatethread_t*) isolatethread);
}

JNIEXPORT jstring JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_svmCheckpoint(
        JNIEnv *env,
        jobject thisObj,
        jint svmid,
        jstring fpath,
        jint concurrency,
        jint requests,
        jstring fin,
        jstring meta_snap_path,
        jstring mem_snap_path) {
    const char* fpath_str = (*env)->GetStringUTFChars(env, fpath, 0);
    const char* meta_snap_path_str = (*env)->GetStringUTFChars(env, meta_snap_path, 0);
    const char* mem_snap_path_str = (*env)->GetStringUTFChars(env, mem_snap_path, 0);
    const char* fin_str = (*env)->GetStringUTFChars(env, fin, 0);
    char fout[256];
    checkpoint_svm(fpath_str, meta_snap_path_str, mem_snap_path_str, svmid, concurrency, requests, fin_str, fout, 256, &abis[svmid], &isolates[svmid]);
    (*env)->ReleaseStringUTFChars(env, fpath, fpath_str);
    (*env)->ReleaseStringUTFChars(env, meta_snap_path, meta_snap_path_str);
    (*env)->ReleaseStringUTFChars(env, mem_snap_path, mem_snap_path_str);
    (*env)->ReleaseStringUTFChars(env, fin, fin_str);
    return (*env)->NewStringUTF(env, fout);
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_svmRestore(
        JNIEnv *env,
        jobject thisObj,
        jint svmid,
        jstring fpath,
        jstring meta_snap_path,
        jstring mem_snap_path) {
    const char* fpath_str = (*env)->GetStringUTFChars(env, fpath, 0);
    const char* meta_snap_path_str = (*env)->GetStringUTFChars(env, meta_snap_path, 0);
    const char* mem_snap_path_str = (*env)->GetStringUTFChars(env, mem_snap_path, 0);
    restore_svm(fpath_str, meta_snap_path_str, mem_snap_path_str, &abis[svmid], &isolates[svmid]);
    (*env)->ReleaseStringUTFChars(env, fpath, fpath_str);
    (*env)->ReleaseStringUTFChars(env, mem_snap_path, mem_snap_path_str);
    (*env)->ReleaseStringUTFChars(env, meta_snap_path, meta_snap_path_str);
}
