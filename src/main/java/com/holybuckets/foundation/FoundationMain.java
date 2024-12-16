package com.holybuckets.foundation;

import net.minecraftforge.fml.common.Mod;

//@Mod( FoundationMain.MODID )
public class FoundationMain {

    public static final String MODID = "hbs_foundation";
    public static final String NAME = "HBs Foundation";
    public static final String VERSION = "1.0.0f";

    public FoundationMain() {
        super();
        LoggerBase.logInit( null, "001000", this.getClass().getName() );
    }

    public static void initMod() {
        LoggerBase.logInit( null, "001001", "Initializing Foundation" );
    }
}
