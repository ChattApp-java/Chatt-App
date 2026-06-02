@echo off
chcp 65001 >nul
title WhatsApp Java - ENSA TETOUAN

echo ============================================
echo   WHATSAPP JAVA - LANCEMENT COMPLET
echo ============================================
echo.

java --module-path "lib" --add-modules javafx.controls,javafx.fxml,javafx.media,javafx.graphics,javafx.swing -jar TPchatJavaFX.jar

echo.
echo ============================================
echo   Application terminee.
echo ============================================
pause