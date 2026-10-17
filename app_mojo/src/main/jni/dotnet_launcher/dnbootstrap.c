#include <jni.h>
#include <linux/limits.h>
#include <dlfcn.h>
#include <unistd.h>
#include <stdio.h>
#include <pthread.h>
#include <stdbool.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <errno.h>
#include "include/hostfxr.h"
#include <android/looper.h>
#include "include/libnethost.h"
#include "log.h"
#include "utils.h"

//
// Created by maks on 14.12.2025.
//

typedef int (*PFNGET_HOSTFXR_PATH)(
        char_t * buffer,
        size_t * buffer_size,
        const struct get_hostfxr_parameters *parameters);


static int dnb_find_hostfxr(const char* dotnet_root, char_t path_buf[PATH_MAX]) {
    struct get_hostfxr_parameters parameters = {
            .assembly_path = NULL,
            .dotnet_root = dotnet_root
    };
    parameters.size = sizeof(parameters);

    size_t path_size = sizeof(char_t[PATH_MAX]) / sizeof(char_t);

    void* nethost = dlopen("libnethost.so", RTLD_NOW | RTLD_LOCAL);
    if(!nethost) {
        LOGE("Cannot dlopen nethost: %s", dlerror());
        return 9; // idk random number
    }

    PFNGET_HOSTFXR_PATH get_path = (PFNGET_HOSTFXR_PATH) dlsym(nethost, "get_hostfxr_path");
    if(!get_path) {
        LOGE("Cannot resolve nethost symbol");
        return 10;
    }

    int result = get_path(path_buf, &path_size, &parameters);

    return result;
}

static void dnb_run_v2(void* hostfxr_ptr, const char* dotnetRoot, heap_str_array* args) {
    hostfxr_main_startupinfo_fn main_startupinfo_fn = dlsym(hostfxr_ptr, "hostfxr_main_startupinfo");
    char appPath[PATH_MAX];
    snprintf(appPath, PATH_MAX, "%s/%s", dotnetRoot, "dotnet.dll");

    LOGI("dotnetRoot: %s appPath: %s", dotnetRoot, appPath);
    pthread_setname_np(pthread_self(), ".NET Main thread");
    int rc = main_startupinfo_fn(args->length, args->strings, args->strings[0], dotnetRoot, appPath);
    LOGI("hostfxr done: %x", rc);
}

JNIEXPORT void JNICALL
Java_git_artdeell_mojo_runtime_dotnet_DotnetStarter_nrunDotnet(JNIEnv *env, jobject thiz,
                                                     jstring dotnetRoot, jstring rootDir, jobjectArray args) {
    char_t path_buf[PATH_MAX];
    const char* dotnet_root = (*env)->GetStringUTFChars(env, dotnetRoot, NULL);
    uint32_t result = dnb_find_hostfxr(dotnet_root, path_buf);

    if(result != 0) {
        LOGE("Cannot find hostfxr: %x", result);
        return;
    }

    void* hostfxr_ptr = dlopen(path_buf, RTLD_NOW);

    heap_str_array* hargs = hstr_from_jni(env, args);

    const char* root_dir = (*env)->GetStringUTFChars(env, rootDir, NULL);
    chdir(root_dir);
    (*env)->ReleaseStringUTFChars(env, rootDir, root_dir);

    dnb_run_v2(hostfxr_ptr, dotnet_root, hargs);
    hstr_free(hargs);
}