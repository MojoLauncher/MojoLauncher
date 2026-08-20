//
// Created by whbex on 20.08.2026.
//

#include <jni.h>

#include "awt.h"

jclass class_AWTClipboard;
jclass class_CTCClipboard;
jmethodID method_putClipboardString;
jmethodID method_queryClipboardString;
jmethodID method_SystemClipboardDataReceived;

void register_methods_clipboard(JNIEnv* env) {
    class_AWTClipboard = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "net/kdt/pojavlaunch/awt/AWTClipboard"));
    method_queryClipboardString = (*env)->GetStaticMethodID(env, class_AWTClipboard, "queryClipboardString", "()V");
    method_putClipboardString = (*env)->GetStaticMethodID(env, class_AWTClipboard, "putClipboardString", "(Ljava/lang/String;)V");
}

JNIEXPORT void JNICALL Java_net_kdt_pojavlaunch_awt_AWTClipboard_nativeClipboardReceived(JNIEnv *env, jclass clazz, jstring clipboardData, jstring clipboardDataMime) {
    if(method_SystemClipboardDataReceived == NULL || class_CTCClipboard == NULL) return;
    if (JNIEnv_InputRuntime == NULL) {
        if (runtimeVM == NULL) {
            return;
        } else {
            (*runtimeVM)->AttachCurrentThreadAsDaemon(runtimeVM, &JNIEnv_InputRuntime, NULL);
        }
    }
    const char* dataChars = clipboardData != NULL ? (*env)->GetStringUTFChars(env, clipboardData, NULL) : NULL;
    const char* mimeChars = clipboardDataMime != NULL ? (*env)->GetStringUTFChars(env, clipboardDataMime, NULL) : NULL;
    (*JNIEnv_InputRuntime)->CallStaticVoidMethod(JNIEnv_InputRuntime, class_CTCClipboard, method_SystemClipboardDataReceived,
                                                    clipboardData != NULL ? (*JNIEnv_InputRuntime)->NewStringUTF(JNIEnv_InputRuntime, dataChars) : NULL,
                                                    clipboardDataMime != NULL ? (*JNIEnv_InputRuntime)->NewStringUTF(JNIEnv_InputRuntime, mimeChars) : NULL);
    if(dataChars != NULL) (*env)->ReleaseStringUTFChars(env, clipboardData, dataChars);
    if(mimeChars != NULL) (*env)->ReleaseStringUTFChars(env, clipboardDataMime, mimeChars);
}

JNIEXPORT void JNICALL Java_net_java_openjdk_cacio_ctc_CTCClipboard_nQuerySystemClipboard(JNIEnv *env, jclass clazz) {
    JNIEnv *dalvikEnv;
    char detachable = 0;
    if((*androidVM)->GetEnv(androidVM, (void **) &dalvikEnv, JNI_VERSION_1_6) == JNI_EDETACHED) {
        (*androidVM)->AttachCurrentThread(androidVM, &dalvikEnv, NULL);
        detachable = 1;
    }
    if(method_SystemClipboardDataReceived == NULL) {
        class_CTCClipboard = (*env)->NewGlobalRef(env, clazz);
        method_SystemClipboardDataReceived = (*env)->GetStaticMethodID(env, clazz, "systemClipboardDataReceived", "(Ljava/lang/String;Ljava/lang/String;)V");
    }
    (*dalvikEnv)->CallStaticVoidMethod(dalvikEnv, class_AWTClipboard, method_queryClipboardString);
    if(detachable) (*androidVM)->DetachCurrentThread(androidVM);
}

JNIEXPORT void JNICALL Java_net_java_openjdk_cacio_ctc_CTCClipboard_nPutClipboardData(JNIEnv* env, jclass clazz, jstring clipboardData, jstring clipboardDataMime) {
    JNIEnv *dalvikEnv;
    char detachable = 0;
    if((*androidVM)->GetEnv(androidVM, (void **) &dalvikEnv, JNI_VERSION_1_6) == JNI_EDETACHED) {
        (*androidVM)->AttachCurrentThread(androidVM, &dalvikEnv, NULL);
        detachable = 1;
    }

    const char* dataChars = (*env)->GetStringUTFChars(env, clipboardData, NULL);
    (*dalvikEnv)->CallStaticVoidMethod(dalvikEnv, class_AWTClipboard, method_putClipboardString,
                                       (*dalvikEnv)->NewStringUTF(dalvikEnv, dataChars));
    (*env)->ReleaseStringUTFChars(env, clipboardData, dataChars);
    if(detachable) (*androidVM)->DetachCurrentThread(androidVM);
}

JNIEXPORT void JNICALL Java_com_github_caciocavallosilano_cacio_ctc_CTCClipboard_nQuerySystemClipboard(JNIEnv *env, jclass clazz) {
    Java_net_java_openjdk_cacio_ctc_CTCClipboard_nQuerySystemClipboard(env, clazz);
}

JNIEXPORT void JNICALL Java_com_github_caciocavallosilano_cacio_ctc_CTCClipboard_nPutClipboardData(JNIEnv* env, jclass clazz, jstring clipboardData, jstring clipboardDataMime) {
    Java_net_java_openjdk_cacio_ctc_CTCClipboard_nPutClipboardData(env, clazz, clipboardData, clipboardDataMime);
}


