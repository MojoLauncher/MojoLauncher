#pragma once

#include <stdbool.h>
#include <jni.h>

typedef struct {
    jint length;
    const char* strings[0];
} heap_str_array;

heap_str_array* hstr_from_jni(JNIEnv *env, jobjectArray jstringArray);
jobjectArray    hstr_to_jni(JNIEnv *env, heap_str_array* array, bool autofree);
void            hstr_free(heap_str_array* arr);

void openLink(const char* link);

JNIEnv* get_attached_env(JavaVM* jvm);

