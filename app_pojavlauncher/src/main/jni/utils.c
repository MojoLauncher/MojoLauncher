#include <jni.h>
#include <dlfcn.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>

#include "log.h"

#include "utils.h"

typedef void (*android_update_LD_LIBRARY_PATH_t)(const char*);

static inline void hstr_free_part(heap_str_array* arr, jint start, jint end) {
    for(jint f = start; f < end; f++) {
        if(arr->strings[f]) free((void*) arr->strings[f]);
    }
    free(arr);
}

heap_str_array* hstr_from_jni(JNIEnv *env, jobjectArray jstringArray) {
    jint num_entries = (*env)->GetArrayLength(env, jstringArray), i = 0;
    heap_str_array* ret_array = (heap_str_array*) malloc(sizeof(heap_str_array*) + num_entries * sizeof(const char*));

    (*env)->PushLocalFrame(env, num_entries);
    for(i = 0; i < num_entries; i++) {
        jstring entry = (*env)->GetObjectArrayElement(env, jstringArray, i);
        const char* str_content = (*env)->GetStringUTFChars(env, entry, NULL);
        if(str_content == NULL) goto fail;
        const char* str_copy = strdup(str_content);
        (*env)->ReleaseStringUTFChars(env, entry, str_content);
        if(str_copy == NULL) goto fail;
        ret_array->strings[i] = str_copy;
    }
    (*env)->PopLocalFrame(env, NULL);

    ret_array->length = num_entries;

    printf("allocated array %p length %i\n", ret_array, ret_array->length);

    return ret_array;

    fail:
    hstr_free_part(ret_array, 0, i);
    return NULL;
}

jobjectArray hstr_to_jni(JNIEnv *env, heap_str_array* array, bool autofree) {
    jclass class_String = (*env)->FindClass(env, "java/lang/String");
    jobjectArray dstArray = (*env)->NewObjectArray(env, array->length, class_String, NULL);
    (*env)->DeleteLocalRef(env, class_String);
    (*env)->PushLocalFrame(env, array->length);
    jint i;

    for(i = 0; i < array->length; i++) {
        const char* str = array->strings[i];
        if(str == NULL) continue;
        jstring new_str = (*env)->NewStringUTF(env, str);
        if(new_str == NULL) goto fail;
        (*env)->SetObjectArrayElement(env, dstArray, i, new_str);
        if(autofree) {
            free((void*) str);
            array->strings[i] = NULL;
        }
    }
    if(autofree) free(array);
    (*env)->PopLocalFrame(env, NULL);
    return dstArray;

    fail:
    hstr_free_part(array, i, array->length);
    (*env)->PopLocalFrame(env, NULL);
    return NULL;
}

void hstr_free(heap_str_array* arr) {
    hstr_free_part(arr, 0, arr->length);
}

JNIEXPORT void JNICALL Java_net_kdt_pojavlaunch_utils_JREUtils_setLdLibraryPath(JNIEnv *env, jclass clazz, jstring ldLibraryPath) {
	// jclass exception_cls = (*env)->FindClass(env, "java/lang/UnsatisfiedLinkError");
	
	android_update_LD_LIBRARY_PATH_t android_update_LD_LIBRARY_PATH;
	
	void *libdl_handle = dlopen("libdl.so", RTLD_LAZY);
	void *updateLdLibPath = dlsym(libdl_handle, "android_update_LD_LIBRARY_PATH");
	if (updateLdLibPath == NULL) {
		updateLdLibPath = dlsym(libdl_handle, "__loader_android_update_LD_LIBRARY_PATH");
		if (updateLdLibPath == NULL) {
			char *dl_error_c = dlerror();
			LOGE("Error getting symbol android_update_LD_LIBRARY_PATH: %s", dl_error_c);
			// (*env)->ThrowNew(env, exception_cls, dl_error_c);
		}
	}

    LOGI("updateLdLibPath: %p", updateLdLibPath);
	
	android_update_LD_LIBRARY_PATH = (android_update_LD_LIBRARY_PATH_t) updateLdLibPath;
	const char* ldLibPathUtf = (*env)->GetStringUTFChars(env, ldLibraryPath, 0);
	android_update_LD_LIBRARY_PATH(ldLibPathUtf);
	(*env)->ReleaseStringUTFChars(env, ldLibraryPath, ldLibPathUtf);
}


JNIEXPORT jint JNICALL Java_net_kdt_pojavlaunch_utils_JREUtils_chdir(JNIEnv *env, jclass clazz, jstring nameStr) {
	const char *name = (*env)->GetStringUTFChars(env, nameStr, NULL);
	int retval = chdir(name);
	(*env)->ReleaseStringUTFChars(env, nameStr, name);
	return retval;
}

JNIEnv* get_attached_env(JavaVM* jvm) {
    JNIEnv *jvm_env = NULL;
    jint env_result = (*jvm)->GetEnv(jvm, (void**)&jvm_env, JNI_VERSION_1_4);
    if(env_result == JNI_EDETACHED) {
        env_result = (*jvm)->AttachCurrentThreadAsDaemon(jvm, &jvm_env, NULL);
    }
    if(env_result != JNI_OK) {
        printf("get_attached_env failed: %i\n", env_result);
        return NULL;
    }
    return jvm_env;
}
