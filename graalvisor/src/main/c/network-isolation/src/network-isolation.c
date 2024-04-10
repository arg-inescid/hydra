#include "network-isolation.h"

#include <sys/time.h>

#define NETNS_RUN_DIR "/var/run/netns"
#define NETNS_FILE_FORMAT "/var/run/netns/%s"
#define DEFAULT_NETWORK_NAMESPACE_FILE "/proc/1/ns/net"

#define SANDBOX_VETH_FORMAT "%s_sb"
#define ENTRYPOINT_VETH_FORMAT "%s_ep"

#define ENTRYPOINT_IP_FORMAT "10.%d.%d.1"
#define SANDBOX_IP_FORMAT "10.%d.%d.2/24"

static const char CREATE_NETWORK_NAMESPACE[] = "ip netns add %s";
static const char DELETE_NETWORK_NAMESPACE[] = "ip netns delete %s";
static const char DELETE_VETH[] = "ip link delete %s";
static const char CREATE_VETH[] = "ip link add %s type veth peer name %s";
static const char LINK_VETH[] = "ip link set %s netns %s";
static const char ENTRYPOINT_ADD_ADDRESS[] = "ip addr add %s/24 dev %s";
static const char SANDBOX_ADD_ADDRESS[] = "ip netns exec %s ip addr add %s dev %s";
static const char SANDBOX_ENABLE_VETH[] = "ip netns exec %s ip link set %s up";
static const char ENTRYPOINT_ENABLE_VETH[] = "ip link set %s up";
static const char SANDBOX_DISABLE_VETH[] = "ip netns exec %s ip link set %s down";
static const char ENTRYPOINT_DISABLE_VETH[] = "ip link set %s down";
static const char SET_NETWORK_GATEWAY[] = "ip netns exec %s ip route add default via %s dev %s";
static const char MASQUERADE[] = "iptables -t nat -A POSTROUTING -s %s/24 -o %s -j MASQUERADE";
static const char FORWARD_RULES_1[] = "iptables -A FORWARD -i %s -o %s -j ACCEPT";
static const char FORWARD_RULES_2[] = "iptables -A FORWARD -o %s -i %s -j ACCEPT";
static const char GET_INTERNET_INTERFACE[] = "ip route get 8.8.8.8 | grep -Po '(?<=(dev ))(\\S+)'";

int switchToDefaultNetworkNamespace() {
    struct timeval tbegin, tend;
    gettimeofday(&tbegin, NULL);
    int fd = open(DEFAULT_NETWORK_NAMESPACE_FILE, O_RDONLY);
    if (setns(fd, CLONE_NEWNET) < 0) {
        fprintf(stderr, "could not change to default network namespace. errno: %s", strerror(errno));
        return -1;
    }
    close(fd);
    gettimeofday(&tend, NULL);
    //printf("switch_default %ld\n", (tend.tv_sec * 1000000 + tend.tv_usec) - (tbegin.tv_sec * 1000000 + tbegin.tv_usec));
    return 0;
}

int switchNetworkNamespace(const char *name) {
    struct timeval tbegin, tend;
    gettimeofday(&tbegin, NULL);
    int namespace;
    char path[PATH_MAX];
    snprintf(path, sizeof(path), NETNS_FILE_FORMAT, name);
    namespace = open(path, O_RDONLY);
    if (namespace == -1) {
        fprintf(stderr, "Error while opening network namespace file\n");
        return -1;
    }
    if (setns(namespace, CLONE_NEWNET) == -1) {
        fprintf(stderr, "Error while setting new namespace\n");
        return -1;
    }
    gettimeofday(&tend, NULL);
    //printf("switch_to_sandbox %ld\n", (tend.tv_sec * 1000000 + tend.tv_usec) - (tbegin.tv_sec * 1000000 + tbegin.tv_usec));
    return 0;
}

int deleteVeth(const char *veth_name) {
    struct timeval tbegin, tend;
    gettimeofday(&tbegin, NULL);
    char command[256];
    if (sprintf(command, DELETE_VETH, veth_name) < 0) {
        fprintf(stderr, "Error formatting delete_veth command\n");
        return -1;
    }
    if (system(command) == -1) {
        fprintf(stderr, "Error while running delete_veth command\n");
        return -1;
    }
    gettimeofday(&tend, NULL);
    //printf("delete_veth %ld\n", (tend.tv_sec * 1000000 + tend.tv_usec) - (tbegin.tv_sec * 1000000 + tbegin.tv_usec));
    return 0;
}

int createVeth(char *entrypointVethName, char *sandboxVethName) {
    struct timeval tbegin, tend;
    gettimeofday(&tbegin, NULL);
    char command[256];
    if (sprintf(command, CREATE_VETH, entrypointVethName, sandboxVethName) < 0) {
        fprintf(stderr, "Error formatting create_veth command\n");
        return -1;
    }
    if (system(command) == -1) {
        fprintf(stderr, "Error while running create_veth command\n");
        return -1;
    }
    gettimeofday(&tend, NULL);
    //printf("create_veth %ld\n", (tend.tv_sec * 1000000 + tend.tv_usec) - (tbegin.tv_sec * 1000000 + tbegin.tv_usec));
    return 0;
}

int linkVeth(char *sandboxVethName, const char *namespaceName) {
    struct timeval tbegin, tend;
    gettimeofday(&tbegin, NULL);
    char command[256];
    if (sprintf(command, LINK_VETH, sandboxVethName, namespaceName) < 0) {
        fprintf(stderr, "Error formatting create_veth command\n");
        return -1;
    }
    if (system(command) == -1) {
        fprintf(stderr, "Error while running create_veth command\n");
        return -1;
    }
    gettimeofday(&tend, NULL);
    //printf("link_veth %ld\n", (tend.tv_sec * 1000000 + tend.tv_usec) - (tbegin.tv_sec * 1000000 + tbegin.tv_usec));
    return 0;
}

int addAddressToEntrypointNamespace(char *ipAddress, char *entrypointVethName) {
    struct timeval tbegin, tend;
    gettimeofday(&tbegin, NULL);
    char command[256];
    if (sprintf(command, ENTRYPOINT_ADD_ADDRESS, ipAddress, entrypointVethName) < 0) {
        fprintf(stderr, "Error formatting add_address_to_entrypoint_namespace command\n");
        return -1;
    }
    if (system(command) == -1) {
        fprintf(stderr, "Error while running add_address_to_entrypoint_namespace command\n");
        return -1;
    }
    gettimeofday(&tend, NULL);
    //printf("add_address_ep %ld\n", (tend.tv_sec * 1000000 + tend.tv_usec) - (tbegin.tv_sec * 1000000 + tbegin.tv_usec));
    return 0;
}

int addAddressToContainerNamespace(const char *ns_name, char *ipAddress, char *sandboxVethName) {
    struct timeval tbegin, tend;
    gettimeofday(&tbegin, NULL);
    char command[256];
    if (sprintf(command, SANDBOX_ADD_ADDRESS, ns_name, ipAddress, sandboxVethName) < 0) {
        fprintf(stderr, "Error formatting add_address_to_sandbox_namespace command\n");
        return -1;
    }
    if (system(command) == -1) {
        fprintf(stderr, "Error while running add_address_to_sandbox_namespace command\n");
        return -1;
    }
    gettimeofday(&tend, NULL);
    //printf("add_address_sandbox %ld\n", (tend.tv_sec * 1000000 + tend.tv_usec) - (tbegin.tv_sec * 1000000 + tbegin.tv_usec));
    return 0;
}

int setContainerDefaultNetworkGateway(const char *namespaceName, char *ip, char *sandboxVethName) {
    struct timeval tbegin, tend;
    gettimeofday(&tbegin, NULL);
    char command[256];
    if (sprintf(command, SET_NETWORK_GATEWAY, namespaceName, ip, sandboxVethName) < 0) {
        fprintf(stderr, "Error formatting set_network_gateway command\n");
        return -1;
    }
    if (system(command) == -1) {
        fprintf(stderr, "Error while running set_network_gateway command\n");
        return -1;
    }
    gettimeofday(&tend, NULL);
    //printf("default_gateway %ld\n", (tend.tv_sec * 1000000 + tend.tv_usec) - (tbegin.tv_sec * 1000000 + tbegin.tv_usec));
    return 0;
}

int sandboxEnableVeth(const char *ns_name, char *sandboxVethName, char *defaultGateway) {
    struct timeval tbegin, tend;
    gettimeofday(&tbegin, NULL);
    char command[256];
    if (sprintf(command, SANDBOX_ENABLE_VETH, ns_name, sandboxVethName) < 0) {
        fprintf(stderr, "Error formatting sandbox_enable_veth command\n");
        return -1;
    }
    if (system(command) == -1) {
        fprintf(stderr, "Error while running sandbox_enable_veth command\n");
        return -1;
    }
    if (setContainerDefaultNetworkGateway(ns_name, defaultGateway, sandboxVethName) == -1) {
        return -1;
    }
    gettimeofday(&tend, NULL);
    //printf("enable_veth_sandbox %ld\n", (tend.tv_sec * 1000000 + tend.tv_usec) - (tbegin.tv_sec * 1000000 + tbegin.tv_usec));
    return 0;
}

int entrypointEnableVeth(char *entrypointVethName) {
    struct timeval tbegin, tend;
    gettimeofday(&tbegin, NULL);
    char command[256];
    if (sprintf(command, ENTRYPOINT_ENABLE_VETH, entrypointVethName) < 0) {
        fprintf(stderr, "Error formatting entrypoint_enable_veth command\n");
        return -1;
    }
    if (system(command) == -1) {
        fprintf(stderr, "Error while running entrypoint_enable_veth command\n");
        return -1;
    }
    gettimeofday(&tend, NULL);
    //printf("enable_veth_ep %ld\n", (tend.tv_sec * 1000000 + tend.tv_usec) - (tbegin.tv_sec * 1000000 + tbegin.tv_usec));
    return 0;
}

int sandboxDisableVeth(const char *ns_name, char *sandboxVethName) {
    struct timeval tbegin, tend;
    gettimeofday(&tbegin, NULL);
    char command[256];
    if (sprintf(command, SANDBOX_DISABLE_VETH, ns_name, sandboxVethName) < 0) {
        fprintf(stderr, "Error formatting sandbox_disable_veth command\n");
        return -1;
    }
    if (system(command) == -1) {
        fprintf(stderr, "Error while running sandbox_disable_veth command\n");
        return -1;
    }
    gettimeofday(&tend, NULL);
    //printf("disable_veth_sandbox %ld\n", (tend.tv_sec * 1000000 + tend.tv_usec) - (tbegin.tv_sec * 1000000 + tbegin.tv_usec));
    return 0;
}

int entrypointDisableVeth(char *entrypointVethName) {
    struct timeval tbegin, tend;
    gettimeofday(&tbegin, NULL);
    char command[256];
    if (sprintf(command, ENTRYPOINT_DISABLE_VETH, entrypointVethName) < 0) {
        fprintf(stderr, "Error formatting entrypoint_disable_veth command\n");
        return -1;
    }
    if (system(command) == -1) {
        fprintf(stderr, "Error while running entrypoint_disable_veth command\n");
        return -1;
    }
    gettimeofday(&tend, NULL);
    //printf("disable_veth_ep %ld\n", (tend.tv_sec * 1000000 + tend.tv_usec) - (tbegin.tv_sec * 1000000 + tbegin.tv_usec));
    return 0;
}

int masquerade(char *entrypointIp, char *forwardInterfaceName) {
    struct timeval tbegin, tend;
    gettimeofday(&tbegin, NULL);
    char command[256];
    if (sprintf(command, MASQUERADE, entrypointIp, forwardInterfaceName) < 0) {
        fprintf(stderr, "Error formatting masquerade command\n");
        return -1;
    }
    if (system(command) == -1) {
        fprintf(stderr, "Error while running masquerade command\n");
        return -1;
    }
    gettimeofday(&tend, NULL);
    //printf("masquerade %ld\n", (tend.tv_sec * 1000000 + tend.tv_usec) - (tbegin.tv_sec * 1000000 + tbegin.tv_usec));
    return 0;
}

int forwardRules1(char *forwardInterfaceName, char *entrypointVethName) {
    struct timeval tbegin, tend;
    gettimeofday(&tbegin, NULL);
    char command[256];
    if (sprintf(command, FORWARD_RULES_1, forwardInterfaceName, entrypointVethName) < 0) {
        fprintf(stderr, "Error formatting forward_rules_1 command\n");
        return -1;
    }
    if (system(command) == -1) {
        fprintf(stderr, "Error while running forward_rules_1 command\n");
        return -1;
    }
    gettimeofday(&tend, NULL);
    //printf("forward_rules_1 %ld\n", (tend.tv_sec * 1000000 + tend.tv_usec) - (tbegin.tv_sec * 1000000 + tbegin.tv_usec));
    return 0;
}

int forwardRules2(char *forwardInterfaceName, char *entrypointVethName) {
    struct timeval tbegin, tend;
    gettimeofday(&tbegin, NULL);
    char command[256];
    if (sprintf(command, FORWARD_RULES_2, forwardInterfaceName, entrypointVethName) < 0) {
        fprintf(stderr, "Error formatting forward_rules_2 command\n");
        return -1;
    }
    if (system(command) == -1) {
        fprintf(stderr, "Error while running forward_rules_2 command\n");
        return -1;
    }
    gettimeofday(&tend, NULL);
    //printf("forward_rules_2 %ld\n", (tend.tv_sec * 1000000 + tend.tv_usec) - (tbegin.tv_sec * 1000000 + tbegin.tv_usec));
    return 0;
}

int getForwardNetworkInterfaceName(char *buffer) {
    FILE *fp;

    fp = popen(GET_INTERNET_INTERFACE, "r");

    if (fp == NULL) {
        fprintf(stderr, "Error getting internet interface\n");
        return -1;
    }

    if (fgets(buffer, 100, fp) == NULL) {
        fprintf(stderr, "Error getting internet interface\n");
        return -1;
    }
    buffer[strcspn(buffer, "\n")] = 0;

    pclose(fp);
    return 0;
}

int createNetworkNamespace(const char *name, int thirdByte, int secondByte) {
    struct timeval tbegin, tend;
    gettimeofday(&tbegin, NULL);
    char entrypointVethName[1024], sandboxVethName[1024], sandboxIp[19], entrypointIp[19], defaultGateway[16];
    snprintf(entrypointVethName, sizeof(entrypointVethName), ENTRYPOINT_VETH_FORMAT, name);
    snprintf(sandboxVethName, sizeof(sandboxVethName), SANDBOX_VETH_FORMAT, name);
    snprintf(sandboxIp, sizeof(sandboxIp), SANDBOX_IP_FORMAT, secondByte, thirdByte);
    snprintf(entrypointIp, sizeof(entrypointIp), ENTRYPOINT_IP_FORMAT, secondByte, thirdByte);
    snprintf(defaultGateway, sizeof(defaultGateway), ENTRYPOINT_IP_FORMAT, secondByte, thirdByte);

    char command[256];
    if (sprintf(command, CREATE_NETWORK_NAMESPACE, name) < 0) {
        fprintf(stderr, "Error formatting create_network_namespace command\n");
        return -1;
    }
    if (system(command) == -1) {
        fprintf(stderr, "Error while running create_network_namespace command\n");
        return -1;
    }
    gettimeofday(&tend, NULL);
    //printf("create_netns %ld\n", (tend.tv_sec * 1000000 + tend.tv_usec) - (tbegin.tv_sec * 1000000 + tbegin.tv_usec));

    if (createVeth(entrypointVethName, sandboxVethName) == -1) {
        return -1;
    }
    if (linkVeth(sandboxVethName, name) == -1) {
        return -1;
    }
    if (addAddressToEntrypointNamespace(entrypointIp, entrypointVethName) == -1) {
        return -1;
    }
    if (addAddressToContainerNamespace(name, sandboxIp, sandboxVethName) == -1) {
        return -1;
    }
    if (entrypointEnableVeth(entrypointVethName) == -1) {
        return -1;
    }
    if (sandboxEnableVeth(name, sandboxVethName, defaultGateway) == -1) {
        return -1;
    }
    char forwardInterfaceName[100];
    if (getForwardNetworkInterfaceName(forwardInterfaceName) == -1) {
        return -1;
    }
    printf("Using interface: %s\n", forwardInterfaceName);
    if (masquerade(entrypointIp, forwardInterfaceName) == -1) {
        return -1;
    }
    if (forwardRules1(forwardInterfaceName, entrypointVethName) == -1) {
        return -1;
    }
    if (forwardRules2(forwardInterfaceName, entrypointVethName) == -1) {
        return -1;
    }
    return 0;
}

int deleteNetworkNamespace(const char *name) {
    char entrypointVethName[1024];
    snprintf(entrypointVethName, sizeof(entrypointVethName), ENTRYPOINT_VETH_FORMAT, name);

    deleteVeth(entrypointVethName);

    struct timeval tbegin, tend;
    gettimeofday(&tbegin, NULL);
    char command[256];
    if (sprintf(command, DELETE_NETWORK_NAMESPACE, name) < 0) {
        fprintf(stderr, "Error formatting delete_network_namespace command\n");
        return -1;
    }
    if (system(command) == -1) {
        fprintf(stderr, "Error while running delete_network_namespace command\n");
        return -1;
    }
    gettimeofday(&tend, NULL);
    //printf("delete_netns %ld\n", (tend.tv_sec * 1000000 + tend.tv_usec) - (tbegin.tv_sec * 1000000 + tbegin.tv_usec));
    return 0;
}

int enableVeths(const char *ns_name, int thirdByte, int secondByte) {
    char entrypointVethName[1024], sandboxVethName[1024], defaultGateway[16];
    snprintf(sandboxVethName, sizeof(entrypointVethName), SANDBOX_VETH_FORMAT, ns_name);
    snprintf(entrypointVethName, sizeof(entrypointVethName), ENTRYPOINT_VETH_FORMAT, ns_name);
    snprintf(defaultGateway, sizeof(defaultGateway), ENTRYPOINT_IP_FORMAT, secondByte, thirdByte);
    if (sandboxEnableVeth(ns_name, sandboxVethName, defaultGateway) == -1) {
        return -1;
    }
    if (entrypointEnableVeth(entrypointVethName) == -1) {
        return -1;
    }
    return 0;
}

int disableVeths(const char *ns_name) {
    char entrypointVethName[1024], sandboxVethName[1024];
    snprintf(sandboxVethName, sizeof(entrypointVethName), SANDBOX_VETH_FORMAT, ns_name);
    snprintf(entrypointVethName, sizeof(entrypointVethName), ENTRYPOINT_VETH_FORMAT, ns_name);
    if (sandboxDisableVeth(ns_name, sandboxVethName) == -1) {
        return -1;
    }
    if (entrypointDisableVeth(entrypointVethName) == -1) {
        return -1;
    }
    return 0;
}


