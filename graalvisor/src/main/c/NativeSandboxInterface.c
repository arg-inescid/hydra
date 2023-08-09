#define _GNU_SOURCE
#include <sched.h>

#include <jni.h>
#include <unistd.h>
#include <signal.h>
#include <sys/types.h>
#ifdef LAZY_ISOLATION
#include "lazyisolation.h"
#endif
#include "org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <errno.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/mount.h>
#include <limits.h>
#include <sys/time.h>

#define NETNS_RUN_DIR "/var/run/netns"

static const char CREATE_NETWORK_NAMESPACE[] = "ip netns add %s";
static const char DELETE_NETWORK_NAMESPACE[] = "ip netns delete %s";
static const char DELETE_VETH[] = "ip link delete %s";
static const char CREATE_VETH[] = "ip link add %s type veth peer name %s";
static const char LINK_VETH[] = "ip link set %s netns %s";
static const char ENTRYPOINT_ADD_ADDRESS[] = "ip addr add %s dev %s";
static const char CONTAINER_ADD_ADDRESS[] = "ip netns exec %s ip addr add %s dev %s";
static const char CONTAINER_ENABLE_VETH[] = "ip netns exec %s ip link set %s up";
static const char ENTRYPOINT_ENABLE_VETH[] = "ip link set %s up";
static const char CONTAINER_DISABLE_VETH[] = "ip netns exec %s ip link set %s down";
static const char ENTRYPOINT_DISABLE_VETH[] = "ip link set %s down";
static const char SET_NETWORK_GATEWAY[] = "ip netns exec %s route add default gw %s %s";

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

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_switchToDefaultNetworkNamespace(JNIEnv *env, jobject thisObj) {
    int fd = open("/proc/1/ns/net", O_RDONLY);
    if (setns(fd, CLONE_NEWNET) < 0) {
        fprintf(stderr, "could not change to default network namespace. errno: %s", strerror(errno));
        close(fd);
    }
    close(fd);
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_switchNetworkNamespace(JNIEnv *env, jobject thisObj, jstring jName) {
    const char *ns_name = (*env)->GetStringUTFChars(env, jName, 0);
    int namespace;
    char path[PATH_MAX];
    snprintf(path, sizeof(path), "/var/run/netns/%s", ns_name);
    namespace = open(path, O_RDONLY);
    if (namespace == -1) {
        printf("Error while opening network namespace file\n");
        exit(0);
    }
    if (setns(namespace, CLONE_NEWNET) == -1) {
        printf("Error while setting new namespace\n");
        exit(0);
    }
    (*env)->ReleaseStringUTFChars(env, jName, ns_name);
}

void deleteVeth(const char *veth_name) {
    char command[256];
    if (sprintf(command, DELETE_VETH, veth_name) < 0) {
        printf("Error formatting delete_veth command\n");
        exit(0);
    }
    if (system(command) == -1) {
        printf("Error while running delete_veth command\n");
        exit(0);
    }
}

void createVeth(char *entrypointVethName, char *containerVethName) {
    char command[256];
    if (sprintf(command, CREATE_VETH, entrypointVethName, containerVethName) < 0) {
        printf("Error formatting create_veth command\n");
        exit(0);
    }
    if (system(command) == -1) {
        printf("Error while running create_veth command\n");
        exit(0);
    }
}

void linkVeth(char *containerVethName, const char *namespaceName) {
    char command[256];
    if (sprintf(command, LINK_VETH, containerVethName, namespaceName) < 0) {
        printf("Error formatting create_veth command\n");
        exit(0);
    }
    if (system(command) == -1) {
        printf("Error while running create_veth command\n");
        exit(0);
    }
}

void addAddressToEntrypointNamespace(char *ipAddress, char *entrypointVethName) {
    char command[256];
    if (sprintf(command, ENTRYPOINT_ADD_ADDRESS, ipAddress, entrypointVethName) < 0) {
        printf("Error formatting add_address_to_entrypoint_namespace command\n");
        exit(0);
    }
    if (system(command) == -1) {
        printf("Error while running add_address_to_entrypoint_namespace command\n");
        exit(0);
    }
}

void addAddressToContainerNamespace(const char *ns_name, char *ipAddress, char *containerVethName) {
    char command[256];
    if (sprintf(command, CONTAINER_ADD_ADDRESS, ns_name, ipAddress, containerVethName) < 0) {
        printf("Error formatting add_address_to_container_namespace command\n");
        exit(0);
    }
    if (system(command) == -1) {
        printf("Error while running add_address_to_container_namespace command\n");
        exit(0);
    }
}

void containerEnableVeth(const char *ns_name, char *containerVethName) {
    char command[256];
    if (sprintf(command, CONTAINER_ENABLE_VETH, ns_name, containerVethName) < 0) {
        printf("Error formatting container_enable_veth command\n");
        exit(0);
    }
    if (system(command) == -1) {
        printf("Error while running container_enable_veth command\n");
        exit(0);
    }
}

void entrypointEnableVeth(char *entrypointVethName) {
    char command[256];
    if (sprintf(command, ENTRYPOINT_ENABLE_VETH, entrypointVethName) < 0) {
        printf("Error formatting entrypoint_enable_veth command\n");
        exit(0);
    }
    if (system(command) == -1) {
        printf("Error while running entrypoint_enable_veth command\n");
        exit(0);
    }
}

void containerDisableVeth(const char *ns_name, char *containerVethName) {
    char command[256];
    if (sprintf(command, CONTAINER_DISABLE_VETH, ns_name, containerVethName) < 0) {
        printf("Error formatting container_disable_veth command\n");
        exit(0);
    }
    if (system(command) == -1) {
        printf("Error while running container_disable_veth command\n");
        exit(0);
    }
}

void entrypointDisableVeth(char *entrypointVethName) {
    char command[256];
    if (sprintf(command, ENTRYPOINT_DISABLE_VETH, entrypointVethName) < 0) {
        printf("Error formatting entrypoint_disable_veth command\n");
        exit(0);
    }
    if (system(command) == -1) {
        printf("Error while running entrypoint_disable_veth command\n");
        exit(0);
    }
}

void setContainerDefaultNetworkGateway(const char *namespaceName, char *ip, char *containerVethName) {
    char command[256];
    if (sprintf(command, SET_NETWORK_GATEWAY, namespaceName, ip, containerVethName) < 0) {
        printf("Error formatting set_network_gateway command\n");
        exit(0);
    }
    if (system(command) == -1) {
        printf("Error while running set_network_gateway command\n");
        exit(0);
    }
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_createNetworkNamespace(JNIEnv *env, jobject thisObj, jstring jName, jint jThirdByte, jint jFourthByte) {
    const char *ns_name = (*env)->GetStringUTFChars(env, jName, 0);
    int fourthByte = (int) jFourthByte;
    int thirdByte = (int) jThirdByte;
    char entrypointVethName[1024], containerVethName[1024], containerIp[19], entrypointIp[19], defaultGateway[16];
    snprintf(entrypointVethName, sizeof(entrypointVethName), "%s_ep", ns_name);
    snprintf(containerVethName, sizeof(containerVethName), "%s_cont", ns_name);
    snprintf(containerIp, sizeof(containerIp), "10.0.%d.%d/16", thirdByte, fourthByte);
    snprintf(entrypointIp, sizeof(entrypointIp), "10.0.0.0/16");
    snprintf(defaultGateway, sizeof(defaultGateway), "10.0.0.0");

    char command[256];
    if (sprintf(command, CREATE_NETWORK_NAMESPACE, ns_name) < 0) {
        printf("Error formatting create_network_namespace command\n");
        exit(0);
    }
    if (system(command) == -1) {
        printf("Error while running create_network_namespace command\n");
        exit(0);
    }


    createVeth(entrypointVethName, containerVethName);

    linkVeth(containerVethName, ns_name);

    addAddressToEntrypointNamespace(entrypointIp, entrypointVethName);

    addAddressToContainerNamespace(ns_name, containerIp, containerVethName);

    entrypointEnableVeth(entrypointVethName);

    containerEnableVeth(ns_name, containerVethName);

    setContainerDefaultNetworkGateway(ns_name, defaultGateway, containerVethName);

    (*env)->ReleaseStringUTFChars(env, jName, ns_name);
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_deleteNetworkNamespace(JNIEnv *env, jobject thisObj, jstring jName) {
    const char *ns_name = (*env)->GetStringUTFChars(env, jName, 0);

    char entrypointVethName[1024];
    snprintf(entrypointVethName, sizeof(entrypointVethName), "%s_ep", ns_name);

    deleteVeth(entrypointVethName);

    char command[256];
    if (sprintf(command, DELETE_NETWORK_NAMESPACE, ns_name) < 0) {
        printf("Error formatting delete_network_namespace command\n");
        exit(0);
    }
    if (system(command) == -1) {
        printf("Error while running delete_network_namespace command\n");
        exit(0);
    }

    (*env)->ReleaseStringUTFChars(env, jName, ns_name);
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_enableVeths(JNIEnv *env, jobject thisObj, jstring jName) {
    const char *ns_name = (*env)->GetStringUTFChars(env, jName, 0);
    char entrypointVethName[1024], containerVethName[1024];
    snprintf(entrypointVethName, sizeof(entrypointVethName), "%s_ep", ns_name);

    containerEnableVeth(ns_name, containerVethName);
    entrypointEnableVeth(entrypointVethName);

    (*env)->ReleaseStringUTFChars(env, jName, ns_name);
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_disableVeths(JNIEnv *env, jobject thisObj, jstring jName) {
    const char *ns_name = (*env)->GetStringUTFChars(env, jName, 0);
    char entrypointVethName[1024], containerVethName[1024];
    snprintf(containerVethName, sizeof(entrypointVethName), "%s_cont", ns_name);
    snprintf(entrypointVethName, sizeof(entrypointVethName), "%s_ep", ns_name);

    containerDisableVeth(ns_name, containerVethName);
    entrypointDisableVeth(entrypointVethName);

    (*env)->ReleaseStringUTFChars(env, jName, ns_name);
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_enterNativeProcessSandbox(JNIEnv *env, jobject thisObj) {

}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_leaveNativeProcessSandbox(JNIEnv *env, jobject thisObj) {

}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_destroyNativeProcessSandbox(JNIEnv *env, jobject thisObj) {

}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_createNativeIsolateSandbox(JNIEnv *env, jobject thisObj, jboolean lazyIsolation) {
#ifdef LAZY_ISOLATION
    if (lazyIsolation) {
        install_thread_filter();
    }
#endif
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_enterNativeIsolateSandbox(JNIEnv *env, jobject thisObj) {

}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_leaveNativeIsolateSandbox(JNIEnv *env, jobject thisObj) {

}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_destroyNativeIsolateSandbox(JNIEnv *env, jobject thisObj) {

}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_createNativeRuntimeSandbox(JNIEnv *env, jobject thisObj, jboolean lazyIsolation) {
#ifdef LAZY_ISOLATION
    if (lazyIsolation) {
        install_thread_filter();
    }
#endif
}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_enterNativeRuntimeSandbox(JNIEnv *env, jobject thisObj) {

}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_leaveNativeRuntimeSandbox(JNIEnv *env, jobject thisObj) {

}

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_destroyNativeRuntimeSandbox(JNIEnv *env, jobject thisObj) {

}
