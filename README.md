<H1 align="center">Hyper Launcher</H1>

<p align="center">A modern Minecraft: Java Edition launcher for Android, forked from MojoLauncher.</p>

<img src="https://yt3.googleusercontent.com/RC9iOqHVK1Q6Cun4MsxPt1D0TNWVM-8dPgdlCekFq7werQ3Uxm7H0VUz4yqho1-zGBn4-JfU=s160-c-k-c0x00ffffff-no-rj" align="left" width="150" height="150" alt="Hyper Launcher logo">

[![Android CI](https://github.com/HyperLauncher/HyperLauncher/workflows/Android%20CI/badge.svg)](https://github.com/HyperLauncher/HyperLauncher/actions)
[![GitHub commit activity](https://img.shields.io/github/commit-activity/m/HyperLauncher/HyperLauncher)](https://github.com/HyperLauncher/HyperLauncher)
[![Crowdin](https://badges.crowdin.net/pojavlauncher/localized.svg)](https://crowdin.com/project/pojavlauncher)
[![Discord](https://img.shields.io/discord/1365346109131722753.svg?label=&logo=discord&logoColor=ffffff&color=7389D8&labelColor=6A7EC2)](https://discord.gg/VHdwQFsaGX)

---

## Navigation
- [About](#about)
- [Features](#features)
- [Supported Mod Loaders](#supported-mod-loaders)
- [Getting Hyper Launcher](#getting-hyper-launcher)
- [Building](#building)
- [Current Roadmap](#current-roadmap)
- [Known Issues](#known-issues)
- [License](#license)
- [Contributing](#contributing)
- [Credits & Third Party Components](#credits--third-party-components-and-their-licenses-if-available)

---

## About

Hyper Launcher is a fast and customizable Minecraft: Java Edition launcher for Android, forked from [MojoLauncher](https://github.com/MojoLauncher/MojoLauncher) and ultimately based on [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher).

It can run almost every version of Minecraft — from `rd-132211` to the latest snapshots (including Combat Test versions) — and supports modloaders such as [Forge](https://files.minecraftforge.net/), [Fabric](http://fabricmc.net/), NeoForge, and Quilt, as well as mods like [OptiFine](https://optifine.net).

Hyper Launcher is designed for:
- Smooth Minecraft Java gameplay on Android
- Modern Material UI 3 
- Better modpack handling
- Cleaner launcher experience
- Experimental performance improvements
- Advanced customization for power users

---

## Features

- Launch Minecraft versions from `rd-132211` to latest snapshots
- Fabric, Forge, NeoForge and Quilt support
- Modpack import support (`.mrpack` / CurseForge zip)
- Instance management system
- Custom Java runtime support
- Integrated account management
- Renderer selection support
- Performance tuning options
- Mouse & keyboard support
- Built-in modding tools

---

## Supported Mod Loaders

- Fabric
- Forge
- NeoForge
- Quilt
- LiteLoader *(experimental)*

---

## Getting Hyper Launcher

You can get Hyper Launcher via three methods:

1. **Automatic Builds** — Get development builds from [GitHub Actions](https://github.com/HyperLauncher/HyperLauncher/actions).

2. **Google Play** — Coming soon.

3. **Build from Source** — See [Building](#building) below.

---

## Building

Build the launcher (it will automatically download all required components):

```bash
./gradlew :app_pojavlauncher:assembleDebug
```

> Replace `./gradlew` with `.\gradlew.bat` if you are building on Windows.

---

## Current Roadmap

- [x] Instance system in favor of profiles
- [x] Out-of-the-box 1.21.5 support
- [x] mrpack/CurseForge zip import
- [ ] Modern Material 3 expressive interface
- [ ] Mod manager
- [ ] Better controller support
- [ ] Enhanced renderer compatibility
- [ ] Download manager improvements
- [ ] LTW: resolve issues with Create
- [ ] LTW: enable compute shader/image extensions
- [ ] LTW: switch to a color-renderable format for framebuffers
- [ ] MMC-compatible instance import
- [ ] Patch-on-dlopen for mod native libraries
- [ ] Replace Holy-GL4ES 1.1.5 with KW (maybe? need to figure out requirements)

---

## Known Issues

- Some physical mice may have very slow mouse speed
- On Holy GL4ES, large texture atlases may be distorted (resulting in stretched/blocky textures in modpacks)
- Probably more — that's why we have a bug tracker 😉

---

## License

Hyper Launcher is licensed under [GNU LGPLv3](LICENSE).

---

## Contributing

Contributions are welcome! We welcome any type of contribution, not only code. For example, you can help the wiki shape up or contribute to the [translation](https://crowdin.com/project/pojavlauncher).

Any code change should be submitted as a pull request. The description should explain what the code does and give steps to execute it.

---

## Credits & Third Party Components and Their Licenses (if available)

Hyper Launcher is a fork of [MojoLauncher](https://github.com/MojoLauncher/MojoLauncher), which is itself based on [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher). Full credit goes to both teams and all upstream contributors.

- [MojoLauncher](https://github.com/MojoLauncher/MojoLauncher): [GNU LGPLv3 License](https://github.com/MojoLauncher/MojoLauncher/blob/v3_openjdk/LICENSE)
- [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher): [GNU LGPLv3 License](https://github.com/PojavLauncherTeam/PojavLauncher/blob/v3_openjdk/LICENSE)
- [Boardwalk](https://github.com/zhuowei/Boardwalk) (JVM Launcher): Unknown License / [Apache License 2.0](https://github.com/zhuowei/Boardwalk/blob/master/LICENSE) or GNU GPLv2
- Android Support Libraries: [Apache License 2.0](https://android.googlesource.com/platform/prebuilts/maven_repo/android/+/master/NOTICE.txt)
- [GL4ES](https://github.com/PojavLauncherTeam/gl4es): [MIT License](https://github.com/ptitSeb/gl4es/blob/master/LICENSE)
- [OpenJDK](https://github.com/PojavLauncherTeam/openjdk-multiarch-jdk8u): [GNU GPLv2 License](https://openjdk.java.net/legal/gplv2+ce.html)
- [LWJGL3](https://github.com/MojoLauncher/lwjgl3): [BSD-3 License](https://github.com/LWJGL/lwjgl3/blob/master/LICENSE.md)
- [Mesa 3D Graphics Library](https://gitlab.freedesktop.org/mesa/mesa): [MIT License](https://docs.mesa3d.org/license.html)
- [pro-grade](https://github.com/pro-grade/pro-grade) (Java sandboxing security manager): [Apache License 2.0](https://github.com/pro-grade/pro-grade/blob/master/LICENSE.txt)
- [bhook](https://github.com/bytedance/bhook) (Used for exit code trapping): [MIT License](https://github.com/bytedance/bhook/blob/main/LICENSE)
- [Authlib-Injector](https://github.com/yushijinhun/authlib-injector) (Used for authorisation via ely.by): [AGPL-3.0](https://github.com/yushijinhun/authlib-injector/blob/develop/LICENSE)
- [alsoft](https://github.com/kcat/openal-soft/) (Audio output library): [GNU LGPL](https://github.com/kcat/openal-soft/blob/master/COPYING) and [modified PFFFT](https://github.com/kcat/openal-soft/blob/master/LICENSE-pffft)
- [oboe](https://github.com/google/oboe): [Apache License 2.0](https://github.com/google/oboe/blob/main/LICENSE)
- Thanks to [Mineskin](https://mineskin.eu/) for providing Minecraft avatars.
