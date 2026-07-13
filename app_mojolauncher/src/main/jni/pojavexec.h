//
// Created by maks on 08.05.2026.
//

#ifndef POJAVLAUNCHER_POJAVEXEC_H
#define POJAVLAUNCHER_POJAVEXEC_H

typedef void* (*acquire_egl_handle_t)(const char*);

typedef struct {
    acquire_egl_handle_t egl_acquire;
    const char* egl_path;
    int force_gles_context;
    int override_major_version;
    bool force_recreate_on_resize;
    int disp_width;
    int disp_height;
    int disp_hz;
} pojavexec_renderspec_t;

void* pojavexec_loadVulkanDriver();
const char* pojavexec_getNativeDirectory();
const pojavexec_renderspec_t* pojavexec_getRenderSpec();

#endif //POJAVLAUNCHER_POJAVEXEC_H
