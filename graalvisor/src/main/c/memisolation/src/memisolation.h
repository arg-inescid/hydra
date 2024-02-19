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
void wait_set(int domain);
void signal_set(int domain);
void wait_filter(int domain);
void signal_filter(int domain);
void wait_perms(int domain);
void signal_perms(int domain);
void mark_supervisor_done(int domain);
void change_supervisor_fd(int domain, int fd);

/* Seccomp */
int install_notify_filter(int domain);

/* Domain management */
int find_domain(const char* id);

void initialize_memory_isolation();

void insert_memory_regions(char* id, const char* path);

#endif
