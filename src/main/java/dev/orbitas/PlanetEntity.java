package dev.orbitas;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlanetEntity extends Entity {
    private static final TrackedData<Integer> PLANET_ID = DataTracker.registerData(PlanetEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private final Map<UUID, Integer> hitCooldown = new HashMap<>();
    private float spin;

    public PlanetEntity(EntityType<? extends PlanetEntity> type, World world) { super(type, world); }
    @Override protected void initDataTracker() { dataTracker.startTracking(PLANET_ID, 0); }
    public void setPlanetType(PlanetType type) { dataTracker.set(PLANET_ID, type.ordinal()); }
    public PlanetType getPlanetType() { return PlanetType.byId(dataTracker.get(PLANET_ID)); }
    public float getSpin(float tickDelta) { return (spin + tickDelta) * 1.6f; }

    @Override public void tick() {
        super.tick();
        spin += 0.35f;
        if (age % 4 == 0 && world.isClient) {
            double angle = random.nextDouble() * Math.PI * 2;
            double y = (random.nextDouble() - 0.5) * 2.5;
            double ring = Math.sqrt(Math.max(0, 1 - y * y / 1.6));
            world.addParticle(getPlanetType().particle, getX() + Math.cos(angle) * ring * 1.65, getY() + y * 1.1,
                    getZ() + Math.sin(angle) * ring * 1.65, 0, 0.015, 0);
        }
        if (!world.isClient) {
            hitCooldown.replaceAll((uuid, ticks) -> ticks - 1);
            hitCooldown.values().removeIf(ticks -> ticks <= 0);
            Box touch = getBoundingBox().expand(0.18);
            for (PlayerEntity player : world.getEntitiesByClass(PlayerEntity.class, touch, p -> p.isAlive() && !hitCooldown.containsKey(p.getUuid()))) {
                Vec3d away = player.getPos().subtract(getPos()).normalize();
                if (away.lengthSquared() < 0.01) away = new Vec3d(0, 1, 0);
                player.addVelocity(away.x * 0.9, Math.max(0.35, away.y * 0.9), away.z * 0.9);
                player.velocityModified = true;
                player.damage(world.getDamageSources().magic(), getPlanetType().collisionDamage);
                if (getPlanetType() == PlanetType.VENUS) player.setOnFireFor(3);
                if (player instanceof ServerPlayerEntity serverPlayer) serverPlayer.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket(
                        net.minecraft.sound.SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, net.minecraft.sound.SoundCategory.PLAYERS,
                        getX(), getY(), getZ(), 0.9f, 0.8f + random.nextFloat() * 0.5f, random.nextLong()));
                hitCooldown.put(player.getUuid(), 18);
            }
            if (age % 10 == 0 && world instanceof net.minecraft.server.world.ServerWorld serverWorld) world.getPlayers().forEach(player -> {
                if (getBoundingBox().expand(3.5).intersects(player.getBoundingBox()))
                    serverWorld.spawnParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), 2, 0.8, 0.8, 0.8, 0.02);
            });
        }
    }

    @Override public boolean isPushable() { return true; }
    @Override public boolean isCollidable() { return true; }
    @Override protected void readCustomDataFromNbt(NbtCompound nbt) { dataTracker.set(PLANET_ID, nbt.getInt("PlanetType")); }
    @Override protected void writeCustomDataToNbt(NbtCompound nbt) { nbt.putInt("PlanetType", dataTracker.get(PLANET_ID)); }
    @Override public Packet<?> createSpawnPacket() { return new net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket(this); }
}
