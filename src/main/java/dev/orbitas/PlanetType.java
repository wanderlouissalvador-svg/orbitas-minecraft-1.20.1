package dev.orbitas;

import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;

public enum PlanetType {
    MERCURIO("mercurio", 0xB7AFA5, 0.92f, 1.2f, ParticleTypes.ASH),
    MARTE("marte", 0xD9573B, 1.18f, 1.6f, ParticleTypes.FLAME),
    VENUS("venus", 0xFFC56B, 1.30f, 2.2f, ParticleTypes.CLOUD),
    LUNA("luna", 0xD8E0E8, 0.78f, 0.8f, ParticleTypes.END_ROD),
    PLUTON("pluton", 0xA8D9E8, 0.70f, 0.5f, ParticleTypes.SNOWFLAKE);

    public final String id;
    public final int color;
    public final float scale;
    public final float collisionDamage;
    public final ParticleEffect particle;

    PlanetType(String id, int color, float scale, float collisionDamage, ParticleEffect particle) {
        this.id = id;
        this.color = color;
        this.scale = scale;
        this.collisionDamage = collisionDamage;
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
