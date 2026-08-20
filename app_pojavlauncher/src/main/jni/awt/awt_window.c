//
// Created by whbex on 20.08.2026.
//

#include <assert.h>
#include <string.h>
#include "awt.h"

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

// TODO: check for memory leaks
JNIEXPORT jboolean JNICALL Java_net_kdt_pojavlaunch_awt_AWTWindow_nativeRenderFrame(JNIEnv* env, jclass clazz, jobject targetBuffer) {
    if (runtimeEnv == NULL) {
        if (runtimeVM == NULL) {
            return JNI_FALSE;
        } else {
            (*runtimeVM)->AttachCurrentThreadAsDaemon(runtimeVM, &runtimeEnv, NULL);
        }
    }
    jintArray jreRgbArray;

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
    jreRgbArray = (jintArray) (*runtimeEnv)->CallStaticObjectMethod(
            runtimeEnv,
            class_CTCScreen,
            method_GetRGB
    );
    if (jreRgbArray == NULL) {
        return JNI_FALSE;
    }

    jint arrayLength = (*runtimeEnv)->GetArrayLength(runtimeEnv, jreRgbArray);

    void* prim_src = (*runtimeEnv)->GetPrimitiveArrayCritical(runtimeEnv, jreRgbArray, NULL);
    void* prim_dst = (*env)->GetDirectBufferAddress(env, targetBuffer);
    if(prim_src == NULL) {
        return JNI_FALSE;
    }
    memcpy(prim_dst, prim_src, arrayLength * sizeof(jint));
    (*runtimeEnv)->ReleasePrimitiveArrayCritical(runtimeEnv, jreRgbArray, prim_src, 0);

    return JNI_TRUE;
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
