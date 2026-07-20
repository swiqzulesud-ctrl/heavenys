package com.heavenys;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entrypoint for Heavenys Client.
 *
 * <p>Heavenys is a client-only utility mod, so almost all of the logic lives in the
 * {@code client} source set ({@link com.heavenys.client.HeavenysClientMod}). This common
 * initializer only exists to expose shared constants (mod id, logger) that any environment
 * can reference safely.
 */
public class HeavenysClient implements ModInitializer {
	public static final String MOD_ID = "heavenys";
	public static final String MOD_NAME = "Heavenys Client";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

	@Override
	public void onInitialize() {
		LOGGER.info("[{}] Common bootstrap complete.", MOD_NAME);
	}
}
