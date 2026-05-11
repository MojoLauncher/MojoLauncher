//
// Created by maks on 09.04.2026.
//

#include "utils.h"
#include "pojavexec.h"
#include "driver_helper/nsbypass.h"
#include <jni.h>
#include <stdio.h>
#include <dlfcn.h>
#include <stdlib.h>

static JavaVM* dalivk;
static jclass class_CallbackBridge;
static jmethodID method_openLink;

static pojavexec_renderspec_t renderspec = {0};

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


JNIEXPORT jboolean JNICALL
Java_net_kdt_pojavlaunch_utils_JREUtils_configureRenderspec(JNIEnv *env, jclass clazz,
                                                            jstring eglPath, jboolean use_loader_bypass,
                                                            jboolean use_gles,
                                                            jint gles_version) {
    void* egl_handle = NULL;
    if(eglPath != NULL) {
        const char* egl_path = (*env)->GetStringUTFChars(env, eglPath, NULL);
        if(use_loader_bypass) {
            const char* native_dir = getenv("POJAV_NATIVEDIR");
            if(!native_dir) return false;
            if(!linker_ns_load(native_dir)) return false;
            egl_handle = linker_ns_dlopen(egl_path, RTLD_LOCAL | RTLD_NOW);
            if(!egl_handle) {
                printf("Failed to dlopen EGL: %s\n", dlerror());
                return false;
            }
        } else {
            egl_handle = dlopen(egl_path, RTLD_NOW);
            if(!egl_handle) {
                printf("Failed to load EGL %s: %s\n", egl_path, dlerror());
                return false;
            }
        }
        printf("Loaded EGL %s: %p\n", egl_path, egl_handle);
        (*env)->ReleaseStringUTFChars(env, eglPath, egl_path);
    }
    renderspec.egl_handle = egl_handle;
    renderspec.force_gles_context = use_gles;
    renderspec.override_major_version = gles_version;
    return true;
}

const pojavexec_renderspec_t* pojavexec_getRenderSpec() {
    return &renderspec;
}


