//
// Created by whbex on 20.08.2026.
//

#include <assert.h>
#include <string.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <android/window.h>
#include <android/log.h>
#include <stdio.h>
#include "awt.h"
#include "../anw.h"

jclass class_Frame;
jclass class_Rectangle;
jmethodID constructor_Rectangle;
jmethodID method_GetFrames;
jmethodID method_GetBounds;
jmethodID method_SetBounds;

jclass class_CTCScreen;
jmethodID method_GetRGB;

jfieldID field_x;
jfieldID field_y;

JNIEnv* runtimeEnv;

ANativeWindow* anw;

static jint w;
static jint h;

void setup_jni() {
    if (method_GetRGB == NULL) {
        class_CTCScreen = (*runtimeEnv)->FindClass(runtimeEnv, "net/java/openjdk/cacio/ctc/CTCScreen");
        if ((*runtimeEnv)->ExceptionCheck(runtimeEnv) == JNI_TRUE) {
            (*runtimeEnv)->ExceptionClear(runtimeEnv);
            class_CTCScreen = (*runtimeEnv)->FindClass(runtimeEnv, "com/github/caciocavallosilano/cacio/ctc/CTCScreen");
        }
        assert(class_CTCScreen != NULL);
        method_GetRGB = (*runtimeEnv)->GetStaticMethodID(runtimeEnv, class_CTCScreen, "getCurrentScreenRGB", "()[I");
        assert(method_GetRGB != NULL);
    }
}

static void* acquire_cacio_screenbuffer(jint* arrayLength, jintArray* rgbArray) {
    *rgbArray = (jintArray) (*runtimeEnv)->CallStaticObjectMethod(
            runtimeEnv,
            class_CTCScreen,
            method_GetRGB
    );
    if (*rgbArray == NULL) {
        return NULL;
    }

    *arrayLength = (*runtimeEnv)->GetArrayLength(runtimeEnv, *rgbArray);
    return (*runtimeEnv)->GetPrimitiveArrayCritical(runtimeEnv, *rgbArray, NULL);
}

static void release_cacio_screenbuffer(jintArray rgbArray, void* src_buf) {
    (*runtimeEnv)->ReleasePrimitiveArrayCritical(runtimeEnv, rgbArray, src_buf, 0);
}

// TODO: check for memory leaks
JNIEXPORT void JNICALL Java_net_kdt_pojavlaunch_awt_AWTWindow_renderFrame(JNIEnv* env, jclass clazz) {
    if(!anw) return;
    if (runtimeEnv == NULL) {
        if (runtimeVM == NULL) {
            return;
        } else {
            (*runtimeVM)->AttachCurrentThreadAsDaemon(runtimeVM, &runtimeEnv, NULL);
        }
    }
    setup_jni();
    ARect rect;
    rect.top = 0;
    rect.left = 0;
    rect.bottom = h;
    rect.right = w;

    ANativeWindow_Buffer buffer;

    int res = ANativeWindow_lock(anw, &buffer, &rect);

    if(res) {
        __android_log_print(ANDROID_LOG_ERROR, "AWT", "Failed to lock native window: %d", res);
        return;
    }

    jintArray array;
    jint length;
    void* buf = acquire_cacio_screenbuffer(&length, &array);
    if(!length) goto end;
    if(!buf) {
        goto end;
    }

    jint *dst = (jint*)buffer.bits;
    jint *src = (jint*)buf;

    for(int y = 0; y < buffer.height; y++) {
        memcpy(&dst[y*buffer.stride], &src[y*buffer.width], buffer.width * sizeof(jint));
    }


    release_cacio_screenbuffer(array, src);
    end:
    ANativeWindow_unlockAndPost(anw);
}

JNIEXPORT void JNICALL
Java_net_kdt_pojavlaunch_awt_AWTWindow_nativeMoveWindow(JNIEnv *env, jclass clazz, jint xoff, jint yoff) {
    if (JNIEnv_InputRuntime == NULL) {
        if (runtimeVM == NULL) {
            return;
        } else {
            (*runtimeVM)->AttachCurrentThreadAsDaemon(runtimeVM, &JNIEnv_InputRuntime, NULL);
        }
    }
    if(field_y == NULL) {
        class_Frame = (*JNIEnv_InputRuntime)->FindClass(JNIEnv_InputRuntime, "java/awt/Frame");
        method_GetFrames = (*JNIEnv_InputRuntime)->GetStaticMethodID(JNIEnv_InputRuntime, class_Frame, "getFrames", "()[Ljava/awt/Frame;");
        method_GetBounds = (*JNIEnv_InputRuntime)->GetMethodID(JNIEnv_InputRuntime, class_Frame, "getBounds", "(Ljava/awt/Rectangle;)Ljava/awt/Rectangle;");
        method_SetBounds = (*JNIEnv_InputRuntime)->GetMethodID(JNIEnv_InputRuntime, class_Frame, "setBounds", "(Ljava/awt/Rectangle;)V");
        class_Rectangle = (*JNIEnv_InputRuntime)->FindClass(JNIEnv_InputRuntime, "java/awt/Rectangle");
        constructor_Rectangle = (*JNIEnv_InputRuntime)->GetMethodID(JNIEnv_InputRuntime, class_Rectangle, "<init>", "()V");
        field_x = (*JNIEnv_InputRuntime)->GetFieldID(JNIEnv_InputRuntime, class_Rectangle, "x", "I");
        field_y = (*JNIEnv_InputRuntime)->GetFieldID(JNIEnv_InputRuntime, class_Rectangle, "y", "I");
    }
    jobject rectangle = (*JNIEnv_InputRuntime)->NewObject(JNIEnv_InputRuntime, class_Rectangle, constructor_Rectangle);
    jobjectArray frames = (*JNIEnv_InputRuntime)->CallStaticObjectMethod(JNIEnv_InputRuntime, class_Frame, method_GetFrames);
    for(jsize i = 0; i < (*JNIEnv_InputRuntime)->GetArrayLength(JNIEnv_InputRuntime, frames); i++) {
        jobject frame = (*JNIEnv_InputRuntime)->GetObjectArrayElement(JNIEnv_InputRuntime, frames, i);
        (*JNIEnv_InputRuntime)->CallObjectMethod(JNIEnv_InputRuntime, frame, method_GetBounds, rectangle);
        (*JNIEnv_InputRuntime)->SetIntField(JNIEnv_InputRuntime, rectangle, field_x, (*JNIEnv_InputRuntime)->GetIntField(JNIEnv_InputRuntime, rectangle, field_x) + xoff);
        (*JNIEnv_InputRuntime)->SetIntField(JNIEnv_InputRuntime, rectangle, field_y, (*JNIEnv_InputRuntime)->GetIntField(JNIEnv_InputRuntime, rectangle, field_y) + yoff);
        (*JNIEnv_InputRuntime)->CallVoidMethod(JNIEnv_InputRuntime, frame, method_SetBounds, rectangle);
        (*JNIEnv_InputRuntime)->DeleteLocalRef(JNIEnv_InputRuntime, frame);
    }
    (*JNIEnv_InputRuntime)->DeleteLocalRef(JNIEnv_InputRuntime, rectangle);
    (*JNIEnv_InputRuntime)->DeleteLocalRef(JNIEnv_InputRuntime, frames);
}

JNIEXPORT void JNICALL
Java_net_kdt_pojavlaunch_awt_AWTWindow_setNativeSurface(JNIEnv *env, jclass clazz,
                                                        jobject surface) {
    anw = ANativeWindow_fromSurface(env, surface);
    __android_log_print(ANDROID_LOG_INFO, "AWT", "Acquired native window : %p", anw);
    ANativeWindow_setBuffersGeometry(anw, w, h, WINDOW_FORMAT_RGBX_8888);
}

JNIEXPORT void JNICALL
Java_net_kdt_pojavlaunch_awt_AWTWindow_destroySurface(JNIEnv *env, jclass clazz) {
    __android_log_print(ANDROID_LOG_INFO, "AWT", "Detaching native window : %p", anw);
    ANativeWindow_setBuffersGeometry(anw, 0, 0, 0);
    native_window_api_disconnect(anw, NATIVE_WINDOW_API_CPU);
    ANativeWindow_release(anw);
    anw = NULL;
}

JNIEXPORT void JNICALL
Java_net_kdt_pojavlaunch_awt_AWTWindow_setNativeSize(JNIEnv *env, jclass clazz, jint width,
                                                     jint height) {
    w = width;
    h = height;
}