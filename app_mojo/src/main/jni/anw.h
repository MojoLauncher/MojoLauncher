//
// Created by whbex on 21.08.2026.
//

// ANativeWindow internals accessor

#ifndef POJAVLAUNCHER_ANW_H
#define POJAVLAUNCHER_ANW_H

#include <android/native_window.h>

#define NATIVE_WINDOW_API_DISCONNECT 14
#define NATIVE_WINDOW_API_CPU 2

// Layout mirrors struct ANativeWindow from <system/window.h> up to perform().
typedef struct {
    int magic;
    int version;
    void *reserved[4];
    void (*incRef)(void *);
    void (*decRef)(void *);
    const uint32_t flags;
    const int minSwapInterval;
    const int maxSwapInterval;
    const float xdpi;
    const float ydpi;
    intptr_t oem[4];
    int (*setSwapInterval)(void *, int);
    int (*dequeueBuffer_DEPRECATED)(void *, void **);
    int (*lockBuffer_DEPRECATED)(void *, void *);
    int (*queueBuffer_DEPRECATED)(void *, void *);
    int (*query)(const void *, int, int *);
    int (*perform)(void *, int, ...);
} ANativeWindow_ABI;

int native_window_api_disconnect(ANativeWindow *window, int api) {
    ANativeWindow_ABI* w = (ANativeWindow_ABI*) window;
    return w->perform(window, NATIVE_WINDOW_API_DISCONNECT, api);
}

#endif //POJAVLAUNCHER_ANW_H
