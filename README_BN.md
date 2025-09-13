<H1 align="center">MojoLauncher</H1>

<a href="https://github.com/MojoLauncher/MojoLauncher/blob/v3_openjdk/README_RU.md">Readme বাংলা ভাষা</a>

<img src="https://github.com/MojoLauncher/MojoLauncher/blob/v3_openjdk/app_pojavlauncher/src/main/assets/pojavlauncher.png" align="left" width="150" height="150" alt="MojoLauncher logo">

[![Android CI](https://github.com/MojoLauncher/MojoLauncher/workflows/Android%20CI/badge.svg)](https://github.com/MojoLauncher/MojoLauncher/actions)  
[![GitHub commit activity](https://img.shields.io/github/commit-activity/m/MojoLauncher/MojoLauncher)](https://github.com/MojoLauncher/MojoLauncher/actions)  
[![Crowdin](https://badges.crowdin.net/pojavlauncher/localized.svg)](https://crowdin.com/project/pojavlauncher)  
[![Discord](https://img.shields.io/discord/1365346109131722753.svg?label=&logo=discord&logoColor=ffffff&color=7389D8&labelColor=6A7EC2)](https://discord.gg/VHdwQFsaGX)  

* **MojoLauncher** হলো একটি লঞ্চার, যা [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher)-এর উপর ভিত্তি করে তৈরি, এবং এটি আপনাকে আপনার Android ডিভাইসে **Minecraft: Java Edition** খেলতে সাহায্য করে!  

* এটি প্রায় সব সংস্করণ চালাতে পারে, এছাড়াও `.jar`-only ইনস্টলার ব্যবহার করে [Forge](https://files.minecraftforge.net/), [Fabric](http://fabricmc.net/) এবং [OptiFine](https://optifine.net)-এর মতো মডলোডার ও মড ইনস্টল করা যায়।  

---

## নেভিগেশন
- [পরিচিতি](#পরিচিতি)  
- [MojoLauncher পাওয়া কেমন ভাবে যাবে ?](#mojolauncher-পাওয়া)  
- [বিল্ড করা](#বিল্ড-করা)  
- [বর্তমান রোডম্যাপ](#বর্তমান-রোডম্যাপ)  
- [লাইসেন্স](#লাইসেন্স)  
- [কন্ট্রিবিউশন](#কন্ট্রিবিউশন)  
- [ক্রেডিটস ও থার্ড পার্টি কম্পোনেন্ট](#ক্রেডিটস--থার্ড-পার্টি-কম্পোনেন্ট-এবং-তাদের-লাইসেন্স)  

---

## পরিচিতি
* **MojoLauncher** হলো Android-এর জন্য Minecraft: Java Edition লঞ্চার যা [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher)-এর উপর ভিত্তি করে তৈরি।  
* এটি প্রায় সব Minecraft সংস্করণ চালাতে পারে — **rd-132211** থেকে শুরু করে **1.21 snapshots** (Combat Test ভার্সন সহ)।  
* Forge ও Fabric দিয়ে মড ব্যবহার করার সুবিধাও রয়েছে।  

---

## MojoLauncher পাওয়া  

MojoLauncher পাওয়ার ৩টি উপায় আছে:

1. [স্বয়ংক্রিয় বিল্ড](https://github.com/MojoLauncher/MojoLauncher/actions) থেকে প্রস্তুত অ্যাপ ডাউনলোড করতে পারেন।  

2. Google Play থেকে পেতে নিচের ব্যাজে ক্লিক করুন:  
[![Google Play](https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png)](https://play.google.com/store/apps/details?id=git.artdeell.mojo)  

3. অথবা [সোর্স থেকে বিল্ড](#বিল্ড-করা) করতে পারেন।  

---

## বিল্ড করা  

লঞ্চার বিল্ড করতে (প্রয়োজনীয় কম্পোনেন্টগুলো স্বয়ংক্রিয়ভাবে ডাউনলোড হবে):
```
./gradlew :app_pojavlauncher:assembleDebug
```
(Windows-এ হলে `.\gradlew.bat` ব্যবহার করুন)।  

---

## বর্তমান রোডম্যাপ
- [x] Instance system in favor of profiles
- [x] Out-of-the box 1.21.5 support
- [ ] LTW: resolve issues with Create
- [ ] LTW: enable compute shader/image extensions
- [ ] LTW: switch to a color-renderable format for framebuffers
- [ ] Modpack/mod management tool
- [ ] mrpack/CurseForge zip import
- [ ] MMC-compatible instance import
- [ ] Patch-on-dlopen for mod native libraries
- [ ] Replace Holy-GL4ES 1.1.5 with KW (maybe? need to figure out requirements)
---

## পরিচিত সমস্যা
- কিছু ফিজিক্যাল মাউস খুব ধীর গতির হতে পারে  
- Holy GL4ES-এ বড় টেক্সচার অ্যাটলাস বিকৃত হতে পারে (মডপ্যাকে ব্লকি/স্ট্রেচড টেক্সচার দেখা যাবে)  
- আরও থাকতে পারে, এজন্যই আমাদের বাগ ট্র্যাকার আছে ;)  

---

## লাইসেন্স
- MojoLauncher [GNU LGPLv3](https://github.com/MojoLauncher/MojoLauncher/blob/v3_openjdk/LICENSE) লাইসেন্সে প্রকাশিত।  

---

## কন্ট্রিবিউশন
ভালো ছেলেদের স্বাগত! শুধু কোড নয়, অন্যান্য অবদানও গুরুত্বপূর্ণ।  
উইকি তৈরি বা [অনুবাদ](https://crowdin.com/project/pojavlauncher)-এ সাহায্য করতে পারেন।  

যে কোনো কোড পরিবর্তন Pull Request আকারে জমা দিতে হবে।  
বর্ণনায় কোড কী করে এবং চালানোর ধাপ উল্লেখ করতে হবে।  

---

## ক্রেডিটস & থার্ড পার্টি কম্পোনেন্ট এবং তাদের লাইসেন্স
- [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher): [GNU LGPLv3 License](https://github.com/PojavLauncherTeam/PojavLauncher/blob/v3_openjdk/LICENSE)  
- [Boardwalk](https://github.com/zhuowei/Boardwalk): Apache 2.0 / GNU GPLv2  
- Android Support Libraries: [Apache License 2.0](https://android.googlesource.com/platform/prebuilts/maven_repo/android/+/master/NOTICE.txt)  
- [GL4ES](https://github.com/PojavLauncherTeam/gl4es): [MIT License](https://github.com/ptitSeb/gl4es/blob/master/LICENSE)  
- [OpenJDK](https://github.com/PojavLauncherTeam/openjdk-multiarch-jdk8u): [GNU GPLv2 License](https://openjdk.java.net/legal/gplv2+ce.html)  
- [LWJGL3](https://github.com/MojoLauncher/lwjgl3): [BSD-3 License](https://github.com/LWJGL/lwjgl3/blob/master/LICENSE.md)  
- [Mesa 3D Graphics Library](https://gitlab.freedesktop.org/mesa/mesa): [MIT License](https://docs.mesa3d.org/license.html)  
- [pro-grade](https://github.com/pro-grade/pro-grade): [Apache License 2.0](https://github.com/pro-grade/pro-grade/blob/master/LICENSE.txt)  
- [bhook](https://github.com/bytedance/bhook): [MIT License](https://github.com/bytedance/bhook/blob/main/LICENSE)  
- [Authlib-Injector](https://github.com/yushijinhun/authlib-injector): [AGPL-3.0](https://github.com/yushijinhun/authlib-injector/blob/develop/LICENSE)  
- [alsoft](https://github.com/kcat/openal-soft/): [GNU LGPL] + [modified PFFFT license]  
- [oboe](https://github.com/google/oboe): [Apache License 2.0](https://github.com/google/oboe/blob/main/LICENSE)  
- ধন্যবাদ [Mineskin](https://mineskin.eu/) কে Minecraft avatars সরবরাহ করার জন্য।
