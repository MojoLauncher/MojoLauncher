# Project Summary: MojoLauncher

## Overview

This project is a feature-rich Android launcher for the Java edition of Minecraft, built upon the foundation of PojavLauncher. It allows users to download, manage, and play various versions of Minecraft, including official releases, snapshots, and modded versions (Forge, Fabric, OptiFine).

## Core Architecture

The application is built using a standard Android architecture, with a few key components that work together to provide its functionality:

*   **Application Entry Point:** The `PojavApplication` class serves as the main entry point, handling initialization, crash reporting, and basic configuration.
*   **Main UI:** The `LauncherActivity` is the primary user-facing component. It uses a fragment-based approach to manage the different screens, including the main menu, settings, and login. It also handles the game launch process and displays download progress.
*   **Java GUI Rendering:** The `JavaGUILauncherActivity` is responsible for rendering the Java GUI. It uses an AWT (Abstract Window Toolkit) canvas to display the game and manages all user input, including a virtual mouse and customizable on-screen controls.
*   **Minecraft Version Management:** The launcher includes a robust system for downloading and managing different Minecraft versions, with support for official releases, snapshots, and older versions.
*   **Mod Support:** The app has built-in support for installing and launching modded versions of the game, including Forge, Fabric, and OptiFine. It can also handle modpacks.

## Key Features

*   **Multi-version support:** Download and manage multiple versions of Minecraft.
*   **Mod support:** Install and launch modded versions of the game.
*   **Customizable controls:** Highly customizable on-screen controls, including a virtual mouse, joysticks, and mappable buttons.
*   **Multiple renderers:** Choose between different graphics renderers (e.g., GL4ES, Zink/Vulkan) to optimize performance.
*   **Java runtime management:** Manage different Java runtimes for compatibility with various Minecraft versions.

## Dependencies

The project relies on a few key external libraries:

*   **AndroidX:** For core Android functionality, including `appcompat` and `preference`.
*   **Google Material Components:** For modern UI elements.
*   **SDP/SSP:** For responsive UI scaling across different screen sizes.

The project also includes several local libraries (`.jar` files) that provide additional functionality, such as `exp4j` for mathematical expression evaluation.

## Next Steps

Now that we have a high-level understanding of the project, we can explore several options for moving forward:

*   **Code quality analysis:** We can perform a more in-depth analysis of the code to identify potential bugs, areas for refactoring, and adherence to best practices.
*   **Dependency analysis:** We can check for outdated or vulnerable libraries to ensure the project remains secure and up-to-date.
*   **Feature development:** We can begin planning and implementing new features.
*   **Bug fixing:** We can investigate and fix any known bugs or issues.