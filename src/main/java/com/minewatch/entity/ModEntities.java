package com.minewatch.entity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModEntities {
    public static final EntityType<BotEntity> BOT = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of("minewatch", "bot"),
            FabricEntityTypeBuilder.create(SpawnGroup.MISC, BotEntity::new)
                    .dimensions(EntityDimensions.fixed(1.0f, 2.6f).withEyeHeight(2.2f))
                    .trackRangeBlocks(96)
                    .build());

    public static void init() {
        FabricDefaultAttributeRegistry.register(BOT, BotEntity.createAttributes());
    }
    private ModEntities() {}
}
