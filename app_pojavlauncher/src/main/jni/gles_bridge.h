#ifndef GLES_BRIDGE_H_
#define GLES_BRIDGE_H_



typedef void (*eglFuncPointer)(void); // getProcAddress function pointer type

extern eglFuncPointer (*sys_eglGetProcAddress)(const char* func_name); // eglGetProcAddress function from the system EGL library

__attribute((constructor)) void init();
eglFuncPointer glXGetProcAddress(const char* func_name);
eglFuncPointer eglGetProcAddress(const char* func_name);

#endif