package dev.orbitas;

import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;

public enum PlanetType {
    MERCURIO("mercurio", 0xB7AFA5, 0.92f, 1.2f, 0.38f, ParticleTypes.ASH),
    MARTE("marte", 0xD9573B, 1.18f, 1.6f, 0.70f, ParticleTypes.FLAME),
    VENUS("venus", 0xFFC56B, 1.30f, 2.2f, 0.90f, ParticleTypes.CLOUD),
    LUNA("luna", 0xD8E0E8, 0.78f, 0.8f, 0.12f, ParticleTypes.END_ROD),
    PLUTON("pluton", 0xA8D9E8, 0.70f, 0.5f, 0.08f, ParticleTypes.SNOWFLAKE),
    TIERRA("tierra", 0x68A8FF, 1.05f, 1.5f, 1.0f, ParticleTypes.CLOUD),
    JUPITER("jupiter", 0xE8B77B, 1.72f, 2.8f, 10.0f, ParticleTypes.CLOUD),
    SATURNO("saturno", 0xE9D29A, 1.48f, 2.4f, 7.0f, ParticleTypes.END_ROD),
    SOL("sol", 0xFFB341, 2.15f, 5.0f, 100.0f, ParticleTypes.FLAME),
    METEORO("meteoro", 0xFF6A22, 0.30f, 3.5f, 0.12f, ParticleTypes.FLAME);

    public final String id;
    public final int color;
    public final float scale;
    public final float collisionDamage;
    public final float mass;
    public final ParticleEffect particle;

    PlanetType(String id, int color, float scale, float collisionDamage, float mass, ParticleEffect particle) {
        this.id = id;
        this.color = color;
        this.scale = scale;
        this.collisionDamage = collisionDamage;
        this.mass = mass;
        this.particle = particle;
    }

    public static PlanetType byId(int id) {
        PlanetType[] values = values();
        return values[Math.floorMod(id, values.length)];
    }

    public static PlanetType byName(String name) {
        for (PlanetType type : values()) if (type.id.equalsIgnoreCase(name)) return type;
        return null;
    }
}
