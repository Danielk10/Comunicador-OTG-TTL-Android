#!/usr/bin/env bash
set -euo pipefail

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VTTY="${BASE_DIR}/vtty"

echo "=== 1. Limpiando procesos previos del emulador ==="
pkill -f "emulador_picmem.py" || true
rm -f "$VTTY"

echo "=== 2. Iniciando el emulador PICMEM v3 en segundo plano ==="
python3 "${BASE_DIR}/emulador_picmem.py" &
EMU_PID=$!

cleanup() {
    echo "=== 4. Deteniendo el emulador PICMEM v3 (PID $EMU_PID) ==="
    kill $EMU_PID 2>/dev/null || true
    rm -f "$VTTY"
}
trap cleanup EXIT

echo "Esperando creación del puerto virtual vtty..."
for i in {1..20}; do
    if [ -L "$VTTY" ] && [ -e "$VTTY" ]; then
        break
    fi
    sleep 0.2
done

if [ ! -e "$VTTY" ]; then
    echo "[ERROR] El puerto virtual $VTTY no se creó a tiempo."
    exit 1
fi

echo "Puerto virtual listo en: $(readlink -f "$VTTY")"
sleep 0.5

echo "=== 3. Ejecutando pruebas automatizadas del protocolo PICMEM ==="
python3 "${BASE_DIR}/probar_emulacion.py"

echo "=========================================================="
echo "¡PRUEBAS DE EMULACIÓN DE COMUNICADOR-OTG COMPLETADAS CON ÉXITO!"
echo "=========================================================="
