//
// Created by maks on 20.09.2025.
//

#include <dlfcn.h>
#include <elf.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <malloc.h>
#include <string.h>
#include <stdlib.h>
#include "elf_hinter.h"

#define TAG __FILE_NAME__
#include <log.h>

#if defined(__x86_64__) || defined(__aarch64__)
#define ELF_EHDR Elf64_Ehdr
#define ELF_SHDR Elf64_Shdr
#define ELF_HALF Elf64_Half
#define ELF_XWORD Elf64_Xword
#define ELF_DYN Elf64_Dyn
#elif defined(__arm__) || defined(__i386__)
#define ELF_EHDR Elf32_Ehdr
#define ELF_SHDR Elf32_Shdr
#define ELF_HALF Elf32_Half
#define ELF_XWORD Elf32_Xword
#define ELF_DYN Elf32_Dyn
#endif

static bool inner_hinter_process(void** popstack, int* stack_top, const char* lib_name);

static bool hinter_for_each_dependency(const char* lib_path, void** popstack, int* stack_top) {
    bool result = false;
    char* target = NULL;
    int lib_fd = open(lib_path, O_RDONLY);
    if(lib_fd == -1) goto exit;
    struct stat realstat;
    if(fstat(lib_fd, &realstat)) goto exit;
    target = malloc(realstat.st_size);
    if(!target) goto exit;
    if(read(lib_fd, target, realstat.st_size) != realstat.st_size) goto exit;

    ELF_EHDR *ehdr = (ELF_EHDR*)target;
    ELF_SHDR *shdr = (ELF_SHDR*)(target + ehdr->e_shoff);
    for(ELF_HALF i = 0; i < ehdr->e_shnum; i++) {
        ELF_SHDR *hdr = &shdr[i];
        if(hdr->sh_type != SHT_DYNAMIC) continue;
        char* strtab = target + shdr[hdr->sh_link].sh_offset;
        // If there's a warning below, it's bogus, ignore it
        ELF_DYN *dynEntries = (ELF_DYN*)(target + hdr->sh_offset);
        for(ELF_XWORD k = 0; k < (hdr->sh_size / hdr->sh_entsize);k++) {
            ELF_DYN* dynEntry = &dynEntries[k];
            if(dynEntry->d_tag != DT_NEEDED) continue;
            const char* needed = strtab + dynEntry->d_un.d_val;
            if(!inner_hinter_process(popstack, stack_top, needed)) goto exit;
        }
        result = true;
    }
    exit:
    if(lib_fd != -1) close(lib_fd);
    if(target) free(target);
    return result;
}

static bool hinter_lookup_path(char fullpath[PATH_MAX], const char* path, const char* name) {
    snprintf(fullpath, PATH_MAX, "%s/%s", path, name);
    return access(fullpath, F_OK) == 0;
}

static bool hinter_lookup_lib(const char* lib_name, char lib_path[PATH_MAX]) {
    if(lib_name[0] == '/') {
        strncpy(lib_path, lib_name, PATH_MAX);
        return true;
    }
    bool eof = false;
    char dir_path[PATH_MAX];
    const char* ld_lib_path = getenv("LD_LIBRARY_PATH");
    const char* path_fragment = ld_lib_path;
    const char* path_next;
    do {
        path_next = strchr(path_fragment, ':');
        if(path_next == NULL) {
            eof = true;
            path_next = strchr(path_fragment, 0);
        }
        size_t copy_len = path_next - path_fragment;
        if(copy_len != 0) {
            memcpy(dir_path, path_fragment, copy_len);
            dir_path[copy_len] = 0;
            if(hinter_lookup_path(lib_path, dir_path, lib_name)) return true;
        }
        if(!eof) {
            path_fragment = path_next + 1;
        }else {
            break;
        }
    } while (true);
    return false;
}

static bool inner_hinter_process(void** popstack, int* stack_top, const char* lib_name) {
    void* lib_noload = dlopen(lib_name, RTLD_NOLOAD);
    if(lib_noload != NULL) {
        dlclose(lib_noload);
        return true;
    }
    char lib_path[PATH_MAX];
    bool result = hinter_lookup_lib(lib_name, lib_path);
    if(!result) {
        LOGW("Hinter lookup failed: %s", lib_name);
        return false;
    }
    result = hinter_for_each_dependency(lib_path, popstack, stack_top);
    if(!result) {
        LOGW("Hinter dep scan failed: %s", lib_path);
        return false;
    }
    void* hint_open = dlopen(lib_path, RTLD_GLOBAL);
    if(hint_open == NULL) {
        LOGW("Hinter load failed: %s", dlerror());
        return false;
    }
    popstack[(*stack_top)++] = hint_open;
    return true;
}

void hinter_process(hinter_t* hinter, const char* lib) {
    hinter->stack_top = 0;
    inner_hinter_process(hinter->popstack, &hinter->stack_top, lib);
}

void hinter_free(hinter_t* hinter) {
    for(int i = 0; i < hinter->stack_top; i++) {
        dlclose(hinter->popstack[i]);
    }
}