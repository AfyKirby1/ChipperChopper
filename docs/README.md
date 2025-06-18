# 🪓 Chipper Chopper - Smart Tree Chopping AI for Minecraft 🤖

*An intelligent Minecraft Fabric mod that adds AI-powered tree chopping with realistic movement, advanced terrain navigation, and a sleek HUD interface!*

---

## ✨ **What's New in v1.2.4** ✨

🧠 **REVOLUTIONARY AI INTELLIGENCE**: Completely redesigned decision-making system!
- **Learning AI**: Tracks failed attempts and avoids repeating the same mistakes
- **Smart Scoring**: Advanced algorithm evaluates target accessibility, distance, obstacles, and previous failures
- **Progress Monitoring**: 15-second timeout system abandons hopeless situations
- **Decision History**: Maintains log of recent choices for better pattern recognition

🎮 **BEAUTIFUL HUD INTERFACE**: Real-time AI monitoring in top-left corner!
- **Live AI Thinking**: See exactly what the AI is deciding moment by moment
- **Progress Tracking**: Distance to targets, current mining operations, repositioning status
- **Decision Log**: Recent AI choices displayed in real-time
- **Color-Coded Display**: Blue for thinking, orange for targets, green for mining, yellow for decisions
- **Status Indicators**: Clear active/inactive status with emoji indicators

🎯 **Enhanced Problem Solving**:
- **Intelligent Target Selection**: Finds the best accessible log blocks using advanced scoring
- **Anti-Cycling**: Prevents endless loops between alternatives that plagued v1.2.3
- **Failure Learning**: Remembers problematic blocks and chooses better alternatives
- **Timeout Recovery**: Smart fallback system when normal approaches fail

## 🎮 **How to Use**

1. **Install the mod** in your `mods` folder
2. **Press G** to toggle the AI (customizable keybind)
3. **Watch the HUD** appear in the top-left showing AI thinking process
4. **Enjoy** as your AI intelligently chops trees and learns from experience!

## 🎯 **What Makes This Special?**

Unlike other tree-chopping mods that just break logs instantly, **Chipper Chopper** creates an AI that behaves like a real player:

🧠 **Smart Decision Making**: Analyzes tree structure and finds the best approach  
🚶 **Realistic Movement**: Moves naturally with smart pathfinding around obstacles  
👀 **Human-like Behavior**: Looks at targets, handles line-of-sight issues, repositions when stuck  
🌿 **Intelligent Leaf Clearing**: Only clears leaves when necessary for wood access  
📦 **Automatic Collection**: Gathers all dropped items after chopping  
🎮 **Live Feedback**: Beautiful HUD shows exactly what the AI is thinking

## 🧠 **Advanced AI Features**

### **Intelligent Pathfinding** (v1.2.2+)
- **Obstacle Detection**: Detects holes, walls, height differences, and terrain obstacles
- **Smart Escape Routes**: When stuck in holes or caves, finds paths to higher ground  
- **Terrain Analysis**: Calculates optimal positions around trees for maximum accessibility
- **Adaptive Movement**: Adjusts speed and aggression based on terrain difficulty

### **Learning & Decision Making** (v1.2.4+)
- **Failure Tracking**: Remembers which targets caused problems and avoids them
- **Smart Scoring**: Evaluates targets based on distance, accessibility, obstacles, and history
- **Progress Monitoring**: Abandons approaches that don't show progress within 15 seconds
- **Adaptive Strategies**: Switches between chopping, leaf clearing, and repositioning intelligently

### **Problem Solving** (v1.2.3+)
- **Multi-Tier Fallbacks**: Alternative targets → repositioning → direct approach → abandon tree
- **Aggressive Timeouts**: Quick (0.5s) abandonment of problematic leaf targets
- **Wood Prioritization**: Always prefers accessible wood blocks over leaf clearing
- **Context Awareness**: Different strategies for different types of obstacles

## 📋 **Installation**

### **Requirements**
- Minecraft 1.21.4
- Fabric Loader 0.16.9+  
- Fabric API 0.119.3+
- Java 21+

### **Quick Install**
1. Download the latest `chipper-chopper-1.0.0.jar` from releases
2. Place in your `.minecraft/mods` folder
3. Launch Minecraft with Fabric
4. Press **G** in-game to activate!

## 🎮 **Controls**

- **G** - Toggle Chipper Chopper AI (shows HUD when active)
- **ESC** - Deactivate AI (if you need manual control)

## 🛠️ **Building from Source**

```bash
# Clone the repository
git clone <repository-url>
cd Chipper_Chopper

# Build the mod (Windows)
.\gradle-8.8\bin\gradle.bat build

# Find your mod in build/libs/
```

## 📈 **Version History**

- **v1.2.4** - Revolutionary AI intelligence + Beautiful HUD interface
- **v1.2.3** - Critical behavior fixes (wood prioritization, anti-spam collection)  
- **v1.2.2** - Intelligent pathfinding and terrain navigation
- **v1.2.1** - Timeout fixes and movement improvements
- **v1.2.0** - Major AI enhancements and fallback systems
- **v1.1.0** - Enhanced leaf clearing and repositioning
- **v1.0.0** - Initial release with basic AI tree chopping

## 🐛 **Known Issues**

- Very rare edge cases with complex terrain (AI will abandon and find new trees)
- Occasional server lag with complex trees (timeout system handles this)

## 💡 **Tips for Best Experience**

- **Use in open areas** for best pathfinding results
- **Let the AI work** - it's designed to handle most situations automatically  
- **Watch the HUD** to understand what the AI is thinking
- **Check recent decisions** if you want to see the AI's reasoning process

---

*Made with ❤️ for the Minecraft community. Enjoy your intelligent tree-chopping companion!*

---

## 🎉 What's Next?

- **Multi-tree harvesting** - Plan routes through multiple trees
- **Custom tree types** - Support for modded wood types  
- **Team coordination** - Multiple AIs working together
- **Advanced inventory management** - Sort and organize items

**Enjoy your automated tree harvesting! 🌲⚡**

---

<div align="center">

**Made with ❤️ for the Minecraft community**

[⭐ Star this repo](https://github.com/yourusername/chipper-chopper) • [🐛 Report Bug](https://github.com/yourusername/chipper-chopper/issues) • [💡 Request Feature](https://github.com/yourusername/chipper-chopper/issues)

</div> 

## 🎯 **What's New in v1.2.1** ✨

🔧 **CRITICAL HOTFIX**: Fixed infinite loop issue where AI would get stuck in `MOVING_TO_TREE` state
- Added 3-second timeout mechanism for movement state transitions
- Enhanced debug logging to track movement progress
- Fixed client-side target detection for `CLEARING_LEAVES` state
- AI now forces progression when stuck, preventing console spam

## 🎯 **What Makes This Special?**

Unlike other tree-chopping mods that just break logs instantly, **Chipper Chopper** creates an AI that behaves like a real player:

🧠 **Smart Decision Making**: Analyzes tree structure and finds the best approach  
🚶 **Realistic Movement**: Walks to trees, positions optimally, and collects items  
🍃 **Leaf Management**: Clears obstructing leaves with intelligent targeting  
🔄 **Adaptive Behavior**: Repositions when stuck, tries alternative approaches  
⚡ **Anti-Stuck Systems**: Multiple fallback mechanisms prevent infinite loops

---

## 🚀 **Quick Start**

### **Installation**
1. **Download** the latest `chipper-chopper-1.0.0.jar` from the `build/libs/` folder
2. **Install Fabric Loader** (0.16.9+) and **Fabric API** (0.119.3+) for Minecraft 1.21.4
3. **Drop the JAR** into your mods folder
4. **Launch Minecraft** and enjoy smart tree chopping!

### **How to Use**
- **Toggle AI**: Press `G` key (configurable)
- **Chat Command**: Type `/chipper toggle` 
- **Watch the Magic**: Your character will intelligently find and chop trees!

---

## 🎮 **Features**

### **🌳 Intelligent Tree Detection**
- Scans 16-block radius for optimal trees
- Prioritizes accessible trees with clear paths
- Identifies tree bases vs. floating logs

### **🧭 Smart Movement & Positioning**
- Walks naturally to trees (no teleporting!)
- Finds optimal chopping positions
- Repositions when obstructed
- Jumps over obstacles automatically

### **🍃 Advanced Leaf Management**
- Detects leaves blocking access to logs
- Clears problematic leaves strategically
- Avoids infinite loops with blacklisting system
- Escalates to repositioning when stuck

### **⚡ Anti-Stuck Technology**
- **1-second detection**: Quickly identifies stuck states
- **Multi-tier fallbacks**: Alternative targets → repositioning → direct chopping → abandon
- **Timeout mechanisms**: Forces progression after reasonable attempts
- **Problematic block tracking**: Avoids known problem areas

### **📦 Item Collection**
- Automatically walks to dropped items
- Intelligent pathfinding to wood/log drops
- Prioritizes nearby items first

---

## 🔧 **Technical Details**

**Minecraft Version**: 1.21.4  
**Mod Loader**: Fabric 0.16.9+  
**Dependencies**: Fabric API 0.119.3+  
**Build Tool**: Gradle 8.8 with Java 21  

### **Performance**
- Lightweight server-side AI logic
- Efficient client-side movement
- No world modification (just player actions)
- Minimal performance impact

---

## 🛠️ **Building from Source**

```bash
# Clone or download this repository
# Ensure Java 21 is installed

# Quick build (Windows)
.\build.bat

# Manual build
.\gradlew build

# Output location
build/libs/chipper-chopper-1.0.0.jar
```

---

## 🤝 **Contributing**

Found a bug? Have an improvement idea? 

1. Check if there's already an issue
2. Create a detailed bug report with console logs
3. Test with the latest version first
4. Include your Minecraft/Fabric versions

---

## 📜 **License**

This project is licensed under the MIT License. Feel free to use, modify, and distribute!

---

**Happy Tree Chopping! 🌲⚡**

*Made with ❤️ for the Minecraft community* 

## ✨ **What's New in v1.2.2** ✨

🧠 **INTELLIGENT PATHFINDING**: Revolutionary terrain navigation system!
- **Obstacle Detection**: AI automatically detects holes, walls, and height differences
- **Smart Escape Routes**: When stuck in holes or caves, AI finds escape paths to higher ground
- **Terrain Analysis**: Calculates best accessible positions around trees for optimal chopping
- **Enhanced Movement**: More aggressive jumping, sprinting, and movement when navigating difficult terrain
- **Adaptive Positioning**: Finds alternative positions when trees are on cliffs or elevated terrain

🎯 **Advanced Navigation Features**:
- ⛰️ **Height-Aware Pathfinding**: Detects when trees are too high up and finds accessible routes
- 🕳️ **Hole Escape System**: Smart detection and escape from pits, caves, and stuck situations  
- 🧗 **Intelligent Jumping**: Enhanced jumping logic for various terrain challenges
- 🏃 **Dynamic Movement Speed**: Increases movement intensity based on distance and difficulty
- 🎪 **Multi-Tier Fallback**: Multiple strategies when primary approach fails

## 🎯 **What Makes This Special?**

Unlike other tree-chopping mods that just break logs instantly, **Chipper Chopper** creates an AI that behaves like a real player with advanced pathfinding: 