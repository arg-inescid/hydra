#define _GNU_SOURCE
#include <sched.h>

#include <jni.h>
#include <unistd.h>
#include <signal.h>
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
static const char SET_NETWORK_GATEWAY[] = "ip netns exec %s route add default gw %s %s";

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

int create_netns_dir()
{
    /* Create the base netns directory if it doesn't exist */
    if (mkdir(NETNS_RUN_DIR, S_IRWXU|S_IRGRP|S_IXGRP|S_IROTH|S_IXOTH)) {
        if (errno != EEXIST) {
            fprintf(stderr, "mkdir %s failed: %s\n", NETNS_RUN_DIR, strerror(errno));
            return -1;
        }
    }
    return 0;
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

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_createNetworkNamespace(JNIEnv *env, jobject thisObj, jstring jName, jint jNumber) {
    const char *ns_name = (*env)->GetStringUTFChars(env, jName, 0);
    int number = (int) jNumber;
    char entrypointVethName[1024], containerVethName[1024], containerIp[19], entrypointIp[19], defaultGateway[16];
    snprintf(entrypointVethName, sizeof(entrypointVethName), "%s_ep", ns_name);
    snprintf(containerVethName, sizeof(containerVethName), "%s_cont", ns_name);
    snprintf(containerIp, sizeof(containerIp), "10.0.0.%d/8", number);
    snprintf(entrypointIp, sizeof(entrypointIp), "10.0.0.0/8");
    snprintf(defaultGateway, sizeof(defaultGateway), "10.0.0.0");

    /*char ns_path[PATH_MAX];

        if (create_netns_dir()) {
            printf("Error creating network namespace directory\n");
            exit(0);
        }

        snprintf(ns_path, PATH_MAX - 1, "%s/%s", NETNS_RUN_DIR, ns_name);
        int fd = open(ns_path, O_RDONLY|O_CREAT|O_EXCL, 0);
        close(fd);
        unshare(CLONE_NEWNET);
        mount("/proc/self/ns/net", ns_path, "none", MS_BIND, NULL);*/

    struct timeval tbegin, tend;
    gettimeofday(&tbegin, NULL);
    char command[256];
    if (sprintf(command, CREATE_NETWORK_NAMESPACE, ns_name) < 0) {
        printf("Error formatting create_network_namespace command\n");
        exit(0);
    }
    if (system(command) == -1) {
        printf("Error while running create_network_namespace command\n");
        exit(0);
    }

    gettimeofday(&tend, NULL);
    printf("time create_netns %ld\n", (tend.tv_sec * 1000000 + tend.tv_usec) - (tbegin.tv_sec * 1000000 + tbegin.tv_usec));

    gettimeofday(&tbegin, NULL);
    createVeth(entrypointVethName, containerVethName);
    gettimeofday(&tend, NULL);
    printf("time create_veth %ld\n", (tend.tv_sec * 1000000 + tend.tv_usec) - (tbegin.tv_sec * 1000000 + tbegin.tv_usec));

    gettimeofday(&tbegin, NULL);
    linkVeth(containerVethName, ns_name);
    gettimeofday(&tend, NULL);
    printf("time link_veth %ld\n", (tend.tv_sec * 1000000 + tend.tv_usec) - (tbegin.tv_sec * 1000000 + tbegin.tv_usec));

    gettimeofday(&tbegin, NULL);
    addAddressToEntrypointNamespace(entrypointIp, entrypointVethName);
    gettimeofday(&tend, NULL);
    printf("time address_ent_ns %ld\n", (tend.tv_sec * 1000000 + tend.tv_usec) - (tbegin.tv_sec * 1000000 + tbegin.tv_usec));

    gettimeofday(&tbegin, NULL);
    addAddressToContainerNamespace(ns_name, containerIp, containerVethName);
    gettimeofday(&tend, NULL);
    printf("time address_cont_ns %ld\n", (tend.tv_sec * 1000000 + tend.tv_usec) - (tbegin.tv_sec * 1000000 + tbegin.tv_usec));

    gettimeofday(&tbegin, NULL);
    entrypointEnableVeth(entrypointVethName);
    gettimeofday(&tend, NULL);
    printf("time enable_ent_veth %ld\n", (tend.tv_sec * 1000000 + tend.tv_usec) - (tbegin.tv_sec * 1000000 + tbegin.tv_usec));

    gettimeofday(&tbegin, NULL);
    containerEnableVeth(ns_name, containerVethName);
    gettimeofday(&tend, NULL);
    printf("time enable_cont_veth %ld\n", (tend.tv_sec * 1000000 + tend.tv_usec) - (tbegin.tv_sec * 1000000 + tbegin.tv_usec));

    gettimeofday(&tbegin, NULL);
    setContainerDefaultNetworkGateway(ns_name, defaultGateway, containerVethName);
    gettimeofday(&tend, NULL);
    printf("time def_cont_netw_gate %ld\n", (tend.tv_sec * 1000000 + tend.tv_usec) - (tbegin.tv_sec * 1000000 + tbegin.tv_usec));

    gettimeofday(&tbegin, NULL);
    gettimeofday(&tend, NULL);
    printf("time switch_netns %ld\n", (tend.tv_sec * 1000000 + tend.tv_usec) - (tbegin.tv_sec * 1000000 + tbegin.tv_usec));

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
