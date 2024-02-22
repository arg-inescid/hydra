#ifndef LIST_H
#define LIST_H

#include <sys/types.h>

// Basic entry of a memory mapping list.
typedef struct mapping {
    // Start address of the mapping.
    void* start;
    // Size (number of bytes) of the mapping.
    size_t size;
    // Array of permissions at checkpoint-time. Each page is represented with one byte.
    // Four permissions are possible and can be combined: PROT_READ, PROT_WRITE, PROT_EXEC, PROT_NONE.
    char* permissions;
    // Array of pages to be included in the snapshot. These are pages that were at least once marked with PROT_WRITE.
    char* dirty;
    // Pointer to the next mapping.
    struct mapping* next;
} mapping_t;

// Adds a new mapping to the list of mappings.
mapping_t* list_push(mapping_t * head, void* start, size_t size);

// This method will return a mapping that includes the requested range.
mapping_t* list_find(mapping_t* head, void* start, size_t size);

// Deletes one element from the list.
void list_delete(mapping_t* head, mapping_t* to_delete);

// Prints the list of mappings starting from `head`.
void print_list(mapping_t * head);

// Returns the permission of the page containing `addr`, which must be withing the mapping.
char permission(void* mapping_start, void* mapping_finish, char* mapping_perms, void* addr);

// Returns the first address within the mapping that does not contain the same perm.
void* repeated(void* mapping_start, void* mapping_finish, char* mapping_perms, void* block_start);

// Update permissions (and dirty) within a mapping.
void mapping_update_permissions(mapping_t* mapping, void* block_start, void* block_finish, char perm);

// Updates the size of a mapping (used to handle munmpap).
void mapping_update_size(mapping_t* head, mapping_t* mapping, void* unmapping_start, void* unmapping_finish);

#endif