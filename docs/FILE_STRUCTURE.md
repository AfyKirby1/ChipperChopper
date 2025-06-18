# 📁 Chipper Chopper - File Structure Documentation

## 🗂️ Project Overview

This document provides a comprehensive overview of the Chipper Chopper mod's file structure and organization.

## 📋 Root Directory Structure

```
Chipper_Chopper/
├── 📁 build/                          # Build output directory
│   ├── 📁 classes/                    # Compiled Java classes
│   ├── 📁 libs/                       # Built JAR files
│   │   ├── chipper-chopper-1.0.0.jar          # Main mod JAR
│   │   └── chipper-chopper-1.0.0-sources.jar  # Source code JAR
│   └── 📁 resources/                  # Processed resources
├── 📁 docs/                           # Documentation files
│   ├── ARCHITECTURE.md                # Technical architecture guide
│   ├── FILE_STRUCTURE.md              # This file
│   └── API.md                         # API documentation (future)
├── 📁 gradle/                         # Gradle wrapper files
│   └── 📁 wrapper/
│       ├── gradle-wrapper.jar         # Gradle wrapper executable
│       └── gradle-wrapper.properties  # Gradle version config
├── 📁 gradle-8.10/                    # Local Gradle installation
├── 📁 src/                            # Source code directory
│   ├── 📁 main/                       # Main source code
│   │   ├── 📁 java/                   # Java source files
│   │   └── 📁 resources/              # Resource files
│   └── 📁 client/                     # Client-side source code
│       └── 📁 java/                   # Client Java files
├── 📄 build.gradle                    # Gradle build configuration
├── 📄 gradle.properties               # Project properties
├── 📄 settings.gradle                 # Gradle settings
├── 📄 gradlew                         # Gradle wrapper (Unix)
├── 📄 gradlew.bat                     # Gradle wrapper (Windows)
├── 📄 README.md                       # Main project documentation
├── 📄 TROUBLESHOOTING.md              # Build and usage troubleshooting
├── 📄 GETTING_STARTED.md              # Quick start guide
├── 📄 LICENSE                         # MIT License
└── 📄 .gitignore                      # Git ignore rules
```

## 🎯 Source Code Structure

### Main Source (`src/main/`)

```
src/main/
├── 📁 java/com/example/chipperChopper/
│   ├── 📄 ChipperChopperMod.java      # Main mod entry point
│   └── 📄 TreeChopperAI.java         # Core AI logic
└── 📁 resources/
    ├── 📄 fabric.mod.json             # Mod metadata
    ├── 📄 chipper_chopper.mixins.json # Mixin configuration
    └── 📁 assets/chipper_chopper/
        ├── 📁 lang/                   # Localization files
        │   └── 📄 en_us.json          # English translations
        └── 📄 icon.png                # Mod icon (future)
```

### Client Source (`src/client/`)

```
src/client/
└── 📁 java/com/example/chipperChopper/
    └── 📄 ChipperChopperClient.java   # Client-side functionality
```

## 📄 Key Files Explained

### Build Configuration Files

| File | Purpose | Key Contents |
|------|---------|--------------|
| `build.gradle` | Main build script | Dependencies, Fabric Loom config, Java settings |
| `gradle.properties` | Project properties | Mod version, Minecraft version, dependencies |
| `settings.gradle` | Gradle settings | Project name and structure |

### Source Code Files

| File | Purpose | Key Classes/Methods |
|------|---------|-------------------|
| `ChipperChopperMod.java` | Mod initialization | `onInitialize()`, command registration |
| `TreeChopperAI.java` | AI logic | State machine, tree detection, movement |
| `ChipperChopperClient.java` | Client features | Keybindings, client events |

### Resource Files

| File | Purpose | Contents |
|------|---------|----------|
| `fabric.mod.json` | Mod metadata | ID, version, dependencies, entry points |
| `chipper_chopper.mixins.json` | Mixin config | Mixin package and compatibility |
| `en_us.json` | Translations | Keybind names, command descriptions |

## 🏗️ Build Output Structure

### Build Directory (`build/`)

```
build/
├── 📁 classes/java/
│   ├── 📁 main/                       # Compiled main classes
│   └── 📁 client/                     # Compiled client classes
├── 📁 libs/
│   ├── chipper-chopper-1.0.0.jar     # Distribution JAR (15KB)
│   └── chipper-chopper-1.0.0-sources.jar # Source JAR (7.9KB)
├── 📁 resources/main/                 # Processed resources
├── 📁 tmp/                            # Temporary build files
└── 📄 MANIFEST.MF                     # JAR manifest
```

## 🔧 Configuration Files Deep Dive

### `build.gradle` Structure
```gradle
plugins {
    id 'fabric-loom' version '1.7.4'    # Fabric development plugin
    id 'maven-publish'                   # Publishing support
}

dependencies {
    minecraft "com.mojang:minecraft:1.21.4"           # Minecraft version
    mappings "net.fabricmc:yarn:1.21.4+build.1"      # Mappings
    modImplementation "net.fabricmc:fabric-loader:0.16.9"     # Fabric Loader
    modImplementation "net.fabricmc.fabric-api:fabric-api:0.119.3+1.21.4" # Fabric API
}
```

### `fabric.mod.json` Structure
```json
{
    "id": "chipper_chopper",             # Unique mod identifier
    "version": "1.0.0",                  # Mod version
    "entrypoints": {
        "main": ["com.example.chipperChopper.ChipperChopperMod"],
        "client": ["com.example.chipperChopper.ChipperChopperClient"]
    },
    "depends": {                         # Required dependencies
        "fabricloader": ">=0.16.9",
        "minecraft": "~1.21.4",
        "java": ">=21"
    }
}
```

## 📚 Documentation Structure

### Documentation Files

| File | Audience | Content |
|------|----------|---------|
| `README.md` | Users & Developers | Overview, installation, usage |
| `ARCHITECTURE.md` | Developers | Technical design, algorithms |
| `FILE_STRUCTURE.md` | Developers | Project organization |
| `TROUBLESHOOTING.md` | Users | Common issues and solutions |
| `GETTING_STARTED.md` | New Users | Quick start tutorial |

## 🎮 Runtime File Locations

### In Minecraft Directory

```
.minecraft/
├── 📁 mods/
│   └── chipper-chopper-1.0.0.jar     # Installed mod
├── 📁 config/                        # Future config files
└── 📁 logs/
    └── latest.log                     # Contains mod logs
```

### Log Entries Format
```
[22:47:44] [Server thread/INFO]: Started AI for player: darksora269
[22:47:44] [Server thread/INFO]: Found tree at: class_2338{x=53, y=70, z=-92}
[22:47:44] [Server thread/INFO]: Chopped log at: [53, 70, -92]
```

## 🔄 Development Workflow

### File Modification Flow
1. **Edit Source**: Modify `.java` files in `src/`
2. **Build**: Run `gradle build` to compile
3. **Test**: Copy JAR from `build/libs/` to Minecraft
4. **Debug**: Check logs and adjust code
5. **Document**: Update relevant `.md` files

### Version Control Structure
```
.gitignore includes:
├── build/                             # Build outputs
├── .gradle/                           # Gradle cache
├── gradle-8.10/                       # Local Gradle install
└── *.jar                              # Compiled JARs
```

## 🚀 Deployment Structure

### Release Package Contents
```
chipper-chopper-v1.0.0/
├── chipper-chopper-1.0.0.jar         # Main mod file
├── README.md                          # User documentation
├── TROUBLESHOOTING.md                 # Support guide
└── LICENSE                            # License file
```

## 🔧 Development Tools Integration

### IDE Project Structure (IntelliJ IDEA)
```
Project View:
├── 📁 External Libraries             # Dependencies
├── 📁 Gradle Scripts                 # Build files
├── 📁 src
│   ├── 📁 main.java                  # Main source
│   ├── 📁 client.java                # Client source
│   └── 📁 main.resources             # Resources
└── 📁 build                          # Build output
```

### Gradle Tasks Structure
```
Tasks:
├── 📁 build
│   ├── build                         # Full build
│   ├── clean                         # Clean build files
│   └── jar                           # Create JAR only
├── 📁 fabric
│   ├── remapJar                      # Remap for distribution
│   └── runClient                     # Test in development
└── 📁 publishing
    └── publish                       # Publish to repository
```

## 📊 File Size Reference

| File/Directory | Typical Size | Purpose |
|----------------|--------------|---------|
| Source files | ~15KB total | Java source code |
| Built JAR | ~15KB | Distribution file |
| Sources JAR | ~8KB | Source archive |
| Build directory | ~500KB | Temporary build files |
| Gradle cache | ~50MB | Dependencies and cache |

## 🔍 File Naming Conventions

### Java Files
- **Classes**: PascalCase (e.g., `TreeChopperAI.java`)
- **Packages**: lowercase.separated (e.g., `com.example.chipperChopper`)

### Resource Files
- **Mod ID**: snake_case (e.g., `chipper_chopper`)
- **Assets**: snake_case (e.g., `chipper_chopper.mixins.json`)
- **Translations**: locale_code (e.g., `en_us.json`)

### Documentation Files
- **Guides**: UPPERCASE.md (e.g., `README.md`)
- **Technical**: PascalCase.md (e.g., `ARCHITECTURE.md`)

This structure ensures maintainability, clear organization, and easy navigation for both developers and users. 