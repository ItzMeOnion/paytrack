package dev.paytrack;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Map;

public class PayTrackScreen extends Screen {
    private boolean showLeaderboard = false;

    public PayTrackScreen() {
        super(Text.literal("PayTrack"));
    }

    @Override
    protected void init() {
        int cx = width / 2;

        if (!showLeaderboard) {
            EffectTier[] tiers = EffectTier.values();
            for (int i = 0; i < tiers.length; i++) {
                EffectTier tier = tiers[i];
                addDrawableChild(ButtonWidget.builder(
                                Text.literal(tier.label + "  (" + PaymentTracker.shortAmount(tier.minAmount) + ")"),
                                b -> EffectRenderer.play(tier, playerName(), tier.minAmount))
                        .dimensions(cx - 110, 40 + i * 26, 220, 20)
                        .build());
            }
            addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
                    .dimensions(cx - 60, height - 56, 120, 20)
                    .build());
        } else {
            addDrawableChild(ButtonWidget.builder(Text.literal("Clear totals"), b -> PaymentTracker.clear())
                    .dimensions(cx - 125, height - 56, 120, 20)
                    .build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
                    .dimensions(cx + 5, height - 56, 120, 20)
                    .build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Previews"), b -> {
                    showLeaderboard = false;
                    clearAndInit();
                })
                .dimensions(cx - 105, height - 30, 100, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Leaderboard"), b -> {
                    showLeaderboard = true;
                    clearAndInit();
                })
                .dimensions(cx + 5, height - 30, 100, 20)
                .build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        int cx = width / 2;
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("PayTrack"), cx, 14, 0xFFFFD700);

        if (showLeaderboard) {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Total paid to you"), cx, 28, 0xFFAAAAAA);
            List<Map.Entry<String, Long>> board = PaymentTracker.leaderboard();
            if (board.isEmpty()) {
                ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("No payments yet"), cx, 44, 0xFF888888);
                return;
            }
            int y = 44;
            int rank = 1;
            for (Map.Entry<String, Long> entry : board) {
                if (rank > 10) break;
                int color = switch (rank) {
                    case 1 -> 0xFFFFD700;
                    case 2 -> 0xFFC0C0C0;
                    case 3 -> 0xFFCD7F32;
                    default -> 0xFFFFFFFF;
                };
                String line = "#" + rank + "  " + entry.getKey() + "  -  "
                        + PaymentTracker.shortAmount(entry.getValue())
                        + "  (" + PaymentTracker.countFor(entry.getKey()) + " payments)";
                ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(line), cx, y, color);
                y += 12;
                rank++;
            }
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static String playerName() {
        return MinecraftClient.getInstance().getSession().getUsername();
    }
}
