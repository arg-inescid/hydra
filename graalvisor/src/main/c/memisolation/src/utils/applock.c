#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "applock.h"

// Hash function for strings
unsigned int hash(char* key, size_t size) {
    unsigned int hash = 0;
    while (*key) {
        hash = (hash * 31) + *key;
        key++;
    }
    return hash % size;
}

// Initialize the map
void init_applock_map(AppLock* map, size_t size) {
    map->size = size;
    map->table = (Node**)calloc(size, sizeof(Node*));
    if (map->table == NULL) {
        perror("Error creating AppLock map");
        exit(EXIT_FAILURE);
    }
    pthread_mutex_init(&(map->mutex), NULL);
}

// Insert a key-value pair into the map
void insert_applock(AppLock* map, char* key) {
    unsigned int index = hash(key, map->size);

    pthread_mutex_lock(&(map->mutex));

    // Check if the key already exists
    Node* current = map->table[index];
    while (current != NULL) {
        if (strcmp(current->key, key) == 0) {
            // Key already exists, do not insert
            pthread_mutex_unlock(&(map->mutex));
            return;
        }
        current = current->next;
    }

    // Create a new node
    Node* newNode = (Node*)malloc(sizeof(Node));
    if (newNode == NULL) {
        pthread_mutex_unlock(&(map->mutex));
        perror("Error creating node");
        exit(EXIT_FAILURE);
    }

    newNode->key = strdup(key);
    pthread_mutex_init(&newNode->mutex, NULL);
    newNode->next = NULL;

    if (map->table[index] == NULL) {
        map->table[index] = newNode;
    } else {
        newNode->next = map->table[index];
        map->table[index] = newNode;
    }

    pthread_mutex_unlock(&(map->mutex));
}

// Lock the mutex associated with a key
void lock_app(AppLock* map, char* key) {
    unsigned int index = hash(key, map->size);

    pthread_mutex_lock(&(map->mutex));

    Node* current = map->table[index];

    // Traverse the linked list at the hash table index
    while (current != NULL) {
        if (strcmp(current->key, key) == 0) {
            pthread_mutex_unlock(&(map->mutex));
            pthread_mutex_lock(&current->mutex);
            return;
        }
        current = current->next;
    }

    pthread_mutex_unlock(&(map->mutex));
}

// Unlock the mutex associated with a key
void unlock_app(AppLock* map, char* key) {
    unsigned int index = hash(key, map->size);

    pthread_mutex_lock(&(map->mutex));

    Node* current = map->table[index];

    // Traverse the linked list at the hash table index
    while (current != NULL) {
        if (strcmp(current->key, key) == 0) {
            pthread_mutex_unlock(&(map->mutex));
            pthread_mutex_unlock(&current->mutex);
            return;
        }
        current = current->next;
    }
    
    pthread_mutex_unlock(&(map->mutex));
}
