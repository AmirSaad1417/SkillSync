@echo off
setlocal
for /f "delims=" %%I in ('powershell -NoProfile -Command "(Get-ChildItem -Recurse '%USERPROFILE%\.m2\repository\org\openjfx' -Filter '*.jar' | Select-Object -ExpandProperty FullName) -join ';'"') do set JFX_JARS=%%I

echo [1/4] Running MainShellVerification...
java -cp "target\classes;target\test-classes;%JFX_JARS%" com.skillsync.ui.MainShellVerification
if errorlevel 1 exit /b 1
echo.

echo [2/4] Running StudentFlowVerification...
java -cp "target\classes;target\test-classes;%JFX_JARS%" com.skillsync.ui.StudentFlowVerification
if errorlevel 1 exit /b 1
echo.

echo [3/4] Running PeerMatchingLifecycleTest...
java -cp "target\classes;target\test-classes;%JFX_JARS%" com.skillsync.ui.PeerMatchingLifecycleTest
if errorlevel 1 exit /b 1
echo.

echo [4/4] Running AdminPortalVerification...
java -cp "target\classes;target\test-classes;%JFX_JARS%" com.skillsync.ui.AdminPortalVerification
if errorlevel 1 exit /b 1
echo.

echo ========================================
echo ALL VERIFICATION SUITES PASSED!
echo ========================================

