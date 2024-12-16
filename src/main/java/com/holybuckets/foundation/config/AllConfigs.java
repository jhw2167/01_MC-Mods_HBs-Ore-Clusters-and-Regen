package com.holybuckets.foundation.config;

import com.holybuckets.foundation.FoundationMain;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.orecluster.OreClustersAndRegenMain;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;


//@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, modid = FoundationMain.MODID)
public class AllConfigs {

	/** Configuration Data **/

	public static final String CLASS_ID = "005";

	private static final Map<ModConfig.Type, ConfigBase> CONFIGS = new EnumMap<>(ModConfig.Type.class);

	//private static CClient client;
	//private static CCommon common;
	private static CServer server;

	//public static CClient client() { return client; }

	//public static CCommon common() { return common; }

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
		//client = register(CClient::new, ModConfig.Type.CLIENT);
		//common = register(CCommon::new, ModConfig.Type.COMMON);
		server = register(CServer::new, ModConfig.Type.SERVER);

		for (Entry<ModConfig.Type, ConfigBase> pair : CONFIGS.entrySet())
			context.registerConfig(pair.getKey(), pair.getValue().specification);
	}



	//@SubscribeEvent
	public static void onLoad(ModConfigEvent.Loading event) {
		for (ConfigBase config : CONFIGS.values())
			if (config.specification == event.getConfig()
				.getSpec())
				config.onLoad();

		LoggerBase.logInit( null,"005001","AllConfigs-onLoad" );
	}

	//@SubscribeEvent
	public static void onReload(ModConfigEvent.Reloading event) {
		for (ConfigBase config : CONFIGS.values())
			if (config.specification == event.getConfig()
				.getSpec())
				config.onReload();

		LoggerBase.logInit( null,"005002","AllConfigs-onReLoad" );
	}

	@SubscribeEvent
	public void onServerStarted(final ServerStartedEvent event)
	{
		LoggerBase.logInfo(null,"005003","**** SERVER STARTED EVENT ****");
	}




}
