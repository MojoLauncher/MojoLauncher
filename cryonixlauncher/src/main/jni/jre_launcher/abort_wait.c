//
// Created by maks on 28.09.2025.
//

#include <stdint.h>
#include <unistd.h>
#include <linux/futex.h>
#include <sys/syscall.h>
#include <pthread.h>
#include <stdbool.h>
#include <jni.h>
#include <stdlib.h>

static uint32_t futex_word = 1;

struct {
    int code;
    bool is_signal;
} abort_info = {0, false};

struct {
    JavaVM *vm;
    jmethodID exit_method;
    jclass exit_class;
    jobject context;
} vm_exit_data = {NULL, NULL, NULL, NULL};

_Noreturn static void callExit() {
    JavaVM *vm = vm_exit_data.vm;
    JNIEnv *env;
    jint result = (*vm)->GetEnv(vm, (void**)&env, JNI_VERSION_1_6);
    if(result == JNI_EDETACHED) {
        result = (*vm)->AttachCurrentThread(vm, &env, NULL);
    }
    if(result != JNI_OK) {
        abort();
    }
    // This will call System.exit()
    (*env)->CallStaticVoidMethod(env, vm_exit_data.exit_class, vm_exit_data.exit_method,
                                 vm_exit_data.context, abort_info.code, abort_info.is_signal);
    while (true) {}
}

static void* abort_wait(void* unused) {
    syscall(SYS_futex, &futex_word, FUTEX_WAIT, 1, NULL);
    callExit();
    return NULL;
}

void setup_abort_wait() {
    pthread_t abort_thread;
    pthread_create(&abort_thread, NULL, abort_wait, NULL);
}

_Noreturn void abort_call(int code, bool is_signal) {
    abort_info.code = code;
    abort_info.is_signal = is_signal;
    futex_word = 0;
    syscall(SYS_futex, &futex_word, FUTEX_WAKE, NULL);
    while(true) {}
}

JNIEXPORT void JNICALL
Java_net_kdt_pojavlaunch_utils_jre_JavaRunner_nativeSetupExit(JNIEnv *env, jclass clazz,
                                                              jobject context) {
    (*env)->GetJavaVM(env, &vm_exit_data.vm);
    jclass class = (*env)->FindClass(env, "net/kdt/pojavlaunch/ExitActivity");
    vm_exit_data.exit_class  = (*env)->NewGlobalRef(env, class);
    vm_exit_data.exit_method = (*env)->GetStaticMethodID(env, class, "showExitMessage", "(Landroid/content/Context;IZ)V");
    if(vm_exit_data.context != NULL) {
        jobject oldRef = vm_exit_data.context;
        vm_exit_data.context = (*env)->NewGlobalRef(env, context);
        (*env)->DeleteGlobalRef(env, oldRef);
    }else {
        vm_exit_data.context = (*env)->NewGlobalRef(env, context);
    }
}