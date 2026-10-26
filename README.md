# Órbitas: Planetas Vivos 🌌

Un mod de Fabric para **Minecraft Java 1.20.1**: invoca cinco planetas esféricos con texturas propias, giro, aura luminosa, estelas de partículas y choques con los jugadores.

## Los cinco mundos

| Comando | Apariencia y efecto | Choque |
| --- | --- | --- |
| `/planeta mercurio` | Superficie gris rocosa y ceniza | Empujón suave |
| `/planeta marte` | Rojos volcánicos y chispas | Empujón intenso |
| `/planeta venus` | Nubes doradas y bruma | Calor y daño |
| `/planeta luna` | Cráteres plateados y destellos | Empujón ligero |
| `/planeta pluton` | Hielo azul y nieve | Empujón suave |

Los planetas son entidades 3D, giran en el sitio y ocupan espacio. Al tocarlos, el jugador recibe un impulso y daño; Venus también prende fuego brevemente. Requiere **Fabric Loader**, **Fabric API** y **Java 17**.

## Instalar

1. Instala Fabric Loader para Minecraft 1.20.1 y Fabric API.
2. Descarga `orbitas-1.0.0.jar` desde Releases y ponlo en la carpeta `mods`.
3. En un mundo con trucos, usa uno de los comandos de arriba.

## Compilar desde el código

Con JDK 17 o superior y Gradle 8.6: ejecuta `gradle build`. El archivo queda en `build/libs/`.

## Créditos

Órbitas es un mod comunitario independiente y no está afiliado con Mojang ni Microsoft. Licencia MIT.
