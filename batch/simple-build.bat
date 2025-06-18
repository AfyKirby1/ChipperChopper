@echo off
echo Simple Build for Chipper Chopper Mod...
echo Compiling Java sources...
echo.

REM Change to project root directory 
cd /d "%~dp0.."

echo NOTE: This is a simplified build that shows the project structure.
echo For a full build, you need Gradle with proper Fabric dependencies.
echo.

echo Source files found:
if exist "src\main\java\com\example\chipper_chopper\*.java" (
    dir /b "src\main\java\com\example\chipper_chopper\*.java"
) else (
    echo No Java files found in expected location
)

echo.
echo Client files found:
if exist "src\client\java\com\example\chipper_chopper\*.java" (
    dir /b "src\client\java\com\example\chipper_chopper\*.java"
)

echo.
echo Your mod source code is ready!
echo To build it properly, you need Gradle.
echo.
echo Try these options:
echo 1. Install Gradle manually and run 'gradle build'
echo 2. Use IntelliJ IDEA to import this Gradle project  
echo 3. Use the Fabric development environment
echo.
pause 