@echo off
cd /d %~dp0
if not exist out mkdir out
javac -d out *.java && java -cp out Main
pause
