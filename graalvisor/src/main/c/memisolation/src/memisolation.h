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

/* Lazy loading */
void insert_app_id(int domain, const char* id);
char* get_app_id(int domain);
int find_app_domain(const char* id);

/* Thread synchronization */
void lock();
void unlock();

/* MPK */
void set_permissions(const char* id, int protectionFlag, int pkey);

/* Seccomp */
void install_notify_filter(int domain);

/* Domain management */
int find_empty_domain();

void initialize_memory_isolation();

#endif
