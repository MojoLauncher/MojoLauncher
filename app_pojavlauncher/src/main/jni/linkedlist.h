//
// Created by andre on 21.07.2025.
//

#ifndef POJAVLAUNCHER_LINKEDLIST_H
#define POJAVLAUNCHER_LINKEDLIST_H

typedef struct LinkedListNode {
    void* value;
    struct LinkedListNode* next;
} LinkedListNode;

typedef struct {
    LinkedListNode* first;
    LinkedListNode* last;
} LinkedList;

LinkedList* linkedlist_init();
void linkedlist_append(LinkedList* list, void* value);
void linkedlist_free(LinkedList* list);

#endif //POJAVLAUNCHER_LINKEDLIST_H
