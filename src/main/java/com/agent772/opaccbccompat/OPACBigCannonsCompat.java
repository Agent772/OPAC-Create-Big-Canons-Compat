package com.agent772.opaccbccompat;

import org.slf4j.Logger;

import com.agent772.opaccbccompat.compat.ExplosionEntityGate;
import com.mojang.logging.LogUtils;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(OPACBigCannonsCompat.MODID)
public class OPACBigCannonsCompat {
    public static final String MODID = "opaccbccompat";
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Whether to log every OPAC protection verdict for CBC projectile damage.
     * Off by default; enable with {@code -Dopaccbccompat.debugLogging=true} on the
     * server JVM. Read once at class-init so the guard constant-folds to a no-op
     * when disabled: {@code logVerdict} does per-query OPAC API calls and string
     * building, and a single shell can query hundreds of positions, so this must
     * be genuinely off unless an admin opts in.
     */
    public static final boolean DEBUG_LOGGING = Boolean.getBoolean("opaccbccompat.debugLogging");

    public OPACBigCannonsCompat() {
        NeoForge.EVENT_BUS.register(new ExplosionEntityGate());
        LOGGER.info("OPAC - Create: Big Cannons Compat initialized!");
    }
}
