@echo off
echo === Instalando RELEASE en EMULADOR ===
cd /d "%~dp0.."
call gradlew.bat installRelease
for /f "tokens=1" %%d in ('adb devices ^| findstr emulator') do (
    echo Lanzando en %%d...
    adb -s %%d shell am start -n com.david.pokedex_api/.MainActivity
    goto :done
)
echo No se encontro ningun emulador.
:done
pause
