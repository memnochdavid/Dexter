"""
Renombra los .webp quitando '_home' del nombre para que coincidan
con los nombres esperados en res/raw/.

Uso:
    python rename_webp.py
"""

from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
WEBP_DIR = SCRIPT_DIR / "webp"


def main():
    files = sorted(WEBP_DIR.glob("*.webp"))
    print(f"Archivos encontrados: {len(files)}")

    renamed = 0
    skipped = 0
    errors = []

    for f in files:
        new_name = f.name.replace("_home", "")

        if f.name == new_name:
            skipped += 1
            continue

        new_path = f.parent / new_name

        if new_path.exists() and new_path != f:
            errors.append(f"  CONFLICTO: {f.name} → {new_name} (ya existe)")
            continue

        f.rename(new_path)
        renamed += 1

    print(f"Renombrados: {renamed}")
    print(f"Ya correctos: {skipped}")
    if errors:
        print(f"Errores: {len(errors)}")
        for e in errors:
            print(e)


if __name__ == "__main__":
    main()
