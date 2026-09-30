# Phone Control

A user-owned Android 11 AccessibilityService controller adapted from the
interaction architecture of [wsong-nj/AccessDroid](https://github.com/wsong-nj/AccessDroid).

This branch is intentionally built as a separate controller rather than
modifying the upstream repository. The upstream repository is a GPL-3.0
research tool for accessibility analysis and normally expects ADB on a host.

This controller is designed for the connected Android 11 device and accepts
commands through an exported broadcast action:

    dev.devanshu.phonecontrol.COMMAND

Examples:

    adb shell am broadcast -a dev.devanshu.phonecontrol.COMMAND --es op back
    adb shell am broadcast -a dev.devanshu.phonecontrol.COMMAND --es op tap --ei x 500 --ei y 800

Supported operations include gestures, global navigation, text entry, UI
element clicks, UI-tree export, screenshots, package launching, and URI opening.

The Android 11 screenshot API is used directly by the accessibility service,
which is available from API 30.

Source licensing: this work is distributed under GPL-3.0 in the spirit of the
upstream project. See the upstream project for the original license text.
