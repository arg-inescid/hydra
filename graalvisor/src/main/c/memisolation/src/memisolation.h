#ifndef __MEMISOLATION_H__
#define __MEMISOLATION_H__

#define _GNU_SOURCE
#include <dlfcn.h>

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
  #define SEC_DBM(...)
#endif

/* Supervisors */
void wait_sem(int domain);
void signal_sem(int domain);
void reset_env(const char* application, int domain);

/* Domain management */
int find_domain(const char* app, int* fd);

void initialize_memory_isolation();

void insert_memory_regions(char* id, const char* path);

#endif
