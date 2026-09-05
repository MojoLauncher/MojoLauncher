# ❄️ CryonixLauncher V2

<p align="center">
  <img src="./app_pojavlauncher/src/main/assets/cryonixlauncher.png" width="150" height="150" alt="CryonixLauncher V2 Logo">
</p>

<h1 align="center">CryonixLauncher V2</h1>

<p align="center">
  <strong>A powerful Minecraft: Java Edition launcher for Android.</strong>
</p>

<p align="center">
  Built on the foundation of PojavLauncher and redesigned for the CryonixLauncher V2 experience.
</p>

<p align="center">

[![Android CI](https://img.shields.io/github/actions/workflow/status/YOUR_USERNAME/CryonixLauncherV2/android.yml?label=Android%20CI\&logo=android)](https://github.com/YOUR_USERNAME/CryonixLauncherV2/actions)

[![GitHub Release](https://img.shields.io/github/v/release/YOUR_USERNAME/CryonixLauncherV2?logo=github)](https://github.com/YOUR_USERNAME/CryonixLauncherV2/releases)

[![License](https://img.shields.io/badge/license-LGPLv3-blue.svg)](LICENSE)

</p>

---

## ✨ About CryonixLauncher V2

**CryonixLauncher V2** is a modern Minecraft: Java Edition launcher designed specifically for Android devices.

It is based on the open-source **PojavLauncher** project and aims to provide a cleaner interface, improved usability, powerful modding support, and a smooth Minecraft Java Edition experience on mobile devices.

CryonixLauncher V2 supports a wide range of Minecraft versions and allows users to install and manage different mod loaders, mods, modpacks, Java runtimes, and game instances.

### 🚀 Features

* 🎮 Minecraft: Java Edition on Android
* ❄️ Modern CryonixLauncher V2 interface
* 📦 Instance-based game management
* 🔧 Forge support
* 🧵 Fabric support
* ⚡ Performance-focused launcher
* ☕ Multiple Java runtime support
* 📁 `.jar` installer support
* 📦 Modpack / `.mrpack` support
* 🎨 Custom controls and UI
* 🖥️ Custom rendering support
* 🔐 Account authentication support
* 🌐 Wide Minecraft version compatibility

---

# 📱 Minecraft Version Support

CryonixLauncher V2 is designed to support a wide range of Minecraft Java Edition versions.

Depending on the installed Java runtime, renderer, and device capabilities, users can run:

* Classic Minecraft versions
* Alpha versions
* Beta versions
* Release versions
* Modern Minecraft versions
* Snapshot versions
* Combat Test versions

> ⚠️ Performance and compatibility may vary depending on your Android device, GPU, renderer, Java version, and Minecraft configuration.

---

# 🧩 Modding Support

CryonixLauncher V2 supports Minecraft modding through popular mod loaders.

### Forge

Run Forge-based Minecraft modpacks and mods directly from your Android device.

### Fabric

Fabric-based Minecraft versions and mods are also supported.

### Mods

CryonixLauncher V2 can work with common Minecraft mod formats such as:

```text
.jar
```

You can install compatible mods into your Minecraft instance's `mods` directory.

---

# 📦 Modpack Support

CryonixLauncher V2 is designed to make modpack installation easier.

Supported formats may include:

```text
.mrpack
.zip
```

Modpacks can be imported into a separate instance, keeping your installations organized.

---

# 🏗️ Building CryonixLauncher V2

## Requirements

Before building the launcher, make sure you have:

* Android Studio
* Android SDK
* Git
* JDK / Java runtime required by the project
* A Windows, Linux, or macOS development environment

## Build

Clone the repository:

```bash
git clone https://github.com/YOUR_USERNAME/CryonixLauncherV2.git
cd CryonixLauncherV2
```

Build the debug version:

### Linux / macOS

```bash
./gradlew :app_pojavlauncher:assembleDebug
```

### Windows

```bat
.\gradlew.bat :app_pojavlauncher:assembleDebug
```

After a successful build, the generated APK can be found inside the project's build output directory.

---

# 🗺️ Roadmap

The CryonixLauncher V2 roadmap is focused on improving performance, stability, UI, and Minecraft compatibility.

* [x] CryonixLauncher V2 rebranding
* [x] Modern launcher interface
* [x] Instance system
* [x] Minecraft version management
* [x] Forge support
* [x] Fabric support
* [x] `.jar` installer support
* [x] Modpack import
* [ ] Advanced mod manager
* [ ] Built-in resource-pack manager
* [ ] Built-in shader manager
* [ ] Improved instance management
* [ ] Advanced Java runtime manager
* [ ] Performance profiles
* [ ] Better controller support
* [ ] Improved renderer configuration
* [ ] Custom CryonixLauncher theme system
* [ ] More Minecraft version compatibility
* [ ] Launcher optimization
* [ ] Improved crash diagnostics

---

# ⚠️ Known Issues

Some issues may occur depending on the Android device and configuration.

Possible issues include:

* Some physical mice may have incorrect or slow mouse movement.
* Certain large texture atlases may appear distorted with some renderers.
* Some mods may not work correctly on Android.
* Performance can vary significantly between devices.
* Certain Minecraft versions may require a specific Java runtime.
* Some modpacks may require additional configuration.

If you encounter a problem, please report it through the project's issue tracker.

---

# 🐛 Bug Reports

Found a bug?

Please create an issue and include as much information as possible.

### Include:

```text
Device:
Android Version:
CryonixLauncher Version:
Minecraft Version:
Java Version:
Renderer:
Mod Loader:
Modpack:
Problem:
Crash Log:
```

Providing logs and reproduction steps makes it much easier to fix problems.

---

# 🤝 Contributing

Contributions are welcome!

You can contribute to CryonixLauncher V2 by:

* 💻 Submitting code
* 🐛 Reporting bugs
* 💡 Suggesting features
* 🎨 Improving the UI
* 🌐 Improving translations
* 📚 Improving documentation
* 🧪 Testing new builds

For code changes, please submit a pull request with a clear description of what was changed and how the changes can be tested.

---

# 📜 License

CryonixLauncher V2 is based on open-source projects and contains components distributed under their respective licenses.

Unless otherwise specified, the launcher follows the licensing requirements of the original components used within the project.

See:

```text
LICENSE
```

for the license applicable to this repository.

---

# 🧱 Third-Party Components

CryonixLauncher V2 uses or is based on various open-source projects and libraries.

### PojavLauncher

Minecraft: Java Edition launcher foundation.

License:

```text
GNU LGPLv3
```

### OpenJDK

Java runtime used for running Minecraft Java Edition.

License:

```text
GNU GPLv2 with Classpath Exception
```

### LWJGL

Lightweight Java Game Library used by Minecraft and related components.

License:

```text
BSD License
```

### GLFW

Window and input management components.

License:

```text
zlib License
```

### Mesa / OpenGL Components

Graphics and rendering components used by supported rendering systems.

License:

```text
MIT / respective upstream licenses
```

### Android Libraries

AndroidX and other Android libraries used by the launcher.

License:

```text
Apache License 2.0
```

> Each third-party component remains subject to its original license. Please refer to the corresponding upstream project for complete license information.

---

# ❤️ Credits

CryonixLauncher V2 would not be possible without the work of the open-source community.

Special thanks to:

* **PojavLauncher contributors**
* **OpenJDK contributors**
* **LWJGL contributors**
* **Mesa contributors**
* **GLFW contributors**
* **Android Open Source Project**
* All developers, testers, translators, and Minecraft modding community members who contribute to the ecosystem.

---

# ❄️ CryonixLauncher V2

<p align="center">
  <strong>Play Minecraft: Java Edition. Anywhere.</strong>
</p>

<p align="center">
  Made with ❄️ for the Minecraft Android community.
</p>