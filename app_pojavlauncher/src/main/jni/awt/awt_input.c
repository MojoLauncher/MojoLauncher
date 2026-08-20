#include <jni.h>
#include <assert.h>
#include <string.h>
#include <stdio.h>

#include "awt.h"

jclass class_CTCAndroidInput;
jmethodID method_ReceiveInput;

JNIEXPORT void JNICALL Java_net_kdt_pojavlaunch_awt_AWTInput_nativeSendData(JNIEnv* env, jclass clazz, jint type, jint i1, jint i2, jint i3, jint i4) {
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


