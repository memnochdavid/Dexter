"""
Descarga los sprites animados de Farfetch'd, Farfetch'd de Galar y Sirfetch'd
desde Wikidex y los convierte a Animated WebP con transparencia (canal alfa VP9).

Requiere:
    pip install requests
    ffmpeg con libvpx-vp9 y libwebp_anim

Uso:
    python download_farfetchd.py
"""

import subprocess
import sys
import time
from pathlib import Path

try:
    import requests
except ImportError:
    print("ERROR: Falta 'requests'. Instálalo con: pip install requests")
    sys.exit(1)

# ── Configuración ──────────────────────────────────────────────
SCRIPT_DIR = Path(__file__).resolve().parent
DOWNLOAD_DIR = SCRIPT_DIR / "webm_farfetchd"
OUTPUT_DIR = SCRIPT_DIR / "webp_farfetchd"
FFMPEG_CMD = "ffmpeg"
USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

# URLs directas de Wikidex CDN → nombre local en res/raw
SPRITES = {
    "farfetchd": "https://images.wikidexcdn.net/mwuploads/wikidex/d/d6/latest/20240103111601/Farfetch%E2%80%99d_HOME.webm",
    "farfetchd_shiny": "https://images.wikidexcdn.net/mwuploads/wikidex/0/0a/latest/20231228145407/Farfetch%E2%80%99d_HOME_variocolor.webm",
    "farfetchd_de_galar": "https://images.wikidexcdn.net/mwuploads/wikidex/e/ef/latest/20240103110944/Farfetch%E2%80%99d_de_Galar_HOME.webm",
    "farfetchd_de_galar_shiny": "https://images.wikidexcdn.net/mwuploads/wikidex/6/60/latest/20231228152026/Farfetch%E2%80%99d_de_Galar_HOME_variocolor.webm",
    "sirfetchd": "https://images.wikidexcdn.net/mwuploads/wikidex/transcoded/6/6a/Sirfetch%E2%80%99d_HOME.webm/Sirfetch%E2%80%99d_HOME.webm.720p.vp9.webm",
    "sirfetchd_shiny": "https://images.wikidexcdn.net/mwuploads/wikidex/9/91/latest/20231228115838/Sirfetch%E2%80%99d_HOME_variocolor.webm",
}
# ───────────────────────────────────────────────────────────────

SESSION = requests.Session()
SESSION.headers.update({"User-Agent": USER_AGENT})


def check_ffmpeg():
    try:
        r = subprocess.run(
            [FFMPEG_CMD, "-version"],
            capture_output=True, text=True, timeout=10
        )
        print(f"  ffmpeg: {r.stdout.split(chr(10))[0]}")
        return True
    except FileNotFoundError:
        print("ERROR: ffmpeg no encontrado en PATH.")
        return False


def download_file(url: str, path: Path) -> tuple[bool, str]:
    try:
        r = SESSION.get(url, timeout=120, stream=True)
        r.raise_for_status()
        with open(path, "wb") as f:
            for chunk in r.iter_content(8192):
                f.write(chunk)
        kb = path.stat().st_size / 1024
        return True, f"{kb:.0f}KB"
    except Exception as e:
        if path.exists():
            path.unlink()
        return False, str(e)


def convert_webm_to_webp(webm_path: Path, webp_path: Path) -> tuple[bool, str]:
    cmd = [
        FFMPEG_CMD,
        "-y",
        "-c:v", "libvpx-vp9",
        "-i", str(webm_path),
        "-vcodec", "libwebp_anim",
        "-lossless", "0",
        "-quality", "75",
        "-loop", "0",
        "-an",
        "-vf", "format=yuva420p,scale=400:-1",
        str(webp_path),
    ]
    try:
        r = subprocess.run(cmd, capture_output=True, text=True, timeout=120)
        if r.returncode == 0 and webp_path.exists():
            orig_kb = webm_path.stat().st_size / 1024
            new_kb = webp_path.stat().st_size / 1024
            return True, f"{orig_kb:.0f}KB → {new_kb:.0f}KB"
        else:
            err = r.stderr.strip().split("\n")[-1] if r.stderr else "error desconocido"
            return False, err
    except subprocess.TimeoutExpired:
        return False, "timeout (>120s)"
    except Exception as e:
        return False, str(e)


def main():
    print("=" * 60)
    print("  Farfetch'd line — Wikidex → Animated WebP (con alfa)")
    print("=" * 60)
    print()

    if not check_ffmpeg():
        sys.exit(1)

    DOWNLOAD_DIR.mkdir(parents=True, exist_ok=True)
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    print(f"  Sprites a procesar: {len(SPRITES)}")
    print()

    # ── Paso 1: Descargar ──
    print("[1/2] Descargando webm desde Wikidex CDN...")
    for local_name, url in SPRITES.items():
        webm_path = DOWNLOAD_DIR / f"{local_name}.webm"

        if webm_path.exists():
            print(f"  ⏭ {local_name} — ya descargado")
            continue

        print(f"  ↓ {local_name}...", end=" ")
        ok, msg = download_file(url, webm_path)
        print(f"{'✓' if ok else '✗'} {msg}")
        time.sleep(0.3)

    print()

    # ── Paso 2: Convertir ──
    print("[2/2] Convirtiendo a WebP con canal alfa...")
    ok_count = 0
    fail_count = 0

    for local_name in SPRITES:
        webm_path = DOWNLOAD_DIR / f"{local_name}.webm"
        webp_path = OUTPUT_DIR / f"{local_name}.webp"

        if not webm_path.exists():
            print(f"  ⏭ {local_name} — webm no disponible")
            continue

        if webp_path.exists():
            print(f"  ⏭ {local_name} — webp ya existe")
            ok_count += 1
            continue

        ok, msg = convert_webm_to_webp(webm_path, webp_path)
        if ok:
            ok_count += 1
            print(f"  ✓ {local_name}.webp — {msg}")
        else:
            fail_count += 1
            print(f"  ✗ {local_name}.webp — {msg}")

    print()
    print("=" * 60)
    print(f"  Convertidos: {ok_count}  |  Fallidos: {fail_count}")
    print(f"  WebP en: {OUTPUT_DIR}")
    print()
    print("  Copia los .webp a:")
    print("    app/src/main/res/raw/")
    print("=" * 60)


if __name__ == "__main__":
    main()
