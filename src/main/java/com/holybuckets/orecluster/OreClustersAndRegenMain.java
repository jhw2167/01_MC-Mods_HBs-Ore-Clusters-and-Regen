package com.holybuckets.orecluster;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.holybuckets.foundation.GeneralRealTimeConfig;
import com.holybuckets.orecluster.config.AllConfigs;
import com.holybuckets.orecluster.core.OreClusterManager;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(OreClustersAndRegenMain.MODID)
public class OreClustersAndRegenMain
{
    public static final String CLASS_ID = "001";    //unused variable, value will be used for logging messages

    // Define mod id in a common place for everything to reference
    public static final String MODID = "hbs_ore_clusters_and_regen";
    public static final String NAME = "HB's Ore Clusters and Regen";
    public static final String VERSION = "1.0.0f";

    public static final Logger LOGGER = LogUtils.getLogger();

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    public static GeneralRealTimeConfig generalRealTimeConfig = GeneralRealTimeConfig.getInstance();
    static {
        generalRealTimeConfig.registerOnLevelLoad( OreClustersAndRegenMain::onLoadWorld );

    }
    public static ModRealTimeConfig modRealTimeConfig = null;
    public static final Boolean DEBUG = true;

    /** Use the {@link Random} of a local {@link Level} or {@link Entity} or create one */
    @Deprecated
    public static final Random RANDOM = new Random();
    public static Map<LevelAccessor, OreClusterManager> oreClusterManagers = new HashMap<>();

    /**
     * <b>Other mods should not use this field!</b> If you are an addon developer, create your own instance of
     *
     *
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(ID);

    static {
        REGISTRATE.setTooltipModifierFactory(item -> {
            return new ItemDescription.Modifier(item, Palette.STANDARD_CREATE)
                    .andThen(TooltipModifier.mapNull(KineticStats.create(item)));
        });
    }

    public static final ServerSchematicLoader SCHEMATIC_RECEIVER = new ServerSchematicLoader();
    public static final RedstoneLinkNetworkHandler REDSTONE_LINK_NETWORK_HANDLER = new RedstoneLinkNetworkHandler();
    public static final TorquePropagator TORQUE_PROPAGATOR = new TorquePropagator();
    public static final GlobalRailwayManager RAILWAYS = new GlobalRailwayManager();
    public static final ServerLagger LAGGER = new ServerLagger();
    */

    public OreClustersAndRegenMain() {
        initMod();
        LoggerProject.logInit( "001000", this.getClass().getName() );
    }

    public static void initMod() {
        ModLoadingContext modLoadingContext = ModLoadingContext.get();

        IEventBus modEventBus = FMLJavaModLoadingContext.get()
                .getModEventBus();
        IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;

        /*
        REGISTRATE.registerEventListeners(modEventBus);

        AllSoundEvents.prepare();
        AllTags.init();
        AllCreativeModeTabs.register(modEventBus);
        AllBlocks.register();
        AllItems.register();
        AllFluids.register();
        AllPaletteBlocks.register();
        AllMenuTypes.register();
        AllEntityTypes.register();
        AllBlockEntityTypes.register();
        AllEnchantments.register();
        AllRecipeTypes.register(modEventBus);
        AllParticleTypes.register(modEventBus);
        AllStructureProcessorTypes.register(modEventBus);
        AllEntityDataSerializers.register(modEventBus);
        AllPackets.registerPackets();
        AllFeatures.register(modEventBus);
        AllPlacementModifiers.register(modEventBus);
        */
        AllConfigs.register(modLoadingContext);

        /*
        // FIXME: some of these registrations are not thread-safe
        AllMovementBehaviours.registerDefaults();
        AllInteractionBehaviours.registerDefaults();
        AllPortalTracks.registerDefaults();
        AllDisplayBehaviours.registerDefaults();
        ContraptionMovementSetting.registerDefaults();
        AllArmInteractionPointTypes.register();
        AllFanProcessingTypes.register();
        BlockSpoutingBehaviour.registerDefaults();
        BogeySizes.init();
        AllBogeyStyles.register();
        // ----

        ComputerCraftProxy.register();

        ForgeMod.enableMilkFluid();
        CopperRegistries.inject();

        modEventBus.addListener(com.simibubi.create.Create::init);
        modEventBus.addListener(EventPriority.LOWEST, CreateDatagen::gatherData);
        modEventBus.addListener(AllSoundEvents::register);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> CreateClient.onCtorClient(modEventBus, forgeEventBus));

        // FIXME: this is not thread-safe
        Mods.CURIOS.executeIfInstalled(() -> () -> Curios.init(modEventBus, forgeEventBus));
         */

    }

    public static void onLoad(ModConfigEvent.Loading event) {
        LoggerProject.logInit( "001001", "Main-onLoad" );
    }

    public static void onReload(ModConfigEvent.Reloading event) {
        LoggerProject.logInit( "001002",  "Main-onReLoad" );
    }

    public static void onLoadWorld( LevelEvent.Load event )
    {
        LoggerProject.logDebug("001003", "**** WORLD LOAD EVENT ****");
        LevelAccessor level = event.getLevel();
        if( modRealTimeConfig == null )
        {
            modRealTimeConfig = new ModRealTimeConfig( level );
        }

        oreClusterManagers.put( level, new OreClusterManager(level, modRealTimeConfig) );
    }

    public static void onUnloadWorld(LevelAccessor world)
    {
        // Capture the world seed
        LoggerProject.logDebug("001004", "**** WORLD UNLOAD EVENT ****");
        /*
        oreClusterManager.shutdown();
        modRealTimeConfig = null;
        oreClusterManager = null;
        */
    }



    /*
    public static void init(final FMLCommonSetupEvent event) {
        AllFluids.registerFluidInteractions();

        event.enqueueWork(() -> {
            // TODO: custom registration should all happen in one place
            // Most registration happens in the constructor.
            // These registrations use Create's registered objects directly so they must run after registration has finished.
            BuiltinPotatoProjectileTypes.register();
            BoilerHeaters.registerDefaults();
            // --

            AttachedRegistry.unwrapAll();
            AllAdvancements.register();
            AllTriggers.register();
        });
    }

    public static ResourceLocation asResource(String path) {
        return new ResourceLocation(ID, path);
    }
    */
}
