//
// Created by maks on 21.09.2025.
//

#ifndef POJAVLAUNCHER_LOAD_STAGES_H
#define POJAVLAUNCHER_LOAD_STAGES_H

#include <jni.h>

#define STAGE_LOAD_RUNTIME 0
#define STAGE_CREATE_RUNTIME 1
#define STAGE_FIND_HOOKS_NATIVE 2
#define STAGE_FIND_HOOKS 3
#define STAGE_INSERT_HOOKS 4
#define STAGE_LOAD_CLASSPATH 5
#define STAGE_RUN_MAIN 6

extern void throwException(JNIEnv *env, jint loadStage, jint errorCode, const char* textInfo);

#endif //POJAVLAUNCHER_LOAD_STAGES_H
