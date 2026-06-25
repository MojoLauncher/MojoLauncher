cmake_minimum_required(VERSION 3.22.1)

project("dnbglfw")

####################################################
# GLFW
####################################################
set(GLFW_BUILD_EXAMPLES OFF CACHE STRING "" FORCE)
set(GLFW_BUILD_TESTS    OFF CACHE STRING "" FORCE)
set(GLFW_INSTALL        OFF CACHE STRING "" FORCE)
set(BUILD_SHARED_LIBS   ON  CACHE STRING "" FORCE)
set(GLFW_BUILD_X11      OFF CACHE STRING "" FORCE)
set(GLFW_BUILD_WAYLAND  OFF CACHE STRING "" FORCE)
set(GLFW_BUILD_ANDROID  ON  CACHE STRING "" FORCE)

add_subdirectory(glfw)