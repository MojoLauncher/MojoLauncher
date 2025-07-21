//
// Created by andre on 21.07.2025.
//

#include "linkedlist.h"
#include <stdlib.h>

LinkedList* linkedlist_init() {
    LinkedList* list = malloc(sizeof(LinkedList));
    list->first = NULL;
    list->last = NULL;
    return list;
}

void linkedlist_append(LinkedList* list, void* value) {
    LinkedListNode* node = malloc(sizeof(LinkedListNode));
    node->value = value;
    node->next = NULL;

    if (list->last) {
        list->last->next = node;
    } else {
        list->first = node;
    }
    list->last = node;
}