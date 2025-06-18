# 🔨 Build Instructions - Agent.Lumber v2.6

## 🚀 **Quick Build (Windows)**

### ⚡ **One-Click Build**
```batch
# Navigate to project directory
cd Chipper_Chopper

# Run the build script
batch\build.bat
```

**Expected Output:**
```
BUILD SUCCESSFUL in 18s
[JAR] Mod JAR created: build\libs\chipper-chopper-1.0.0.jar
[JAR] Sources JAR: build\libs\chipper-chopper-1.0.0-sources.jar
```

---

## 🎯 **Installation & Usage**

### 📦 **Install the Mod**
1. **Copy JAR**: Take `build\libs\chipper-chopper-1.0.0.jar`
2. **Place in Mods**: Put it in your `.minecraft\mods\` folder
3. **Launch Minecraft**: Start with Fabric Loader 0.16.9+

### 🎮 **In-Game Usage**
1. **Join World**: Enter any world (Creative or Survival)
2. **Toggle AI**: Press **`O`** key to activate Agent.Lumber
3. **Watch Magic**: AI will automatically find and chop trees using behavior tree logic!

### 📺 **HUD Display**
```
§b§lAgent.Lumber v2.0 (Advanced + Intelligence)§r
§7═════════════════════════════
§a● STATUS: ACTIVE
§e§lSTATE: Behavior Tree: Looking for tree

§6§lTREE TARGET: (29, 65, -51)
§c§lMINING TARGET: (29, 66, -51)
§d§lPATHFINDING: A* Route Active
§a§lSERVER INTELLIGENCE: Analyzing patterns
```

---

## 🛠️ **Development Build**

### 📋 **Prerequisites**
- **Java 21** (Eclipse Temurin recommended)
- **Gradle 8.8** (included in project)
- **Fabric Loom 1.7.4** (auto-downloaded)

### 📥 **Dependencies**
```gradle
// Minecraft 1.21.4
minecraft = "1.21.4"
loader_version = "0.16.9"
fabric_version = "0.119.3+1.21.4"
loom_version = "1.7.4"
```

### 🔧 **Alternative Build Methods**

#### **Command Line Build**
```batch
# Windows
.\gradlew.bat build

# Linux/Mac
./gradlew build
```

#### **Direct Gradle**
```batch
# If you have Gradle installed globally
gradle build
```

#### **IDE Setup**
1. **Import Project**: Open in IntelliJ IDEA or Eclipse
2. **Gradle Sync**: Let IDE download dependencies
3. **Run**: Use IDE's built-in build tools

---

## 🧪 **Testing & Development**

### 🎮 **Run in Development**
```batch
# Client (Test in development environment)
.\gradlew.bat runClient

# Server (Test dedicated server)
.\gradlew.bat runServer
```

### 🔍 **Debug Mode**
The mod includes extensive logging. Check `latest.log` for:
```
[INFO] Agent.Lumber: Behavior Tree: Looking for tree
[INFO] Agent.Lumber: A* Pathfinding: Found route with 5 waypoints
[INFO] Agent.Lumber: Server Intelligence: Pattern detected, switching to forced exploration
[INFO] Agent.Lumber: Emergency override activated - client redirected
```

### 🧠 **AI Development Tips**
- **Behavior Tree**: Modify `ChipperChopperClient.java` for client logic
- **Server Intelligence**: Edit `TreeChopperAI.java` for pattern detection
- **New Nodes**: Add custom Condition/Action nodes to behavior tree
- **Pathfinding**: Enhance A* algorithm in `AStarPathfinder` class

---

## 🚨 **Troubleshooting**

### ❌ **Common Issues**

#### **Build Fails: "cannot find symbol getMaterial()"**
```java
// OLD (1.20.x and earlier)
if (state.getMaterial().isSolid())

// NEW (1.21.4+)  
if (!state.isAir())
```
**Status**: ✅ **FIXED** in v2.6.0

#### **Gradle Daemon Issues**
```batch
# Kill all Gradle daemons and rebuild
.\gradlew.bat --stop
.\gradlew.bat clean build
```

#### **Missing Dependencies**
```batch
# Force refresh dependencies
.\gradlew.bat build --refresh-dependencies
```

#### **Java Version Mismatch**
```batch
# Check Java version (should be 21+)
java -version

# Set JAVA_HOME if needed
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.1.12-hotspot
```

---

## 📁 **Project Structure**

```
Chipper_Chopper/
├── src/
│   ├── main/java/com/example/chipper_chopper/
│   │   ├── ChipperChopperMod.java        # Main mod class
│   │   └── TreeChopperAI.java            # Server-side intelligence
│   └── client/java/com/example/chipper_chopper/
│       └── ChipperChopperClient.java     # Behavior tree client
├── batch/
│   └── build.bat                         # Quick build script
├── docs/                                 # Documentation
└── build.gradle                          # Gradle configuration
```

---

## 🎯 **Version Compatibility**

| Component | Version | Status |
|-----------|---------|--------|
| **Minecraft** | 1.21.4 | ✅ Tested |
| **Fabric Loader** | 0.16.9+ | ✅ Compatible |
| **Fabric API** | 0.119.3+ | ✅ Required |
| **Java** | 21+ | ✅ Required |
| **Gradle** | 8.8 | ✅ Included |

---

## 🚀 **Performance Notes**

### ⚡ **Optimizations in v2.6**
- **Client-Side Processing**: Behavior tree runs on client for responsiveness
- **Server Intelligence**: Pattern detection prevents expensive loops
- **Efficient Pathfinding**: A* algorithm with waypoint caching
- **Smart Timeouts**: Ultra-fast 1-second abandonment of problematic targets

### 📊 **Expected Performance**
- **FPS Impact**: Minimal (< 1-2 FPS in most cases)
- **Memory Usage**: ~5-10MB additional for AI structures
- **Network Traffic**: Minimal server-client coordination messages

---

**🎉 Agent.Lumber v2.6 - The most advanced AI tree-chopping system ever created for Minecraft!** 