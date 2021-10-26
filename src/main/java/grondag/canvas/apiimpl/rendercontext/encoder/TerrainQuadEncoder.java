/*
 * Copyright © Contributing Authors
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
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.canvas.apiimpl.rendercontext.encoder;

import io.vram.frex.api.model.InputContext;
import io.vram.frex.base.renderer.mesh.BaseQuadEmitter;

import grondag.canvas.buffer.format.TerrainEncoder;
import grondag.canvas.buffer.input.VertexCollectorList;
import grondag.canvas.material.state.CanvasRenderMaterial;
import grondag.canvas.render.terrain.TerrainSectorMap.RegionRenderSector;
import grondag.canvas.terrain.region.RegionPosition;

public class TerrainQuadEncoder extends BaseQuadEncoder {
	/** Used by some terrain render configs to pass a region ID into vertex encoding. */
	private int sectorId;
	private int sectorRelativeRegionOrigin;

	public TerrainQuadEncoder(BaseQuadEmitter emitter, InputContext inputContext) {
		super(emitter, inputContext);
		collectors = new VertexCollectorList(true, true);
	}

	public final int sectorId() {
		return sectorId;
	}

	public final int sectorRelativeRegionOrigin() {
		return sectorRelativeRegionOrigin;
	}

	public void updateSector(RegionRenderSector renderSector, RegionPosition origin) {
		sectorId = renderSector.sectorId();
		sectorRelativeRegionOrigin = renderSector.sectorRelativeRegionOrigin(origin);
	}

	public void encode() {
		trackAnimation(emitter);
		TerrainEncoder.encodeQuad(this, collectors.get((CanvasRenderMaterial) emitter.material()));
	}

	public BaseQuadEmitter emitter() {
		return emitter;
	}

	public InputContext inputContext() {
		return inputContext;
	}
}