#pragma once

#include <stdbool.h>
#include <jni.h>

const char** convert_to_char_array(JNIEnv *env, jobjectArray jstringArray);
jobjectArray convert_from_char_array(JNIEnv *env, const char **charArray, jint num_rows);
void free_char_array(JNIEnv *env, jobjectArray jstringArray, const char **charArray);
void openLink(const char* link);
jstring convertStringJVM(JNIEnv* srcEnv, JNIEnv* dstEnv, jstring srcStr);

JNIEnv* get_attached_env(JavaVM* jvm);

