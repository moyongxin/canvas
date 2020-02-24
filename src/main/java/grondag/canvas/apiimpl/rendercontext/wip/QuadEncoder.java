package grondag.canvas.apiimpl.rendercontext.wip;

import static grondag.canvas.apiimpl.util.GeometryHelper.LIGHT_FACE_FLAG;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.Matrix3f;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.math.BlockPos;

import grondag.canvas.apiimpl.RenderMaterialImpl;
import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.apiimpl.rendercontext.BlockRenderInfo;
import grondag.canvas.apiimpl.util.ColorHelper;

public class QuadEncoder {
	static final int FULL_BRIGHTNESS = 0xF000F0;

	public static final QuadEncoder INSTANCE = new QuadEncoder();

	/**
	 * Determines color index and render layer, then routes to appropriate
	 * tesselate routine based on material properties.
	 */
	public void tesselateQuad(MutableQuadViewImpl quad, EncoderContext context) {
		final RenderMaterialImpl.Value mat = quad.material();

		// needs to happen before offsets are applied
		// TODO: move this check to the encoder
		if (!mat.disableAo(0) && MinecraftClient.isAmbientOcclusionEnabled()) {
			context.computeLighting(quad);
		}

		// TODO: handle multiple
		final int textureIndex = 0;

		if (context.blockInfo().defaultAo && !mat.disableAo(textureIndex)) {
			if (mat.emissive(textureIndex)) {
				tesselateSmoothEmissive(quad, context);
			} else {
				tesselateSmooth(quad, context);
			}
		} else {
			if (mat.emissive(textureIndex)) {
				tesselateFlatEmissive(quad, context);
			} else {
				tesselateFlat(quad, context);
			}
		}
	}

	private void bufferQuad(MutableQuadViewImpl quad, EncoderContext context) {
		final Matrix4f matrix = context.matrix();
		final int overlay = context.overlay();
		final Matrix3f normalMatrix = context.normalMatrix();
		final Vector3f normalVec = context.normalVec();
		final VertexConsumer buff = context.consumer(quad);

		final boolean useNormals = quad.hasVertexNormals();

		if (useNormals) {
			quad.populateMissingNormals();
		} else {
			final Vector3f faceNormal = quad.faceNormal();
			normalVec.set(faceNormal.getX(), faceNormal.getY(), faceNormal.getZ());
			normalVec.transform(normalMatrix);
		}

		for (int i = 0; i < 4; i++) {
			buff.vertex(matrix, quad.x(i), quad.y(i), quad.z(i));
			final int color = quad.spriteColor(i, 0);
			buff.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
			buff.texture(quad.spriteU(i, 0), quad.spriteV(i, 0));
			buff.overlay(overlay);
			buff.light(quad.lightmap(i));

			if (useNormals) {
				normalVec.set(quad.normalX(i), quad.normalY(i), quad.normalZ(i));
				normalVec.transform(normalMatrix);
			}

			buff.normal(normalVec.getX(), normalVec.getY(), normalVec.getZ());
			buff.next();
		}
	}

	// routines below have a bit of copy-paste code reuse to avoid conditional execution inside a hot loop

	/** for non-emissive mesh quads and all fallback quads with smooth lighting. */
	private void tesselateSmooth(MutableQuadViewImpl q, EncoderContext context) {
		colorizeQuad(q, context);

		for (int i = 0; i < 4; i++) {
			q.spriteColor(i, 0, ColorHelper.multiplyRGB(q.spriteColor(i, 0), q.ao[i]));
			q.lightmap(i, ColorHelper.maxBrightness(q.lightmap(i), q.light[i]));
		}

		bufferQuad(q, context);
	}

	/** for emissive mesh quads with smooth lighting. */
	private void tesselateSmoothEmissive(MutableQuadViewImpl q, EncoderContext context) {
		colorizeQuad(q, context);

		for (int i = 0; i < 4; i++) {
			q.spriteColor(i, 0, ColorHelper.multiplyRGB(q.spriteColor(i, 0), q.ao[i]));
			q.lightmap(i, FULL_BRIGHTNESS);
		}

		bufferQuad(q, context);
	}

	/** for non-emissive mesh quads and all fallback quads with flat lighting. */
	private void tesselateFlat(MutableQuadViewImpl quad, EncoderContext context) {
		colorizeQuad(quad, context);
		final int brightness = flatBrightness(quad, context.blockInfo());

		for (int i = 0; i < 4; i++) {
			quad.lightmap(i, ColorHelper.maxBrightness(quad.lightmap(i), brightness));
		}

		bufferQuad(quad, context);
	}

	/** for emissive mesh quads with flat lighting. */
	private void tesselateFlatEmissive(MutableQuadViewImpl quad, EncoderContext context) {
		colorizeQuad(quad, context);

		for (int i = 0; i < 4; i++) {
			quad.lightmap(i, FULL_BRIGHTNESS);
		}

		bufferQuad(quad, context);
	}

	private final BlockPos.Mutable mpos = new BlockPos.Mutable();

	/**
	 * Handles geometry-based check for using self brightness or neighbor brightness.
	 * That logic only applies in flat lighting.
	 */
	private int flatBrightness(MutableQuadViewImpl quad, BlockRenderInfo blockInfo) {
		final BlockState blockState = blockInfo.blockState;
		final BlockPos pos = blockInfo.blockPos;

		mpos.set(pos);

		if ((quad.geometryFlags() & LIGHT_FACE_FLAG) != 0 || Block.isShapeFullCube(blockState.getCollisionShape(blockInfo.blockView, pos))) {
			mpos.setOffset(quad.lightFace());
		}

		// Unfortunately cannot use brightness cache here unless we implement one specifically for flat lighting. See #329
		return WorldRenderer.getLightmapCoordinates(blockInfo.blockView, blockState, mpos);
	}

	/** handles block color and red-blue swizzle, common to all renders. */
	private void colorizeQuad(MutableQuadViewImpl quad, EncoderContext context) {

		final int colorIndex = quad.colorIndex();

		// TODO: handle layers

		if (colorIndex == -1 || quad.material().disableColorIndex(0)) {
			for (int i = 0; i < 4; i++) {
				quad.spriteColor(i, 0, ColorHelper.swapRedBlueIfNeeded(quad.spriteColor(i, 0)));
			}
		} else {
			final int indexedColor = context.indexedColor(colorIndex);

			for (int i = 0; i < 4; i++) {
				quad.spriteColor(i, 0, ColorHelper.swapRedBlueIfNeeded(ColorHelper.multiplyColor(indexedColor, quad.spriteColor(i, 0))));
			}
		}
	}
}