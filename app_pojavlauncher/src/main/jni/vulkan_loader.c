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
#include <pojavexec.h>

static bool driver_loaded = false;

#ifdef ENABLE_TURNIP_LOADER
void load_turnip_vulkan(const char* custom_path) {
    if(driver_loaded || !custom_path) return;
    const char* cache_dir = getenv("TMPDIR");
    if(!linker_ns_load(pojavexec_getNativeDirectory())) return;
    void* linkerhook = linker_ns_dlopen("liblinkerhook.so", RTLD_LOCAL | RTLD_NOW);
    if(linkerhook == NULL) return;
    printf("DriverHook: opening vulkan library at %s...\n", custom_path);
    void* turnip_driver_handle = linker_ns_dlopen(custom_path, RTLD_LOCAL | RTLD_NOW);
    if(turnip_driver_handle == NULL) {
        printf("DriverHook: Failed to load custom Vulkan library!\n%s\n", dlerror());
        goto fail_l;
    }

    void* dl_android = linker_ns_dlopen("libdl_android.so", RTLD_LOCAL | RTLD_LAZY);
    if(dl_android == NULL) goto fail_t;

    void* android_get_exported_namespace = dlsym(dl_android, "android_get_exported_namespace");
    void (*linkerhook_pass_handles)(void*, void*, void*) = dlsym(linkerhook, "app__pojav_linkerhook_pass_handles");

    if(linkerhook_pass_handles == NULL || android_get_exported_namespace == NULL) goto fail_d;
    linkerhook_pass_handles(turnip_driver_handle, android_dlopen_ext, android_get_exported_namespace);

    void* libvulkan = linker_ns_dlopen_unique(cache_dir, "libvulkan.so", "libmjlvlk.so", RTLD_LOCAL | RTLD_NOW);
    printf("DriverHook: Loaded mjlvlk, ptr=%p\n", libvulkan);
    if(libvulkan) {
        driver_loaded = true;
    }
    fail_d: dlclose(dl_android);
    fail_t: dlclose(turnip_driver_handle);
    fail_l: dlclose(linkerhook);
}
#endif

void* pojavexec_loadVulkanDriver(const char* custom_path) {
#ifdef ENABLE_TURNIP_LOADER
    if(android_get_device_api_level() >= 28) { // the loader does not support below that
        if(custom_path) load_turnip_vulkan(custom_path);
        if(driver_loaded)
            // Reference the vulkan driver separately to avoid weirdness from libraries calling dlclose
            return linker_ns_dlopen("libmjlvlk.so", RTLD_LOCAL);
    }
#endif
    void* vulkan_ptr = dlopen("libvulkan.so", RTLD_LAZY | RTLD_LOCAL);
    printf("VulkanLoader: loaded system vulkan, ptr=%p\n", vulkan_ptr);
    return vulkan_ptr;
}

JNIEXPORT void JNICALL
Java_net_kdt_pojavlaunch_utils_JREUtils_loadVulkanLibrary(JNIEnv *env, jclass clazz,
                                                          jstring absolute_path) {
    if(absolute_path == NULL) return;
    const char* _absolute_path = (*env)->GetStringUTFChars(env, absolute_path, NULL);
    pojavexec_loadVulkanDriver(_absolute_path);
    (*env)->ReleaseStringUTFChars(env, absolute_path, _absolute_path);
}