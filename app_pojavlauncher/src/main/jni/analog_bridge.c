//
// Мост 360° движения: лаунчер (Dalvik) <-> мод (игровая JVM).
// libpojavexec грузят обе VM в одном процессе -> статики ниже общие.
// Нет мода -> счётчик 0 -> джойстик шлёт WASD как раньше.
// Публичный API для любых модов: класс git.mojo.api.AnalogMovement, см. docs/analog-movement.md
//

#include <jni.h>
#include <stdatomic.h>
#include <stdbool.h>
#include <stdint.h>
#include <string.h>

// поднимать при любом изменении API для мода
#define ANALOG_API_VERSION 1

static atomic_bool launcher_seen = false;  // ставит minibridgeInit (сторона Dalvik)
static atomic_int analog_users = 0;       // сколько модов сейчас включили аналог
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
    return atomic_load(&analog_users) > 0;
}

// x: вправо +, y: вперёд +, длина 0..1
JNIEXPORT void JNICALL
Java_net_kdt_pojavlaunch_CallbackBridge_sendAnalogMovement(JNIEnv *env, jclass clazz, jfloat x, jfloat y) {
    atomic_store(&analog_xy, pack_xy(x, y));
}

// ---- сторона игры (git.mojo.api.AnalogMovement, копия класса в каждом моде) ----

// вернёт версию API; 0 = лаунчер не виден (счётчик тогда не трогаем)
// каждый register парный с одним unregister
JNIEXPORT jint JNICALL
Java_git_mojo_api_AnalogMovement_registerAnalogMovement(JNIEnv *env, jclass clazz) {
    if(!atomic_load(&launcher_seen)) return 0;
    atomic_fetch_add(&analog_users, 1);
    return ANALOG_API_VERSION;
}

// последний ушёл -> обнуляем вектор, лаунчер вернётся к WASD
JNIEXPORT void JNICALL
Java_git_mojo_api_AnalogMovement_unregisterAnalogMovement(JNIEnv *env, jclass clazz) {
    int users = atomic_load(&analog_users);
    // не уходим в минус при лишнем unregister
    while(users > 0 && !atomic_compare_exchange_weak(&analog_users, &users, users - 1));
    if(users == 1) atomic_store(&analog_xy, 0);
}

// x,y упакованы как в pack_xy, распаковка на стороне Java
JNIEXPORT jlong JNICALL
Java_git_mojo_api_AnalogMovement_pollAnalogMovement(JNIEnv *env, jclass clazz) {
    return (jlong) atomic_load(&analog_xy);
}
