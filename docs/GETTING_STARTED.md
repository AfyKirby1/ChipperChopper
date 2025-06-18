# 🚀 Getting Started with Chipper Chopper Development

Welcome to Minecraft modding! This guide will help you get started with developing the Chipper Chopper mod, even if you're completely new to modding.

## 📋 Prerequisites

### Required Software
1. **Java 21** - Download from [Oracle](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://openjdk.org/)
2. **IntelliJ IDEA** (Recommended) - Download the free Community Edition from [JetBrains](https://www.jetbrains.com/idea/)
3. **Git** - Download from [git-scm.com](https://git-scm.com/)

### Optional but Helpful
- **Minecraft Development Kit (MDK)** knowledge
- Basic Java programming knowledge
- Understanding of Minecraft game mechanics

## 🛠️ Setting Up Your Development Environment

### Step 1: Install Java 21
1. Download and install Java 21
2. Verify installation by opening Command Prompt/Terminal and typing:
   ```
   java -version
   ```
   You should see Java 21.x.x

### Step 2: Clone the Project
```bash
git clone https://github.com/yourusername/chipper-chopper.git
cd chipper-chopper
```

### Step 3: Open in IntelliJ IDEA
1. Open IntelliJ IDEA
2. Click "Open" and select the `chipper-chopper` folder
3. Wait for IntelliJ to import the Gradle project (this may take a few minutes)

### Step 4: Build the Project
**Windows**: Double-click `build.bat` or run it from Command Prompt
**Other**: Run `./gradlew build` in terminal

## 📁 Project Structure Explained

```
chipper-chopper/
├── src/
│   ├── main/
│   │   ├── java/com/example/chipperChopper/
│   │   │   ├── ChipperChopperMod.java      # Main mod class
│   │   │   └── TreeChopperAI.java          # AI logic
│   │   └── resources/
│   │       ├── fabric.mod.json             # Mod metadata
│   │       ├── chipper_chopper.mixins.json # Mixin configuration
│   │       └── assets/                     # Textures, sounds, etc.
│   └── client/
│       └── java/com/example/chipperChopper/
│           └── ChipperChopperClient.java   # Client-side code
├── build.gradle                            # Build configuration
├── gradle.properties                       # Project properties
└── README.md                              # This file!
```

## 🎯 Key Files to Understand

### `ChipperChopperMod.java`
- Main entry point for the mod
- Registers commands and events
- Initializes the AI system

### `TreeChopperAI.java`
- Contains all the AI logic
- Handles tree detection, pathfinding, and chopping
- Manages player states and AI behavior

### `ChipperChopperClient.java`
- Client-side initialization
- Handles keybindings and client events
- Manages client-server communication

### `fabric.mod.json`
- Mod metadata (name, version, dependencies)
- Entry points for main and client code
- Mixin configuration references

## 🔧 Making Your First Changes

### Adding a New Feature
1. **Plan your feature** - What should it do?
2. **Find the right place** - Which file should contain your code?
3. **Write the code** - Implement your feature
4. **Test it** - Build and test in Minecraft
5. **Debug if needed** - Check logs for errors

### Example: Adding a New Command
```java
// In ChipperChopperMod.java, inside registerCommands()
.then(CommandManager.literal("debug")
    .executes(context -> {
        context.getSource().sendFeedback(() -> 
            Text.literal("§eDebug info: AI is working!"), false);
        return 1;
    }))
```

## 🐛 Debugging Tips

### Common Issues
1. **Build fails** - Check Java version and dependencies
2. **Mod doesn't load** - Check `fabric.mod.json` syntax
3. **Features don't work** - Check server logs for errors
4. **Client crashes** - Check client logs in `.minecraft/logs/`

### Useful Commands
```bash
# Clean build (if having issues)
.\gradlew.bat clean build

# Run Minecraft with your mod for testing
.\gradlew.bat runClient

# Generate development workspace
.\gradlew.bat genEclipseRuns
.\gradlew.bat genIntellijRuns
```

## 📚 Learning Resources

### Fabric Documentation
- [Fabric Wiki](https://fabricmc.net/wiki/) - Official documentation
- [Fabric Example Mod](https://github.com/FabricMC/fabric-example-mod) - Template project

### Java Learning
- [Oracle Java Tutorials](https://docs.oracle.com/javase/tutorial/)
- [Codecademy Java Course](https://www.codecademy.com/learn/learn-java)

### Minecraft Modding
- [Fabric Discord](https://discord.gg/v6v4pMv) - Community support
- [ModdingByKaupenjoe](https://www.youtube.com/c/ModdingByKaupenjoe) - YouTube tutorials

## 🤝 Contributing

Ready to contribute? Here's how:

1. **Fork the repository** on GitHub
2. **Create a feature branch**: `git checkout -b my-new-feature`
3. **Make your changes** and test them
4. **Commit your changes**: `git commit -am 'Add some feature'`
5. **Push to the branch**: `git push origin my-new-feature`
6. **Create a Pull Request** on GitHub

## 🆘 Getting Help

Stuck? Here's where to get help:

1. **Check the logs** - Look in `.minecraft/logs/latest.log`
2. **Read error messages** - They usually tell you what's wrong
3. **Search online** - Someone probably had the same issue
4. **Ask the community** - Fabric Discord or Reddit r/feedthebeast
5. **Create an issue** - If you found a bug, report it on GitHub

## 🎉 Next Steps

Once you're comfortable with the basics:

1. **Explore the AI logic** - Understand how the tree detection works
2. **Add new tree types** - Support modded wood types
3. **Improve pathfinding** - Make the AI smarter about navigation
4. **Add configuration** - Let users customize AI behavior
5. **Create your own mod** - Use this as a template for your ideas!

---

**Happy Modding!** 🎮✨

*Remember: Every expert was once a beginner. Don't be afraid to experiment and break things - that's how you learn!* 