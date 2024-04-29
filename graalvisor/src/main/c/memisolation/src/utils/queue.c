#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "queue.h"

void initQueue(Queue* queue) {
  queue->front = queue->rear = NULL;
}

void displayQueue(Queue* queue) {
  if (queue->front == NULL) {
    printf("Queue is empty!\n");
    return;
  }

  Node* current = queue->front;
  printf("Queue Contents:\n");
  while (current != NULL) {
    printf("String 1: %s, String 2: %s\n", current->string1, current->string2);
    current = current->next;
  }
}

void enqueue(Queue* queue, const char* string1, const char* string2) {
  Node* newNode = (Node*)malloc(sizeof(Node));
  if (newNode == NULL) {
    printf("Memory allocation failed!\n");
    exit(1);
  }
  strcpy(newNode->string1, string1);
  strcpy(newNode->string2, string2);
  newNode->next = NULL;

  if (queue->front == NULL) {
    queue->front = queue->rear = newNode;
  } else {
    queue->rear->next = newNode;
    queue->rear = newNode;
  }
}

Node* dequeue(Queue* queue) {
  if (queue->front == NULL) {
    printf("Queue is empty!\n");
    exit(1);
  }
  Node* node = queue->front;
  queue->front = queue->front->next;
  if (queue->front == NULL) {
    queue->rear = NULL;
  }
  return node;
}

int isEmptyQueue(Queue* queue) {
  return (queue->front == NULL);
}

