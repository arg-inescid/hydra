#include <jni.h>
#include <unistd.h>
#include <signal.h>
#include "org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface.h"

void close_parent_fds() {
    // TODO - we should try to get a sense for the used file descriptors.
    for (int fd = 3; fd < 1024; fd++) {
        close(fd);
    }
}

void reset_parent_signal_handlers() {
    signal(SIGTERM, SIG_DFL);
    signal(SIGINT, SIG_DFL);
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_ginit(JNIEnv *env, jobject thisObj) {
    setbuf(stdout, NULL);
}

JNIEXPORT int JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_gfork(JNIEnv *env, jobject thisObj) {
    int pid = fork();
    if (pid == 0) {
        close_parent_fds();
        reset_parent_signal_handlers();
    }
    return pid;
}
