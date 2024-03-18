#ifndef __MEMISOLATION_H__
#define __MEMISOLATION_H__

#define _GNU_SOURCE
#include <dlfcn.h>

#if !defined(EAGER_PERMS) && !defined(EAGER_MPK)
  #define LAZY_PERMS
#endif

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

extern __thread int domain;

/* Supervisors */
void wait_sem();
void signal_sem();
void reset_env(const char* application, int isLast);

/* Domain management */
void acquire_domain(const char* app, int* fd);

#ifdef EAGER_MPK
  void find_domain_eager(const char* app);
#endif

/* Initalizer */
void initialize_memory_isolation();

/* Auxiliary */
void insert_memory_regions(char* id, const char* path);

#endif
