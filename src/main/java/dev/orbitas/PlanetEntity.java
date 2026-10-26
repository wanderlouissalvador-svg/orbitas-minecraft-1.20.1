package dev.orbitas;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.particle.ParticleTypes;
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
    private final Map<Integer, Integer> planetHitCooldown = new HashMap<>();
    private float spin;

    public PlanetEntity(EntityType<? extends PlanetEntity> type, World world) {
        super(type, world);
        setNoGravity(true);
    }
    @Override protected void initDataTracker() { dataTracker.startTracking(PLANET_ID, 0); }
    public void setPlanetType(PlanetType type) { dataTracker.set(PLANET_ID, type.ordinal()); }
    public PlanetType getPlanetType() { return PlanetType.byId(dataTracker.get(PLANET_ID)); }
    public float getSpin(float tickDelta) { return (spin + tickDelta) * 1.6f; }

    @Override public void tick() {
        super.tick();
        spin += 0.35f;
        if (age % 4 == 0 && getWorld().isClient) {
            double angle = random.nextDouble() * Math.PI * 2;
            double y = (random.nextDouble() - 0.5) * 2.5;
            double ring = Math.sqrt(Math.max(0, 1 - y * y / 1.6));
            getWorld().addParticle(getPlanetType().particle, getX() + Math.cos(angle) * ring * 1.65, getY() + y * 1.1,
                    getZ() + Math.sin(angle) * ring * 1.65, 0, 0.015, 0);
        }
        if (getWorld().isClient && getPlanetType() == PlanetType.METEORO) {
            getWorld().addParticle(ParticleTypes.FLAME, getX(), getY(), getZ(), 0, 0.02, 0);
            if (age % 2 == 0) getWorld().addParticle(ParticleTypes.SMOKE, getX(), getY(), getZ(), 0, 0.01, 0);
        }
        if (!getWorld().isClient) {
            applyGravityAndMove();
            if (getPlanetType() == PlanetType.METEORO && (horizontalCollision || verticalCollision)) {
                if (getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                    serverWorld.spawnParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
                    serverWorld.spawnParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 28, 0.7, 0.7, 0.7, 0.12);
                }
                discard();
                return;
            }
            hitCooldown.replaceAll((uuid, ticks) -> ticks - 1);
            hitCooldown.values().removeIf(ticks -> ticks <= 0);
            planetHitCooldown.replaceAll((id, ticks) -> ticks - 1);
            planetHitCooldown.values().removeIf(ticks -> ticks <= 0);
            collideWithPlanets();
            Box touch = getBoundingBox().expand(0.18);
            for (PlayerEntity player : getWorld().getEntitiesByClass(PlayerEntity.class, touch, p -> p.isAlive() && !hitCooldown.containsKey(p.getUuid()))) {
                Vec3d away = player.getPos().subtract(getPos()).normalize();
                if (away.lengthSquared() < 0.01) away = new Vec3d(0, 1, 0);
                player.addVelocity(away.x * 0.9, Math.max(0.35, away.y * 0.9), away.z * 0.9);
                player.velocityModified = true;
                player.damage(getDamageSources().magic(), getPlanetType().collisionDamage);
                if (getPlanetType() == PlanetType.VENUS) player.setOnFireFor(3);
                if (getPlanetType() == PlanetType.SOL) player.setOnFireFor(8);
                player.playSound(net.minecraft.sound.SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 0.8f + random.nextFloat() * 0.5f);
                hitCooldown.put(player.getUuid(), 18);
            }
            if (age % 10 == 0 && getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld) getWorld().getPlayers().forEach(player -> {
                if (getBoundingBox().expand(3.5).intersects(player.getBoundingBox()))
                    serverWorld.spawnParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), 2, 0.8, 0.8, 0.8, 0.02);
            });
        }
    }

    private void applyGravityAndMove() {
        Vec3d acceleration = Vec3d.ZERO;
        Box gravityRange = getBoundingBox().expand(96);
        for (PlanetEntity other : getWorld().getEntitiesByClass(PlanetEntity.class, gravityRange,
                body -> body != this && body.getPlanetType() != PlanetType.METEORO)) {
            Vec3d delta = other.getPos().subtract(getPos());
            double distanceSquared = Math.max(16.0, delta.lengthSquared());
            double strength = 0.0015 * other.getPlanetType().mass / distanceSquared;
            acceleration = acceleration.add(delta.normalize().multiply(strength));
        }
        if (acceleration.lengthSquared() > 0.0004) acceleration = acceleration.normalize().multiply(0.02);

        Vec3d velocity = getVelocity().multiply(0.9995).add(acceleration);
        if (getPlanetType() != PlanetType.METEORO && velocity.lengthSquared() > 0.36)
            velocity = velocity.normalize().multiply(0.6);
        setVelocity(velocity);
        move(MovementType.SELF, velocity);
    }

    private void collideWithPlanets() {
        Box nearby = getBoundingBox().expand(2.8);
        for (PlanetEntity other : getWorld().getEntitiesByClass(PlanetEntity.class, nearby,
                planet -> planet != this && getId() < planet.getId())) {
            if (!getBoundingBox().intersects(other.getBoundingBox())) continue;

            if (getPlanetType() == PlanetType.METEORO || other.getPlanetType() == PlanetType.METEORO) {
                double x = (getX() + other.getX()) * 0.5;
                double y = (getY() + other.getY()) * 0.5;
                double z = (getZ() + other.getZ()) * 0.5;
                if (getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                    serverWorld.spawnParticles(ParticleTypes.EXPLOSION, x, y, z, 2, 0.25, 0.25, 0.25, 0.02);
                    serverWorld.spawnParticles(ParticleTypes.FLAME, x, y, z, 32, 0.6, 0.6, 0.6, 0.14);
                    serverWorld.spawnParticles(ParticleTypes.LAVA, x, y, z, 10, 0.45, 0.45, 0.45, 0.05);
                }
                if (getPlanetType() == PlanetType.METEORO) discard();
                if (other.getPlanetType() == PlanetType.METEORO) other.discard();
                continue;
            }

            Vec3d direction = other.getPos().subtract(getPos());
            if (direction.lengthSquared() < 1.0E-4) {
                double angle = (getId() * 31L + other.getId() * 17L) * 0.37;
                direction = new Vec3d(Math.cos(angle), 0.15, Math.sin(angle));
            }
            direction = direction.normalize();

            // Separate both spheres gradually so overlapping spawns visibly bounce apart.
            setPosition(getPos().subtract(direction.multiply(0.12)));
            other.setPosition(other.getPos().add(direction.multiply(0.12)));

            if (!planetHitCooldown.containsKey(other.getId())) {
                double impactX = (getX() + other.getX()) * 0.5;
                double impactY = (getY() + other.getY()) * 0.5;
                double impactZ = (getZ() + other.getZ()) * 0.5;
                if (getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                    serverWorld.spawnParticles(ParticleTypes.END_ROD, impactX, impactY, impactZ, 18, 0.55, 0.55, 0.55, 0.08);
                    serverWorld.spawnParticles(ParticleTypes.FIREWORK, impactX, impactY, impactZ, 12, 0.45, 0.45, 0.45, 0.06);
                }
                for (PlayerEntity player : getWorld().getPlayers()) {
                    if (player.squaredDistanceTo(impactX, impactY, impactZ) < 64)
                        player.playSound(net.minecraft.sound.SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.65f + random.nextFloat() * 0.35f);
                }
                planetHitCooldown.put(other.getId(), 12);
                other.planetHitCooldown.put(getId(), 12);
            }
        }
    }

    @Override public boolean isPushable() { return true; }
    @Override public boolean isCollidable() { return true; }
    @Override protected void readCustomDataFromNbt(NbtCompound nbt) { dataTracker.set(PLANET_ID, nbt.getInt("PlanetType")); }
    @Override protected void writeCustomDataToNbt(NbtCompound nbt) { nbt.putInt("PlanetType", dataTracker.get(PLANET_ID)); }
    @Override public Packet<ClientPlayPacketListener> createSpawnPacket() { return new net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket(this); }
}
