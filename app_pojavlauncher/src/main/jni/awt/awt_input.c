#include <jni.h>
#include <assert.h>
#include <string.h>
#include <stdio.h>

#include "awt.h"

jclass class_CTCAndroidInput;
jmethodID method_ReceiveInput;

static void invokeReceiveInput(jint type, jint i1, jint i2, jint i3, jint i4) {
    if (JNIEnv_InputRuntime == NULL) {
        if (runtimeVM == NULL) {
            return;
        } else {
            (*runtimeVM)->AttachCurrentThreadAsDaemon(runtimeVM, &JNIEnv_InputRuntime, NULL);
        }
    }

    if (method_ReceiveInput == NULL) {
        class_CTCAndroidInput = (*JNIEnv_InputRuntime)->FindClass(JNIEnv_InputRuntime, "net/java/openjdk/cacio/ctc/CTCAndroidInput");
        if ((*JNIEnv_InputRuntime)->ExceptionCheck(JNIEnv_InputRuntime) == JNI_TRUE) {
            (*JNIEnv_InputRuntime)->ExceptionClear(JNIEnv_InputRuntime);
            class_CTCAndroidInput = (*JNIEnv_InputRuntime)->FindClass(JNIEnv_InputRuntime, "com/github/caciocavallosilano/cacio/ctc/CTCAndroidInput");
        }
        assert(class_CTCAndroidInput != NULL);
        method_ReceiveInput = (*JNIEnv_InputRuntime)->GetStaticMethodID(JNIEnv_InputRuntime, class_CTCAndroidInput, "receiveData", "(IIIII)V");
        assert(method_ReceiveInput != NULL);
    }

    (*JNIEnv_InputRuntime)->CallStaticVoidMethod(
            JNIEnv_InputRuntime,
            class_CTCAndroidInput,
            method_ReceiveInput,
            type, i1, i2, i3, i4
    );
}


JNIEXPORT void JNICALL
Java_net_kdt_pojavlaunch_awt_AWTBridge_nativeSendCursorPos(JNIEnv *env,
                                                                              jclass clazz, jint x,
                                                                              jint y) {
    invokeReceiveInput(EVENT_TYPE_CURSOR_POS, (jint)((float) x * inputXRatio), (jint) ((float) y * inputYRatio), 0, 0);
}

JNIEXPORT jboolean JNICALL
Java_net_kdt_pojavlaunch_awt_AWTBridge_nativeSendKeyEvent(JNIEnv *env,
                                                                             jclass clazz,
                                                                             jint keycode,
                                                                             jint state, jint mods,
                                                                             jint codepoint) {
    jint translated_code = translate_awt_keycode(keycode);
    if(keycode != 0 && translated_code == 0) return false;
    invokeReceiveInput(EVENT_TYPE_KEY, codepoint, translated_code, state, 0);
    return true;
}

JNIEXPORT void JNICALL
Java_net_kdt_pojavlaunch_awt_AWTBridge_nativeSendMouseEvent(JNIEnv *env,
                                                                               jclass clazz,
                                                                               jint button,
                                                                               jint state,
                                                                               jint mods) {
    jint translated_key = translate_awt_mouse(button);
    if(translated_key == -1) return;
    invokeReceiveInput(EVENT_TYPE_MOUSE_BUTTON, translated_key, state, 0, 0);
}

JNIEXPORT void JNICALL
Java_net_kdt_pojavlaunch_awt_AWTBridge_nativeTypeChars(JNIEnv *env, jclass clazz,
                                                                          jstring chars) {
    jsize len = (*env)->GetStringLength(env, chars);
    const jchar* nchars = (*env)->GetStringChars(env, chars, NULL);
    // Dogshit
    for(jsize i = 0; i < len; i++) {
        invokeReceiveInput(EVENT_TYPE_CHAR, (jint)nchars[i], 0, 0 ,0);
    }
    (*env)->ReleaseStringChars(env, chars, nchars);
}