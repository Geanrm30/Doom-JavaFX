package doom.wad;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Lector de archivos WAD de Doom (IWAD/PWAD). Solo lee el directorio al abrirlo y trae cada
 * pieza (lump) bajo demanda, asi que un WAD de 14 MB no se carga entero en memoria.
 */
public final class Wad {

    /** Datos crudos de un mapa: las piezas que necesita el conversor. */
    public static final class MapData {
        public final String name;
        public final byte[] things, linedefs, sidedefs, vertexes, sectors;

        MapData(String name, byte[] things, byte[] linedefs, byte[] sidedefs, byte[] vertexes, byte[] sectors) {
            this.name = name;
            this.things = things;
            this.linedefs = linedefs;
            this.sidedefs = sidedefs;
            this.vertexes = vertexes;
            this.sectors = sectors;
        }
    }

    private record Entry(String name, long offset, int size) {}

    public final Path path;
    private final List<Entry> dir = new ArrayList<>();
    private final List<String> maps = new ArrayList<>();

    private Wad(Path path) {
        this.path = path;
    }

    /** Abre un WAD; devuelve null si el archivo no es un WAD valido. */
    public static Wad open(Path path) {
        Wad w = new Wad(path);
        try (RandomAccessFile f = new RandomAccessFile(path.toFile(), "r")) {
            if (f.length() < 12) return null;
            byte[] head = new byte[12];
            f.readFully(head);
            String id = new String(head, 0, 4, StandardCharsets.US_ASCII);
            if (!id.equals("IWAD") && !id.equals("PWAD")) return null;
            ByteBuffer hb = ByteBuffer.wrap(head).order(ByteOrder.LITTLE_ENDIAN);
            int count = hb.getInt(4);
            long dirPos = hb.getInt(8) & 0xFFFFFFFFL;
            if (count < 0 || count > 200_000 || dirPos + 16L * count > f.length()) return null;
            byte[] raw = new byte[16 * count];
            f.seek(dirPos);
            f.readFully(raw);
            ByteBuffer b = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < count; i++) {
                long off = b.getInt(i * 16) & 0xFFFFFFFFL;
                int size = b.getInt(i * 16 + 4);
                String name = new String(raw, i * 16 + 8, 8, StandardCharsets.US_ASCII);
                int z = name.indexOf('\0');
                if (z >= 0) name = name.substring(0, z);
                w.dir.add(new Entry(name.toUpperCase(), off, size));
            }
        } catch (IOException e) {
            return null;
        }
        for (int i = 0; i < w.dir.size(); i++) {
            String n = w.dir.get(i).name;
            if (isMapName(n) && w.find("LINEDEFS", i + 1, 3) >= 0 && w.find("SECTORS", i + 1, 10) >= 0) w.maps.add(n);
        }
        return w.maps.isEmpty() ? null : w;
    }

    static boolean isMapName(String n) {
        if (n.length() == 4 && n.charAt(0) == 'E' && n.charAt(2) == 'M'
                && Character.isDigit(n.charAt(1)) && Character.isDigit(n.charAt(3))) return true;
        return n.length() == 5 && n.startsWith("MAP") && Character.isDigit(n.charAt(3)) && Character.isDigit(n.charAt(4));
    }

    /** Busca una pieza por nombre en las `span` entradas siguientes a `from`. */
    private int find(String name, int from, int span) {
        for (int i = from; i < Math.min(dir.size(), from + span); i++) {
            if (dir.get(i).name.equals(name)) return i;
        }
        return -1;
    }

    public List<String> maps() {
        return maps;
    }

    private byte[] read(int index) {
        Entry e = dir.get(index);
        if (e.size() <= 0) return new byte[0];
        try (RandomAccessFile f = new RandomAccessFile(path.toFile(), "r")) {
            if (e.offset() + e.size() > f.length()) return new byte[0];
            byte[] out = new byte[e.size()];
            f.seek(e.offset());
            f.readFully(out);
            return out;
        } catch (IOException ex) {
            return new byte[0];
        }
    }

    /** Lee las piezas de un mapa, o null si falta alguna. */
    public MapData readMap(String mapName) {
        int at = find(mapName, 0, dir.size());
        if (at < 0) return null;
        int t = find("THINGS", at + 1, 10), l = find("LINEDEFS", at + 1, 10), sd = find("SIDEDEFS", at + 1, 10);
        int v = find("VERTEXES", at + 1, 10), s = find("SECTORS", at + 1, 12);
        if (t < 0 || l < 0 || sd < 0 || v < 0 || s < 0) return null;
        return new MapData(mapName, read(t), read(l), read(sd), read(v), read(s));
    }

    /** Nombre del archivo, para mostrarlo en el menu. */
    public String displayName() {
        return path.getFileName().toString();
    }
}
