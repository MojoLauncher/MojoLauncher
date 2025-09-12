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

LinkedListNode *linkedlist_append(LinkedList* list, void* value) {
    LinkedListNode* node = malloc(sizeof(LinkedListNode));
    node->value = value;
    node->next = NULL;
    node->prev = list->last;

    if (list->last) {
        list->last->next = node;
    } else {
        list->first = node;
    }
    list->last = node;

    return node;
}

void linkedlist_remove(LinkedList* list, LinkedListNode* node) {
    if (node->prev) {
        node->prev->next = node->next;
    } else {
        list->first = node->next;
    }

    if (node->next) {
        node->next->prev = node->prev;
    } else {
        list->last = node->prev;
    }

    node->next = NULL;
    node->prev = NULL;
    free(node);
}