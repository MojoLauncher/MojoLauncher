
cmake_minimum_required(VERSION 3.22.1)

project("MojoLauncher")

add_library(mojolaunch SHARED
        affinity.c
        jvm_hooks/emui_iterator_fix_hook.c
        jvm_hooks/java_exec_hooks.c
        jvm_hooks/lwjgl_dlopen_hook.c
        jre_launcher/jre_launcher.c
        jre_launcher/native_library_hook.c
        jre_launcher/elf_hinter.c
        jre_launcher/abort_wait.c
        utils.c
        stdio_is.c
        minibridge.c
        vulkan_loader.c

)

target_include_directories(mojolaunch PRIVATE .)
target_link_libraries(mojolaunch PRIVATE log)

if(CMAKE_ANDROID_ARCH_ABI STREQUAL "arm64-v8a")
    target_sources(mojolaunch PRIVATE
            driver_helper/nsbypass.c
    )
    target_compile_definitions(mojolaunch PRIVATE ADRENO_POSSIBLE)

    add_library(linkerhook SHARED
            driver_helper/hook.c
    )
    target_link_options(linkerhook PRIVATE "-Wl,-z,global")
endif()

add_library(mojolaunch_awt SHARED
        awt_bridge.c
)

add_library(awt_headless SHARED 
	awt_xawt/dummy.c
)

add_library(awt_xawt SHARED
        awt_xawt/xawt_fake.c
)
target_link_libraries(awt_xawt PRIVATE awt_headless)
