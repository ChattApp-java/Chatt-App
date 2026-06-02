@echo off
chcp 65001 >nul
title WhatsApp Java - CLIENT SEUL

echo ============================================
echo   WHATSAPP JAVA - CLIENT ADDITIONNEL
echo ============================================
echo.

java --module-path "lib" --add-modules javafx.controls,javafx.fxml,javafx.media,javafx.graphics,javafx.swing -jar TPchatJavaFX.jar

echo.
echo ============================================
echo   Client ferme.
echo ============================================
pause