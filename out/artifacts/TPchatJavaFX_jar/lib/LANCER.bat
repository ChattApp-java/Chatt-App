@echo off
chcp 65001 >nul
title WhatsApp Java - ENSA TETOUAN
color 0A

echo ============================================
echo   WHATSAPP JAVA - GInfo 2025/2026
echo ============================================
echo.

:: Vérifier que le JAR existe
if not exist "TPchatJavaFX.jar" (
    echo [ERREUR] TPchatJavaFX.jar non trouve !
    pause
    exit /b 1
)

:: Vérifier le dossier lib
if not exist "lib" (
    echo [ERREUR] Dossier 'lib' non trouve !
    pause
    exit /b 1
)

echo [OK] JAR et librairies detectes
echo [OK] Lancement en cours...
echo.

:: Lancement avec modules JavaFX
java --module-path "lib" --add-modules javafx.controls,javafx.fxml,javafx.media,javafx.web,javafx.graphics -jar TPchatJavaFX.jar

echo.
echo ============================================
echo   Application terminee.
echo ============================================
pause