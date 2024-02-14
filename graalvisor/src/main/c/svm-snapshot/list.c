#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/mman.h>
#include "list.h"
#include "main.h"

size_t bytes_to_pages(size_t bytes) {
    return bytes / getpagesize();
}

char permission(void* mapping_start, void* mapping_finish, char* mapping_perms, void* addr) {

    // Check if the requested address is within the mapping limits.
    if (addr < mapping_start || addr > mapping_finish) {
        err("warning, trying to get permission of page outside mapping %p\n", addr);
    }

    // Number of bytes after the start of the mapping.
    size_t bytes = (char*) addr - (char*) mapping_start;
    return mapping_perms[bytes_to_pages(bytes)];
}

void* repeated(void* mapping_start, void* mapping_finish, char* mapping_perms, void* block_start) {
    char  block_perm = permission(mapping_start, mapping_finish, mapping_perms, block_start);
    char* block_finish = block_start;
    for (; block_finish < (char*) mapping_finish; block_finish += getpagesize()) {
        if (permission(mapping_start, mapping_finish, mapping_perms, block_finish) != block_perm) {
            break;
        }
    }
    return block_finish;
}

void init_mapping(mapping_t* mapping, void* start, size_t size) {
    mapping->start = start;
    mapping->size = size;
    mapping->permissions = (char*) malloc(bytes_to_pages(size) * sizeof(char));
    mapping->dirty = (char*) malloc(bytes_to_pages(size) * sizeof(char));
    memset(mapping->permissions, 0, bytes_to_pages(size));
    memset(mapping->dirty, 0, bytes_to_pages(size));
    mapping->next = NULL;
}

mapping_t* list_push(mapping_t* head, void* start, size_t size) {
    mapping_t* current = head;

    // If the list is empty, fill in the head and return.
    if (current->start == NULL) {
        init_mapping(current, start, size);
        return current;
    }

    // Otherwise, find te last element.
    while (current->next != NULL) {
        current = current->next;
    }

    // Insert new element.
    mapping_t * new = (mapping_t *) malloc(sizeof(mapping_t));
    init_mapping(new, start, size);
    current->next = new;
    current->next->next = NULL;
    return new;
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

        // If the range is included in this mapping.
        if (start >= current->start && finish <= current_finish) {
            return current;
        }
        // We do not support partial overlaps. Quit with error message.
        // Note that start is part of the range but the finish address is already outside.
        else if ((start >= current->start && start < current_finish) || (finish > current->start && finish <= current_finish)) {
            err("error: requested mapping %16p - %16p partially overlaps with existing mapping %16p - %16p\n",
               start, finish, current->start, current_finish);
            return NULL;
        }
        // Continue looping.
        else {
            current = current->next;
        }
    }

    return NULL;
}

void print_block(void* block_start, void* block_finish, char block_perm) {
    log("pmapping: %16p - %16p size = 0x%16lx prot = %s%s%s%s\n",
        block_start,
        block_finish,
        (char*) block_finish - (char*) block_start,
        block_perm & PROT_EXEC   ? "x" : "-",
        block_perm & PROT_READ   ? "r" : "-",
        block_perm & PROT_WRITE  ? "w" : "-",
        block_perm == PROT_NONE  ? "n" : "-");
}

void print_mapping(void* mapping_start, void* mapping_finish, char* mapping_perms) {
    void* block_start = mapping_start;

    while (block_start < mapping_finish) {
        char  block_perm = permission(mapping_start, mapping_finish, mapping_perms, block_start);
        char* block_finish = repeated(mapping_start, mapping_finish, mapping_perms, block_start);
        print_block(block_start, block_finish, block_perm);
        block_start = block_finish;
    }
}

void print_list(mapping_t * head) {
    mapping_t* current = head;

    // If the list if empty, nothing to print.
    if (current->start == NULL) {
        return;
    }

    while (current != NULL) {
        void* mapping_start = current->start;
        void* mapping_finish = ((char*) mapping_start) + current->size;
        print_mapping(mapping_start, mapping_finish, current->permissions);
        current = current->next;
    }
}

void mapping_update_permissions(mapping_t* mapping, void* block_start, void* block_finish, char block_perm) {
    // First page index;
    int i = bytes_to_pages((char*) block_start - (char*) mapping->start);
    // Last page index.
    int j = bytes_to_pages((char*) block_finish - (char*) mapping->start);
    // Update permissions between pages i and f.
    memset(mapping->permissions + i, block_perm, j - i);
    // If permission allow writting or disable it, then also update dirty set.
    if (block_perm & PROT_WRITE) {
        memset(mapping->dirty + i, PROT_WRITE, j - i);
    } else if (block_perm == PROT_NONE) {
        memset(mapping->dirty + i, PROT_NONE, j - i);
    }
    log("tracking  %16p - %16p (permissions)\n", block_start, block_finish);
}


void mapping_update_size(mapping_t* mapping, void* unmapping_start, void* unmapping_finish) {
    void* mapping_finish = ((char*) mapping->start) + mapping->size;
    size_t unmapping_size = (char*) unmapping_finish - (char*) unmapping_start;

    // If we are removing a block from the start.
    if (unmapping_start == mapping->start && unmapping_finish <= mapping_finish) {
        mapping->size -= unmapping_size;
        mapping->start = unmapping_finish;
        log("tracking  %16p - %16p (clipping beg)\n", mapping->start, mapping_finish);
        if (mapping->size == 0) {
            // TODO - remove
        }
    }
    // If we are removing a block from the end.
    else if (unmapping_finish == mapping_finish && unmapping_start >= mapping->start) {
        mapping->size -= unmapping_size;
        mapping_finish = ((char*) mapping->start) + mapping->size;
        log("tracking  %16p - %16p (clipping end)\n", mapping->start, mapping_finish);
            if (mapping->size == 0) {
            // TODO - remove
        }
    }
    // If we are removing a range that includes this mapping.
    else if (unmapping_start < mapping->start && unmapping_finish > mapping_finish) {
        mapping->size = 0;
        log("tracking  %16p - %16p (deleting)\n", mapping->start, mapping_finish);
        // TODO - remove
    }
    // Unsupported unmapping range.
    else {
        log("error: unsupported munmap: len = %lx [%16p to %16p] from [%16p to %16p]\n",
            unmapping_size, unmapping_start, unmapping_finish, mapping->start, mapping_finish);
    }
}