package us.kenny.core.profile;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import us.kenny.ProfileManager;
import us.kenny.mixin.OptionsSubScreenAccessor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Vanilla-style manager modelled on the Select World screen: a scrollable
 * ObjectSelectionList in the middle, contextual action buttons below.
 */
public final class ManageProfilesScreen extends Screen {
    private static final int ITEM_HEIGHT = 36;
    private static final int LIST_TOP = 40;
    private static final int FOOTER_ROWS_HEIGHT = 56;
    private static final int WIDE_BUTTON_WIDTH = 150;
    private static final int NARROW_BUTTON_WIDTH = 73;

    private static final int ACTIVE_BADGE_COLOR = 0xFF77CC77;

    private final KeyBindsScreen parent;
    private ProfileList list;
    private Button setActiveButton;
    private Button renameButton;
    private Button duplicateButton;
    private Button deleteButton;

    public ManageProfilesScreen(KeyBindsScreen parent) {
        super(Component.translatable("multi.profile.manage.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.list = new ProfileList(this.minecraft, this.width, this.height - LIST_TOP - FOOTER_ROWS_HEIGHT,
                LIST_TOP, ITEM_HEIGHT);
        addRenderableWidget(this.list);

        int cx = this.width / 2;
        int topRowY = this.height - 52;
        int bottomRowY = this.height - 28;

        // Top row: 2 x 150px + 4px gap = 304px, centered.
        this.setActiveButton = Button.builder(
                Component.translatable("multi.profile.manage.set_active"), b -> onSetActive())
                .bounds(cx - 152, topRowY, WIDE_BUTTON_WIDTH, 20).build();
        addRenderableWidget(this.setActiveButton);
        addRenderableWidget(Button.builder(
                Component.translatable("multi.profile.manage.new"), b -> openNewDialog())
                .bounds(cx + 2, topRowY, WIDE_BUTTON_WIDTH, 20).build());

        // Bottom row: 4 x 73px + 3 x 4px gaps = 304px, centered.
        this.renameButton = Button.builder(
                Component.translatable("multi.profile.manage.rename"), b -> openRenameDialog())
                .bounds(cx - 152, bottomRowY, NARROW_BUTTON_WIDTH, 20).build();
        this.duplicateButton = Button.builder(
                Component.translatable("multi.profile.manage.duplicate"), b -> onDuplicate())
                .bounds(cx - 75, bottomRowY, NARROW_BUTTON_WIDTH, 20).build();
        this.deleteButton = Button.builder(
                Component.translatable("multi.profile.manage.delete"), b -> onDelete())
                .bounds(cx + 2, bottomRowY, NARROW_BUTTON_WIDTH, 20).build();
        addRenderableWidget(this.renameButton);
        addRenderableWidget(this.duplicateButton);
        addRenderableWidget(this.deleteButton);
        addRenderableWidget(Button.builder(
                Component.translatable("multi.profile.manage.back"),
                b -> Minecraft.getInstance().setScreenAndShow(parent))
                .bounds(cx + 79, bottomRowY, NARROW_BUTTON_WIDTH, 20).build());

        refreshActionButtons();
    }

    private void refreshActionButtons() {
        ProfileListEntry selected = this.list == null ? null : this.list.getSelected();
        boolean hasSelection = selected != null;
        boolean isSelectedActive = hasSelection
                && selected.name.equalsIgnoreCase(ProfileManager.getActiveProfileName());
        this.setActiveButton.active = hasSelection && !isSelectedActive;
        this.renameButton.active = hasSelection;
        this.duplicateButton.active = hasSelection;
        this.deleteButton.active = hasSelection && !isSelectedActive;
    }

    private void onSetActive() {
        ProfileListEntry sel = this.list.getSelected();
        if (sel == null) {
            return;
        }
        ProfileManager.load(sel.name);
        // Stay on the same screen but rebuild against a fresh KeyBindsScreen so that
        // Back returns to a screen reflecting the new active profile.
        OptionsSubScreenAccessor accessor = (OptionsSubScreenAccessor) (Object) parent;
        KeyBindsScreen freshParent = new KeyBindsScreen(accessor.getLastScreen(), accessor.getOptions());
        Minecraft.getInstance().setScreenAndShow(new ManageProfilesScreen(freshParent));
    }

    private void openNewDialog() {
        Minecraft.getInstance().setScreenAndShow(new ProfileNameDialog(
                this,
                Component.translatable("multi.profile.dialog.new.title"),
                Component.translatable("multi.profile.dialog.create"),
                "",
                name -> {
                    ProfileManager.create(name);
                    Minecraft.getInstance().setScreenAndShow(new ManageProfilesScreen(parent));
                }));
    }

    private void openRenameDialog() {
        ProfileListEntry sel = this.list.getSelected();
        if (sel == null) {
            return;
        }
        String currentName = sel.name;
        Minecraft.getInstance().setScreenAndShow(new ProfileNameDialog(
                this,
                Component.translatable("multi.profile.dialog.rename.title"),
                Component.translatable("multi.profile.dialog.rename.confirm"),
                currentName,
                newName -> {
                    ProfileManager.rename(currentName, newName);
                    Minecraft.getInstance().setScreenAndShow(new ManageProfilesScreen(parent));
                }));
    }

    private void onDuplicate() {
        ProfileListEntry sel = this.list.getSelected();
        if (sel == null) {
            return;
        }
        ProfileManager.duplicate(sel.name, null);
        Minecraft.getInstance().setScreenAndShow(new ManageProfilesScreen(parent));
    }

    private void onDelete() {
        ProfileListEntry sel = this.list.getSelected();
        if (sel == null) {
            return;
        }
        String name = sel.name;
        Minecraft.getInstance().setScreenAndShow(new ConfirmScreen(
                confirmed -> {
                    if (confirmed) {
                        ProfileManager.delete(name);
                    }
                    Minecraft.getInstance().setScreenAndShow(new ManageProfilesScreen(parent));
                },
                Component.translatable("multi.profile.manage.confirm_delete.title", name),
                Component.translatable("multi.profile.manage.confirm_delete.body")));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTicks) {
        gfx.text(this.font, this.title,
                this.width / 2 - this.font.width(this.title) / 2, 20, 0xFFFFFFFF);
        super.extractRenderState(gfx, mouseX, mouseY, partialTicks);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreenAndShow(parent);
    }

    private static String readModifiedDate(String name) {
        try {
            FileTime t = Files.getLastModifiedTime(ProfileManager.getProfilePath(name));
            LocalDate d = t.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            return d.toString();
        } catch (IOException e) {
            return "?";
        }
    }

    private final class ProfileList extends ObjectSelectionList<ProfileListEntry> {
        ProfileList(Minecraft mc, int width, int height, int y, int itemHeight) {
            super(mc, width, height, y, itemHeight);
            for (String name : ProfileManager.listProfileNames()) {
                this.addEntry(new ProfileListEntry(name));
            }
        }

        @Override
        public void setSelected(ProfileListEntry entry) {
            super.setSelected(entry);
            refreshActionButtons();
        }
    }

    private final class ProfileListEntry extends ObjectSelectionList.Entry<ProfileListEntry> {
        private final String name;
        private final String modifiedDate;
        private final boolean isActive;

        ProfileListEntry(String name) {
            this.name = name;
            this.modifiedDate = readModifiedDate(name);
            this.isActive = name.equalsIgnoreCase(ProfileManager.getActiveProfileName());
        }

        @Override
        public void extractContent(GuiGraphicsExtractor gfx, int mouseX, int mouseY, boolean hovered,
                float partialTicks) {
            var font = Minecraft.getInstance().font;
            int x = getContentX() + 4;
            int y = getContentY() + 4;

            gfx.text(font, Component.literal(name), x, y, 0xFFFFFFFF);
            if (isActive) {
                int nameWidth = font.width(name);
                Component badge = Component.translatable("multi.profile.manage.active");
                gfx.text(font, badge, x + nameWidth + 8, y, ACTIVE_BADGE_COLOR);
            }
            gfx.text(font, Component.literal(modifiedDate), x, y + 12, 0xFF999999);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
            list.setSelected(this);
            return super.mouseClicked(event, bl);
        }

        @Override
        public Component getNarration() {
            return Component.literal(name);
        }

        @Override
        public void updateNarration(NarrationElementOutput narrationElementOutput) {
        }
    }
}
