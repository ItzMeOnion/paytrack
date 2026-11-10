package dev.paytrack;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** Draws all celebration effects on the local screen only. Nothing here is sent to the server. */
public final class EffectRenderer {
    private static final Random RNG = new Random();
    private static final int[] PALETTE = {
            0xFFFF5555, 0xFF55FF55, 0xFF55FFFF, 0xFFFFFF55, 0xFFFF55FF, 0xFFFFAA00, 0xFFFFFFFF
    };

    private static final List<Toast> toasts = new ArrayList<>();
    private static final List<Spark> sparks = new ArrayList<>();
    private static Banner banner = null;
    private static int flashLife = 0;
    private static int flashMax = 1;
    private static int flashColor = 0;
    private static int rainbowLife = 0;
    private static int showerTicks = 0;
    private static float showerChance = 0f;
    private static int showerColor = 0xFFFFFFFF;
    private static int tickCount = 0;

    private EffectRenderer() {}

    // ---------------------------------------------------------------- triggers

    public static void play(EffectTier tier, String name, long amount) {
        toasts.add(0, new Toast("\"" + name + "\" paid " + PaymentTracker.shortAmount(amount), tier.color, 100));
        while (toasts.size() > 5) {
            toasts.remove(toasts.size() - 1);
        }
        playSound(tier);

        switch (tier) {
            case TOAST -> { }
            case SMALL_FIREWORKS -> {
                for (int i = 0; i < 3; i++) burst(tier.color, 30);
            }
            case BIG_FIREWORKS -> {
                setFlash(0x66FF55FF, 14);
                shower(tier.color, 40, 0.25f);
                showBanner("WOW", name, tier.color, 50, false);
            }
            case JACKPOT -> {
                setFlash(0x66FFD700, 24);
                shower(0xFFFFAA00, 70, 0.45f);
                showBanner("JACKPOT!", name, 0xFFFFD700, 100, false);
            }
            case BILLION -> {
                setFlash(0x88FFFFFF, 30);
                rainbowLife = 200;
                shower(0xFFFFFFFF, 220, 0.7f);
                showBanner("BILLION!", name + "  -  really really really cool", 0, 200, true);
            }
        }
    }

    private static void playSound(EffectTier tier) {
        MinecraftClient mc = MinecraftClient.getInstance();
        SoundEvent sound = switch (tier) {
            case TOAST, JACKPOT -> SoundEvents.ENTITY_PLAYER_LEVELUP;
            case SMALL_FIREWORKS, BIG_FIREWORKS -> SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST;
            case BILLION -> SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH;
        };
        float pitch = tier == EffectTier.BILLION ? 0.6f : 1.0f;
        mc.getSoundManager().play(PositionedSoundInstance.master(sound, pitch));
    }

    private static void burst(int baseColor, int count) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int w = mc.getWindow().getScaledWidth();
        int h = mc.getWindow().getScaledHeight();
        double cx = w * (0.2 + RNG.nextDouble() * 0.6);
        double cy = h * (0.12 + RNG.nextDouble() * 0.35);
        for (int i = 0; i < count; i++) {
            double angle = RNG.nextDouble() * Math.PI * 2;
            double speed = 1.5 + RNG.nextDouble() * 3.0;
            int color = RNG.nextBoolean() ? baseColor : PALETTE[RNG.nextInt(PALETTE.length)];
            int life = 30 + RNG.nextInt(20);
            sparks.add(new Spark(cx, cy, Math.cos(angle) * speed, Math.sin(angle) * speed, color, life));
        }
    }

    private static void shower(int color, int ticks, float chance) {
        showerColor = color;
        showerTicks = ticks;
        showerChance = chance;
    }

    private static void setFlash(int color, int life) {
        flashColor = color;
        flashLife = life;
        flashMax = life;
    }

    private static void showBanner(String title, String subtitle, int color, int life, boolean rainbow) {
        banner = new Banner(title, subtitle, color, life, rainbow);
    }

    // ---------------------------------------------------------------- simulation

    public static void tick() {
        tickCount++;
        if (flashLife > 0) flashLife--;
        if (rainbowLife > 0) rainbowLife--;

        if (showerTicks > 0) {
            showerTicks--;
            if (RNG.nextFloat() < showerChance) {
                burst(showerColor, 14);
            }
        }

        Iterator<Spark> sparkIt = sparks.iterator();
        while (sparkIt.hasNext()) {
            Spark s = sparkIt.next();
            s.x += s.vx;
            s.y += s.vy;
            s.vy += 0.06;
            s.vx *= 0.98;
            s.vy *= 0.98;
            if (--s.life <= 0) sparkIt.remove();
        }

        Iterator<Toast> toastIt = toasts.iterator();
        while (toastIt.hasNext()) {
            Toast t = toastIt.next();
            if (--t.life <= 0) toastIt.remove();
        }

        if (banner != null && --banner.life <= 0) {
            banner = null;
        }
    }

    // ---------------------------------------------------------------- drawing

    public static void render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) {
            return;
        }
        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();
        TextRenderer tr = mc.textRenderer;

        if (flashLife > 0) {
            int a = (flashColor >>> 24) * flashLife / flashMax;
            ctx.fill(0, 0, w, h, argb(a, flashColor));
        }

        if (rainbowLife > 0) {
            drawRainbowBorder(ctx, w, h);
        }

        for (Spark s : sparks) {
            int a = 255 * s.life / s.max;
            int x = (int) s.x;
            int y = (int) s.y;
            ctx.fill(x, y, x + 2, y + 2, argb(a, s.color));
        }

        if (banner != null) {
            drawBanner(ctx, tr, w, h);
        }

        drawToasts(ctx, tr, w);
    }

    private static void drawToasts(DrawContext ctx, TextRenderer tr, int w) {
        int y = 8;
        for (Toast t : toasts) {
            int alpha = Math.min(255, t.life * 13);
            int boxW = tr.getWidth(t.text) + 16;
            int x = w - boxW - 8;
            ctx.fill(x, y, w - 8, y + 16, argb(Math.min(alpha, 200), 0x101018));
            ctx.fill(x, y, x + 2, y + 16, argb(alpha, t.color));
            ctx.drawTextWithShadow(tr, Text.literal(t.text), x + 8, y + 4, argb(alpha, 0xFFFFFF));
            y += 20;
        }
    }

    private static void drawBanner(DrawContext ctx, TextRenderer tr, int w, int h) {
        int elapsed = banner.max - banner.life;
        int alpha = Math.min(banner.life * 13, elapsed * 25);
        alpha = Math.max(0, Math.min(255, alpha));
        int rgb = banner.rainbow ? rainbow(tickCount * 6) : (banner.color & 0xFFFFFF);
        int cx = w / 2;
        int cy = h / 3;
        ctx.drawCenteredTextWithShadow(tr, Text.literal(banner.title), cx, cy, argb(alpha, rgb));
        ctx.drawCenteredTextWithShadow(tr, Text.literal(banner.subtitle), cx, cy + 14, argb(alpha, 0xFFFFFF));
    }

    private static void drawRainbowBorder(DrawContext ctx, int w, int h) {
        int thickness = 3;
        int seg = 40;
        int alpha = Math.min(255, rainbowLife * 6);
        for (int x = 0; x < w; x += seg) {
            int c = argb(alpha, rainbow(tickCount * 6 + x));
            ctx.fill(x, 0, x + seg, thickness, c);
            ctx.fill(x, h - thickness, x + seg, h, c);
        }
        for (int y = 0; y < h; y += seg) {
            int c = argb(alpha, rainbow(tickCount * 6 + y));
            ctx.fill(0, y, thickness, y + seg, c);
            ctx.fill(w - thickness, y, w, y + seg, c);
        }
    }

    // ---------------------------------------------------------------- helpers

    private static int argb(int alpha, int rgb) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
    }

    private static int rainbow(int hue) {
        return Color.getHSBColor((hue % 360) / 360f, 1f, 1f).getRGB() & 0xFFFFFF;
    }

    // ---------------------------------------------------------------- state holders

    private static final class Toast {
        final String text;
        final int color;
        int life;

        Toast(String text, int color, int life) {
            this.text = text;
            this.color = color;
            this.life = life;
        }
    }

    private static final class Spark {
        double x, y, vx, vy;
        final int color;
        int life;
        final int max;

        Spark(double x, double y, double vx, double vy, int color, int life) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
            this.life = life;
            this.max = life;
        }
    }

    private static final class Banner {
        final String title;
        final String subtitle;
        final int color;
        final boolean rainbow;
        final int max;
        int life;

        Banner(String title, String subtitle, int color, int life, boolean rainbow) {
            this.title = title;
            this.subtitle = subtitle;
            this.color = color;
            this.rainbow = rainbow;
            this.life = life;
            this.max = life;
        }
    }
}
