#!/usr/bin/env python3
"""
Generador de niveles para DOOM JavaFX.

Escribe src/main/resources/levels/levelN.txt. Cada nivel es una cuadricula de
salas conectadas, con puertas, llaves roja/azul, enemigos, items y una salida.
Cada nivel se valida (todo es alcanzable, las llaves estan antes de sus puertas)
y se regenera con otra semilla si falla.

Uso:  python tools/generate_levels.py
Leyenda del mapa:
  1-6 paredes, D puerta, R puerta roja, U puerta azul, X salida, . suelo
  P inicio | z zombi, i imp, d demonio, c cacodemonio, B baron
  J baron jefe, Y ciberdemonio (jefes: guardan la salida, que no abre hasta matarlos)
  H megasalud, v invulnerabilidad, o barril explosivo
  h botiquin, s estimulante, a armadura, k balas, e cartuchos
  g escopeta, m ametralladora, r llave roja, u llave azul
  S puerta secreta (parece una pared; se abre con el boton de usar)
"""
import os
import random
from collections import deque

S = 9  # tamano de celda de sala (incluye una pared)
THING_CHARS = set("Pzidc" + "BYJhsakegmruHvo")

LEVELS = [
    dict(name="Hangar", rows=3, cols=3, keys=0, ceil=(26, 26, 40), floor=(58, 54, 50), fog=0.20,
         walls="12", enemies=(1, 2), mix=dict(z=5, i=2), guns=[], arm=0.0),
    dict(name="Planta Nuclear", rows=3, cols=3, keys=1, ceil=(20, 32, 32), floor=(44, 62, 56), fog=0.22,
         walls="23", enemies=(1, 3), mix=dict(z=3, i=4), guns=["g"], arm=0.2),
    dict(name="Refineria de Toxinas", rows=3, cols=4, keys=1, ceil=(18, 36, 22), floor=(50, 62, 40), fog=0.24,
         walls="35", enemies=(1, 3), mix=dict(z=2, i=4, d=2), guns=[], arm=0.3, bosses="J"),
    dict(name="Centro de Mando", rows=4, cols=4, keys=2, ceil=(30, 24, 32), floor=(66, 50, 46), fog=0.24,
         walls="24", enemies=(1, 3), mix=dict(z=2, i=4, d=3, c=1), guns=["m"], arm=0.3),
    dict(name="Laboratorios Fobos", rows=4, cols=4, keys=2, ceil=(14, 24, 40), floor=(40, 52, 70), fog=0.26,
         walls="25", enemies=(2, 3), mix=dict(z=1, i=3, d=3, c=2), guns=[], arm=0.35, bosses="J"),
    dict(name="Almacen de Residuos", rows=4, cols=5, keys=2, ceil=(30, 30, 14), floor=(70, 66, 36), fog=0.27,
         walls="43", enemies=(2, 3), mix=dict(z=1, i=3, d=3, c=3), guns=[], arm=0.35, bosses="JJ"),
    dict(name="Complejo Infernal", rows=4, cols=5, keys=2, ceil=(46, 12, 10), floor=(72, 30, 24), fog=0.30,
         walls="16", enemies=(2, 4), mix=dict(i=3, d=4, c=3, B=1), guns=[], arm=0.4, bosses="J"),
    dict(name="Santuario del Dolor", rows=5, cols=5, keys=2, ceil=(50, 8, 8), floor=(60, 20, 16), fog=0.32,
         walls="6", enemies=(2, 4), mix=dict(i=2, d=4, c=4, B=2), guns=[], arm=0.45, bosses="JJ"),
    dict(name="Camara Final", rows=4, cols=4, keys=2, ceil=(30, 0, 0), floor=(42, 10, 10), fog=0.35,
         walls="61", enemies=(2, 4), mix=dict(d=3, c=3, B=3), guns=[], arm=0.5, bosses="YJ"),
]


def neighbors(r, c, rows, cols):
    for dr, dc in ((-1, 0), (1, 0), (0, -1), (0, 1)):
        nr, nc = r + dr, c + dc
        if 0 <= nr < rows and 0 <= nc < cols:
            yield nr, nc


def edge_key(a, b):
    return (a, b) if a <= b else (b, a)


def build(cfg, rng):
    rows, cols = cfg["rows"], cfg["cols"]
    W, H = cols * S + 1, rows * S + 1

    # 1. arbol de salas (DFS aleatorio) desde la sala de inicio (abajo-izquierda)
    start = (rows - 1, 0)
    parent = {start: None}
    depth = {start: 0}
    tree = set()
    stack = [start]
    while stack:
        cur = stack[-1]
        opts = [n for n in neighbors(*cur, rows, cols) if n not in parent]
        if not opts:
            stack.pop()
            continue
        nxt = rng.choice(opts)
        parent[nxt] = cur
        depth[nxt] = depth[cur] + 1
        tree.add(edge_key(cur, nxt))
        stack.append(nxt)

    exit_room = max(depth, key=lambda k: (depth[k], rng.random()))
    path = [exit_room]
    while parent[path[-1]] is not None:
        path.append(parent[path[-1]])
    path.reverse()  # start ... exit
    if len(path) < cfg["keys"] + 2:
        return None

    # 2. cerraduras sobre el camino
    locks = {}  # edge -> 'R' / 'U'
    if cfg["keys"] >= 1:
        locks[edge_key(path[int(len(path) * 0.35)], path[int(len(path) * 0.35) + 1])] = "R"
    if cfg["keys"] >= 2:
        i = max(int(len(path) * 0.7), int(len(path) * 0.35) + 1)
        if i + 1 >= len(path):
            return None
        locks[edge_key(path[i], path[i + 1])] = "U"

    # 3. zonas = cuantas cerraduras se cruzan desde el inicio
    zone = {start: 0}
    q = deque([start])
    while q:
        cur = q.popleft()
        for n in neighbors(*cur, rows, cols):
            e = edge_key(cur, n)
            if e in tree and n not in zone:
                zone[n] = zone[cur] + (1 if e in locks else 0)
                q.append(n)

    # 4. conexiones extra (bucles) solo dentro de la misma zona
    edges = {e: locks.get(e, "") for e in tree}
    for r in range(rows):
        for c in range(cols):
            for n in neighbors(r, c, rows, cols):
                e = edge_key((r, c), n)
                if e not in edges and zone[(r, c)] == zone[n] and rng.random() < 0.35:
                    edges[e] = ""
    for e, k in list(edges.items()):
        if not k:
            edges[e] = "D" if rng.random() < 0.45 else "."

    # 5. paredes y salas
    grid = [["1"] * W for _ in range(H)]
    rooms = {}
    for r in range(rows):
        for c in range(cols):
            wc = rng.choice(cfg["walls"])
            rooms[(r, c)] = wc
            for y in range(r * S, r * S + S + 1):
                for x in range(c * S, c * S + S + 1):
                    grid[y][x] = wc
    pillar = {"1": "3", "2": "3", "3": "4", "4": "2", "5": "3", "6": "1"}
    for (r, c), wc in rooms.items():
        x0, y0 = c * S, r * S
        for y in range(y0 + 1, y0 + S):
            for x in range(x0 + 1, x0 + S):
                grid[y][x] = "."
        pc = pillar[wc]
        for cx, cy in ((1, 1), (S - 1, 1), (1, S - 1), (S - 1, S - 1)):
            if rng.random() < 0.3:
                k = rng.choice((2, 3))
                for dy in range(k):
                    for dx in range(k):
                        x = x0 + (cx + dx if cx == 1 else cx - dx)
                        y = y0 + (cy + dy if cy == 1 else cy - dy)
                        grid[y][x] = pc
        if rng.random() < 0.3:
            for dy in (4, 5):
                for dx in (4, 5):
                    grid[y0 + dy][x0 + dx] = pc

    # 6. puertas / aberturas
    def door_cell(a, b):
        (ar, ac), (br, bc) = a, b
        if ar == br:  # vecinos horizontales
            return (max(ac, bc) * S, ar * S + 4)
        return (ac * S + 4, max(ar, br) * S)

    connected = {rc: set() for rc in rooms}
    for (a, b), kind in edges.items():
        x, y = door_cell(a, b)
        grid[y][x] = kind
        connected[a].add(b)
        connected[b].add(a)

    # 7. salida: pared sin conexion de la sala final
    er, ec = exit_room
    sides = []
    if er == 0 or (er - 1, ec) not in connected[exit_room]:
        sides.append((ec * S + 4, er * S, ec * S + 4, er * S + 1))
    if (er + 1, ec) not in connected[exit_room]:
        sides.append((ec * S + 4, (er + 1) * S, ec * S + 4, (er + 1) * S - 1))
    if (er, ec - 1) not in connected[exit_room]:
        sides.append((ec * S, er * S + 4, ec * S + 1, er * S + 4))
    if (er, ec + 1) not in connected[exit_room]:
        sides.append(((ec + 1) * S, er * S + 4, (ec + 1) * S - 1, er * S + 4))
    if not sides:
        return None
    xx, xy, ix, iy = rng.choice(sides)
    grid[xy][xx] = "X"
    exit_in = (ix, iy)

    # 8. cosas
    free = {}
    for (r, c) in rooms:
        cells = []
        for y in range(r * S + 1, r * S + S):
            for x in range(c * S + 1, c * S + S):
                if grid[y][x] == ".":
                    cells.append((x, y))
        rng.shuffle(cells)
        free[(r, c)] = cells
    free[exit_room] = [c for c in free[exit_room] if c != exit_in]

    things = []

    def put(room, ch):
        if not free[room]:
            return False
        x, y = free[room].pop()
        grid[y][x] = ch
        things.append((ch, x, y))
        return True

    # inicio
    sx, sy = free[start].pop()
    grid[sy][sx] = "P"
    first = [n for n in connected[start]][0]
    dx_, dy_ = door_cell(start, first)
    import math
    angle = math.degrees(math.atan2(dy_ - sy, dx_ - sx))

    mix = cfg["mix"]
    pool = [k for k, w in mix.items() for _ in range(w)]
    lo, hi = cfg["enemies"]
    non_start = [rc for rc in rooms if rc != start]
    for rc in non_start:
        n = rng.randint(lo, hi)
        if rc == exit_room:
            for ch in cfg.get("bosses", ""):
                put(rc, ch)
            if cfg.get("bosses"):
                put(rc, "h")
                put(rc, "h")
                put(rc, "k")
                put(rc, "k")
        for _ in range(n):
            put(rc, rng.choice(pool))
    # items
    have_shotgun = any("g" in LEVELS[i]["guns"] for i in range(0, LEVELS.index(cfg) + 1))
    for rc in rooms:
        if rng.random() < 0.9:
            if have_shotgun and rng.random() < 0.4:
                put(rc, "e")
            else:
                put(rc, "k")
        if rng.random() < 0.45:
            put(rc, "h")
        if rng.random() < 0.25:
            put(rc, "s")
        if rng.random() < cfg["arm"] * 0.5:
            put(rc, "a")
    # barriles explosivos
    for rc in rooms:
        for _ in range(rng.choice((0, 0, 1, 2))):
            put(rc, "o")
    # megasalud (desde el nivel 3) e invulnerabilidad (desde el nivel 5)
    idx = LEVELS.index(cfg) + 1
    if idx >= 3:
        put(rng.choice(non_start), "H")
    if idx >= 5:
        put(rng.choice(non_start), "v")
    # armas
    zone0 = [rc for rc in rooms if zone[rc] == 0 and rc != start] or [start]
    for g in cfg["guns"]:
        put(rng.choice(zone0), g)
    # llaves
    if cfg["keys"] >= 1:
        put(rng.choice([rc for rc in rooms if zone[rc] == 0] ), "r")
    if cfg["keys"] >= 2:
        z1 = [rc for rc in rooms if zone[rc] == 1]
        put(rng.choice(z1), "u")

    # armas pesadas y su municion (se colocan al final para no alterar el resto del nivel)
    if idx >= 5:
        put(rng.choice(zone0), "l")
        for rc in rooms:
            if rng.random() < 0.3:
                put(rc, "w")
    if idx >= 7:
        put(rng.choice(zone0), "q")
        for rc in rooms:
            if rng.random() < 0.35:
                put(rc, "n")

    # cuartos secretos al este del mapa: puerta secreta 'S' (imita la pared) y 3 premios
    if idx >= 2:
        want = 2 if idx >= 5 else 1
        extra = 4
        for y in range(H):
            wc = rooms[(min(y // S, rows - 1), cols - 1)]
            grid[y].extend([wc] * extra)
        east = [r for r in range(rows) if grid[r * S + 4][W - 2] == "." and grid[r * S + 4][W - 1] in "123456"]
        rng.shuffle(east)
        for r in east[:want]:
            y0 = r * S + 4
            grid[y0][W - 1] = "S"
            for dy in (-1, 0, 1):
                for dx in range(3):
                    grid[y0 + dy][W + dx] = "."
            loot = ["H" if idx >= 3 else "h", "a", "e" if have_shotgun else "k"]
            for dx, ch in enumerate(loot):
                grid[y0][W + dx] = ch
                things.append((ch, W + dx, y0))
        W += extra

    return dict(grid=grid, angle=angle, exit_in=exit_in, things=things, W=W, H=H, start=(sx, sy))


def validate(lv):
    grid, W, H = lv["grid"], lv["W"], lv["H"]
    keys = set()
    seen = set()
    changed = True
    while changed:
        changed = False
        seen = set()
        q = deque([lv["start"]])
        seen.add(lv["start"])
        while q:
            x, y = q.popleft()
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if not (0 <= nx < W and 0 <= ny < H) or (nx, ny) in seen:
                    continue
                ch = grid[ny][nx]
                if ch in "123456X":
                    continue
                if ch == "R" and "r" not in keys:
                    continue
                if ch == "U" and "u" not in keys:
                    continue
                seen.add((nx, ny))
                q.append((nx, ny))
        for ch, x, y in lv["things"]:
            if ch in "ru" and (x, y) in seen and ch not in keys:
                keys.add(ch)
                changed = True
    for ch, x, y in lv["things"]:
        if (x, y) not in seen:
            return False
    return lv["exit_in"] in seen


def write(idx, cfg, lv):
    path = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "levels",
                        f"level{idx}.txt")
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(f"name={cfg['name']}\n")
        f.write("ceiling=%d,%d,%d\n" % cfg["ceil"])
        f.write("floor=%d,%d,%d\n" % cfg["floor"])
        f.write(f"fog={cfg['fog']}\n")
        f.write(f"angle={lv['angle']:.1f}\n")
        f.write("---\n")
        for row in lv["grid"]:
            f.write("".join(row) + "\n")


def main():
    for i, cfg in enumerate(LEVELS, start=1):
        for attempt in range(500):
            rng = random.Random(i * 1000 + attempt)
            lv = build(cfg, rng)
            if lv and validate(lv):
                write(i, cfg, lv)
                n_en = sum(1 for t in lv["things"] if t[0] in "zidcBYJ")
                print(f"nivel {i}: {cfg['name']:<22} {lv['W']}x{lv['H']}  enemigos={n_en}  semilla={i*1000+attempt}")
                break
        else:
            raise SystemExit(f"no se pudo generar el nivel {i}")


if __name__ == "__main__":
    main()
