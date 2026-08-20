//
// Created by whbex on 20.08.2026.
//

#ifndef POJAVLAUNCHER_AWT_H
#define POJAVLAUNCHER_AWT_H

#include <jni.h>

extern JavaVM* androidVM;
extern JavaVM* runtimeVM;

extern JNIEnv* JNIEnv_InputRuntime;

void register_methods_clipboard(JNIEnv* env);
void register_methods_util(JNIEnv* env);

#endif //POJAVLAUNCHER_AWT_H
