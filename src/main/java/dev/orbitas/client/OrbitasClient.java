package dev.orbitas.client;

import dev.orbitas.OrbitasMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class OrbitasClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        EntityRendererRegistry.register(OrbitasMod.PLANET, PlanetRenderer::new);
    }
}
