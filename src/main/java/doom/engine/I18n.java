package doom.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Traduccion al ingles. Los textos del juego estan escritos en espanol; en modo ingles se traducen
 * al mostrarlos, primero por frase completa y luego sustituyendo fragmentos (del mas largo al mas corto).
 */
public final class I18n {

    private I18n() { }

    public static volatile boolean english;

    private static final Map<String, String> EXACT = new HashMap<>();
    private static final List<String[]> PARTS = new ArrayList<>();
    private static final Map<String, String> CACHE = new HashMap<>();

    private static void exact(String es, String en) { EXACT.put(es, en); }

    private static final List<Object[]> RULES = new ArrayList<>();

    private static void part(String es, String en) {
        PARTS.add(new String[]{es, en});
    }

    /** Conserva el estilo del texto original: TODO MAYUSCULAS, Capitalizado o minusculas. */
    private static String like(String matched, String en) {
        boolean anyLetter = false, allUpper = true;
        for (char c : matched.toCharArray()) {
            if (Character.isLetter(c)) {
                anyLetter = true;
                if (!Character.isUpperCase(c)) allUpper = false;
            }
        }
        if (anyLetter && allUpper && matched.length() > 2) return en.toUpperCase(Locale.ROOT);
        if (!matched.isEmpty() && Character.isUpperCase(matched.charAt(0))) {
            return Character.toUpperCase(en.charAt(0)) + en.substring(1);
        }
        return en;
    }

    static {
        exact("Giro con el raton: SI", "Mouse look: ON");
        exact("Giro con el raton: NO", "Mouse look: OFF");
        exact("Giro del raton sin recentrado (mueve el raton dentro de la ventana)",
                "Mouse turning without recentering (keep the mouse inside the window)");
        exact("W A S D mover   raton o Q E girar   SHIFT correr   clic o ESPACIO disparar",
                "W A S D move   mouse or Q E turn   SHIFT run   click or SPACE fire");
        exact("F usar   R recargar   1-6 o TAB armas   N minimapa   P o ESC pausa   F11 pantalla",
                "F use   R reload   1-6 or TAB weapons   N minimap   P or ESC pause   F11 screen");
        exact("ARRIBA/ABAJO mover    ENTER elegir    IZQ/DER cambiar    ESC volver",
                "UP/DOWN move    ENTER select    LEFT/RIGHT change    ESC back");
        exact("Mueve el raton y suelta TAB (o haz clic) para equipar",
                "Move the mouse and release TAB (or click) to equip");

        // frases largas primero (el orden final se fija por longitud)
        part("seleccionar nivel", "select level");
        part("termina el nivel anterior para desbloquearlo", "finish the previous level to unlock it");
        part("sensibilidad del raton", "mouse sensitivity");
        part("giro con el raton", "mouse look");
        part("pantalla completa", "fullscreen");
        part("borrar progreso", "reset progress");
        part("seguro? pulsa enter otra vez", "sure? press enter again");
        part("progreso borrado", "progress erased");
        part("volver al titulo", "back to title");
        part("salir del juego", "quit game");
        part("no tienes esa arma", "you don't have that weapon");
        part("has encontrado un secreto", "you found a secret");
        part("derrota al jefe para abrir la salida", "defeat the boss to open the exit");
        part("jefe derrotado: la salida esta abierta", "boss defeated: the exit is open");
        part("sin municion: usa la katana", "out of ammo: use the katana");
        part("sin municion", "out of ammo");
        part("necesitas la llave roja", "you need the red key");
        part("necesitas la llave azul", "you need the blue key");
        part("llave roja", "red key");
        part("llave azul", "blue key");
        part("llaves obtenidas", "keys obtained");
        part("llaves", "keys");
        part("megasalud: salud al maximo y armadura", "megahealth: health to maximum and armor");
        part("invulnerable durante 12 segundos", "invulnerable for 12 seconds");
        part("botiquin", "medkit");
        part("estimulante", "stimpack");
        part("de salud", "health");
        part("armadura", "armor");
        part("tienes la", "you got the");
        part("tienes el", "you got the");
        part("(tecla ", "(key ");
        part("cargadores de", "magazines for");
        part("ejecucion", "execution");
        part("enter: reintentar el nivel", "ENTER: retry the level");
        part("enter: siguiente nivel", "ENTER: next level");
        part("enter: volver al titulo", "ENTER: back to title");
        part("has terminado el juego", "you beat the game");
        part("has muerto", "you died");
        part("nivel completado", "level complete");
        part("niveles completados", "levels completed");
        part("(mas abajo)", "(more below)");
        part("(mas arriba)", "(more above)");
        part("comando desconocido", "unknown command");
        part("armas y municion al maximo", "weapons and ammo maxed");
        part("salud restaurada", "health restored");
        part("enemigos eliminados", "enemies eliminated");
        part("desactivado", "disabled");
        part("activado", "enabled");
        part("escudo", "shield");
        part("fantasma", "ghost");
        // menus y HUD
        part("dificultad", "difficulty");
        part("opciones", "options");
        part("continuar", "continue");
        part("jugar", "play");
        part("salir", "quit");
        part("volver", "back");
        part("pausa", "paused");
        part("desbloqueados", "unlocked");
        part("desbloqueado", "unlocked");
        part("bloqueado", "locked");
        part("mejor ", "best ");
        part("volumen", "volume");
        part("silencio", "muted");
        part("musica", "music");
        part("idioma", "language");
        part("facil", "easy");
        part("dificil", "hard");
        part("salud", "health");
        part("armad ", "armor ");
        part("demonios", "demons");
        part("enemigos", "enemies");
        part("objetos", "items");
        part("secretos", "secrets");
        part("tiempo", "time");
        part("recargando", "reloading");
        part("elegir", "select");
        part("sin arma", "no weapon");
        part("cuerpo", "melee");
        part("niveles", "levels");
        part("nivel", "level");
        part("jefe", "boss");
        // niveles
        part("planta nuclear", "nuclear plant");
        part("refineria de toxinas", "toxin refinery");
        part("centro de mando", "command center");
        part("laboratorios fobos", "phobos labs");
        part("almacen de residuos", "waste storage");
        part("complejo infernal", "infernal complex");
        part("santuario del dolor", "sanctum of pain");
        part("camara final", "final chamber");
        // enemigos y armas
        part("zombi", "zombie");
        part("cacodemonio", "cacodemon");
        part("ciberdemonio", "cyberdemon");
        part("demonio", "demon");
        part("baron jefe", "boss baron");
        part("escopetero", "shotgunner");
        part("ametrallador", "chaingunner");
        part("soldado ss", "ss soldier");
        part("espectro", "spectre");
        part("alma perdida", "lost soul");
        part("elemental del dolor", "pain elemental");
        part("caballero infernal", "hell knight");
        part("icono del pecado", "icon of sin");
        part("pistola", "pistol");
        part("escopeta", "shotgun");
        part("ametralladora", "chaingun");
        part("lanzacohetes", "rocket launcher");
        part("rifle de plasma", "plasma rifle");
        PARTS.sort((a, b) -> b[0].length() - a[0].length());
        for (String[] p : PARTS) {
            RULES.add(new Object[]{java.util.regex.Pattern.compile(java.util.regex.Pattern.quote(p[0]),
                    java.util.regex.Pattern.CASE_INSENSITIVE), p[1]});
        }
    }

    public static String yes() { return english ? "YES" : "SI"; }

    public static String no() { return english ? "NO" : "NO"; }

    /** Elige el texto segun el idioma (para etiquetas con letras distintas). */
    public static String pick(String es, String en) { return english ? en : es; }

    public static String tr(String s) {
        if (!english || s == null || s.isEmpty()) return s;
        String hit = CACHE.get(s);
        if (hit != null) return hit;
        String out = EXACT.get(s);
        if (out == null) {
            out = s;
            for (Object[] rule : RULES) {
                java.util.regex.Matcher m = ((java.util.regex.Pattern) rule[0]).matcher(out);
                if (!m.find()) continue;
                StringBuilder sb = new StringBuilder();
                int last = 0;
                do {
                    sb.append(out, last, m.start()).append(like(m.group(), (String) rule[1]));
                    last = m.end();
                } while (m.find());
                out = sb.append(out, last, out.length()).toString();
            }
        }
        if (CACHE.size() > 3000) CACHE.clear();
        CACHE.put(s, out);
        return out;
    }

    /** Idioma por defecto segun el sistema. */
    public static boolean systemIsEnglish() {
        return !Locale.getDefault().getLanguage().equals("es");
    }
}
