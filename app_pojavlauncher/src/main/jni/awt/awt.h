//
// Created by whbex on 20.08.2026.
//

#ifndef POJAVLAUNCHER_AWT_H
#define POJAVLAUNCHER_AWT_H

#include <jni.h>
#include <bits/pthread_types.h>
#include <stdbool.h>

extern JavaVM* androidVM;
extern JavaVM* runtimeVM;

// Runtime VM can appear later
extern pthread_mutex_t vm_wait_mutex;
extern pthread_cond_t vm_wait_cond;
extern _Atomic bool isVmConnected;


extern JNIEnv* JNIEnv_InputRuntime;

void register_methods_clipboard(JNIEnv* env);
void register_methods_util(JNIEnv* env);

#endif //POJAVLAUNCHER_AWT_H
