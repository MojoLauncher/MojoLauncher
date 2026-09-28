//
// Мост 360° движения: лаунчер (Dalvik) <-> мод (игровая JVM).
// libpojavexec грузят обе VM в одном процессе -> статики ниже общие.
// Нет мода -> флаг false -> джойстик шлёт WASD как раньше.
//

#include <jni.h>
#include <stdatomic.h>
#include <stdbool.h>
#include <stdint.h>
#include <string.h>

// поднимать при любом изменении API для мода
#define ANALOG_API_VERSION 1

static atomic_bool launcher_seen = false;  // ставит minibridgeInit (сторона Dalvik)
static atomic_bool analog_enabled = false; // ставит/снимает мод
static _Atomic uint64_t analog_xy = 0;     // x,y float в одном слове -> читаем целиком

static uint64_t pack_xy(float x, float y) {
    uint32_t xb, yb;
    memcpy(&xb, &x, sizeof(xb));
    memcpy(&yb, &y, sizeof(yb));
    return ((uint64_t) xb << 32) | yb;
}

// из minibridgeInit: мод по этому поймёт, что видит тот же экземпляр либы
void analog_bridge_mark_launcher() {
    atomic_store(&launcher_seen, true);
}

// ---- сторона лаунчера (net.kdt.pojavlaunch.CallbackBridge) ----

JNIEXPORT jboolean JNICALL
Java_net_kdt_pojavlaunch_CallbackBridge_isAnalogMovement(JNIEnv *env, jclass clazz) {
    return atomic_load(&analog_enabled);
}

// x: вправо +, y: вперёд +, длина 0..1
JNIEXPORT void JNICALL
Java_net_kdt_pojavlaunch_CallbackBridge_sendAnalogMovement(JNIEnv *env, jclass clazz, jfloat x, jfloat y) {
    atomic_store(&analog_xy, pack_xy(x, y));
}

// ---- сторона игры (ru.evga314.mojo360.bridge.AnalogBridge в моде) ----

// вернёт версию API; 0 = лаунчер не виден (флаг тогда не ставим)
JNIEXPORT jint JNICALL
Java_ru_evga314_mojo360_bridge_AnalogBridge_registerAnalogMovement(JNIEnv *env, jclass clazz) {
    if(!atomic_load(&launcher_seen)) return 0;
    atomic_store(&analog_xy, 0);
    atomic_store(&analog_enabled, true);
    return ANALOG_API_VERSION;
}

JNIEXPORT void JNICALL
Java_ru_evga314_mojo360_bridge_AnalogBridge_unregisterAnalogMovement(JNIEnv *env, jclass clazz) {
    atomic_store(&analog_enabled, false);
    atomic_store(&analog_xy, 0);
}

// x,y упакованы как в pack_xy, распаковка на стороне Java
JNIEXPORT jlong JNICALL
Java_ru_evga314_mojo360_bridge_AnalogBridge_pollAnalogMovement(JNIEnv *env, jclass clazz) {
    return (jlong) atomic_load(&analog_xy);
}
