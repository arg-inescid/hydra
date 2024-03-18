#ifndef CACHE_H
#define CACHE_H

typedef struct {
    char* appId;
    int domain;
} Entry;

typedef struct {
    Entry** entries;
    size_t size;
} Cache;

void init_cache(Cache* cache, size_t size);
void insert_entry(Cache* cache, const char* appId, int domain);
int get_domain(Cache* cache, const char* appId);
void free_cache(Cache* cache);

#endif /* CACHE_H */
