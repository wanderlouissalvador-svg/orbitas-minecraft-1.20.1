# Órbitas: Planetas Vivos 🌌

Un mod de Fabric para **Minecraft Java 1.20.1**: invoca planetas y el Sol como cuerpos esféricos con texturas renovadas, gravedad, órbitas, choques y meteoros.

## Los cinco mundos

| Comando | Apariencia y efecto | Choque |
| --- | --- | --- |
| `/planeta mercurio` | Superficie gris rocosa y ceniza | Empujón suave |
| `/planeta marte` | Rojos volcánicos y chispas | Empujón intenso |
| `/planeta venus` | Nubes doradas y bruma | Calor y daño |
| `/planeta luna` | Cráteres plateados y destellos | Empujón ligero |
| `/planeta pluton` | Hielo azul y nieve | Empujón suave |
| `/planeta tierra` | Océanos, continentes y nubes | Órbita y gravedad |
| `/planeta jupiter` | Bandas nubosas y Gran Mancha Roja | Gran masa y atracción |
| `/planeta saturno` | Bandas doradas y anillos visibles | Gran masa y atracción |
| `/planeta sol` | Fotosfera brillante y corona | Mucho calor y gravedad |

Los cuerpos se atraen entre sí y siguen órbitas dinámicas. Cuando se tocan, el más dominante absorbe al otro, aumenta su masa y crece; el impacto produce destellos, nube y onda de choque. Usa `/choqueplanetario` para ver a Júpiter absorber a la Tierra. El Sol es el cuerpo más masivo. Los meteoros dejan una estela ardiente y son absorbidos por los planetas.

Requiere **Fabric Loader**, **Fabric API** y **Java 17**.

## Instalar

1. Instala Fabric Loader para Minecraft 1.20.1 y Fabric API.
2. En GitHub, abre **Actions → Build mod → la ejecución más reciente que terminó correctamente** y descarga el artefacto `orbitas-fabric-1.20.1`. Extrae el `.jar` del ZIP y ponlo en la carpeta `mods`.
3. En un mundo con trucos, usa uno de los comandos de arriba. Para lanzar un meteoro, escribe `/meteoro`.

## Compilar desde el código

Con JDK 17 o superior y Gradle 8.6: ejecuta `gradle build`. El archivo queda en `build/libs/`.

## Créditos

Órbitas es un mod comunitario independiente y no está afiliado con Mojang ni Microsoft. Licencia MIT.
