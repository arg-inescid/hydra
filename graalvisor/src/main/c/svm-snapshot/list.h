#ifndef LIST_H
#define LIST_H

#include <sys/types.h>

// Reference: https://www.learn-c.org/en/Linked_lists
typedef struct mapping {
    void* start;
    size_t size;
    struct mapping* next;
} mapping_t;

void list_push(mapping_t * head, void* start, size_t size);
void list_print(mapping_t * head);

#endif