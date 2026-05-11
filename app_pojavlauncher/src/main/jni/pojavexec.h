//
// Created by maks on 08.05.2026.
//

#ifndef POJAVLAUNCHER_POJAVEXEC_H
#define POJAVLAUNCHER_POJAVEXEC_H

typedef struct {
    void* egl_handle; // Set to a dlopen handle in order to force GLFW to load EGL symbols from a particular library
    int force_gles_context;
    int override_major_version;
} pojavexec_renderspec_t;

void* pojavexec_loadVulkanDriver();
const pojavexec_renderspec_t* pojavexec_getRenderSpec();

#endif //POJAVLAUNCHER_POJAVEXEC_H
