package com.levi.aeroshipsaver.client;

import org.lwjgl.glfw.GLFW;

import com.levi.aeroshipsaver.AeroShipSaver;

import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/** Registers the keybinding that opens the AeroShip menu (default: G). */
@EventBusSubscriber(modid = AeroShipSaver.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class AeroKeyMappings {
    public static final KeyMapping OPEN_MENU = new KeyMapping(
        "key.aeroshipsaver.open_menu",
        GLFW.GLFW_KEY_G,
        "key.categories.aeroshipsaver");

    private AeroKeyMappings() {}

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_MENU);
    }
}
