#ifndef APPMAP_H
#define APPMAP_H

#include <dlfcn.h>
#include <pthread.h>

typedef struct {
    void* address;
    size_t size;
} MemoryRegion;

typedef struct AppNode {
    char id[256];
    MemoryRegion memReg;
    struct AppNode* next;
} AppNode;

typedef struct AppMap {
    pthread_mutex_t mutex;
    AppNode** buckets;
    size_t size;
} AppMap;

void init_app_map(AppMap* map, size_t size);
void insert_app(AppMap* map, char* id, MemoryRegion memReg);
unsigned long hash_str(const char *str);
AppNode* create_app_node(char* id, MemoryRegion memReg);
MemoryRegion* get_regions(AppMap map, char* id, size_t* count);

#endif
