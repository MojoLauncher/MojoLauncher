//
// Created by maks on 20.09.2025.
//

#include <jni.h>
#include <stdbool.h>
#include <dlfcn.h>
#include <stdlib.h>
#include "elf_hinter.h"
#include "load_stages.h"

#define TAG __FILE_NAME__
#include <log.h>



// Java 21 style hook
typedef jboolean (*NativeLibraries_load)(JNIEnv *env, jclass cls, jobject lib, jstring name, jboolean isBuiltin, jboolean throwExceptionIfFail);

// Java 17 style hook
typedef jboolean (*j17_NativeLibraries_load)(JNIEnv *env, jclass cls, jobject lib, jstring name, jboolean isBuiltin, jboolean isJNI, jboolean throwExceptionIfFail);

// Java 8 style hook
typedef void(*ClassLoader_00024NativeLibrary_load)(JNIEnv *env, jobject this, jstring name, jboolean isBuiltin);

union {
    NativeLibraries_load j21;
    j17_NativeLibraries_load j17;
    ClassLoader_00024NativeLibrary_load j8;
    void* generic;
} original_func;



static jboolean hook_NativeLibraries_load(JNIEnv *env, jclass cls, jobject lib, jstring name, jboolean isBuiltin, jboolean throwExceptionIfFail) {
    const char* name_n = (*env)->GetStringUTFChars(env, name, NULL);
    hinter_t hinter;
    hinter_process(&hinter, name_n);
    (*env)->ReleaseStringUTFChars(env, name, name_n);
    jboolean result = original_func.j21(env, cls, lib, name, isBuiltin, throwExceptionIfFail);
    hinter_free(&hinter);
    return result;
}

static jboolean hook_j17_NativeLibraries_load(JNIEnv *env, jclass cls, jobject lib, jstring name, jboolean isBuiltin, jboolean isJNI, jboolean throwExceptionIfFail) {
    const char* name_n = (*env)->GetStringUTFChars(env, name, NULL);
    hinter_t hinter;
    hinter_process(&hinter, name_n);
    (*env)->ReleaseStringUTFChars(env, name, name_n);
    jboolean result = original_func.j17(env, cls, lib, name, isBuiltin, isJNI, throwExceptionIfFail);
    hinter_free(&hinter);
    return result;
}

static void hook_ClassLoader_00024NativeLibrary_load(JNIEnv *env, jobject this, jstring name, jboolean isBuiltin) {
    const char* name_n = (*env)->GetStringUTFChars(env, name, NULL);
    hinter_t hinter;
    hinter_process(&hinter, name_n);
    (*env)->ReleaseStringUTFChars(env, name, name_n);
    original_func.j8(env, this, name, isBuiltin);
    hinter_free(&hinter);
}

static bool testMethod(JNIEnv *vm_env, jclass hookClass, const char* sign, JNINativeMethod* hookMethod) {
    jmethodID hookMethod_j17 = (*vm_env)->GetStaticMethodID(vm_env, hookClass, hookMethod->name, sign);
    if(hookMethod_j17 != NULL) {
        hookMethod->signature = sign;
        return true;
    }else {
        (*vm_env)->ExceptionClear(vm_env);
        return false;
    }
}

bool installClassLoaderHooks(JNIEnv *env, JNIEnv* vm_env) {
    void* libjava = dlopen("libjava.so", RTLD_NOLOAD);
    if(libjava == NULL) {
        throwException(env, STAGE_FIND_HOOKS_NATIVE, JNI_ERR, "Failed to find libjava.so after VM startup");
        return false;
    }
    jclass hookClass;
    JNINativeMethod hookMethod;
    hookClass = (*vm_env)->FindClass(vm_env,"java/lang/ClassLoader$NativeLibrary");
    if(hookClass != NULL) {
        hookMethod.name = "load";
        hookMethod.signature = "(Ljava/lang/String;Z)V";
        hookMethod.fnPtr = hook_ClassLoader_00024NativeLibrary_load;
        original_func.j8 = dlsym(libjava, "Java_java_lang_ClassLoader_00024NativeLibrary_load");
    }else {
        (*vm_env)->ExceptionClear(vm_env);
        hookClass = (*vm_env)->FindClass(vm_env, "jdk/internal/loader/NativeLibraries");
        if(hookClass == NULL) goto err;
        hookMethod.name = "load";
        original_func.generic = dlsym(libjava, "Java_jdk_internal_loader_NativeLibraries_load");
        if(testMethod(vm_env, hookClass, "(Ljdk/internal/loader/NativeLibraries$NativeLibraryImpl;Ljava/lang/String;ZZZ)Z", &hookMethod)) {
            hookMethod.fnPtr = hook_j17_NativeLibraries_load;
        }else if(testMethod(vm_env, hookClass, "(Ljdk/internal/loader/NativeLibraries$NativeLibraryImpl;Ljava/lang/String;ZZ)Z", &hookMethod)) {
            hookMethod.fnPtr = hook_NativeLibraries_load;
        }else {
            throwException(env, STAGE_FIND_HOOKS, JNI_ERR, "Cannot find hook target.");
        }
    }
    jint result = (*vm_env)->RegisterNatives(vm_env, hookClass, &hookMethod, 1);
    if(result != JNI_OK) {
        if((*vm_env)->ExceptionCheck(vm_env)) (*vm_env)->ExceptionDescribe(vm_env);
        throwException(env, STAGE_INSERT_HOOKS, result, "Cannot register hooks");
        return false;
    }
    return true;
    err:
    throwException(env, STAGE_FIND_HOOKS, JNI_ERR, "Cannot find hook target.");
    return false;
}