package com.levi.aeroshipsaver.client;

import com.levi.aeroshipsaver.AeroShipSaver;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opens the menu when the keybinding is pressed and the player is in-game. */
@EventBusSubscriber(modid = AeroShipSaver.MODID, value = Dist.CLIENT)
public final class AeroClientEvents {
    private AeroClientEvents() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }
        while (AeroKeyMappings.OPEN_MENU.consumeClick()) {
            mc.setScreen(new AeroShipScreen());
        }
    }
}
