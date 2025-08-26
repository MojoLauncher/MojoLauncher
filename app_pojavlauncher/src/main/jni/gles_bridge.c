/* Simple OpenGL ES driver stub */
/* created by whitebelyash on 26.08.25 */

#include "gles_bridge.h"

#include <EGL/egl.h>
#include <stdlib.h>
#include <stdio.h>
#include <dlfcn.h>

eglFuncPointer (*sys_eglGetProcAddress)(const char* func_name);


__attribute((constructor)) void init(){
    // Open system EGL
    void* handle = dlopen("libEGL.so", RTLD_LAZY | RTLD_LOCAL);
    if(!handle){
        fprintf(stderr, "GLESBridge: Failed to load system EGL library!\n");
        abort();
    }
    // Load system function locator from EGL lib
    sys_eglGetProcAddress = dlsym(handle, "eglGetProcAddress");
    if(!sys_eglGetProcAddress){
        fprintf(stderr, "GLESBridge: Unable to locate eglGetProcAddress symbol in EGL!");
        abort();
    }
}

// Stub required for LWJGL to locate GL/GLES functions
eglFuncPointer glXGetProcAddress(const char* func_name){
    eglFuncPointer func = (eglFuncPointer) sys_eglGetProcAddress(func_name);
    if(!func){
        fprintf(stderr, "GLESBridge: Unknown function %s!\n", func_name);
        return NULL;
    }
    return func;
}
// Stub required for libpojavexec - it loads EGL functions via wrapper library
eglFuncPointer eglGetProcAddress(const char* func_name){
    return sys_eglGetProcAddress(func_name);
}

