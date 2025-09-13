<H1 align="center">MojoLauncher</H1>

<img src="https://github.com/MojoLauncher/MojoLauncher/blob/v3_openjdk/app_pojavlauncher/src/main/assets/pojavlauncher.png" align="left" width="150" height="150" alt="MojoLauncher লোগো">

[![Android CI](https://github.com/MojoLauncher/MojoLauncher/workflows/Android%20CI/badge.svg)](https://github.com/MojoLauncher/MojoLauncher/actions)
[![GitHub commit activity](https://img.shields.io/github/commit-activity/m/MojoLauncher/MojoLauncher)](https://github.com/MojoLauncher/MojoLauncher/actions)
[![Crowdin](https://badges.crowdin.net/pojavlauncher/localized.svg)](https://crowdin.com/project/pojavlauncher)
[![Discord](https://img.shields.io/discord/1365346109131722753.svg?label=&logo=discord&logoColor=ffffff&color=7389D8&labelColor=6A7EC2)](https://discord.gg/VHdwQFsaGX)

* MojoLauncher হলো একটি লঞ্চার, যা [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher)-এর উপর ভিত্তি করে তৈরি, এবং এটি আপনাকে আপনার অ্যান্ড্রয়েড ডিভাইসে Minecraft: Java Edition খেলতে দেয়!  

* এটি প্রায় প্রতিটি Minecraft সংস্করণ চালাতে পারে, এবং আপনাকে শুধুমাত্র `.jar` ইনস্টলার ব্যবহার করে [Forge](https://files.minecraftforge.net/) ও [Fabric](http://fabricmc.net/) এর মতো মডলোডার, [OptiFine](https://optifine.net) ও এবং আরও অনেক কিছু ব্যবহার করতে দেয়‌!

## নেভিগেশন
- [পরিচিতি](#introduction)  
- [MojoLauncher সংগ্রহ](#getting-mojolauncher)
- [বিল্ডিং](#building) 
- [বর্তমান অবস্থা](#current-status) 
- [লাইসেন্স](#license) 
- [Contributing](#contributing) 
- [ক্রেডিটস ও Third party components এবং তাদের লাইসেন্স](#credits--third-party-components-and-their-licenses-if-available)

## পরিচিতি 
* MojoLauncher হলো একটি Minecraft: Java Edition লঞ্চার অ্যান্ড্রয়েডের জন্য, যা [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher)-এর উপর ভিত্তি করে তৈরি।
* এই লঞ্চারটি প্রায় সব Minecraft version চালাতে পারে, যেমন rd-132211 থেকে 1.21 snapshots (Combat Test version)।  
* Forge ও Fabric-এর মাধ্যমে মডিং-ও সমর্থিত।  

## MojoLauncher সংগ্রহ  

MojoLauncher সংগ্রহের তিনটি পদ্ধতি রয়েছে:  

1. [অটোমেটিক বিল্ড](https://github.com/MojoLauncher/MojoLauncher/actions) থেকে প্রি-বিল্ট অ্যাপ নিন।  

2. Google Play থেকে সংগ্রহ করতে পারেন এই ব্যাজে ক্লিক করে:  
[![Google Play](https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png)](https://play.google.com/store/apps/details?id=git.artdeell.mojo)  

3. সোর্স থেকে [বিল্ড](#building) করতে পারেন।  

## বিল্ডিং   
* লঞ্চার বিল্ড করুন (এটি স্বয়ংক্রিয়ভাবে সব প্রয়োজনীয় কম্পোনেন্ট ডাউনলোড করবে)
```
./gradlew :app_pojavlauncher:assembleDebug
```
(আপনি যদি Windows ব্যবহার করেন তবে `./gradlew` এর পরিবর্তে `.\gradlew.bat` ব্যবহার করুন)।  

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

## পরিচিত সমস্যাসমূহ
- কিছু ফিজিক্যাল মাউসে খুব ধীর গতি হতে পারে  
- Holy GL4ES-এ বড় texture atlases বিকৃত হতে পারে (ফলে মডপ্যাকে ব্লকি/স্ট্রেচড টেক্সচার দেখা যায়)  
- সম্ভবত আরও সমস্যা আছে, তাই আমাদের একটি বাগ ট্র্যাকার রয়েছে ;)  

## লাইসেন্স
- MojoLauncher [GNU LGPLv3](https://github.com/MojoLauncher/MojoLauncher/blob/v3_openjdk/LICENSE) এর অধীনে লাইসেন্সকৃত।  

## অবদান রাখা
ভালো ছেলেদের স্বাগত! আমরা যেকোনো ধরনের ভালো ছেলেদের স্বাগত জানা। শুধু কোড নয়, উদাহরণস্বরূপ, আপনি উইকি উন্নত করতে সাহায্য করতে পারেন। আপনি [অনুবাদে](https://crowdin.com/project/pojavlauncher)ও সাহায্য করতে পারেন!  

এই রিপোজিটরিতে যেকোনো কোড পরিবর্তন পুল রিকোয়েস্ট আকারে জমা দিতে হবে। বর্ণনায় ব্যাখ্যা থাকতে হবে কোড কী করে এবং এটি চালানোর ধাপগুলো কী।  

## ক্রেডিটস ও Third party components এবং তাদের লাইসেন্স (যদি পাওয়া যায়)  
- [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher): [GNU LGPLv3 License](https://github.com/PojavLauncherTeam/PojavLauncher/blob/v3_openjdk/LICENSE)  
- [Boardwalk](https://github.com/zhuowei/Boardwalk) (JVM Launcher): Unknown License/[Apache License 2.0](https://github.com/zhuowei/Boardwalk/blob/master/LICENSE) অথবা GNU GPLv2।  
- Android Support Libraries: [Apache License 2.0](https://android.googlesource.com/platform/prebuilts/maven_repo/android/+/master/NOTICE.txt)  
- [GL4ES](https://github.com/PojavLauncherTeam/gl4es): [MIT License](https://github.com/ptitSeb/gl4es/blob/master/LICENSE)  
- [OpenJDK](https://github.com/PojavLauncherTeam/openjdk-multiarch-jdk8u): [GNU GPLv2 License](https://openjdk.java.net/legal/gplv2+ce.html)  
- [LWJGL3](https://github.com/MojoLauncher/lwjgl3): [BSD-3 License](https://github.com/LWJGL/lwjgl3/blob/master/LICENSE.md)  
- [Mesa 3D Graphics Library](https://gitlab.freedesktop.org/mesa/mesa): [MIT License](https://docs.mesa3d.org/license.html)  
- [pro-grade](https://github.com/pro-grade/pro-grade) (Java sandboxing security manager): [Apache License 2.0](https://github.com/pro-grade/pro-grade/blob/master/LICENSE.txt)  
- [bhook](https://github.com/bytedance/bhook) (Exit code ট্র্যাপ করার জন্য ব্যবহৃত): [MIT license](https://github.com/bytedance/bhook/blob/main/LICENSE)  
- ধন্যবাদ [MCHeads](https://mc-heads.net) কে, Minecraft avatar সরবরাহের জন্য।
