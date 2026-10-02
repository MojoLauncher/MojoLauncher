//
// Created by maks on 09.04.2026.
//

#include "utils.h"
#include <jni.h>
#include <stdio.h>
#include <dlfcn.h>
#include <stdlib.h>
#include <string.h>

static JavaVM* dalivk;
static jclass class_CallbackBridge;
static jmethodID method_openLink;


void openLink(const char* link) {
    JNIEnv *attachedEnv = get_attached_env(dalivk);
    (*attachedEnv)->CallStaticVoidMethod(attachedEnv, class_CallbackBridge, method_openLink, (*attachedEnv)->NewStringUTF(attachedEnv, link));
}

JNIEXPORT void JNICALL
Java_net_kdt_pojavlaunch_CallbackBridge_minibridgeInit(JNIEnv *env, jclass clazz) {
    (*env)->GetJavaVM(env, &dalivk);
    class_CallbackBridge = (*env)->NewGlobalRef(env, clazz);
    method_openLink = (*env)->GetStaticMethodID(env, clazz, "openLink", "(Ljava/lang/String;)V");
}