#include "../utils/appmap.h"
#include <semaphore.h>

enum Status {
    ACTIVE = 0,
    DONE = 1
};

struct Supervisor {
    sem_t sem;
    enum Status status;
    int fd;
};

struct CacheApp {
    char app[33];
    int value;
};

/* Eager/Lazy setting */
void init_cache_array(struct CacheApp cache[], int size);

/* Seccomp */
void init_supervisors(struct Supervisor array[], int size);

/* Preload */
char* extract_basename(const char* filePath);
void get_memory_regions(AppMap* map, char* id, const char* path);
