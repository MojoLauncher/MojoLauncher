//
// Created by whbex on 20.08.2026.
//

#include <assert.h>
#include <string.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <android/window.h>
#include <android/log.h>
#include <sys/time.h>
#include <stdio.h>
#include <pthread.h>
#include "awt.h"
#include "../anw.h"

static jclass class_Frame;
static jclass class_Rectangle;
static jmethodID constructor_Rectangle;
static jmethodID method_GetFrames;
static jmethodID method_GetBounds;
static jmethodID method_SetBounds;

static jclass class_CTCScreen = NULL;
static jmethodID method_GetRGB = NULL;

jfieldID field_x;
jfieldID field_y;

static _Atomic bool is_rendering = false;
static pthread_t render_thread = 0;

void setup_jni(JNIEnv *env) {
    if(method_GetRGB != NULL) return;
    class_CTCScreen = (*env)->FindClass(env, "net/java/openjdk/cacio/ctc/CTCScreen");
    if ((*env)->ExceptionCheck(env) == JNI_TRUE) {
        (*env)->ExceptionClear(env);
        class_CTCScreen = (*env)->FindClass(env, "com/github/caciocavallosilano/cacio/ctc/CTCScreen");
    }
    assert(class_CTCScreen != NULL);
    method_GetRGB = (*env)->GetStaticMethodID(env, class_CTCScreen, "getCurrentScreenRGB", "()[I");
    assert(method_GetRGB != NULL);
}

static void* acquire_cacio_screenbuffer(JNIEnv *env, jintArray* rgbArray) {
    *rgbArray = (jintArray) (*env)->CallStaticObjectMethod(
            env,
            class_CTCScreen,
            method_GetRGB
    );
    if((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionClear(env);
        return NULL;
    }
    if (*rgbArray == NULL) {
        return NULL;
    }

    return (*env)->GetPrimitiveArrayCritical(env, *rgbArray, NULL);
}

static void release_cacio_screenbuffer(JNIEnv *env, jintArray rgbArray, void* src_buf) {
    (*env)->ReleasePrimitiveArrayCritical(env, rgbArray, src_buf, 0);
}

static inline void add_nsec(long nsec, struct timespec *spec) {
    spec->tv_nsec += nsec;
    if(spec->tv_nsec >= 1000000000) {
        spec->tv_nsec -= 1000000000;
        spec->tv_sec += 1;
    }
}

static void* render_loop_thread(void* param) {
    ANativeWindow *window = (ANativeWindow*) param;
    JNIEnv *env;

    is_rendering = true;

    if (!isVmConnected) {
        pthread_mutex_lock(&vm_wait_mutex);
        pthread_cond_wait(&vm_wait_cond, &vm_wait_mutex);
        pthread_mutex_unlock(&vm_wait_mutex);
        // If the VM was not connected but thread shutdown is wanted, then do not proceed,
        // just release and exit.
        if(!is_rendering) goto exit;
    }

    (*runtimeVM)->AttachCurrentThreadAsDaemon(runtimeVM, &env, NULL);

    setup_jni(env);

    jintArray array;
    ANativeWindow_Buffer buffer;
    struct timespec frame_now;

    while(is_rendering) {
        clock_gettime(CLOCK_MONOTONIC, &frame_now);
        add_nsec(4000000, &frame_now);
        void* buf = acquire_cacio_screenbuffer(env, &array);
        if(!buf) continue;

        int32_t res;
        if((res = ANativeWindow_lock(window, &buffer, NULL))) {
            __android_log_print(ANDROID_LOG_ERROR, "AWTRender", "Failed to lock buffer: %"PRIi32, res);
            goto end;
        }

        jint *dst = (jint*)buffer.bits;
        jint *src = (jint*)buf;

        for(int y = 0; y < buffer.height; y++) {
            memcpy(&dst[y * buffer.stride], &src[y * buffer.width], buffer.width * sizeof(jint));
        }

        ANativeWindow_unlockAndPost(window);
        end:
        release_cacio_screenbuffer(env, array, src);
        clock_nanosleep(CLOCK_MONOTONIC, TIMER_ABSTIME, &frame_now, NULL);
    }

    (*runtimeVM)->DetachCurrentThread(runtimeVM);
    native_window_api_disconnect(window, NATIVE_WINDOW_API_CPU);
    exit:
    ANativeWindow_release(window);

    return NULL;
}

static void render_loop_shutdown() {
    is_rendering = false;
    pthread_mutex_lock(&vm_wait_mutex);
    pthread_cond_broadcast(&vm_wait_cond);
    pthread_mutex_unlock(&vm_wait_mutex);
    pthread_join(render_thread, NULL);
    render_thread = 0;
}

static void update_dims(jint width, jint height) {
    inputXRatio = CANVAS_WIDTH / (float)width;
    inputYRatio = CANVAS_HEIGHT / (float)height;
}

JNIEXPORT void JNICALL
Java_net_kdt_pojavlaunch_awt_AWTBridge_nativeBeginRendering(JNIEnv *env,
                                                                               jclass clazz,
                                                                               jobject surface,
                                                                               jint bridge_width,
                                                                               jint bridge_height) {
    ANativeWindow *window = ANativeWindow_fromSurface(env, surface);
    ANativeWindow_setBuffersGeometry(window, CANVAS_WIDTH, CANVAS_HEIGHT, AHARDWAREBUFFER_FORMAT_R8G8B8X8_UNORM);
    if(render_thread) {
        render_loop_shutdown();
    }
    update_dims(bridge_width, bridge_height);
    pthread_create(&render_thread, NULL, render_loop_thread, window);
}

JNIEXPORT void JNICALL
Java_net_kdt_pojavlaunch_awt_AWTBridge_nativeMoveWindow(JNIEnv *env,
                                                                           jclass clazz, jint xoff,
                                                                           jint yoff) {
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
Java_net_kdt_pojavlaunch_awt_AWTBridge_nativeEndRendering(JNIEnv *env,
                                                                             jclass clazz) {
    render_loop_shutdown();
}

JNIEXPORT void JNICALL
Java_net_kdt_pojavlaunch_awt_AWTBridge_nativeResize(JNIEnv *env, jclass clazz,
                                                                       jint bridge_width,
                                                                       jint bridge_height) {
    update_dims(bridge_width, bridge_height);
}