#ifndef GLES_BRIDGE_H_
#define GLES_BRIDGE_H_


typedef void* (*eglFuncPointer)(const char* func_name); // getProcAddress function pointer type
extern eglFuncPointer sys_eglGetProcAddress; // eglGetProcAddress function from the system EGL library

__attribute((constructor)) void init();
eglFuncPointer glXGetProcAddress(const char* func_name);

#endif