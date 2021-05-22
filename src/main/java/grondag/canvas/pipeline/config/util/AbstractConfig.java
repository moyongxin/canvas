/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package grondag.canvas.pipeline.config.util;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonObject;

import grondag.canvas.CanvasMod;

public abstract class AbstractConfig {
	protected final ConfigContext context;

	protected AbstractConfig(ConfigContext context) {
		this.context = context;
	}

	public abstract boolean validate();

	public static boolean assertAndWarn(boolean isOK, String msg) {
		if (!isOK) {
			CanvasMod.LOG.warn(msg);
		}

		return isOK;
	}

	public static boolean assertAndWarn(boolean isOK, String msg, Object... args) {
		return assertAndWarn(isOK, String.format(msg, args));
	}

	protected static String[] readerSamplerNames(ConfigContext ctx, JsonObject config, String programName) {
		if (!config.containsKey("samplers")) {
			return new String[0];
		} else {
			final JsonArray names = config.get(JsonArray.class, "samplers");
			final int limit = names.size();
			final String[] samplerNames = new String[limit];

			for (int i = 0; i < limit; ++i) {
				final String s = JanksonHelper.asString(names.get(i));

				if (s == null) {
					CanvasMod.LOG.warn(String.format("Sampler name %s (%d of %d) for %s is not a valid string and was skipped.",
							names.get(i).toString(), i, limit, programName));
				} else {
					samplerNames[i] = s;
				}
			}

			return samplerNames;
		}
	}
}
