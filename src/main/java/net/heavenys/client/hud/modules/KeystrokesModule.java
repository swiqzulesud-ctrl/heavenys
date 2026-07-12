package net.heavenys.client.hud.modules;

import net.heavenys.client.config.HeavenysConfig;
import net.heavenys.client.hud.HudModule;
import net.heavenys.client.util.HeavenysColors;
import net.heavenys.client.util.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;

/**
 * Keystrokes module — displays WASD + Space + LMB/RMB with highlight
 * when each key is held. Purely visual; reads {@link KeyBinding#isPressed()}
 * which is the same data the game engine uses for movement.
 *
 * Layout (each cell is KEY_SIZE × KEY_SIZE with GAP spacing):
 *
 *        [ W ]
 *   [A]  [ S ]  [D]
 *          [ SPACE ]
 *   [LMB]  [RMB]
 */
public class KeystrokesModule extends HudModule {

    private static final int KEY_SIZE  = 18;
    private static final int GAP       = 2;
    private static final int SPACE_W   = KEY_SIZE * 3 + GAP * 2;

    private final HeavenysConfig config;

    public KeystrokesModule(HeavenysConfig config) {
        super("keystrokes", "Keystrokes", config.keystrokesEnabled);
        this.config = config;
    }

    @Override
    public void render(DrawContext ctx, int scaledWidth, int scaledHeight, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        int baseX = config.keystrokesX;
        int baseY = config.keystrokesY;

        // ---- WASD row --------------------------------------------------------
        boolean w = mc.options.forwardKey.isPressed();
        boolean a = mc.options.leftKey.isPressed();
        boolean s = mc.options.backKey.isPressed();
        boolean d = mc.options.rightKey.isPressed();
        boolean jump   = mc.options.jumpKey.isPressed();
        boolean lmb    = mc.options.attackKey.isPressed();
        boolean rmb    = mc.options.useKey.isPressed();

        // Row 0: W (centered above A/S/D row)
        int wX = baseX + KEY_SIZE + GAP;
        drawKey(ctx, mc, wX, baseY, KEY_SIZE, KEY_SIZE, "W", w);

        // Row 1: A  S  D
        drawKey(ctx, mc, baseX,               baseY + KEY_SIZE + GAP, KEY_SIZE, KEY_SIZE, "A", a);
        drawKey(ctx, mc, baseX + KEY_SIZE + GAP, baseY + KEY_SIZE + GAP, KEY_SIZE, KEY_SIZE, "S", s);
        drawKey(ctx, mc, baseX + (KEY_SIZE + GAP) * 2, baseY + KEY_SIZE + GAP, KEY_SIZE, KEY_SIZE, "D", d);

        // Row 2: SPACE (wide)
        drawKey(ctx, mc, baseX, baseY + (KEY_SIZE + GAP) * 2, SPACE_W, KEY_SIZE, "SPC", jump);

        // Row 3: LMB  RMB
        int halfW = (SPACE_W - GAP) / 2;
        drawKey(ctx, mc, baseX,           baseY + (KEY_SIZE + GAP) * 3, halfW, KEY_SIZE, "LMB", lmb);
        drawKey(ctx, mc, baseX + halfW + GAP, baseY + (KEY_SIZE + GAP) * 3, halfW, KEY_SIZE, "RMB", rmb);
    }

    private static void drawKey(DrawContext ctx, MinecraftClient mc,
                                int x, int y, int w, int h, String label, boolean pressed) {
        int bg     = pressed ? HeavenysColors.withAlpha(HeavenysColors.BUTTER_YELLOW, 200)
                             : HeavenysColors.PANEL_BG;
        int border = HeavenysColors.BORDER;
        int text   = pressed ? HeavenysColors.PANEL_BG : HeavenysColors.WHITE;

        RenderUtil.drawFilledRect(ctx, x, y, w, h, bg);
        // Border
        ctx.fill(x, y, x + w, y + 1, border);
        ctx.fill(x, y + h - 1, x + w, y + h, border);
        ctx.fill(x, y, x + 1, y + h, border);
        ctx.fill(x + w - 1, y, x + w, y + h, border);

        // Centered label
        int tw = mc.textRenderer.getWidth(label);
        ctx.drawText(mc.textRenderer, label,
            x + (w - tw) / 2,
            y + (h - mc.textRenderer.fontHeight) / 2,
            text, false);
    }

    @Override public int getPreferredWidth()  { return KEY_SIZE * 3 + GAP * 2 + 2; }
    @Override public int getPreferredHeight() { return (KEY_SIZE + GAP) * 4 + 2; }
}
