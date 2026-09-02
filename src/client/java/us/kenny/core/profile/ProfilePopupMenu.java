package us.kenny.core.profile;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import us.kenny.ProfileManager;

import java.util.List;
import java.util.function.Consumer;

/**
 * Upward-opening menu anchored above the dropdown button. Lists profiles
 * with the active one marked and additional options.
 */
public final class ProfilePopupMenu {
    private static final int ROW_HEIGHT = 12;
    private static final int PADDING = 4;
    private static final int MIN_WIDTH = 140;
    private static final int SEPARATOR_HEIGHT = 4;

    private final int x;
    private final int y;
    private final int width;
    private final int height;

    private final List<String> profiles;
    private final String activeProfile;
    private final Consumer<String> onProfileClicked;
    private final Runnable onNew;
    private final Runnable onManage;

    public ProfilePopupMenu(int anchorX, int anchorTopY,
            Consumer<String> onProfileClicked,
            Runnable onNew,
            Runnable onManage) {
        this.profiles = ProfileManager.listProfileNames();
        this.activeProfile = ProfileManager.getActiveProfileName();
        this.onProfileClicked = onProfileClicked;
        this.onNew = onNew;
        this.onManage = onManage;

        int rows = profiles.size() + 2;
        this.width = MIN_WIDTH;
        this.height = rows * ROW_HEIGHT + PADDING * 2 + SEPARATOR_HEIGHT;
        this.x = anchorX;
        this.y = anchorTopY - this.height;
    }

    /**
     * Render the menu at its anchored position.
     */
    public void render(GuiGraphicsExtractor gfx, int mouseX, int mouseY) {
        gfx.fill(x, y, x + width, y + height, 0xF01A1A1A);
        int border = 0xFFFFFFFF;
        gfx.fill(x, y, x + width, y + 1, border);
        gfx.fill(x, y + height - 1, x + width, y + height, border);
        gfx.fill(x, y, x + 1, y + height, border);
        gfx.fill(x + width - 1, y, x + width, y + height, border);

        int rowY = y + PADDING;
        for (String name : profiles) {
            boolean isActive = name.equals(activeProfile);
            String label = (isActive ? "* " : "  ") + name;
            int color = isActive ? 0xFF55FFFF : 0xFFAAAAAA;
            renderRow(gfx, mouseX, mouseY, rowY, label, color);
            rowY += ROW_HEIGHT;
        }

        gfx.fill(x + 4, rowY + 1, x + width - 4, rowY + 2, 0x66FFFFFF);
        rowY += SEPARATOR_HEIGHT;

        renderRow(gfx, mouseX, mouseY, rowY,
                Component.translatable("multi.profile.menu.new").getString(), 0xFFFFFFFF);
        rowY += ROW_HEIGHT;
        renderRow(gfx, mouseX, mouseY, rowY,
                Component.translatable("multi.profile.menu.manage").getString(), 0xFFFFFFFF);
    }

    private void renderRow(GuiGraphicsExtractor gfx, int mouseX, int mouseY, int rowY, String label, int color) {
        if (inRow(mouseX, mouseY, rowY)) {
            gfx.fill(x + 1, rowY, x + width - 1, rowY + ROW_HEIGHT, 0xFF3A4A5A);
        }
        gfx.text(Minecraft.getInstance().font,
                Component.literal(label), x + 6, rowY + 2, color);
    }

    /**
     * Route a click to the row under the pointer, if any.
     *
     * @param mouseX Screen-space X.
     * @param mouseY Screen-space Y.
     * @param button GLFW mouse button.
     * @return true if a row consumed the click.
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        int rowY = y + PADDING;
        for (String name : profiles) {
            if (inRow(mouseX, mouseY, rowY)) {
                onProfileClicked.accept(name);
                return true;
            }
            rowY += ROW_HEIGHT;
        }
        rowY += SEPARATOR_HEIGHT;
        if (inRow(mouseX, mouseY, rowY)) {
            onNew.run();
            return true;
        }
        rowY += ROW_HEIGHT;
        if (inRow(mouseX, mouseY, rowY)) {
            onManage.run();
            return true;
        }
        return false;
    }

    /**
     * Check whether the menu rectangle contains the given screen-space
     * point.
     *
     * @param mouseX Screen-space X.
     * @param mouseY Screen-space Y.
     */
    public boolean containsPoint(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width
                && mouseY >= y && mouseY < y + height;
    }

    private boolean inRow(double mouseX, double mouseY, int rowTop) {
        return mouseX >= x && mouseX < x + width
                && mouseY >= rowTop && mouseY < rowTop + ROW_HEIGHT;
    }
}
