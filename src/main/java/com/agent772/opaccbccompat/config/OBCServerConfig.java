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
                .comment(
                        "Master switch for CBC projectile block protection.",
                        "When true (default), CBC block destruction is routed through OPAC so that",
                        "your existing per-claim block-protection options (and any 'blockAccessEntityGroups'",
                        "you define in OPAC's server config) decide whether cannon fire may break blocks.",
                        "When false, the bridge is inert and cannons break blocks exactly like vanilla CBC.")
                .define("protectBlocks", true);
        PROTECT_ENTITIES = b
                .comment(
                        "Master switch for CBC projectile entity protection.",
                        "When true (default), CBC entity damage is routed through OPAC so that",
                        "your existing per-claim entity-protection options (and any 'entityAccessEntityGroups'",
                        "you define in OPAC's server config) decide whether cannon fire may hurt entities.",
                        "When false, the bridge is inert and cannons damage entities exactly like vanilla CBC.")
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
