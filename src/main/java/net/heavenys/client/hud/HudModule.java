package net.heavenys.client.hud;

import net.minecraft.client.gui.DrawContext;

/**
 * Base contract for every Heavenys HUD overlay module.
 *
 * Modules are purely <em>client-side visual overlays</em>. They MUST NOT:
 * <ul>
 *   <li>Send packets or modify server-bound data.</li>
 *   <li>Read or expose information beyond what the vanilla HUD already shows.</li>
 *   <li>Provide automation (auto-click, auto-aim, etc.).</li>
 * </ul>
 */
public abstract class HudModule {

    private final String id;
    private final String displayName;
    private boolean enabled;

    protected HudModule(String id, String displayName, boolean defaultEnabled) {
        this.id          = id;
        this.displayName = displayName;
        this.enabled     = defaultEnabled;
    }

    // ---- Identity -----------------------------------------------------------

    public String getId()          { return id; }
    public String getDisplayName() { return displayName; }

    // ---- Lifecycle ----------------------------------------------------------

    public boolean isEnabled()              { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void toggle()                    { this.enabled = !this.enabled; }

    /**
     * Called once per frame when this module is enabled and the in-game HUD
     * is visible. Implementations should draw their overlay here using the
     * provided {@link DrawContext}.
     *
     * @param ctx         Minecraft draw context (Sodium-compatible)
     * @param scaledWidth  current scaled screen width
     * @param scaledHeight current scaled screen height
     * @param tickDelta   partial tick for smooth animation
     */
    public abstract void render(DrawContext ctx, int scaledWidth, int scaledHeight, float tickDelta);

    /**
     * Returns the preferred width of this module's panel (used by the settings
     * screen to preview layout). Override if the module has a dynamic width.
     */
    public int getPreferredWidth()  { return 80; }
    public int getPreferredHeight() { return 16; }
}
