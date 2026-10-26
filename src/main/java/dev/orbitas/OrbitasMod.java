package dev.orbitas;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class OrbitasMod implements ModInitializer {
    public static final String MOD_ID = "orbitas";
    public static final EntityType<PlanetEntity> PLANET = Registry.register(
            Registries.ENTITY_TYPE, new Identifier(MOD_ID, "planet"),
            FabricEntityTypeBuilder.<PlanetEntity>create(SpawnGroup.MISC, PlanetEntity::new)
                    .dimensions(EntityDimensions.fixed(2.8f, 2.8f)).trackRangeBlocks(96).trackedUpdateRate(2).build());

    @Override public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(CommandManager.literal("planeta")
                .then(CommandManager.argument("nombre", StringArgumentType.word()).executes(context -> {
                    PlanetType type = PlanetType.byName(StringArgumentType.getString(context, "nombre"));
                    if (type == null || type == PlanetType.METEORO) {
                        context.getSource().sendError(Text.literal("Usa: /planeta mercurio|marte|venus|luna|pluton|tierra|jupiter|saturno|sol"));
                        return 0;
                    }
                    var source = context.getSource();
                    var world = source.getWorld();
                    Vec3d origin = source.getPosition();
                    PlanetEntity anchor = world.getEntitiesByClass(PlanetEntity.class, Box.of(origin, 96, 96, 96),
                            entity -> entity.getPlanetType() != PlanetType.METEORO).stream()
                            .min(java.util.Comparator.comparingDouble(entity -> entity.squaredDistanceTo(origin))).orElse(null);
                    PlanetEntity planet = PLANET.create(world);
                    if (planet == null) return 0;
                    planet.setPlanetType(type);
                    if (anchor != null) {
                        double radius = anchor.getPlanetType() == PlanetType.SOL
                                ? switch (type) {
                                    case MERCURIO -> 6.0;
                                    case VENUS -> 8.5;
                                    case TIERRA -> 11.0;
                                    case MARTE -> 14.0;
                                    case JUPITER -> 21.0;
                                    case SATURNO -> 28.0;
                                    case PLUTON -> 35.0;
                                    case LUNA -> 12.5;
                                    default -> 16.0;
                                }
                                : 7.0 + (type.ordinal() % 6) * 1.8;
                        double angle = type.ordinal() * 2.399963229728653;
                        Vec3d offset = new Vec3d(Math.cos(angle) * radius, Math.sin(angle * 0.5) * 1.5, Math.sin(angle) * radius);
                        Vec3d position = anchor.getPos().add(offset);
                        planet.refreshPositionAndAngles(position.x, position.y, position.z, 0, 0);
                        Vec3d tangent = new Vec3d(-Math.sin(angle), 0.08, Math.cos(angle)).normalize()
                                .multiply(Math.sqrt(0.0015 * anchor.getPlanetType().mass / radius));
                        planet.setVelocity(tangent.add(anchor.getVelocity()));
                        anchor.addVelocity(tangent.negate().multiply(type.mass / Math.max(anchor.getPlanetType().mass, 0.1f)));
                    } else {
                        Vec3d look = lookVector(source.getRotation().x, source.getRotation().y);
                        Vec3d position = origin.add(look.multiply(8)).add(0, 2, 0);
                        planet.refreshPositionAndAngles(position.x, position.y, position.z, 0, 0);
                    }
                    world.spawnEntity(planet);
                    context.getSource().sendFeedback(() -> Text.literal("✦ ¡" + type.id.toUpperCase() + " ha aparecido!"), false);
                    return 1;
                }))));

        dispatcher.register(CommandManager.literal("meteoro").executes(context -> {
            var source = context.getSource();
            Vec3d look = lookVector(source.getRotation().x, source.getRotation().y);
            Vec3d position = source.getPosition().add(look.multiply(18)).add(0, 12, 0);
            PlanetEntity meteor = PLANET.create(source.getWorld());
            if (meteor == null) return 0;
            meteor.setPlanetType(PlanetType.METEORO);
            meteor.refreshPositionAndAngles(position.x, position.y, position.z, 0, 0);
            meteor.setVelocity(look.multiply(0.55).add(0, -0.55, 0).normalize().multiply(0.85));
            source.getWorld().spawnEntity(meteor);
            source.sendFeedback(() -> Text.literal("☄ ¡Meteoro en camino!"), false);
            return 1;
        }));
    }

    private static Vec3d lookVector(float pitchDegrees, float yawDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        double pitch = Math.toRadians(pitchDegrees);
        return new Vec3d(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch)).normalize();
    }
}
