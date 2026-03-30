"""
Convierte todos los .webm de una carpeta a Animated WebP con transparencia.
Requiere ffmpeg instalado y en el PATH.

Uso:
    python convert_webm_to_webp.py

Por defecto:
    - Entrada: ../app/src/main/res/raw/ (solo .webm)
    - Salida:  ./webp_output/

Cuando termine, copia los .webp a app/src/main/res/raw/ reemplazando/complementando los .webm.
"""

import subprocess
import sys
import os
import time
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor, as_completed

# ── Configuración ──────────────────────────────────────────────
SCRIPT_DIR = Path(__file__).resolve().parent
INPUT_DIR = SCRIPT_DIR / "webm"
OUTPUT_DIR = SCRIPT_DIR / "webp"
MAX_WORKERS = 8          # conversiones en paralelo
FFMPEG_CMD = "ffmpeg"    # cambiar si ffmpeg no está en PATH
# ───────────────────────────────────────────────────────────────


def check_ffmpeg():
    try:
        r = subprocess.run(
            [FFMPEG_CMD, "-version"],
            capture_output=True, text=True, timeout=10
        )
        first_line = r.stdout.split("\n")[0]
        print(f"  ffmpeg encontrado: {first_line}")
        return True
    except FileNotFoundError:
        print("ERROR: ffmpeg no encontrado en PATH.")
        print("  Instálalo desde https://ffmpeg.org/download.html")
        print("  o con:  winget install ffmpeg")
        return False
    except Exception as e:
        print(f"ERROR comprobando ffmpeg: {e}")
        return False


def convert_one(webm_path: Path, output_dir: Path) -> tuple[str, bool, str]:
    """Convierte un .webm a .webp. Devuelve (nombre, éxito, mensaje)."""
    name = webm_path.stem
    out_path = output_dir / f"{name}.webp"

    if out_path.exists():
        return (name, True, "ya existía, saltado")

    cmd = [
        FFMPEG_CMD,
        "-y",                       # sobreescribir sin preguntar
        "-i", str(webm_path),
        "-vcodec", "libwebp_anim",  # animated webp encoder
        "-lossless", "0",           # lossy
        "-quality", "75",           # buen balance calidad/tamaño
        "-loop", "0",               # loop infinito
        "-an",                      # sin audio
        "-vf", "colorkey=white:0.01:0.15,scale=400:-1,format=yuva420p",  # quitar fondo blanco + reducir + preservar alfa
        str(out_path)
    ]

    try:
        r = subprocess.run(
            cmd, capture_output=True, text=True, timeout=120
        )
        if r.returncode == 0 and out_path.exists():
            # Comparar tamaños
            orig_kb = webm_path.stat().st_size / 1024
            new_kb = out_path.stat().st_size / 1024
            return (name, True, f"{orig_kb:.0f}KB -> {new_kb:.0f}KB")
        else:
            err = r.stderr.strip().split("\n")[-1] if r.stderr else "sin detalle"
            return (name, False, err)
    except subprocess.TimeoutExpired:
        return (name, False, "timeout (>120s)")
    except Exception as e:
        return (name, False, str(e))


def main():
    input_dir = INPUT_DIR.resolve()
    output_dir = OUTPUT_DIR.resolve()

    print("=" * 60)
    print("  WebM → Animated WebP (con transparencia)")
    print("=" * 60)
    print(f"  Entrada: {input_dir}")
    print(f"  Salida:  {output_dir}")
    print()

    # Verificar ffmpeg
    if not check_ffmpeg():
        sys.exit(1)

    # Verificar directorio de entrada
    if not input_dir.exists():
        print(f"ERROR: No existe el directorio de entrada: {input_dir}")
        sys.exit(1)

    # Recoger todos los .webm
    webm_files = sorted(input_dir.glob("*.webm"))
    total = len(webm_files)
    if total == 0:
        print("No se encontraron archivos .webm")
        sys.exit(0)

    print(f"  Archivos .webm encontrados: {total}")
    print(f"  Workers paralelos: {MAX_WORKERS}")
    print()

    # Crear directorio de salida
    output_dir.mkdir(parents=True, exist_ok=True)

    # Convertir en paralelo
    ok_count = 0
    fail_count = 0
    skip_count = 0
    start = time.time()

    with ThreadPoolExecutor(max_workers=MAX_WORKERS) as pool:
        futures = {
            pool.submit(convert_one, f, output_dir): f.stem
            for f in webm_files
        }

        for i, future in enumerate(as_completed(futures), 1):
            name, success, msg = future.result()
            if success:
                if "saltado" in msg:
                    skip_count += 1
                    symbol = "⏭"
                else:
                    ok_count += 1
                    symbol = "✓"
            else:
                fail_count += 1
                symbol = "✗"

            # Progreso
            pct = (i / total) * 100
            print(f"  [{i:4d}/{total}] {pct:5.1f}%  {symbol} {name:30s} {msg}")

    elapsed = time.time() - start

    # Resumen
    print()
    print("=" * 60)
    print(f"  Completado en {elapsed:.1f}s")
    print(f"  Convertidos: {ok_count}")
    print(f"  Saltados:    {skip_count}")
    print(f"  Fallidos:    {fail_count}")
    print("=" * 60)

    if fail_count > 0:
        print()
        print("  Revisa los errores arriba. Puedes re-ejecutar el script")
        print("  y solo procesará los que falten (salta los ya convertidos).")

    if ok_count > 0 or skip_count > 0:
        print()
        print(f"  Los .webp están en: {output_dir}")
        print("  Cópialos a app/src/main/res/raw/ cuando estés listo.")


if __name__ == "__main__":
    main()
