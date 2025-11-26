//
// Created by maks on 24.09.2022.
//

#include "environ.h"

__thread bool glfw_main_thread = false;
static struct pojav_environ_s pojav_environ_storage;

struct pojav_environ_s *pojav_environ = &pojav_environ_storage;