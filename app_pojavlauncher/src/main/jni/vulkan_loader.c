//
// Created by maks on 10.04.2026.
//

#include <android/api-level.h>
#include <stdio.h>
#include <dlfcn.h>
#include <stdlib.h>
#include <jni.h>

#define TAG __FILE_NAME__
#include <log.h>

#include <driver_helper/nsbypass.h>
#include <android/dlext.h>

void* load_turnip_vulkan() {
    //if(getenv("POJAV_LOAD_TURNIP") == NULL) return NULL;
    const char* native_dir = getenv("POJAV_NATIVEDIR");
    const char* cache_dir = getenv("TMPDIR");
    if(!linker_ns_load(native_dir)) return NULL;
    void* linkerhook = linker_ns_dlopen("liblinkerhook.so", RTLD_LOCAL | RTLD_NOW);
    if(linkerhook == NULL) return NULL;
    void* turnip_driver_handle = linker_ns_dlopen("libvulkan_freedreno.so", RTLD_LOCAL | RTLD_NOW);
    if(turnip_driver_handle == NULL) {
        printf("DriverHook: Failed to load Turnip!\n%s\n", dlerror());
        dlclose(linkerhook);
        return NULL;
    }
    void* dl_android = linker_ns_dlopen("libdl_android.so", RTLD_LOCAL | RTLD_LAZY);
    if(dl_android == NULL) {
        dlclose(linkerhook);
        dlclose(turnip_driver_handle);
        return NULL;
    }
    void* android_get_exported_namespace = dlsym(dl_android, "android_get_exported_namespace");
    void (*linkerhook_pass_handles)(void*, void*, void*) = dlsym(linkerhook, "app__pojav_linkerhook_pass_handles");
    if(linkerhook_pass_handles == NULL || android_get_exported_namespace == NULL) {
        dlclose(dl_android);
        dlclose(linkerhook);
        dlclose(turnip_driver_handle);
        return NULL;
    }
    linkerhook_pass_handles(turnip_driver_handle, android_dlopen_ext, android_get_exported_namespace);
    void* libvulkan = linker_ns_dlopen_unique(cache_dir, "libvulkan.so", "libmjlvlk.so", RTLD_LOCAL | RTLD_NOW);
    return libvulkan;
}

static void* load_vulkan() {
    if(android_get_device_api_level() >= 28) { // the loader does not support below that
        void* result = load_turnip_vulkan();
        if(result != NULL) {
            printf("DriverHook: Loaded Turnip, ptr=%p\n", result);
            return result;
        }
    }
    void* vulkan_ptr = dlopen("libvulkan.so", RTLD_LAZY | RTLD_LOCAL);
    printf("VulkanLoader: loaded system vulkan, ptr=%p\n", vulkan_ptr);
    return vulkan_ptr;
}

void* pojavexec_loadVulkanDriver() {
    static void* handle = NULL;
    if(handle != NULL) return handle;
    handle = load_vulkan();
    return handle;
}

JNIEXPORT void JNICALL
Java_net_kdt_pojavlaunch_utils_JREUtils_preloadVulkan(JNIEnv *env, jclass clazz) {
    void* handle = pojavexec_loadVulkanDriver();
    if(!handle) {
        printf("VulkanLoader: failed to preload Vulkan driver!\n");
    }
}