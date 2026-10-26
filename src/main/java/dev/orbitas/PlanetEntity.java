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
    private static final TrackedData<Float> BODY_MASS = DataTracker.registerData(PlanetEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> GROWTH = DataTracker.registerData(PlanetEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private final Map<UUID, Integer> hitCooldown = new HashMap<>();
    private float spin;

    public PlanetEntity(EntityType<? extends PlanetEntity> type, World world) {
        super(type, world);
        setNoGravity(true);
    }
    @Override protected void initDataTracker() {
        dataTracker.startTracking(PLANET_ID, 0);
        dataTracker.startTracking(BODY_MASS, 1.0f);
        dataTracker.startTracking(GROWTH, 1.0f);
    }
    public void setPlanetType(PlanetType type) {
        dataTracker.set(PLANET_ID, type.ordinal());
        dataTracker.set(BODY_MASS, type.mass);
        dataTracker.set(GROWTH, 1.0f);
    }
    public PlanetType getPlanetType() { return PlanetType.byId(dataTracker.get(PLANET_ID)); }
    public float getMass() { return dataTracker.get(BODY_MASS); }
    public float getVisualScale() { return getPlanetType().scale * dataTracker.get(GROWTH); }
    public float getSpin(float tickDelta) { return (spin + tickDelta) * 1.6f; }

    private void absorb(PlanetEntity smaller) {
        double thisRadius = 0.91 * getVisualScale();
        double absorbedRadius = 0.91 * smaller.getVisualScale();
        double combinedRadius = Math.cbrt(thisRadius * thisRadius * thisRadius + absorbedRadius * absorbedRadius * absorbedRadius);
        float growth = (float) (combinedRadius / (0.91 * getPlanetType().scale));
        dataTracker.set(GROWTH, Math.min(4.0f, growth));
        dataTracker.set(BODY_MASS, getMass() + smaller.getMass());

        double x = (getX() + smaller.getX()) * 0.5;
        double y = (getY() + smaller.getY()) * 0.5;
        double z = (getZ() + smaller.getZ()) * 0.5;
        if (getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.EXPLOSION, x, y, z, 2, 0.25, 0.25, 0.25, 0.02);
            serverWorld.spawnParticles(ParticleTypes.END_ROD, x, y, z, 56, 0.8, 0.8, 0.8, 0.13);
            serverWorld.spawnParticles(ParticleTypes.FIREWORK, x, y, z, 40, 0.7, 0.7, 0.7, 0.12);
            serverWorld.spawnParticles(ParticleTypes.CLOUD, x, y, z, 28, 0.65, 0.65, 0.65, 0.08);
            if (getPlanetType() == PlanetType.SOL || getPlanetType() == PlanetType.JUPITER)
                serverWorld.spawnParticles(ParticleTypes.FLAME, x, y, z, 36, 0.65, 0.65, 0.65, 0.11);
        }
        for (PlayerEntity player : getWorld().getPlayers()) {
            if (player.squaredDistanceTo(x, y, z) < 144) {
                Vec3d shock = player.getPos().subtract(x, y, z).normalize();
                if (shock.lengthSquared() > 0.01) {
                    player.addVelocity(shock.x * 0.55, Math.max(0.18, shock.y * 0.55), shock.z * 0.55);
                    player.velocityModified = true;
                }
                player.playSound(net.minecraft.sound.SoundEvents.ENTITY_GENERIC_EXPLODE, 1.25f, 0.75f);
            }
        }
        smaller.discard();
    }

    @Override public void tick() {
        super.tick();
        spin += 0.35f;
        if (age % 4 == 0 && getWorld().isClient) {
            double angle = random.nextDouble() * Math.PI * 2;
            double y = (random.nextDouble() - 0.5) * 2.5;
            double ring = Math.sqrt(Math.max(0, 1 - y * y / 1.6));
            double radius = getVisualScale() * 1.12;
            getWorld().addParticle(getPlanetType().particle, getX() + Math.cos(angle) * ring * radius, getY() + y * radius * 0.7,
                    getZ() + Math.sin(angle) * ring * radius, 0, 0.015, 0);
        }
        if (getWorld().isClient && getPlanetType() == PlanetType.SOL && age % 2 == 0) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = getVisualScale() * 1.15;
            getWorld().addParticle(ParticleTypes.FLAME, getX() + Math.cos(angle) * radius, getY(), getZ() + Math.sin(angle) * radius,
                    Math.cos(angle) * 0.025, 0.02, Math.sin(angle) * 0.025);
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
            double strength = 0.0015 * other.getMass() / distanceSquared;
            acceleration = acceleration.add(delta.normalize().multiply(strength));
        }
        if (acceleration.lengthSquared() > 0.0004) acceleration = acceleration.normalize().multiply(0.02);

        Vec3d velocity = getVelocity().multiply(0.99998).add(acceleration);
        if (getPlanetType() != PlanetType.METEORO && velocity.lengthSquared() > 0.36)
            velocity = velocity.normalize().multiply(0.6);
        setVelocity(velocity);
        move(MovementType.SELF, velocity);
    }

    private void collideWithPlanets() {
        Box nearby = getBoundingBox().expand(16.0);
        for (PlanetEntity other : getWorld().getEntitiesByClass(PlanetEntity.class, nearby,
                planet -> planet != this && getId() < planet.getId())) {
            double thisRadius = 0.91 * getVisualScale();
            double otherRadius = 0.91 * other.getVisualScale();
            Vec3d between = other.getPos().subtract(getPos());
            double distance = between.length();
            double contactDistance = thisRadius + otherRadius;
            if (distance >= contactDistance) continue;

            if (getPlanetType() == PlanetType.METEORO) {
                other.absorb(this);
            } else if (other.getPlanetType() == PlanetType.METEORO) {
                absorb(other);
            } else {
                double thisDominance = getMass() * Math.pow(getVisualScale(), 3);
                double otherDominance = other.getMass() * Math.pow(other.getVisualScale(), 3);
                if (thisDominance >= otherDominance) absorb(other);
                else other.absorb(this);
            }
        }
    }

    @Override public boolean isPushable() { return true; }
    @Override public boolean isCollidable() { return true; }
    @Override protected void readCustomDataFromNbt(NbtCompound nbt) {
        int id = nbt.getInt("PlanetType");
        PlanetType type = PlanetType.byId(id);
        dataTracker.set(PLANET_ID, id);
        dataTracker.set(BODY_MASS, nbt.contains("BodyMass") ? Math.max(type.mass, nbt.getFloat("BodyMass")) : type.mass);
        dataTracker.set(GROWTH, nbt.contains("Growth") ? Math.max(1.0f, nbt.getFloat("Growth")) : 1.0f);
    }
    @Override protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("PlanetType", dataTracker.get(PLANET_ID));
        nbt.putFloat("BodyMass", dataTracker.get(BODY_MASS));
        nbt.putFloat("Growth", dataTracker.get(GROWTH));
    }
    @Override public Packet<ClientPlayPacketListener> createSpawnPacket() { return new net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket(this); }
}
