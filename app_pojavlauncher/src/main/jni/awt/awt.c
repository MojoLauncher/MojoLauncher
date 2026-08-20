//
// Created by whbex on 20.08.2026.
//

#include "awt.h"

JavaVM* androidVM;
JavaVM* runtimeVM;

// This is used across all PojavExec AWT library
JNIEnv* JNIEnv_InputRuntime;

jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    if (androidVM == NULL) {
        //Save dalvik global JavaVM pointer
        androidVM = vm;
        JNIEnv *env = NULL;
        (*vm)->GetEnv(vm, (void**)&env, JNI_VERSION_1_4);

        register_methods_util(env);
        register_methods_clipboard(env);
    } else if (androidVM != vm) {
        runtimeVM = vm;
    }

    return JNI_VERSION_1_4;
}


