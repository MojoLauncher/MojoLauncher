# 360° joystick movement API

Mojo 360 lets any game-side mod receive the on-screen joystick as an analog vector
instead of W/A/S/D key presses. The API is open: every mod can implement it, and several mods can use it together.

Reference implementation: the `mojo360` Fabric mod (`mojo360-mod/` in this repo).

## How it works

- `libpojavexec` is loaded by both the launcher and the game JVM in one process, so both see the same memory.
- The launcher sends W/A/S/D exactly as before as long as no mod has registered.
- After a mod calls `register`, the joystick sends an `(x, y)` vector instead of keys.
  When the last registered mod unregisters, the joystick switches back to W/A/S/D immediately, no restart needed.

## Java side

Copy this class into your mod **unchanged**: its package, class and method names are the JNI symbol names.
Source: [`AnalogMovement.java`](../mojo360-mod/src/main/java/git/mojo/api/AnalogMovement.java).

```java
package git.mojo.api;

public final class AnalogMovement {
    public static native int  registerAnalogMovement();   // launcher API version, 0 = launcher not visible
    public static native void unregisterAnalogMovement(); // pairs with one successful register
    public static native long pollAnalogMovement();       // packed x/y, see below
}
```

1. Load the library from your mod: `System.loadLibrary("pojavexec")`. If that fails, search `LD_LIBRARY_PATH` for `libpojavexec.so` and load it with `System.load`.
   Loading it again when another mod already loaded it is harmless.
2. Call `registerAnalogMovement()`:
   - `UnsatisfiedLinkError` (no library or no method): not Mojo 360, or an old launcher. Do nothing.
   - `0`: the launcher side is not visible. Do nothing.
   - `> 0`: the handshake succeeded. The value is the launcher's API version.
3. Poll every tick with `pollAnalogMovement()`:
   - high 32 bits: `x` as float bits, right is `+`;
   - low 32 bits: `y` as float bits, forward is `+`;
   - vector length is `0..1`; `(0, 0)` means the joystick is released.
4. Call `unregisterAnalogMovement()` exactly once for each successful register: when the user disables the feature, or on shutdown.

Registrations are counted: the launcher stays in analog mode while at least one mod is registered.
An extra `unregister` never drops the counter below zero.

## Versions

| API | Change |
|-----|--------|
| 1   | First version: `register` / `unregister` / `poll`, refcount |

Any incompatible change bumps the version returned by `register`.
