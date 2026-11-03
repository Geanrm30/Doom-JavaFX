# DOOM JavaFX

Un juego tipo Doom hecho desde cero en Java 21 y JavaFX. Usa un raycaster (el mismo
principio de Wolfenstein 3D): el mundo es una cuadricula y cada columna de pantalla se
dibuja lanzando un rayo. No usa imagenes ni archivos de audio: las texturas, los
sprites y los sonidos se generan por codigo.

## Como ejecutarlo

Requisitos: JDK 21 y Maven (o IntelliJ, que ya los trae).

```
./mvnw javafx:run
```

En Windows: `mvnw.cmd javafx:run`. Desde IntelliJ, ejecuta la clase `doom.Launcher`.

## Controles

| Tecla | Accion |
| --- | --- |
| W A S D | Moverse y desplazarse de lado |
| Q E o flechas izquierda/derecha | Girar |
| Raton | Girar (el cursor se oculta y se recentra solo) |
| Clic izquierdo | Disparar |
| Clic derecho | Usar |
| Rueda del raton | Cambiar de arma |
| L | Activar o desactivar el giro con el raton |
| F11 | Pantalla completa o ventana |
| SHIFT | Correr |
| ESPACIO o CTRL | Disparar |
| F o ENTER | Usar (abrir puertas, activar la salida) |
| 1 2 3 | Cambiar de arma |
| TAB | Mostrar u ocultar el minimapa |
| M | Silenciar o activar el sonido |
| ESC o P | Pausa. El menu de pausa permite continuar, cambiar opciones, volver al titulo o salir |
| ESC | En los menus vuelve atras; en las pantallas de muerte y estadisticas vuelve al titulo |

Los menus se manejan con las flechas (o W y S), ENTER para elegir e izquierda y derecha para
cambiar valores. Tambien funcionan con el raton: pasar el cursor, clic para elegir, clic
derecho para volver y la rueda para moverse.

- **Menu principal:** Jugar o Continuar (el nivel desbloqueado mas alto), Seleccionar nivel,
  Dificultad (Facil: la mitad de dano, Normal, Dificil: 60% mas de dano), Opciones y Salir.
- **Opciones:** sensibilidad del raton, giro con el raton, sonido, pantalla completa y borrar progreso.
- Al morir, ENTER reintenta el nivel actual.

El juego arranca en pantalla completa (se puede cambiar en Opciones o con F11) y la imagen
se escala a la ventana manteniendo la proporcion. En pantalla completa ESC no sale: es la pausa.
El juego se pausa solo si la ventana pierde el foco.

Si el cursor se sale de la ventana al girar (algunos escalados de pantalla de Windows lo
provocan), el juego lo detecta, prueba otra calibracion y, si no funciona, sigue girando sin
recentrar el cursor.

## Progreso

Cada nivel se desbloquea al terminar el anterior. El progreso (niveles desbloqueados,
mejores tiempos, arma y municion con la que sigues, y tus ajustes) se guarda solo al
terminar un nivel y al cerrar el juego, en `~/.doom-javafx/progreso.properties`
(en Windows, `C:\Users\TU_USUARIO\.doom-javafx\progreso.properties`). Si lo borras, empiezas de cero.
No se guarda la posicion dentro de un nivel a medias.

## Niveles

Hay 9 niveles que van subiendo de dificultad:

| N | Nombre | Notas |
| --- | --- | --- |
| 1 | Hangar | Sin llaves, solo zombis e imps |
| 2 | Planta Nuclear | Llave roja, aparece la escopeta |
| 3 | Refineria de Toxinas | Primer jefe: Baron jefe |
| 4 | Centro de Mando | Llaves roja y azul, aparece la ametralladora |
| 5 | Laboratorios Fobos | Baron jefe, esferas de invulnerabilidad |
| 6 | Almacen de Residuos | Dos barones jefe |
| 7 | Complejo Infernal | Baron jefe y barones normales |
| 8 | Santuario del Dolor | El mapa mas grande, dos barones jefe |
| 9 | Camara Final | Ciberdemonio y Baron jefe |

Para terminar un nivel hay que llegar a la pared verde con la flecha y usarla. Si el
nivel tiene jefe, la salida no abre hasta que lo derrotes. Las puertas rojas y azules
piden la llave de su color, que siempre esta en una zona a la que puedes llegar antes.

## Enemigos

| Enemigo | Vida | Como ataca |
| --- | --- | --- |
| Zombi | 30 | Dispara a distancia y suelta balas al morir |
| Imp | 60 | Bolas de fuego y garras |
| Demonio | 110 | Muy rapido, solo cuerpo a cuerpo |
| Cacodemonio | 160 | Flota y lanza bolas de fuego |
| Baron | 450 | Bolas de fuego rapidas y golpe fuerte |
| Baron jefe | 700 | Como el baron, mas grande y agresivo |
| Ciberdemonio | 1000 | Lanza tres bolas de fuego a la vez |

## Armas y objetos

- Pistola (balas), escopeta (cartuchos, 7 perdigones) y ametralladora (balas, cadencia alta).
- Botiquin, estimulante, armadura, cargadores y cartuchos.
- Megasalud: salud hasta 200 y armadura al maximo.
- Esfera de invulnerabilidad: 12 segundos sin recibir dano.
- Llaves roja y azul.
- Barriles explosivos: disparales y arrastran a los enemigos cercanos (y a ti, si estas cerca).

## Estructura del proyecto

```
src/main/java/doom/
  DoomApp.java, Launcher.java   ventana y bucle principal
  world/    Level, World, Player, Weapon  (reglas del juego)
  entities/ Enemy, EnemyType, Projectile, Pickup, Barrel, Effect
  render/   Renderer (paredes), SpriteRenderer, TextureManager
  hud/      HudRenderer (barra de estado, minimapa, pantallas)
  engine/   Constants, GameState, ColorUtils, SoundFx
src/main/resources/levels/      level1.txt ... level9.txt
tools/generate_levels.py        generador de niveles
```

## Crear o regenerar niveles

Los niveles son archivos de texto: una cabecera (nombre, colores, niebla) y un mapa de
caracteres. Si agregas `level10.txt`, el juego lo detecta solo.

Leyenda del mapa:

```
1-6 paredes     D puerta     R puerta roja     U puerta azul     X salida     . suelo
P inicio        z zombi      i imp             d demonio         c cacodemonio
B baron         J baron jefe Y ciberdemonio
h botiquin      s estimulante  a armadura      k balas           e cartuchos
g escopeta      m ametralladora  r llave roja  u llave azul
H megasalud     v invulnerabilidad             o barril explosivo
```

Para regenerar los 9 niveles con otras semillas o cambiar la dificultad, edita la tabla
`LEVELS` al inicio de `tools/generate_levels.py` y ejecuta:

```
python tools/generate_levels.py
```

El generador comprueba que todo sea alcanzable y que cada llave aparezca antes de su puerta.

## Notas

- Los niveles son propios; no usan los mapas originales de Doom.
- El sonido usa `javax.sound`. Si el equipo no tiene dispositivo de audio, el juego
  sigue funcionando en silencio.
