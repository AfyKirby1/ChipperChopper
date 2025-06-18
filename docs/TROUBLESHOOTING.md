# 🔧 Troubleshooting Guide

Having trouble building the Chipper Chopper mod? This guide will help you resolve common issues.

## 🚨 Common Build Issues

### Issue 1: "no main manifest attribute" Error
**Problem**: The Gradle wrapper jar is corrupted or missing.

**Solution**:
1. Delete the `gradle/wrapper/gradle-wrapper.jar` file
2. Use IntelliJ IDEA to import the project (recommended)
3. Or install Gradle manually and run `gradle build`

### Issue 2: "Could not create parent directory" Error
**Problem**: Gradle wrapper can't create necessary directories.

**Solution**:
1. **Recommended**: Use IntelliJ IDEA instead of command line
2. Or manually install Gradle from https://gradle.org/install/
3. Run `gradle build` instead of `.\gradlew.bat build`

### Issue 3: Java Version Issues
**Problem**: Wrong Java version or Java not found.

**Solution**:
1. Install Java 21+ from [Oracle](https://www.oracle.com/java/technologies/downloads/)
2. Verify installation: `java -version`
3. Set JAVA_HOME environment variable if needed

### Issue 4: Network/Download Issues
**Problem**: Can't download dependencies or Gradle.

**Solution**:
1. Check your internet connection
2. Try using a VPN if behind corporate firewall
3. Use IntelliJ IDEA which handles downloads better

## 🎯 Recommended Solution: Use IntelliJ IDEA

The easiest way to build this mod is with IntelliJ IDEA:

### Step-by-Step IntelliJ Setup:

1. **Download IntelliJ IDEA**:
   - Go to https://www.jetbrains.com/idea/
   - Download the free Community Edition
   - Install it

2. **Install Java 21**:
   - Download from https://www.oracle.com/java/technologies/downloads/
   - Install and verify with `java -version`

3. **Open the Project**:
   - Launch IntelliJ IDEA
   - Click "Open" (not "New Project")
   - Navigate to your `chipper-chopper` folder
   - Click "OK"

4. **Wait for Import**:
   - IntelliJ will detect it's a Gradle project
   - It will automatically download dependencies
   - This may take 5-10 minutes on first import

5. **Build the Mod**:
   - Look for the "Gradle" panel on the right side
   - Expand "Tasks" → "build"
   - Double-click "build"
   - Wait for completion

6. **Find Your Mod**:
   - Built mod will be in `build/libs/`
   - Look for `chipper-chopper-1.0.0.jar`

## 🛠️ Alternative: Manual Gradle Installation

If you prefer command line:

### Windows:
1. Download Gradle from https://gradle.org/install/
2. Extract to `C:\Gradle`
3. Add `C:\Gradle\bin` to your PATH
4. Open new Command Prompt
5. Navigate to project folder
6. Run: `gradle build`

### Linux/Mac:
```bash
# Install Gradle using package manager
# Ubuntu/Debian:
sudo apt install gradle

# macOS with Homebrew:
brew install gradle

# Then build:
gradle build
```

## 🔍 Verifying Your Setup

Run this to check your project structure:
```batch
simple-build.bat
```

This will verify all files are in the right place.

## 📋 System Requirements Checklist

- ✅ Java 21+ installed and in PATH
- ✅ Internet connection for downloading dependencies
- ✅ At least 2GB free disk space
- ✅ IntelliJ IDEA Community Edition (recommended)

## 🆘 Still Having Issues?

If you're still having problems:

1. **Check the logs**: Look for specific error messages
2. **Try IntelliJ IDEA**: It handles most issues automatically
3. **Verify Java**: Make sure `java -version` shows 21+
4. **Clean start**: Delete `.gradle` folder and try again
5. **Ask for help**: Create an issue on GitHub with:
   - Your operating system
   - Java version (`java -version` output)
   - Complete error message
   - What you were trying to do

## 🎮 Quick Start for Impatient Developers

Just want to get started quickly?

1. Install IntelliJ IDEA Community Edition
2. Install Java 21
3. Open the project folder in IntelliJ
4. Wait for it to import (grab a coffee ☕)
5. Use the Gradle panel to build
6. Done! 🎉

---

**Remember**: Minecraft modding can be complex, but don't give up! Every expert was once a beginner. 🌟 