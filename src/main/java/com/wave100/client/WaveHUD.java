package com.wave100.client;

import com.wave100.entity.EngineState;
import com.wave100.entity.WaveMotorcycleEntity;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Minimal cockpit HUD shown while riding:
 * WAVE 100 title, speed (km/h), RPM bar with redline, gear, fuel bar and the
 * headlight state. Appears when mounting, disappears when dismounting.
 */
public final class WaveHUD {

    private static final int PANEL_BG = 0xB00D0F14;
    private static final int PANEL_EDGE = 0xFF3A4050;
    private static final int TEXT_DIM = 0xFF9AA4B2;
    private static final int TEXT_BRIGHT = 0xFFF2F5FA;
    private static final int BAR_BG = 0xFF232833;
    private static final int FUEL_OK = 0xFF37C871;
    private static final int FUEL_LOW = 0xFFE0562A;
    private static final int RPM_OK = 0xFF37C871;
    private static final int RPM_HIGH = 0xFFE8B93A;
    private static final int RPM_REDLINE = 0xFFE0562A;

    private WaveHUD() {
    }

    /** Rendered via a registered GUI layer (see WaveClientEvents). */
    public static void render(GuiGraphics gui, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        if (!(mc.player.getVehicle() instanceof WaveMotorcycleEntity bike)) {
            return;
        }

        Font font = mc.font;
        int x = 8;
        int y = gui.guiHeight() - 78;
        int w = 138;
        int h = 70;

        gui.fill(x - 3, y - 3, x + w + 3, y + h + 3, PANEL_BG);
        gui.fill(x - 3, y - 3, x + w + 3, y - 2, PANEL_EDGE);
        gui.fill(x - 3, y + h + 2, x + w + 3, y + h + 3, PANEL_EDGE);
        gui.fill(x - 3, y - 3, x - 2, y + h + 3, PANEL_EDGE);
        gui.fill(x + w + 2, y - 3, x + w + 3, y + h + 3, PANEL_EDGE);

        // title
        gui.drawString(font, Component.translatable("hud.wave100.title"), x, y, TEXT_BRIGHT);

        // speed
        int kmh = Mth.floor(bike.getSpeedKmh());
        String speed = kmh + " km/h";
        gui.drawString(font, "SPEED", x, y + 13, TEXT_DIM);
        gui.drawString(font, speed, x + w - font.width(speed), y + 13, TEXT_BRIGHT);

        // gear + engine state
        String gear = "N";
        if (bike.getEngineState() == EngineState.RUNNING) {
            gear = String.valueOf(bike.getGear());
        }
        gui.drawString(font, "GEAR", x, y + 25, TEXT_DIM);
        gui.drawString(font, gear, x + 34, y + 25, TEXT_BRIGHT);

        String light = bike.isHeadlightOn() ? "ON" : "OFF";
        int lightColor = bike.isHeadlightOn() ? 0xFFF2D06B : TEXT_DIM;
        gui.drawString(font, "LIGHT", x + 48, y + 25, TEXT_DIM);
        gui.drawString(font, light, x + 86, y + 25, lightColor);

        // RPM bar
        gui.drawString(font, "RPM", x, y + 38, TEXT_DIM);
        String rpmText = String.valueOf(bike.getRpm());
        gui.drawString(font, rpmText, x + w - font.width(rpmText), y + 38, rpmColor(bike));
        drawBar(gui, x, y + 47, w, 6, bike.getRpm() / 8000F, rpmColor(bike));

        // fuel bar
        gui.drawString(font, "FUEL", x, y + 57, TEXT_DIM);
        float fuelFrac = bike.getFuel() / Math.max(1F, bike.getMaxFuel());
        String fuelText = Math.round(fuelFrac * 100) + "%";
        gui.drawString(font, fuelText, x + w - font.width(fuelText), y + 57,
                fuelFrac < 0.2F ? FUEL_LOW : TEXT_BRIGHT);
        drawBar(gui, x, y + 66, w, 4, fuelFrac, fuelFrac < 0.2F ? FUEL_LOW : FUEL_OK);
    }

    private static int rpmColor(WaveMotorcycleEntity bike) {
        float frac = bike.getRpm() / 8000F;
        if (frac > 0.86F) {
            return RPM_REDLINE;
        }
        if (frac > 0.62F) {
            return RPM_HIGH;
        }
        return RPM_OK;
    }

    private static void drawBar(GuiGraphics gui, int x, int y, int w, int h, float frac, int color) {
        frac = Mth.clamp(frac, 0F, 1F);
        gui.fill(x, y, x + w, y + h, BAR_BG);
        gui.fill(x, y, x + (int) (w * frac), y + h, color);
    }
}
