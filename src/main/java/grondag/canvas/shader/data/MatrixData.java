/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package grondag.canvas.shader.data;

import java.nio.FloatBuffer;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;

import io.vram.frex.api.math.FrexMathUtil;

import grondag.canvas.mixinterface.GameRendererExt;

public final class MatrixData {
	private MatrixData() { }

	private static final Matrix3f IDENTITY = new Matrix3f();

	static {
		IDENTITY.identity();
	}

	static void update(PoseStack.Pose view, Matrix4f projectionMatrix, Camera camera, float tickDelta) {
		// write values for prior frame before updating
		FrexMathUtil.writeToBuffer(viewMatrix, VIEW_LAST * 16, MATRIX_DATA);
		FrexMathUtil.writeToBuffer(projMatrix, PROJ_LAST * 16, MATRIX_DATA);
		FrexMathUtil.writeToBuffer(viewProjMatrix, VP_LAST * 16, MATRIX_DATA);
		FrexMathUtil.writeToBuffer(cleanProjMatrix, CLEAN_PROJ_LAST * 16, MATRIX_DATA);
		FrexMathUtil.writeToBuffer(cleanViewProjMatrix, CLEAN_VP_LAST * 16, MATRIX_DATA);

		viewNormalMatrix.set(view.normal());

		viewMatrix.set(view.pose());
		FrexMathUtil.writeToBuffer(viewMatrix, VIEW * 16, MATRIX_DATA);
		projMatrix.set(projectionMatrix);
		FrexMathUtil.writeToBuffer(projMatrix, PROJ * 16, MATRIX_DATA);

		viewMatrixInv.set(viewMatrix);
		// reliable inversion of rotation matrix
		viewMatrixInv.transpose();
		FrexMathUtil.writeToBuffer(viewMatrixInv, VIEW_INVERSE * 16, MATRIX_DATA);

		projMatrixInv.set(projMatrix);
		projMatrixInv.invert();
		FrexMathUtil.writeToBuffer(projMatrixInv, PROJ_INVERSE * 16, MATRIX_DATA);

		viewProjMatrix.set(projMatrix);
		viewProjMatrix.mul(viewMatrix);
		FrexMathUtil.writeToBuffer(viewProjMatrix, VP * 16, MATRIX_DATA);

		viewProjMatrixInv.set(viewMatrixInv);
		viewProjMatrixInv.mul(projMatrixInv);
		FrexMathUtil.writeToBuffer(viewProjMatrixInv, VP_INVERSE * 16, MATRIX_DATA);

		computeCleanProjection(camera, tickDelta);
		FrexMathUtil.writeToBuffer(cleanProjMatrix, CLEAN_PROJ * 16, MATRIX_DATA);
		FrexMathUtil.writeToBuffer(cleanProjMatrixInv, CLEAN_PROJ_INVERSE * 16, MATRIX_DATA);

		cleanViewProjMatrix.set(cleanProjMatrix);
		cleanViewProjMatrix.mul(viewMatrix);
		FrexMathUtil.writeToBuffer(cleanViewProjMatrix, CLEAN_VP * 16, MATRIX_DATA);

		cleanViewProjMatrixInv.set(viewMatrixInv);
		cleanViewProjMatrixInv.mul(cleanProjMatrixInv);
		FrexMathUtil.writeToBuffer(cleanViewProjMatrixInv, CLEAN_VP_INVERSE * 16, MATRIX_DATA);

		//cleanFrustum.prepare(viewMatrix, tickDelta, camera, cleanProjMatrix);
		//cleanFrustum.computeCircumCenter(viewMatrixInv, cleanProjMatrixInv);
	}

	/**
	 * Computes projection that doesn't include nausea or view bob and doesn't have 4X depth like vanilla.
	 */
	private static void computeCleanProjection(Camera camera, float tickDelta) {
		final Minecraft mc = Minecraft.getInstance();
		final GameRendererExt gx = (GameRendererExt) mc.gameRenderer;
		final float zoom = gx.canvas_zoom();

		cleanProjMatrix.identity();

		if (zoom != 1.0F) {
			cleanProjMatrix.translate(gx.canvas_zoomX(), -gx.canvas_zoomY(), 0.0f);
			cleanProjMatrix.scale(zoom, zoom, 1.0F);
		}

		cleanProjMatrix.mul(new Matrix4f().perspective((float) gx.canvas_getFov(camera, tickDelta, true), (float) mc.getWindow().getWidth() / (float) mc.getWindow().getHeight(), 0.05F, mc.gameRenderer.getRenderDistance()));

		cleanProjMatrixInv.set(cleanProjMatrix);
		cleanProjMatrixInv.invert();
	}

	public static final Matrix4f viewMatrix = new Matrix4f();
	private static final Matrix4f viewMatrixInv = new Matrix4f();

	public static final Matrix4f projMatrix = new Matrix4f();
	private static final Matrix4f projMatrixInv = new Matrix4f();

	private static final Matrix4f viewProjMatrix = new Matrix4f();
	private static final Matrix4f viewProjMatrixInv = new Matrix4f();

	public static final Matrix4f cleanProjMatrix = new Matrix4f();
	private static final Matrix4f cleanProjMatrixInv = new Matrix4f();

	private static final Matrix4f cleanViewProjMatrix = new Matrix4f();
	private static final Matrix4f cleanViewProjMatrixInv = new Matrix4f();

	public static final Matrix3f viewNormalMatrix = new Matrix3f();

	private static final int VIEW = 0;
	private static final int VIEW_INVERSE = 1;
	private static final int VIEW_LAST = 2;
	private static final int PROJ = 3;
	private static final int PROJ_INVERSE = 4;
	private static final int PROJ_LAST = 5;
	private static final int VP = 6;
	private static final int VP_INVERSE = 7;
	private static final int VP_LAST = 8;

	static final int SHADOW_VIEW = 9;
	static final int SHADOW_VIEW_INVERSE = 10;
	// base index of cascades 0-3
	static final int SHADOW_PROJ_0 = 11;
	// base index of cascades 0-3
	static final int SHADOW_VIEW_PROJ_0 = 15;

	private static final int CLEAN_PROJ = 19;
	private static final int CLEAN_PROJ_INVERSE = 20;
	private static final int CLEAN_PROJ_LAST = 21;
	private static final int CLEAN_VP = 22;
	private static final int CLEAN_VP_INVERSE = 23;
	private static final int CLEAN_VP_LAST = 24;

	public static final int COUNT = 25;
	public static final FloatBuffer MATRIX_DATA = BufferUtils.createFloatBuffer(COUNT * 16);
}
