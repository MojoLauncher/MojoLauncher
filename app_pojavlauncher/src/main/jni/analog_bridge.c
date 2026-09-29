//
// 360° movement bridge: launcher (Dalvik) <-> mod (game JVM).
// Both VMs load libpojavexec in one process -> the statics below are shared.
// No mod -> counter is 0 -> the joystick sends WASD as before.
// Public API for any mod: class git.mojo.api.AnalogMovement, see docs/analog-movement.md
//

#include <jni.h>
#include <stdatomic.h>
#include <stdbool.h>
#include <stdint.h>
#include <string.h>

// bump on any change of the mod-facing API
#define ANALOG_API_VERSION 1

static atomic_bool launcher_seen = false;  // set by minibridgeInit (Dalvik side)
static atomic_int analog_users = 0;       // how many mods currently enabled analog mode
static _Atomic uint64_t analog_xy = 0;     // x,y floats in one word -> read atomically

static uint64_t pack_xy(float x, float y) {
    uint32_t xb, yb;
    memcpy(&xb, &x, sizeof(xb));
    memcpy(&yb, &y, sizeof(yb));
    return ((uint64_t) xb << 32) | yb;
}

// from minibridgeInit: lets the mod verify it sees the same library instance
void analog_bridge_mark_launcher(void) {
    atomic_store(&launcher_seen, true);
}

// ---- launcher side (net.kdt.pojavlaunch.CallbackBridge) ----

JNIEXPORT jboolean JNICALL
Java_net_kdt_pojavlaunch_CallbackBridge_isAnalogMovement(JNIEnv *env, jclass clazz) {
    return atomic_load(&analog_users) > 0;
}

// x: right +, y: forward +, length 0..1
JNIEXPORT void JNICALL
Java_net_kdt_pojavlaunch_CallbackBridge_sendAnalogMovement(JNIEnv *env, jclass clazz, jfloat x, jfloat y) {
    atomic_store(&analog_xy, pack_xy(x, y));
}

// ---- game side (git.mojo.api.AnalogMovement, each mod ships a copy of the class) ----

// returns the API version; 0 = launcher not visible (counter untouched)
// every successful register pairs with one unregister
JNIEXPORT jint JNICALL
Java_git_mojo_api_AnalogMovement_registerAnalogMovement(JNIEnv *env, jclass clazz) {
    if(!atomic_load(&launcher_seen)) return 0;
    atomic_fetch_add(&analog_users, 1);
    return ANALOG_API_VERSION;
}

// last mod gone -> clear the vector; the joystick falls back to WASD on its next event
JNIEXPORT void JNICALL
Java_git_mojo_api_AnalogMovement_unregisterAnalogMovement(JNIEnv *env, jclass clazz) {
    int users = atomic_load(&analog_users);
    // never go below zero on an extra unregister
    while(users > 0 && !atomic_compare_exchange_weak(&analog_users, &users, users - 1));
    if(users == 1) atomic_store(&analog_xy, 0);
}

// x,y packed as in pack_xy, unpacked on the Java side
JNIEXPORT jlong JNICALL
Java_git_mojo_api_AnalogMovement_pollAnalogMovement(JNIEnv *env, jclass clazz) {
    return (jlong) atomic_load(&analog_xy);
}
