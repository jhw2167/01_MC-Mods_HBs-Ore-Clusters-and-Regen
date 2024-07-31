package com.holybuckets.orecluster.config;

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

//MineCraft Imports
import com.holybuckets.orecluster.OreClustersAndRegenMain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.telemetry.events.WorldLoadEvent;
import net.minecraft.core.Vec3i;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.server.ServerStartedEvent;
import org.apache.commons.lang3.tuple.Pair;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import com.holybuckets.foundation.ConfigBase;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.orecluster.OreClusterManager;
import com.holybuckets.orecluster.OreClustersAndRegenMain;
import com.holybuckets.orecluster.RealTimeConfig;


@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, modid = OreClustersAndRegenMain.MODID)
public class AllConfigs {

	/** World Data **/
	public static Minecraft mc = Minecraft.getInstance();
	public static Long WORLD_SEED;
	public static Vec3i WORLD_SPAWN;

	/** Configuration Data **/

	private static final Map<ModConfig.Type, ConfigBase> CONFIGS = new EnumMap<>(ModConfig.Type.class);

	private static CClient client;
	private static CCommon common;
	private static CServer server;

	public static CClient client() { return client; }

	public static CCommon common() {
		return common;
	}

	public static CServer server() {
		return server;
	}

	public static ConfigBase byType(ModConfig.Type type) {
		return CONFIGS.get(type);
	}

	private static <T extends ConfigBase> T register(Supplier<T> factory, ModConfig.Type side) {
		Pair<T, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(builder -> {
			T config = factory.get();
			config.registerAll(builder);
			return config;
		});

		T config = specPair.getLeft();
		config.specification = specPair.getRight();
		CONFIGS.put(side, config);
		return config;
	}

	public static void register(ModLoadingContext context) {
		client = register(CClient::new, ModConfig.Type.CLIENT);
		common = register(CCommon::new, ModConfig.Type.COMMON);
		server = register(CServer::new, ModConfig.Type.SERVER);

		for (Entry<ModConfig.Type, ConfigBase> pair : CONFIGS.entrySet())
			context.registerConfig(pair.getKey(), pair.getValue().specification);
	}

	public static void initWorldConfigs( MinecraftServer server )
	{
		// Capture the world seed
		LoggerBase.logInfo("**** WORLD LOAD EVENT ****");
		WORLD_SEED = server.overworld().getSeed();
		WORLD_SPAWN = server.overworld().getSharedSpawnPos();
		LoggerBase.logInfo("World Seed: " + WORLD_SEED);
		LoggerBase.logInfo("World Spawn: " + WORLD_SPAWN);
	}

	@SubscribeEvent
	public static void onLoad(ModConfigEvent.Loading event) {
		for (ConfigBase config : CONFIGS.values())
			if (config.specification == event.getConfig()
				.getSpec())
				config.onLoad();

		OreClusterManager.config = new RealTimeConfig();
		System.out.println("RealTimeConfig initialized current JSON property: ");
		System.out.println(AllConfigs.server().cOreClusters.oreClusters.get());
	}

	@SubscribeEvent
	public static void onReload(ModConfigEvent.Reloading event) {
		for (ConfigBase config : CONFIGS.values())
			if (config.specification == event.getConfig()
				.getSpec())
				config.onReload();

		OreClusterManager.config = new RealTimeConfig();
	}

	@SubscribeEvent
	public void onServerStarted(final ServerStartedEvent event)
	{
		LoggerBase.logInfo("**** SERVER STARTED EVENT ****");
	}




}
