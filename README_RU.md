<H1 align="center">MojoLauncher</H1>

<a href="https://github.com/MojoLauncher/MojoLauncher/blob/v3_openjdk/README.md">Readme in English</a>

<img src="https://github.com/MojoLauncher/MojoLauncher/blob/v3_openjdk/app_pojavlauncher/src/main/assets/pojavlauncher.png" align="left" width="150" height="150" alt="MojoLauncher logo">

[![Android CI](https://github.com/MojoLauncher/MojoLauncher/workflows/Android%20CI/badge.svg)](https://github.com/MojoLauncher/MojoLauncher/actions)
[![GitHub commit activity](https://img.shields.io/github/commit-activity/m/MojoLauncher/MojoLauncher)](https://github.com/MojoLauncher/MojoLauncher/actions)
[![Crowdin](https://badges.crowdin.net/pojavlauncher/localized.svg)](https://crowdin.com/project/pojavlauncher)
[![Discord](https://img.shields.io/discord/1365346109131722753.svg?label=&logo=discord&logoColor=ffffff&color=7389D8&labelColor=6A7EC2)](https://discord.gg/VHdwQFsaGX)

* MojoLauncher это лаунчер, основанный на [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher), позволяющий играть в Minecraft: Java Edition на устройствах Android!

* Он может запускать почти любую версию Minecraft, позволяя устанваливать через .jar загрузчики модов такие как [Forge](https://files.minecraftforge.net/) и [Fabric](http://fabricmc.net/), и моды по типу [OptiFine](https://optifine.net).

## Навигация
- [Введение](#introduction)  
- [Как получить MojoLauncher](#getting-mojolauncher)
- [Сборка](#building) 
- [Текущие планы](#current-roadmap) 
- [Лицензия](#license) 
- [Внести свой вклад в проект](#contributing) 
- [Благодарности & Сторонние компоненты и их лицензии](#credits--third-party-components-and-their-licenses-if-available)

## Введение 
* MojoLauncher это лаунчер Minecraft: Java Edition для Android основанный на [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher)
* Лаунчер может запускать почти все доступные версии Minecraft в диапазоне от rd-132211 до снапшотов 1.21 (включая версии Combat Test). 
* Моддинг через Forge и Fabric так же поддерживается. 

## Как получить MojoLauncher

Получить копию MojoLauncher можно тремя способами:

1. Можно скачать готовый билд с раздела [автоматических сборок](https://github.com/MojoLauncher/MojoLauncher/actions).

2. Можно скачать с Google Play нажав на кнопку ниже:
[![Google Play](https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png)](https://play.google.com/store/apps/details?id=git.artdeell.mojo)

3. Можно [собрать](#building) с исходного кода.
## Сборка  
* Скомпилируйте лаунчер (все необходимые компоненты скачаются автоматически)
```
./gradlew :app_pojavlauncher:assembleDebug
```
(Замените `./gradlew` на `.\gradlew.bat` если вы компилируете на Windows).

## Текущие планы
- [x] Система инстансов на замену профилям версий
- [x] Поддержка 1.21.5 "из коробки"
- [ ] LTW: исправить проблемы с Create
- [ ] LTW: добавить поддержку Compute расширений
- [ ] LTW: использовать форматы с поддержкой Color Attachment для объектов фреймбуфера (FBO)
- [ ] Менеджер модов/модпаков
- [ ] Импорт mrpack/CurseForge zip
- [ ] Импорт инстансов формата MultiMC
- [ ] Patch-on-dlopen для нативных библиотек модов
- [ ] Заменить Holy-GL4ES 1.1.5 на KW (нужно уточнить требования)

## Известные проблемы
- Некоторые физические мышки могут иметь низкую скорость курсора
- На Holy GL4ES большие алиасы текстур могут быть искажены (из-за чего в некоторых модпаках могут быть растянутые или излишне квадратные текстуры)
- Скорее всего есть ещё, поэтому у нас есть баг-трекер ;) 

## Лицензия
- MojoLauncher лицензирован под [GNU LGPLv3](https://github.com/MojoLauncher/MojoLauncher/blob/v3_openjdk/LICENSE).

## Внести свой вклад в проект
Мы привествуем желающих внести свой вклад в проект! Нам не помешает любая помощь, не только код. Например, вы можете помочь в разработке и формировании вики. Вы так же можете помочь [перевести проект](https://crowdin.com/project/pojavlauncher) на ваш язык!


Любые изменения в коде этого репозитория должны быть отправлены в виде pull request-а. Описание должно объяснять что делает код и предоставлять шаги для его запуска.

## Благодарности & Сторонние компоненты и их лицензии (если таковые имеются)
- [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher): [GNU LGPLv3 License](https://github.com/PojavLauncherTeam/PojavLauncher/blob/v3_openjdk/LICENSE)
- [Boardwalk](https://github.com/zhuowei/Boardwalk) (JVM Лаунчер): Неизвестная Лицензия/[Apache License 2.0](https://github.com/zhuowei/Boardwalk/blob/master/LICENSE) или GNU GPLv2.
- Android Support Libraries: [Apache License 2.0](https://android.googlesource.com/platform/prebuilts/maven_repo/android/+/master/NOTICE.txt).
- [GL4ES](https://github.com/PojavLauncherTeam/gl4es): [MIT License](https://github.com/ptitSeb/gl4es/blob/master/LICENSE).<br>
- [OpenJDK](https://github.com/PojavLauncherTeam/openjdk-multiarch-jdk8u): [GNU GPLv2 License](https://openjdk.java.net/legal/gplv2+ce.html).<br>
- [LWJGL3](https://github.com/MojoLauncher/lwjgl3): [BSD-3 License](https://github.com/LWJGL/lwjgl3/blob/master/LICENSE.md).
- [Mesa 3D Graphics Library](https://gitlab.freedesktop.org/mesa/mesa): [MIT License](https://docs.mesa3d.org/license.html).
- [pro-grade](https://github.com/pro-grade/pro-grade) (Менеджер контейнеризации Java): [Apache License 2.0](https://github.com/pro-grade/pro-grade/blob/master/LICENSE.txt).
- [bhook](https://github.com/bytedance/bhook) (Используется для получения кода ошибки): [MIT license](https://github.com/bytedance/bhook/blob/main/LICENSE).
- [Authlib-Injector](https://github.com/yushijinhun/authlib-injector) (Используется для авторизации через ely.by): [AGPL-3.0](https://github.com/yushijinhun/authlib-injector/blob/develop/LICENSE).
- [alsoft](https://github.com/kcat/openal-soft/) (Библиотека вывода звука): [GNU LIBRARY GENERAL PUBLIC LICENSE](https://github.com/kcat/openal-soft/blob/master/COPYING) и [modified PFFFT](https://github.com/kcat/openal-soft/blob/master/LICENSE-pffft).
- [oboe](https://github.com/google/oboe): [Apache License 2.0](https://github.com/google/oboe/blob/main/LICENSE).
- Отдельная благодарность к [Mineskin](https://mineskin.eu/) за предоставление аватаров Minecraft.
