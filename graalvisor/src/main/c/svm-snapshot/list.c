#include <stdio.h>
#include <stdlib.h>
#include "list.h"

mapping_t* new_mapping(void* start, size_t size) {
    mapping_t * mapping = (mapping_t *) malloc(sizeof(mapping_t));
    mapping->start = start;
    mapping->size = size;
    mapping->next = NULL;
    return mapping;
}

void list_push(mapping_t* head, void* start, size_t size) {
    mapping_t* current = head;

    // If the list is empty, fill in the head and return.
    if (current->start == NULL) {
        current->start = start;
        current->size = size;
        return;
    }

    // Otherwise, find te last element.
    while (current->next != NULL) {
        current = current->next;
    }

    // Insert new element.
    current->next = new_mapping(start, size);
    current->next->next = NULL;
}

mapping_t* list_find(mapping_t* head, void* start, size_t size) {
    mapping_t* current = head;
    void* finish = ((char*) start) + size;

    // If the list if empty, nothing to print.
    if (current->start == NULL) {
        return NULL;
    }

    while (current != NULL) {
        void* current_finish = ((char*) current->start) + current->size;
        if (start >= current->start && finish <= current_finish) {
            return current;
        } else {
            current = current->next;
        }
    }
}

void list_print(mapping_t * head) {
    mapping_t* current = head;

    // If the list if empty, nothing to print.
    if (current->start == NULL) {
        return;
    }

    while (current != NULL) {
        void* finish = ((char*) current->start) + current->size;
        fprintf(stderr, "mapping: start = %16p finish = %16p size = 0x%lx\n",
            current->start, finish, current->size);
        current = current->next;
    }
}