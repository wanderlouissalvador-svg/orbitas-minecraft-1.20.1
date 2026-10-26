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
    private static final int LONGITUDE_STEPS = 48;
    private static final int LATITUDE_STEPS = 32;

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
        int bodyLight = type == PlanetType.SOL ? 15728880 : light;
        matrices.push();
        matrices.translate(0, 0.08, 0);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(spin));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(18));

        if (type == PlanetType.SATURNO) drawRings(matrices, consumers, bodyLight, getTexture(planet), size);

        int r = type.color >> 16 & 255;
        int g = type.color >> 8 & 255;
        int b = type.color & 255;
        VertexConsumer aura = consumers.getBuffer(RenderLayer.getEntityTranslucent(getTexture(planet)));
        drawSphere(matrices, aura, bodyLight, size * 1.075f, r, g, b, 62);
        if (type == PlanetType.SOL) {
            drawSphere(matrices, aura, bodyLight, size * 1.18f, 255, 132, 28, 58);
            drawSphere(matrices, aura, bodyLight, size * 1.32f, 255, 72, 12, 28);
        }
        VertexConsumer surface = consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(getTexture(planet)));
        drawSphere(matrices, surface, bodyLight, size, 255, 255, 255, 255);
        matrices.pop();
        super.render(planet, yaw, tickDelta, matrices, consumers, light);
    }

    private static void drawRings(MatrixStack matrices, VertexConsumerProvider consumers, int light,
                                  Identifier texture, float size) {
        VertexConsumer out = consumers.getBuffer(RenderLayer.getEntityTranslucent(texture));
        float[][] bands = {{1.12f, 1.35f}, {1.40f, 1.62f}, {1.68f, 1.92f}, {1.98f, 2.12f}};
        int[][] colors = {{232, 218, 178, 205}, {169, 145, 104, 150}, {238, 224, 190, 220}, {128, 111, 88, 125}};
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f position = entry.getPositionMatrix();
        Matrix3f normal = entry.getNormalMatrix();
        int segments = 72;
        for (int band = 0; band < bands.length; band++) {
            float inner = bands[band][0] * size;
            float outer = bands[band][1] * size;
            int[] color = colors[band];
            for (int i = 0; i < segments; i++) {
                double a0 = Math.PI * 2 * i / segments;
                double a1 = Math.PI * 2 * (i + 1) / segments;
                float x0 = (float) Math.cos(a0), z0 = (float) Math.sin(a0);
                float x1 = (float) Math.cos(a1), z1 = (float) Math.sin(a1);
                float v0 = (float) i / segments, v1 = (float) (i + 1) / segments;
                ringVertex(out, position, normal, x0 * inner, 0, z0 * inner, 0, v0, color, light);
                ringVertex(out, position, normal, x0 * outer, 0, z0 * outer, 1, v0, color, light);
                ringVertex(out, position, normal, x1 * outer, 0, z1 * outer, 1, v1, color, light);
                ringVertex(out, position, normal, x0 * inner, 0, z0 * inner, 0, v0, color, light);
                ringVertex(out, position, normal, x1 * outer, 0, z1 * outer, 1, v1, color, light);
                ringVertex(out, position, normal, x1 * inner, 0, z1 * inner, 0, v1, color, light);
            }
        }
    }

    private static void ringVertex(VertexConsumer out, Matrix4f position, Matrix3f normal,
                                   float x, float y, float z, float u, float v, int[] c, int light) {
        out.vertex(position, x, y, z).color(c[0], c[1], c[2], c[3]).texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normal, 0, 1, 0).next();
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
