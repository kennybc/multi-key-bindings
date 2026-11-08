package us.kenny.core.profile;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import us.kenny.ProfileManager;

import java.util.List;
import java.util.function.Consumer;

/**
 * Upward-opening menu anchored above the dropdown button. Lists profiles
 * with the active one marked and additional options. When there are more
 * profiles than fit above the anchor, the profile rows scroll (mouse wheel)
 * and a scrollbar is drawn; the New/Manage rows are always visible.
 */
public final class ProfilePopupMenu {
    private static final int ROW_HEIGHT = 12;
    private static final int PADDING = 4;
    private static final int MIN_WIDTH = 140;
    private static final int MAX_WIDTH = 220;
    private static final int TEXT_INSET = 6;
    private static final int SCROLLBAR_WIDTH = 3;
    private static final int SEPARATOR_HEIGHT = 4;
    private static final int SCREEN_MARGIN = 4;

    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final int visibleRows;
    private int scroll;

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

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int screenWidth = mc.getWindow().getGuiScaledWidth();

        // Width: fit the widest row, clamped to [MIN_WIDTH, MAX_WIDTH] and to the screen.
        int widest = Math.max(font.width(Component.translatable("multi.profile.menu.new")),
                font.width(Component.translatable("multi.profile.menu.manage")));
        for (String name : profiles) {
            widest = Math.max(widest, font.width("* " + name));
        }
        int maxWidth = Math.min(MAX_WIDTH, screenWidth - SCREEN_MARGIN * 2);
        this.width = Math.max(MIN_WIDTH, Math.min(maxWidth, widest + TEXT_INSET * 2 + SCROLLBAR_WIDTH));

        // Height: only as many profile rows as fit between the screen top and the anchor.
        int fixed = PADDING * 2 + SEPARATOR_HEIGHT + ROW_HEIGHT * 2;
        int available = anchorTopY - SCREEN_MARGIN - fixed;
        int maxProfileRows = Math.max(1, available / ROW_HEIGHT);
        this.visibleRows = Math.min(profiles.size(), maxProfileRows);
        this.height = this.visibleRows * ROW_HEIGHT + fixed;

        this.x = Math.max(SCREEN_MARGIN, Math.min(anchorX, screenWidth - SCREEN_MARGIN - this.width));
        this.y = anchorTopY - this.height;

        // Start scrolled so the active profile is visible.
        int activeIndex = indexOfIgnoreCase(activeProfile);
        if (activeIndex >= this.visibleRows) {
            this.scroll = Math.min(activeIndex - this.visibleRows + 1, maxScroll());
        }
    }

    private int indexOfIgnoreCase(String name) {
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).equalsIgnoreCase(name)) {
                return i;
            }
        }
        return -1;
    }

    private int maxScroll() {
        return Math.max(0, profiles.size() - visibleRows);
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

        Font font = Minecraft.getInstance().font;
        int textWidth = width - TEXT_INSET * 2 - SCROLLBAR_WIDTH;
        int rowY = y + PADDING;
        for (int i = scroll; i < scroll + visibleRows; i++) {
            String name = profiles.get(i);
            boolean isActive = name.equalsIgnoreCase(activeProfile);
            String label = ellipsize(font, (isActive ? "* " : "  ") + name, textWidth);
            int color = isActive ? 0xFF55FFFF : 0xFFAAAAAA;
            renderRow(gfx, mouseX, mouseY, rowY, label, color);
            rowY += ROW_HEIGHT;
        }

        if (maxScroll() > 0) {
            int trackTop = y + PADDING;
            int trackHeight = visibleRows * ROW_HEIGHT;
            int thumbHeight = Math.max(6, trackHeight * visibleRows / profiles.size());
            int thumbTop = trackTop + (trackHeight - thumbHeight) * scroll / maxScroll();
            int barX = x + width - 1 - SCROLLBAR_WIDTH - 1;
            gfx.fill(barX, trackTop, barX + SCROLLBAR_WIDTH, trackTop + trackHeight, 0x40FFFFFF);
            gfx.fill(barX, thumbTop, barX + SCROLLBAR_WIDTH, thumbTop + thumbHeight, 0xFFAAAAAA);
        }

        gfx.fill(x + 4, rowY + 1, x + width - 4, rowY + 2, 0x66FFFFFF);
        rowY += SEPARATOR_HEIGHT;

        renderRow(gfx, mouseX, mouseY, rowY,
                Component.translatable("multi.profile.menu.new").getString(), 0xFFFFFFFF);
        rowY += ROW_HEIGHT;
        renderRow(gfx, mouseX, mouseY, rowY,
                Component.translatable("multi.profile.menu.manage").getString(), 0xFFFFFFFF);
    }

    /**
     * Trim {@code text} to {@code maxWidth} pixels, appending "..." when cut.
     */
    public static String ellipsize(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        return font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width(ellipsis))) + ellipsis;
    }

    private void renderRow(GuiGraphicsExtractor gfx, int mouseX, int mouseY, int rowY, String label, int color) {
        if (inRow(mouseX, mouseY, rowY)) {
            gfx.fill(x + 1, rowY, x + width - 1, rowY + ROW_HEIGHT, 0xFF3A4A5A);
        }
        gfx.text(Minecraft.getInstance().font,
                Component.literal(label), x + TEXT_INSET, rowY + 2, color);
    }

    /**
     * Route a click to the row under the pointer, if any.
     *
     * @param mouseX Screen-space X.
     * @param mouseY Screen-space Y.
     * @param button Mouse button.
     * @return true if a row consumed the click.
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        int rowY = y + PADDING;
        for (int i = scroll; i < scroll + visibleRows; i++) {
            if (inRow(mouseX, mouseY, rowY)) {
                onProfileClicked.accept(profiles.get(i));
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
     * Scroll the profile rows when the wheel is used over the menu.
     *
     * @param mouseX  Screen-space X.
     * @param mouseY  Screen-space Y.
     * @param scrollY Vertical wheel delta (positive = up).
     * @return true if the menu consumed the scroll.
     */
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (!containsPoint(mouseX, mouseY)) {
            return false;
        }
        if (scrollY != 0) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(scrollY)));
        }
        return true;
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
