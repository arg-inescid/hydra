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


/* Seccomp */
void init_supervisors(struct Supervisor supervisors[], int size);

/* Thread Count */
void init_thread_count(int threadCount[], int size);

/* Lazy Process Isolation */
void init_process_pool(int procIDs[]);

/* Eager/Lazy setting */
void init_cache(char* cache[], int size);

/* Preload */
char* extract_basename(const char* filePath);
void get_memory_regions(AppMap* map, char* id, const char* path);
