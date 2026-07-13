@echo off

set thisdir = "%~dp0"
set langfile = %thisdir%\..\app_mojolauncher\src\main\assets\language_list.txt

del %langfile%
dir %thisdir%\..\app_mojolauncher\src\main\res\values-* /s /b > %langfile%

