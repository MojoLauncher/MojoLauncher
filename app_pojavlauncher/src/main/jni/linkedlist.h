//
// Created by andre on 21.07.2025.
//

#ifndef POJAVLAUNCHER_LINKEDLIST_H
#define POJAVLAUNCHER_LINKEDLIST_H

typedef struct LinkedListNode {
    void* value;
    struct LinkedListNode* next;
    struct LinkedListNode* prev;
} LinkedListNode;

typedef struct {
    LinkedListNode* first;
    LinkedListNode* last;
} LinkedList;

LinkedList* linkedlist_init();
LinkedListNode *linkedlist_append(LinkedList* list, void* value);
void linkedlist_remove(LinkedList* list, LinkedListNode* node);

#endif //POJAVLAUNCHER_LINKEDLIST_H
