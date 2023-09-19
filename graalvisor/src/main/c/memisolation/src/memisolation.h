#ifndef __MEMISOLATION_H__
#define __MEMISOLATION_H__

#include <pthread.h>

/*
 * Debug prints
 */
#ifdef SEC_DBG
  #define SEC_DBM(...)				\
    do {					\
      fprintf(stderr, __VA_ARGS__);		\
      fprintf(stderr, "\n");			\
    } while(0)
#else // disable debug
  #define SEC_DBG(...)
#endif

int find_empty_domain();
void insert_app_id(int domain, const char* id);
int find_app_domain(const char* id);
char* get_app_id(int domain);
void set_permissions(const char* id, int protectionFlag, int pkey);
void install_notify_filter(int domain);
void initialize_memory_isolation();

extern pthread_mutex_t mutex;

#endif
