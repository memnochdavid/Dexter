"""
Descarga todos los sprites animados de Pokémon HOME desde Wikidex
y los convierte a Animated WebP con transparencia (canal alfa VP9).

Usa la API MediaWiki de Wikidex para listar los archivos de la categoría
"Sprites animados de Pokémon HOME", descarga los originales del CDN
y los convierte con ffmpeg usando el decodificador libvpx-vp9.

Requisitos:
    pip install requests
    ffmpeg con libvpx-vp9 y libwebp_anim (ffmpeg estándar lo trae)

Uso:
    python download_home_sprites.py
"""

import subprocess
import sys
import re
import time
import unicodedata
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor, as_completed

try:
    import requests
except ImportError:
    print("ERROR: Falta 'requests'. Instálalo con: pip install requests")
    sys.exit(1)

# ── Configuración ──────────────────────────────────────────────
SCRIPT_DIR = Path(__file__).resolve().parent
DOWNLOAD_DIR = SCRIPT_DIR / "webm_original"
OUTPUT_DIR = SCRIPT_DIR / "webp"
PROJECT_DIR = Path("C:/Users/davdu/Desktop/Pokedex_API")
RES_RAW_DIR = PROJECT_DIR / "app" / "src" / "main" / "res" / "raw"

API_URL = "https://www.wikidex.net/api.php"
CATEGORY = "Categoría:Sprites animados de Pokémon HOME"

MAX_CONVERT_WORKERS = 8
FFMPEG_CMD = "ffmpeg"
DOWNLOAD_DELAY = 0.05
USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
# ───────────────────────────────────────────────────────────────


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


def wikidex_to_local_name(wikidex_filename: str) -> str:
    """
    Transforma nombre de archivo de Wikidex al nombre local de res/raw.
    """
    name = wikidex_filename
    name = re.sub(r"\.\w+$", "", name)       # quitar extensión
    name = name.replace(" ", "_")             # espacios → underscores (ANTES de quitar HOME)
    name = name.replace("_HOME", "")          # quitar _HOME
    name = name.replace("♀", "_hembra")
    name = name.replace("♂", "_macho")
    name = name.lower()
    name = name.replace("-", "_")
    name = name.replace(".", "")
    name = re.sub(r"[''ʼ']", "", name)
    name = unicodedata.normalize("NFD", name)
    name = "".join(c for c in name if unicodedata.category(c) != "Mn")
    while "__" in name:
        name = name.replace("__", "_")
    name = name.strip("_")
    return name


# ── API de Wikidex ─────────────────────────────────────────────

SESSION = requests.Session()
SESSION.headers.update({"User-Agent": USER_AGENT})


def api_get(params: dict, retries: int = 3) -> dict:
    for attempt in range(retries):
        r = SESSION.get(API_URL, params=params, timeout=30)
        if r.status_code == 429:
            wait = 2 ** (attempt + 1)  # 2, 4, 8 segundos
            print(f"\n    ⏳ Rate limited (429), esperando {wait}s...", end="\r")
            time.sleep(wait)
            continue
        r.raise_for_status()
        return r.json()
    r.raise_for_status()  # lanza error si agotamos reintentos


def fetch_category_files() -> list[str]:
    titles = []
    cmcontinue = None

    while True:
        params = {
            "action": "query",
            "list": "categorymembers",
            "cmtitle": CATEGORY,
            "cmtype": "file",
            "cmlimit": "500",
            "format": "json",
        }
        if cmcontinue:
            params["cmcontinue"] = cmcontinue

        data = api_get(params)
        members = data.get("query", {}).get("categorymembers", [])
        titles.extend(m["title"] for m in members)

        print(f"    {len(titles)} archivos listados...", end="\r")
        time.sleep(1)

        if "continue" in data:
            cmcontinue = data["continue"]["cmcontinue"]
        else:
            break

    print()
    return titles


def fetch_file_urls(titles: list[str]) -> dict[str, str]:
    urls = {}

    for i in range(0, len(titles), 50):
        batch = titles[i : i + 50]
        params = {
            "action": "query",
            "titles": "|".join(batch),
            "prop": "imageinfo",
            "iiprop": "url",
            "format": "json",
        }
        data = api_get(params)

        for page in data.get("query", {}).get("pages", {}).values():
            if "imageinfo" in page:
                title = page["title"]
                url = page["imageinfo"][0]["url"]
                filename = title.split(":", 1)[-1]
                urls[filename] = url

        print(f"    URLs: {len(urls)}/{len(titles)}", end="\r")
        time.sleep(1)  # pausa entre lotes para evitar 429

    print()
    return urls


# ── Descarga (CDN directo) ─────────────────────────────────────

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


# ── Conversión ─────────────────────────────────────────────────

def convert_one(webm_path: Path, webp_path: Path) -> tuple[bool, str]:
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


# ── Main ───────────────────────────────────────────────────────

def main():
    print("=" * 65)
    print("  Wikidex HOME Sprites → Animated WebP (con alfa VP9)")
    print("=" * 65)
    print()

    if not check_ffmpeg():
        sys.exit(1)

    DOWNLOAD_DIR.mkdir(parents=True, exist_ok=True)
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    # ── Cargar nombres locales esperados ──
    local_names = set()
    if RES_RAW_DIR.exists():
        for f in RES_RAW_DIR.iterdir():
            if f.suffix in (".webm", ".webp"):
                local_names.add(f.stem)
    if not local_names:
        try:
            r = subprocess.run(
                ["git", "ls-tree", "HEAD", "--name-only",
                 "app/src/main/res/raw/"],
                capture_output=True, text=True, cwd=str(PROJECT_DIR)
            )
            for line in r.stdout.strip().splitlines():
                p = Path(line)
                if p.suffix == ".webm":
                    local_names.add(p.stem)
        except Exception:
            pass

    print(f"  Nombres locales conocidos: {len(local_names)}")
    print()

    # ── Paso 1: Listar archivos de la categoría ──
    print("[1/4] Consultando categoría en Wikidex...")
    all_titles = fetch_category_files()
    print(f"  Total en categoría: {len(all_titles)}")

    webm_titles = [
        t for t in all_titles
        if t.lower().endswith(".webm") and "variocolor" not in t.lower()
    ]
    print(f"  Filtrados (sin variocolor): {len(webm_titles)}")
    print()

    # ── Paso 2: Obtener URLs originales ──
    print("[2/4] Obteniendo URLs de archivos originales...")
    file_urls = fetch_file_urls(webm_titles)
    print(f"  URLs obtenidas: {len(file_urls)}")
    print()

    # ── Construir mapeo wikidex_name → local_name ──
    mapping = {}
    unmatched_wikidex = []

    for wikidex_name in file_urls:
        local_name = wikidex_to_local_name(wikidex_name)
        if local_names and local_name not in local_names:
            unmatched_wikidex.append((wikidex_name, local_name))
        mapping[wikidex_name] = local_name

    if unmatched_wikidex:
        print(f"  ⚠ {len(unmatched_wikidex)} archivos de Wikidex sin match local:")
        for wname, lname in unmatched_wikidex[:20]:
            print(f"      {wname} → {lname}")
        if len(unmatched_wikidex) > 20:
            print(f"      ... y {len(unmatched_wikidex) - 20} más")

        report = SCRIPT_DIR / "unmatched_sprites.txt"
        with open(report, "w", encoding="utf-8") as f:
            f.write("# Archivos de Wikidex sin match en res/raw\n")
            f.write("# Formato: wikidex_name → local_name_generado\n\n")
            for wname, lname in sorted(unmatched_wikidex):
                f.write(f"{wname} → {lname}\n")
        print(f"      Lista completa en: {report}")
    print()

    if local_names:
        found_locals = set(mapping.values())
        missing = local_names - found_locals
        if missing:
            print(f"  ⚠ {len(missing)} archivos locales sin match en Wikidex:")
            for name in sorted(missing)[:20]:
                print(f"      {name}")
            if len(missing) > 20:
                print(f"      ... y {len(missing) - 20} más")

            report2 = SCRIPT_DIR / "missing_from_wikidex.txt"
            with open(report2, "w", encoding="utf-8") as f:
                f.write("# Archivos locales sin match en Wikidex\n\n")
                for name in sorted(missing):
                    f.write(f"{name}\n")
            print(f"      Lista completa en: {report2}")
        print()

    # ── Paso 3: Descargar originales del CDN ──
    print(f"[3/4] Descargando webm originales a {DOWNLOAD_DIR.name}/...")
    dl_ok = 0
    dl_skip = 0
    dl_fail = 0
    total_dl = len(file_urls)

    for i, (wikidex_name, url) in enumerate(file_urls.items(), 1):
        webm_path = DOWNLOAD_DIR / wikidex_name

        if webm_path.exists():
            dl_skip += 1
            if i % 100 == 0:
                print(f"    [{i}/{total_dl}] descargados:{dl_ok} saltados:{dl_skip}", end="\r")
            continue

        ok, msg = download_file(url, webm_path)
        if ok:
            dl_ok += 1
        else:
            dl_fail += 1
            print(f"    ✗ {wikidex_name}: {msg}")

        if i % 50 == 0:
            print(f"    [{i}/{total_dl}] descargados:{dl_ok} saltados:{dl_skip} fallidos:{dl_fail}", end="\r")

        time.sleep(DOWNLOAD_DELAY)

    print(f"\n  Descargados: {dl_ok}  |  Saltados: {dl_skip}  |  Fallidos: {dl_fail}")
    print()

    # ── Paso 4: Convertir a WebP ──
    print(f"[4/4] Convirtiendo a WebP con alfa ({MAX_CONVERT_WORKERS} workers)...")

    convert_jobs = []
    for wikidex_name, local_name in mapping.items():
        webm_path = DOWNLOAD_DIR / wikidex_name
        webp_path = OUTPUT_DIR / f"{local_name}.webp"

        if not webm_path.exists():
            continue
        if webp_path.exists():
            continue

        convert_jobs.append((webm_path, webp_path, wikidex_name, local_name))

    cv_ok = 0
    cv_fail = 0
    cv_skip = len(mapping) - len(convert_jobs)
    total_cv = len(convert_jobs)

    if total_cv == 0:
        print("  Nada que convertir (ya existen todos los webp).")
    else:
        with ThreadPoolExecutor(max_workers=MAX_CONVERT_WORKERS) as pool:
            futures = {}
            for webm_path, webp_path, wname, lname in convert_jobs:
                fut = pool.submit(convert_one, webm_path, webp_path)
                futures[fut] = (wname, lname)

            for i, fut in enumerate(as_completed(futures), 1):
                wname, lname = futures[fut]
                ok, msg = fut.result()

                if ok:
                    cv_ok += 1
                    sym = "✓"
                else:
                    cv_fail += 1
                    sym = "✗"

                pct = (i / total_cv) * 100
                print(f"    [{i:4d}/{total_cv}] {pct:5.1f}%  {sym} {lname}.webp  {msg}")

    print()
    print("=" * 65)
    print(f"  Descargados:  {dl_ok} nuevos, {dl_skip} ya existían")
    print(f"  Convertidos:  {cv_ok} nuevos, {cv_skip} ya existían, {cv_fail} fallidos")
    print(f"  WebP en:      {OUTPUT_DIR}")
    print()
    print("  Cuando estés listo, copia los .webp a:")
    print(f"    {RES_RAW_DIR}")
    print("=" * 65)


if __name__ == "__main__":
    main()
