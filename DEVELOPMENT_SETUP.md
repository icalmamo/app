# Android Development Setup with Cursor

## Overview
This project is configured to use Cursor for development while maintaining Android Studio for building, running, and debugging.

## Required Extensions
Install these extensions in Cursor for optimal Android development:

1. **Extension Pack for Java** - Complete Java development support
2. **Gradle for Java** - Gradle build system integration
3. **Language Support for Java** - Core Java language features
4. **XML** - XML file support for Android layouts
5. **Auto Rename Tag** - Automatic tag renaming in XML
6. **Prettier** - Code formatting
7. **ESLint** - Code linting (if using any web components)

## Quick Setup
1. Open Cursor in this project directory
2. Install recommended extensions when prompted
3. Wait for Java language server to initialize
4. Use Android Studio for running/debugging the app

## Development Workflow

### In Cursor:
- ✅ Write and edit Java/Kotlin code
- ✅ Edit XML layouts and resources
- ✅ Manage Gradle dependencies
- ✅ Use Git for version control
- ✅ Code completion and IntelliSense
- ✅ Refactoring tools

### In Android Studio:
- ✅ Build and run the app
- ✅ Debug on device/emulator
- ✅ Profiling and performance analysis
- ✅ Layout editor and visual design
- ✅ APK generation and signing

## Build Commands (Available in Cursor)
Use `Ctrl+Shift+P` → "Tasks: Run Task" to access:

- **Gradle: Build Debug** - Build debug APK
- **Gradle: Build Release** - Build release APK  
- **Gradle: Clean** - Clean build artifacts
- **Gradle: Test** - Run unit tests

## Project Structure
```
app/
├── src/main/java/com/example/h_cas/    # Java source files
├── src/main/res/                       # Android resources
│   ├── layout/                         # XML layouts
│   ├── values/                         # Strings, colors, themes
│   └── drawable/                       # Images and icons
├── build.gradle                        # App-level build config
└── .cursorrules                        # Cursor-specific rules
```

## Tips for Efficient Development
1. Keep both Cursor and Android Studio open
2. Use Cursor for code editing and Android Studio for testing
3. Sync frequently between the two IDEs
4. Use Gradle tasks in Cursor for quick builds
5. Leverage Cursor's AI features for code generation and refactoring

## Troubleshooting
- If Java language server isn't working, restart Cursor
- Ensure Android SDK is properly configured in Android Studio
- Check that Gradle wrapper is executable
- Verify project builds successfully in Android Studio first




















