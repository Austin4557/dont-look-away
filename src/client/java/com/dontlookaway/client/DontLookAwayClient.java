package com.dontlookaway.client;

import java.util.concurrent.ThreadLocalRandom;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class DontLookAwayClient implements ClientModInitializer {
    private static final Identifier STALKER_TEXTURE = Identifier.fromNamespaceAndPath("dont_look_away", "textures/gui/stalker.png");
    private static final Identifier STALKER_HUD = Identifier.fromNamespaceAndPath("dont_look_away", "stalker");
    private final EncounterDirector director = new EncounterDirector();

    @Override
    public void onInitializeClient() {
        HudElementRegistry.addLast(STALKER_HUD, (graphics, deltaTracker) -> {
            StalkerEncounter stalker = director.active;
            if (stalker == null || !stalker.isVisible()) return;
            Minecraft client = Minecraft.getInstance();
            int sw = client.getWindow().getGuiScaledWidth();
            int sh = client.getWindow().getGuiScaledHeight();
            float difference = Mth.wrapDegrees(client.player.getYRot() - stalker.bearing);
            int size = Mth.clamp((int)(sh * (0.20 + (1.0 - Math.min(stalker.distance, 42.0) / 42.0) * 0.72)), 30, (int)(sh * 0.90));
            double agitation = stalker.distance < 8.0 ? 2.8 : stalker.distance < 16.0 ? 1.6 : 0.8;
            int sway = (int)(Math.sin(stalker.age * 0.12) * agitation);
            int x = sw / 2 - size / 2 + (int)(difference / 31.0F * sw * 0.32F) + sway;
            int bob = stalker.distance < 8.0 ? (int)(Math.sin(stalker.age * 0.21) * 3.0) : stalker.distance < 16.0 ? (int)(Math.sin(stalker.age * 0.15) * 2.0) : 0;
            int y = sh - size - Math.max(4, sh / 18) + bob;
            if (stalker.catchTicks > 0) {
                graphics.fill(0, 0, sw, sh, 0xD9000000);
                size = Math.min(sh, (int)(sh * 0.96));
                x = sw / 2 - size / 2;
                y = sh / 2 - size / 2;
            }
            graphics.blit(RenderPipelines.GUI_TEXTURED, STALKER_TEXTURE, x, y, 0.0F, 0.0F, size, size, 512, 512);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) {
                director.reset();
                return;
            }
            director.tick(client);
        });
    }

    static final class EncounterDirector {
        private int cooldown = 20 * 90;
        private StalkerEncounter active;

        void reset() {
            cooldown = 20 * 90;
            active = null;
        }

        void tick(Minecraft client) {
            if (active != null) {
                if (active.tick(client)) {
                    active = null;
                    cooldown = ThreadLocalRandom.current().nextInt(20 * 180, 20 * 421);
                }
                return;
            }

            if (--cooldown <= 0) {
                active = new StalkerEncounter(client, ThreadLocalRandom.current());
            }
        }
    }

    static final class StalkerEncounter {
        private double distance;
        private float bearing;
        private int lifetime = 20 * 75;
        private int unseenTicks;
        private Vec3 lastPlayerPosition;
        private int age;
        private boolean watched;
        private int catchTicks;

        StalkerEncounter(Minecraft client, ThreadLocalRandom rng) {
            distance = rng.nextDouble(28.0, 42.0);
            bearing = Mth.wrapDegrees(client.player.getYRot() + 180.0F + rng.nextFloat(-42.0F, 42.0F));
            lastPlayerPosition = client.player.position();
        }

        boolean tick(Minecraft client) {
            age++;
            Vec3 now = client.player.position();
            double moved = now.distanceTo(lastPlayerPosition);
            lastPlayerPosition = now;

            float lookDifference = Math.abs(Mth.wrapDegrees(client.player.getYRot() - bearing));
            watched = lookDifference < 31.0F;

            if (watched) {
                unseenTicks = 0;
            } else {
                unseenTicks++;
                // It moves in unsettling bursts rather than gliding continuously.
                if (unseenTicks % 8 == 0) distance -= distance > 14.0 ? 1.15 : 0.62;
            }

            // Running away can still save you. Looking at it is defensive, not a guaranteed win.
            if (moved > 0.18 && lookDifference > 105.0F) distance += 0.18;

            if (catchTicks > 0) return --catchTicks <= 0;
            if (distance >= 52.0) return true;
            if (distance <= 1.8) {
                distance = 1.0;
                watched = true;
                catchTicks = 12;
                client.player.playSound(SoundEvents.ENDERMAN_STARE, 0.72F, 0.74F);
                client.player.playSound(SoundEvents.GHAST_SCREAM, 0.58F, 1.38F);
                return false;
            }
            return --lifetime <= 0;
        }

        boolean isVisible() {
            return watched;
        }
    }
}
