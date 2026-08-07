package com.agent772.opaccbccompat.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class OBCServerConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue PROTECT_BLOCKS;
    public static final ModConfigSpec.BooleanValue PROTECT_ENTITIES;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("protection");
        PROTECT_BLOCKS = b
                .comment("If true (default), Create Big Cannons projectiles cannot destroy blocks inside OPAC chunk claims.")
                .define("protectBlocks", true);
        PROTECT_ENTITIES = b
                .comment("If true (default), Create Big Cannons projectiles cannot damage entities inside OPAC chunk claims.")
                .define("protectEntities", true);
        b.pop();

        SPEC = b.build();
    }

    /**
     * Whether block destruction protection is enabled. Falls back to the defined
     * default when the config spec is not yet bound (e.g. contexts with no world
     * loaded), where reading the value directly would throw.
     */
    public static boolean protectBlocks() {
        return SPEC.isLoaded() ? PROTECT_BLOCKS.get() : PROTECT_BLOCKS.getDefault();
    }

    /**
     * Whether entity damage protection is enabled. Falls back to the defined
     * default when the config spec is not yet bound.
     */
    public static boolean protectEntities() {
        return SPEC.isLoaded() ? PROTECT_ENTITIES.get() : PROTECT_ENTITIES.getDefault();
    }

    private OBCServerConfig() {}
}
