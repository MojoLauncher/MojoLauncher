//
// Created by whbex on 20.08.2026.
//

#include <stdbool.h>
#include <pthread.h>
#include "awt.h"

jclass class_AWTBridge;

float inputXRatio = 0;
float inputYRatio = 0;

JavaVM* androidVM;
JavaVM* runtimeVM;

pthread_mutex_t vm_wait_mutex;
pthread_cond_t vm_wait_cond;

_Atomic bool isVmConnected = false;

// This is used across all PojavExec AWT library
JNIEnv* JNIEnv_InputRuntime;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    if (androidVM == NULL) {
        //Save dalvik global JavaVM pointer
        androidVM = vm;
        JNIEnv *env = NULL;
        (*vm)->GetEnv(vm, (void**)&env, JNI_VERSION_1_4);

        class_AWTBridge = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "net/kdt/pojavlaunch/awt/AWTBridge"));
        if (class_AWTBridge == NULL) {
            return JNI_ERR;
        }

        /*
         * Register the AWTBridge native methods explicitly instead of relying
         * on ART to discover exported Java_* symbols at call time. This avoids
         * UnsatisfiedLinkError when the packaged AWT .so was built with a
         * different symbol-visibility/linker configuration.
         */
        static const JNINativeMethod awt_bridge_methods[] = {
                {"nativeBeginRendering", "(Landroid/view/Surface;II)V",
                        (void *) Java_net_kdt_pojavlaunch_awt_AWTBridge_nativeBeginRendering},
                {"nativeEndRendering", "()V",
                        (void *) Java_net_kdt_pojavlaunch_awt_AWTBridge_nativeEndRendering},
                {"nativeMoveWindow", "(II)V",
                        (void *) Java_net_kdt_pojavlaunch_awt_AWTBridge_nativeMoveWindow},
                {"nativeResize", "(II)V",
                        (void *) Java_net_kdt_pojavlaunch_awt_AWTBridge_nativeResize},
                {"nativeSendCursorPos", "(II)V",
                        (void *) Java_net_kdt_pojavlaunch_awt_AWTBridge_nativeSendCursorPos},
                {"nativeSendKeyEvent", "(IIII)Z",
                        (void *) Java_net_kdt_pojavlaunch_awt_AWTBridge_nativeSendKeyEvent},
                {"nativeSendMouseEvent", "(III)V",
                        (void *) Java_net_kdt_pojavlaunch_awt_AWTBridge_nativeSendMouseEvent},
                {"nativeTypeChars", "(Ljava/lang/String;)V",
                        (void *) Java_net_kdt_pojavlaunch_awt_AWTBridge_nativeTypeChars},
                {"nativeClipboardReceived", "(Ljava/lang/String;Ljava/lang/String;)V",
                        (void *) Java_net_kdt_pojavlaunch_awt_AWTBridge_nativeClipboardReceived},
        };

        if ((*env)->RegisterNatives(env, class_AWTBridge, awt_bridge_methods,
                sizeof(awt_bridge_methods) / sizeof(awt_bridge_methods[0])) != JNI_OK) {
            return JNI_ERR;
        }

        register_methods_util(env);
        register_methods_clipboard(env);
    } else if (runtimeVM != vm) {
        runtimeVM = vm;
        isVmConnected = true;
        pthread_cond_broadcast(&vm_wait_cond);
    }

    return JNI_VERSION_1_4;
}


