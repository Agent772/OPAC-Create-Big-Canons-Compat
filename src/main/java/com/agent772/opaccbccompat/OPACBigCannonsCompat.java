package com.agent772.opaccbccompat;

import org.slf4j.Logger;

import com.agent772.opaccbccompat.compat.ExplosionEntityGate;
import com.agent772.opaccbccompat.config.OBCServerConfig;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(OPACBigCannonsCompat.MODID)
public class OPACBigCannonsCompat {
    public static final String MODID = "opaccbccompat";
    public static final Logger LOGGER = LogUtils.getLogger();

    public OPACBigCannonsCompat(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, OBCServerConfig.SPEC);
        NeoForge.EVENT_BUS.register(new ExplosionEntityGate());
        LOGGER.info("OPAC - Create: Big Cannons Compat initialized!");
    }
}
