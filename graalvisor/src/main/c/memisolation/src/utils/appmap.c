#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "appmap.h"

void
init_app_map(AppMap* map, size_t size)
{
    map->size = size;
    map->buckets = (AppNode**)calloc(size, sizeof(AppNode*));
    if (map->buckets == NULL) {
        perror("Error creating App map");
        exit(EXIT_FAILURE);
    }
    pthread_mutex_init(&(map->mutex), NULL);
}

unsigned long
hash_string(const char* key, size_t size)
{
    unsigned long hashValue = 14695981039346656037UL;
    const unsigned char* str = (const unsigned char*)key;

    while (*str) {
        hashValue ^= *str++;
        hashValue *= 1099511628211UL;
    }

    return hashValue % size;
}

AppNode*
create_app_node(char* id, MemoryRegion memReg)
{
    AppNode* newNode = (AppNode*)malloc(sizeof(AppNode));
    if (newNode == NULL) {
        fprintf(stderr, "Memory allocation failed!\n");
        exit(EXIT_FAILURE);
    }
    
    strcpy(newNode->id, id);
    newNode->memReg = memReg;
    newNode->next = NULL;
    return newNode;
}

void
insert_app(AppMap* map, char* id, MemoryRegion memReg)
{
    unsigned long index = hash_string((const char*)id, map->size);
    AppNode* newNode = create_app_node(id, memReg);
    
    pthread_mutex_lock(&(map->mutex));

    if (map->buckets[index] == NULL) {
        map->buckets[index] = newNode;
    } else {
        AppNode* currentNode = map->buckets[index];
        while (currentNode->next != NULL) {
            currentNode = currentNode->next;
        }
        currentNode->next = newNode;
    }

    pthread_mutex_unlock(&(map->mutex));
}

MemoryRegion*
get_regions(AppMap map, char* id, size_t* count)
{
    unsigned long index = hash_string((const char*)id, map.size);
    AppNode* currentNode = map.buckets[index];
    MemoryRegion* values = NULL;
    size_t numValues = 0;

    pthread_mutex_lock(&map.mutex);

    while (currentNode != NULL) {
        if (!strcmp(currentNode->id, id)) {
            // Add the value to the dynamic array
            values = (MemoryRegion*)realloc(values, (numValues + 1) * sizeof(MemoryRegion));
            values[numValues] = currentNode->memReg;
            ++numValues;
        }
        currentNode = currentNode->next;
    }

    pthread_mutex_unlock(&map.mutex);

    *count = numValues;

    return values;
}

void
remove_app(AppMap* map, char* id, void* address)
{
    unsigned long index = hash_string((const char*)id, map->size);

    pthread_mutex_lock(&(map->mutex));

    AppNode* currentNode = map->buckets[index];
    AppNode* prevNode = NULL;

    while (currentNode != NULL) {
        if (!strcmp(currentNode->id, id) && currentNode->memReg.address == address) {
            if (prevNode == NULL) {
                map->buckets[index] = currentNode->next;
            } else {
                prevNode->next = currentNode->next;
            }
            free(currentNode);
            pthread_mutex_unlock(&(map->mutex));
            return;
        }
        prevNode = currentNode;
        currentNode = currentNode->next;
    }

    pthread_mutex_unlock(&(map->mutex));
}