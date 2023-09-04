#define _GNU_SOURCE
#include <sched.h>

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <errno.h>
#include <fcntl.h>
#include <limits.h>

int switchToDefaultNetworkNamespace();

int switchNetworkNamespace(const char *name);

int createNetworkNamespace(const char *name, int thirdByte, int fourthByte);

int deleteNetworkNamespace(const char *name);

int enableVeths(const char *ns_name);

int disableVeths(const char *ns_name);
