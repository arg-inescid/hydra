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

/* Lazy loading */
char* get_app_id(int domain);
void insert_app_id(int domain, const char* id);

/* Supervisors */
void wait_sem(int domain);
void signal_sem(int domain);
void mark_supervisor_done(int domain);
void prepare_environment(int domain, const char* application);
void reset_environment(int domain, const char* application);

/* Seccomp */
void install_notify_filter(int domain);

/* Domain management */
int find_domain(const char* id);

void initialize_memory_isolation();

void insert_memory_regions(char* id, const char* path);

#endif
