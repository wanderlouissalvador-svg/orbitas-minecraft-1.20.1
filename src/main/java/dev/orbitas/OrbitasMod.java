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
                    if (type == null) {
                        context.getSource().sendError(Text.literal("Usa: /planeta mercurio|marte|venus|luna|pluton"));
                        return 0;
                    }
                    PlanetEntity planet = PLANET.create(context.getSource().getWorld());
                    if (planet == null) return 0;
                    planet.setPlanetType(type);
                    var pos = context.getSource().getPosition();
                    planet.refreshPositionAndAngles(pos.x, pos.y, pos.z, context.getSource().getRotation().y, 0);
                    context.getSource().getWorld().spawnEntity(planet);
                    context.getSource().sendFeedback(() -> Text.literal("✦ ¡" + type.id.toUpperCase() + " ha aparecido!"), false);
                    return 1;
                }))));
    }
}
