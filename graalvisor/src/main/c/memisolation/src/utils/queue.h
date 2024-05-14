#ifndef QUEUE_H
#define QUEUE_H

#define MAX_STRING_LENGTH 50

typedef struct Node {
    char string1[MAX_STRING_LENGTH];
    char string2[MAX_STRING_LENGTH];
    struct Node* next;
} Node;

typedef struct {
    Node* front;
    Node* rear;
} Queue;

void initQueue(Queue* queue);
void enqueue(Queue* queue, const char* string1, const char* string2);
Node* dequeue(Queue* queue);
void displayQueue(Queue* queue);
int isEmptyQueue(Queue* queue);
#endif

