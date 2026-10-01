package com.dontlookaway.client;

import java.util.concurrent.ThreadLocalRandom;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class DontLookAwayClient implements ClientModInitializer {
    private final EncounterDirector director = new EncounterDirector();

    @Override
    public void onInitializeClient() {
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

        StalkerEncounter(Minecraft client, ThreadLocalRandom rng) {
            distance = rng.nextDouble(28.0, 42.0);
            bearing = Mth.wrapDegrees(client.player.getYRot() + 180.0F + rng.nextFloat(-42.0F, 42.0F));
            lastPlayerPosition = client.player.position();
        }

        boolean tick(Minecraft client) {
            Vec3 now = client.player.position();
            double moved = now.distanceTo(lastPlayerPosition);
            lastPlayerPosition = now;

            float lookDifference = Math.abs(Mth.wrapDegrees(client.player.getYRot() - bearing));
            boolean watched = lookDifference < 31.0F;

            if (watched) {
                unseenTicks = 0;
            } else {
                unseenTicks++;
                // It moves in unsettling bursts rather than gliding continuously.
                if (unseenTicks % 8 == 0) distance -= distance > 14.0 ? 1.15 : 0.62;
            }

            // Running away can still save you. Looking at it is defensive, not a guaranteed win.
            if (moved > 0.18 && lookDifference > 105.0F) distance += 0.18;

            // Escape, catch, or timeout. Rendering/catch presentation comes next.
            if (distance >= 52.0) return true;
            if (distance <= 1.8) return true;
            return --lifetime <= 0;
        }
    }
}
