@echo off
echo Building Chipper Chopper Mod...
echo.

REM Change to project root directory 
cd /d "%~dp0.."

REM Check if local Gradle exists
if exist "gradle-8.8\bin\gradle.bat" (
    echo Using local Gradle 8.8...
    call "gradle-8.8\bin\gradle.bat" build
) else (
    echo System Gradle not found. Setting up Gradle...
    echo Downloading Gradle 8.8...
    powershell -Command "& { Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-8.8-bin.zip' -OutFile 'gradle-8.8-bin.zip' }"
    
    if exist "gradle-8.8-bin.zip" (
        echo Extracting Gradle...
        powershell -Command "& { Expand-Archive -Path 'gradle-8.8-bin.zip' -DestinationPath '.' -Force }"
        del "gradle-8.8-bin.zip"
        echo Gradle setup complete!
        call "gradle-8.8\bin\gradle.bat" build
    ) else (
        echo Failed to download Gradle
    )
)

if %ERRORLEVEL% EQU 0 (
    echo.
    echo [SUCCESS] BUILD SUCCESSFUL!
    echo.
    echo [JAR] Mod JAR created: build\libs\chipper-chopper-1.0.0.jar
    echo [JAR] Sources JAR: build\libs\chipper-chopper-1.0.0-sources.jar
    echo.
    echo [INFO] Copy the mod JAR to your Minecraft mods folder to use it!
) else (
    echo.
    echo [ERROR] Build failed! Check the output above for errors.
)

echo.
pause 