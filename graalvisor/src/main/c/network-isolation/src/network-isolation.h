#ifndef __NETWORKISOLATION_H__
#define __NETWORKISOLATION_H__

void initialize_network_isolation();

void teardown_network_isolation();

int switchToDefaultNetworkNamespace();

int switchNetworkNamespace(const char *name);

int createNetworkNamespace(const char *name, int thirdByte, int secondByte);

int deleteNetworkNamespace(const char *name);

#endif