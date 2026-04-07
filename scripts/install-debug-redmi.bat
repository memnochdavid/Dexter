@echo off
echo === Instalando DEBUG en REDMI ===
cd /d "%~dp0.."
call gradlew.bat installDebug
for /f "tokens=1" %%d in ('adb devices ^| findstr /v "emulator List attached"') do (
    echo Lanzando en %%d...
    adb -s %%d shell am start -n com.david.pokedex_api/.MainActivity
    goto :done
)
echo No se encontro ningun dispositivo fisico.
:done
pause
