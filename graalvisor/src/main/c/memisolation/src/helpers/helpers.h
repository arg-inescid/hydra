#include "../utils/appmap.h"
#include <semaphore.h>

enum Status {
    ACTIVE = 0,
    DONE = 1,
};

enum Flag {
    NATIVE = 0,
    MANAGED = 1
};

struct Supervisor {
    enum Flag execution;
    enum Status status;
    sem_t sem;
    int fd;
};

struct CacheApp {
    char app[33];
    int value;
};


/* Seccomp */
void init_supervisors(struct Supervisor supervisors[], int size);

/* Thread Count */
void init_thread_count(int threadCount[], int size);

/* Eager/Lazy setting */
void init_cache(struct CacheApp cache[], int size);

/* Preload */
char* extract_basename(const char* filePath);
void get_memory_regions(AppMap* map, char* id, const char* path);
