#include <jni.h>
#include <android/log.h>
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

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, __FILE_NAME__, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, __FILE_NAME__, __VA_ARGS__)

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
        LOGI("Cannot dlopen nethost: %s", dlerror());
        return 9; // idk random number
    }

    PFNGET_HOSTFXR_PATH get_path = (PFNGET_HOSTFXR_PATH) dlsym(nethost, "get_hostfxr_path");
    if(!get_path) {
        LOGI("Cannot resolve nethost symbol");
        return 10;
    }

    int result = get_path(path_buf, &path_size, &parameters);

    return result;
}

// TODO: decouple from VintageStory
static void dnb_run_v2(void* hostfxr_ptr, const char* dotnetRoot) {
    hostfxr_main_startupinfo_fn main_startupinfo_fn = dlsym(hostfxr_ptr, "hostfxr_main_startupinfo");
    //setenv("MONO_ENV_OPTIONS", "--trace=T:Vintagestory.Client.NoObf.ShaderProgramFinal", true);
    //setenv("MONO_ENV_OPTIONS", "--interpreter", true);
    char hostPath[PATH_MAX];
    snprintf(hostPath, PATH_MAX, "%s/%s", dotnetRoot, "dotnet");
    char appPath[PATH_MAX];
    snprintf(appPath, PATH_MAX, "%s/%s", dotnetRoot, "dotnet.dll");

    const char* argv[] = { hostPath, "Vintagestory.dll" };
    const int argc = 2;

    LOGI("dotnetRoot: %s appPath: %s", dotnetRoot, appPath);
    pthread_setname_np(pthread_self(), "dnb main thread");
    int rc = main_startupinfo_fn(argc, argv, hostPath, dotnetRoot, appPath);
    LOGI("hostfxr done: %x", rc);
}

JNIEXPORT void JNICALL
Java_git_artdeell_mojo_runtime_dotnet_DotnetStarter_runDotnet(JNIEnv *env, jobject thiz,
                                                     jstring dotnetRoot, jstring vsDir) {
    char_t path_buf[PATH_MAX];
    const char* dotnet_root = (*env)->GetStringUTFChars(env, dotnetRoot, NULL);
    uint32_t result = dnb_find_hostfxr(dotnet_root, path_buf);

    if(result != 0) {
        LOGI("Cannot find hostfxr: %x", result);
    }

    void* hostfxr_ptr = dlopen(path_buf, RTLD_NOW);

    const char* vs_dir = (*env)->GetStringUTFChars(env, vsDir, NULL);
    chdir(vs_dir);
    (*env)->ReleaseStringUTFChars(env, vsDir, vs_dir);

    dnb_run_v2(hostfxr_ptr, dotnet_root);
}