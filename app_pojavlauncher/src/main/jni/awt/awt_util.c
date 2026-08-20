//
// Created by whbex on 20.08.2026.
//

#include "awt.h"

jclass class_CallbackBridge;
jmethodID method_OpenLink;
jmethodID method_OpenPath;

void register_methods_util(JNIEnv* env) {
    class_CallbackBridge = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "net/kdt/pojavlaunch/CallbackBridge"));
    method_OpenLink= (*env)->GetStaticMethodID(env, class_CallbackBridge, "openLink", "(Ljava/lang/String;)V");
    method_OpenPath= (*env)->GetStaticMethodID(env, class_CallbackBridge, "openLink", "(Ljava/lang/String;)V");
}

JNIEXPORT void JNICALL Java_net_java_openjdk_cacio_ctc_CTCDesktopPeer_openFile(JNIEnv *env, jclass clazz, jstring filePath) {
    JNIEnv *dalvikEnv;
    char detachable = 0;
    if((*androidVM)->GetEnv(androidVM, (void **) &dalvikEnv, JNI_VERSION_1_6) == JNI_EDETACHED) {
        (*androidVM)->AttachCurrentThread(androidVM, &dalvikEnv, NULL);
        detachable = 1;
    }
    const char* stringChars = (*env)->GetStringUTFChars(env, filePath, NULL);
    (*dalvikEnv)->CallStaticVoidMethod(dalvikEnv, class_CallbackBridge, method_OpenPath, (*dalvikEnv)->NewStringUTF(dalvikEnv, stringChars));
    (*env)->ReleaseStringUTFChars(env, filePath, stringChars);
    if(detachable) (*androidVM)->DetachCurrentThread(androidVM);
}

JNIEXPORT void JNICALL Java_net_java_openjdk_cacio_ctc_CTCDesktopPeer_openUri(JNIEnv *env, jclass clazz, jstring uri) {
    JNIEnv *dalvikEnv;char detachable = 0;
    if((*androidVM)->GetEnv(androidVM, (void **) &dalvikEnv, JNI_VERSION_1_6) == JNI_EDETACHED) {
        (*androidVM)->AttachCurrentThread(androidVM, &dalvikEnv, NULL);
        detachable = 1;
    }
    const char* stringChars = (*env)->GetStringUTFChars(env, uri, NULL);
    (*dalvikEnv)->CallStaticVoidMethod(dalvikEnv, class_CallbackBridge, method_OpenLink, (*dalvikEnv)->NewStringUTF(dalvikEnv, stringChars));
    (*env)->ReleaseStringUTFChars(env, uri, stringChars);
    if(detachable) (*androidVM)->DetachCurrentThread(androidVM);
}
