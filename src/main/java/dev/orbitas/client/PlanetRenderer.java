package dev.orbitas.client;

import dev.orbitas.OrbitasMod;
import dev.orbitas.PlanetEntity;
import dev.orbitas.PlanetType;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class PlanetRenderer extends EntityRenderer<PlanetEntity> {
    private static final int LONGITUDE_STEPS = 32;
    private static final int LATITUDE_STEPS = 20;

    public PlanetRenderer(EntityRendererFactory.Context context) {
        super(context);
        shadowRadius = 0.15f;
    }

    @Override public Identifier getTexture(PlanetEntity planet) {
        return new Identifier(OrbitasMod.MOD_ID, "textures/entity/" + planet.getPlanetType().id + ".png");
    }

    @Override public void render(PlanetEntity planet, float yaw, float tickDelta, MatrixStack matrices,
                                 VertexConsumerProvider consumers, int light) {
        PlanetType type = planet.getPlanetType();
        float spin = planet.getSpin(tickDelta);
        float size = 0.91f * type.scale;
        matrices.push();
        matrices.translate(0, 0.08, 0);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(spin));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(18));

        int r = type.color >> 16 & 255;
        int g = type.color >> 8 & 255;
        int b = type.color & 255;
        VertexConsumer aura = consumers.getBuffer(RenderLayer.getEntityTranslucent(getTexture(planet)));
        drawSphere(matrices, aura, light, size * 1.075f, r, g, b, 62);
        VertexConsumer surface = consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(getTexture(planet)));
        drawSphere(matrices, surface, light, size, 255, 255, 255, 255);
        matrices.pop();
        super.render(planet, yaw, tickDelta, matrices, consumers, light);
    }

    private static void drawSphere(MatrixStack matrices, VertexConsumer out, int light, float radius,
                                   int red, int green, int blue, int alpha) {
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f position = entry.getPositionMatrix();
        Matrix3f normalMatrix = entry.getNormalMatrix();
        for (int y = 0; y < LATITUDE_STEPS; y++) {
            float v0 = (float) y / LATITUDE_STEPS;
            float v1 = (float) (y + 1) / LATITUDE_STEPS;
            float lat0 = (float) (-Math.PI / 2 + Math.PI * v0);
            float lat1 = (float) (-Math.PI / 2 + Math.PI * v1);
            for (int x = 0; x < LONGITUDE_STEPS; x++) {
                float u0 = (float) x / LONGITUDE_STEPS;
                float u1 = (float) (x + 1) / LONGITUDE_STEPS;
                float lon0 = (float) (u0 * Math.PI * 2);
                float lon1 = (float) (u1 * Math.PI * 2);
                float[] a = point(lon0, lat0), b = point(lon1, lat0), c = point(lon1, lat1), d = point(lon0, lat1);
                vertex(out, position, normalMatrix, a, radius, u0, 1 - v0, red, green, blue, alpha, light);
                vertex(out, position, normalMatrix, b, radius, u1, 1 - v0, red, green, blue, alpha, light);
                vertex(out, position, normalMatrix, c, radius, u1, 1 - v1, red, green, blue, alpha, light);
                vertex(out, position, normalMatrix, a, radius, u0, 1 - v0, red, green, blue, alpha, light);
                vertex(out, position, normalMatrix, c, radius, u1, 1 - v1, red, green, blue, alpha, light);
                vertex(out, position, normalMatrix, d, radius, u0, 1 - v1, red, green, blue, alpha, light);
            }
        }
    }

    private static float[] point(float longitude, float latitude) {
        float cos = MathHelper.cos(latitude);
        return new float[]{-MathHelper.sin(longitude) * cos, MathHelper.sin(latitude), MathHelper.cos(longitude) * cos};
    }

    private static void vertex(VertexConsumer out, Matrix4f matrix, Matrix3f normalMatrix, float[] n, float radius,
                               float u, float v, int r, int g, int b, int a, int light) {
        out.vertex(matrix, n[0] * radius, n[1] * radius, n[2] * radius)
                .color(r, g, b, a).texture(u, v).overlay(OverlayTexture.DEFAULT_UV).light(light)
                .normal(normalMatrix, n[0], n[1], n[2]).next();
    }
}
