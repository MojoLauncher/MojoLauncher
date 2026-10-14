//
// Created by whbex on 20.08.2026.
//

#include "awt.h"

jmethodID method_OpenLink;
jmethodID method_OpenPath;
jmethodID method_NotifyAwtWindow;

void register_methods_util(JNIEnv* env) {
    method_OpenLink = (*env)->GetStaticMethodID(env, class_AWTBridge, "openLink", "(Ljava/lang/String;)V");
    method_OpenPath = (*env)->GetStaticMethodID(env, class_AWTBridge, "openLink", "(Ljava/lang/String;)V");
    method_NotifyAwtWindow = (*env)->GetStaticMethodID(env, class_AWTBridge, "notifyWindowOpened", "()V");
}

JNIEXPORT void JNICALL Java_net_java_openjdk_cacio_ctc_CTCDesktopPeer_openFile(JNIEnv *env, jclass clazz, jstring filePath) {
    DVMENV_ENTER()
    const char* stringChars = (*env)->GetStringUTFChars(env, filePath, NULL);
    (*dalvikEnv)->CallStaticVoidMethod(dalvikEnv, class_AWTBridge, method_OpenPath, (*dalvikEnv)->NewStringUTF(dalvikEnv, stringChars));
    (*env)->ReleaseStringUTFChars(env, filePath, stringChars);
    DVMENV_EXIT()
}

JNIEXPORT void JNICALL Java_net_java_openjdk_cacio_ctc_CTCDesktopPeer_openUri(JNIEnv *env, jclass clazz, jstring uri) {
    DVMENV_ENTER()
    const char* stringChars = (*env)->GetStringUTFChars(env, uri, NULL);
    (*dalvikEnv)->CallStaticVoidMethod(dalvikEnv, class_AWTBridge, method_OpenLink, (*dalvikEnv)->NewStringUTF(dalvikEnv, stringChars));
    (*env)->ReleaseStringUTFChars(env, uri, stringChars);
    DVMENV_EXIT()
}

JNIEXPORT void JNICALL Java_net_java_openjdk_cacio_ctc_NotifierWindowFactory_onToplevelWindowCreated(JNIEnv *env, jclass clazz) {
    DVMENV_ENTER()
    (*dalvikEnv)->CallStaticVoidMethod(dalvikEnv, class_AWTBridge, method_NotifyAwtWindow);
    DVMENV_EXIT()
}

JNIEXPORT void JNICALL Java_com_github_caciocavallosilano_cacio_ctc_NotifierWindowFactory_onToplevelWindowCreated(JNIEnv *env, jclass clazz) {
    Java_net_java_openjdk_cacio_ctc_NotifierWindowFactory_onToplevelWindowCreated(env, clazz);
}