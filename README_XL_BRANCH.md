# XL Launcher - XL branch additions

This branch (xl/add-features) contains scaffolded, production-oriented stubs for the XL Launcher feature set you requested.

What this commit contains:
- Runtime system stubs (xy.runtime)
- AI, Mod, Performance, Profile, Store, World, Cloud, CAM, Community subsystems (com.xl.launcher.xy.*)
- Low-end optimization package
- Authentication placeholders
- Predictive AI placeholders
- Simple Compose-based Home screen + MockMainActivity (requires Jetpack Compose dependencies in your build)
- Drawable/vector placeholders replacing the launcher icon and a few UI assets (ic_xl_logo.xml, bg_profile_card.xml, button_play.xml)

Notes:
- I did NOT modify gradle, gradlew, wrapper, or any existing .github workflows.
- I replaced/add vector drawable placeholders for the X icon. Replace these with the exact PNG/SVG images if you want pixel-perfect artwork.
- Many classes are implemented as documented stubs with TODOs. They are safe to extend and unit-test.

How to proceed after reviewing this PR:
- If you want me to further refine the UI with exact screenshots (replace vector placeholders with your PNGs), upload the high-res images and I will commit them into res/mipmap-* and res/drawable-*.
- If you want the workflow added later, confirm and I'll add the workflow file.
