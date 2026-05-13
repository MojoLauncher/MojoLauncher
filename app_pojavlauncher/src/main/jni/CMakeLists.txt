
cmake_minimum_required(VERSION 3.22.1)

project("MojoLauncher")

add_library(pojavexec SHARED
        affinity.c
        jvm_hooks/emui_iterator_fix_hook.c
        jvm_hooks/java_exec_hooks.c
        jvm_hooks/lwjgl_dlopen_hook.c
        jre_launcher/jre_launcher.c
        jre_launcher/native_library_hook.c
        jre_launcher/elf_hinter.c
        jre_launcher/abort_wait.c
        driver_helper/nsbypass.c
        driver_helper/fake_dlfcn.c
        driver_helper/func_locator.c
        utils.c
        stdio_is.c
        minibridge.c
        vulkan_loader.c

)

target_include_directories(pojavexec PRIVATE .)
target_link_libraries(pojavexec PRIVATE log)
if(CMAKE_ANDROID_ARCH_ABI STREQUAL "arm64-v8a")
    target_sources(pojavexec PRIVATE driver_helper/arm64_func_locator.c)
    target_compile_definitions(pojavexec PRIVATE USE_ARM64_LOCATOR)
    target_compile_definitions(pojavexec PRIVATE ENABLE_TURNIP_LOADER)

    add_library(linkerhook SHARED
            driver_helper/hook.c
    )
    target_link_options(linkerhook PRIVATE "-Wl,-z,global")
endif()

add_library(pojavexec_awt SHARED
        awt_bridge.c
)

add_library(awt_headless SHARED 
	awt_xawt/dummy.c
)

add_library(awt_xawt SHARED
        awt_xawt/xawt_fake.c
)
target_link_libraries(awt_xawt PRIVATE awt_headless)