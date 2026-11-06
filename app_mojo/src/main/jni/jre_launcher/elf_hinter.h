//
// Created by maks on 20.09.2025.
//

#ifndef POJAVLAUNCHER_ELF_HINTER_H
#define POJAVLAUNCHER_ELF_HINTER_H

typedef struct {
    void* popstack[512];
    int stack_top;
} hinter_t;

void hinter_process(hinter_t* hinter, const char* lib);
void hinter_free(hinter_t* hinter);

#endif //POJAVLAUNCHER_ELF_HINTER_H
