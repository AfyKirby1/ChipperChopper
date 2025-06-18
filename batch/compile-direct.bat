@echo off
echo Direct Compilation Attempt for Chipper Chopper Mod...
echo.

REM Change to project root directory 
cd /d "%~dp0.."

echo WARNING: This is a direct compilation attempt without proper Fabric dependencies.
echo It will likely fail, but helps test basic Java syntax.
echo.

REM Check if Java is installed
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java 21 or higher
    pause
    exit /b 1
)

REM Create build directories
if not exist "temp_build" mkdir "temp_build"
if not exist "temp_build\classes" mkdir "temp_build\classes"

echo Attempting to compile main source files...
echo.

REM Try to compile main java files (will fail due to missing Minecraft/Fabric dependencies)
javac -d temp_build\classes src\main\java\com\example\chipper_chopper\*.java 2>compile_errors.txt

echo Compilation attempt complete.
echo.

if exist "compile_errors.txt" (
    echo Compilation errors (expected due to missing dependencies):
    type compile_errors.txt
    echo.
    echo This is normal - you need Gradle and Fabric dependencies to compile properly.
) else (
    echo Surprisingly, no compilation errors found!
)

echo.
echo For proper compilation, use:
echo   batch\build.bat
echo.
echo Cleaning up temporary files...
if exist "temp_build" rmdir /s /q "temp_build"
if exist "compile_errors.txt" del "compile_errors.txt"

pause 