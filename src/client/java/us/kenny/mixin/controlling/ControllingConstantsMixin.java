package us.kenny.mixin.controlling;

import com.blamejared.controlling.ControllingConstants;
import com.blamejared.controlling.api.entries.ICategoryEntry;
import com.blamejared.controlling.api.entries.IInputEntry;
import com.blamejared.controlling.api.entries.IKeyEntry;
import com.blamejared.searchables.api.SearchableComponent;
import com.blamejared.searchables.api.SearchableType;
import net.minecraft.client.gui.screen.option.ControlsListWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import us.kenny.core.MultiKeyBindingEntry;

import java.util.Optional;

@Mixin(value = ControllingConstants.class, remap = false)
public class ControllingConstantsMixin {

    @Shadow
    @Mutable
    public static SearchableType<ControlsListWidget.Entry> SEARCHABLE_KEYBINDINGS;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void replaceSearchableKeybindings(CallbackInfo ci) {
        SEARCHABLE_KEYBINDINGS = new SearchableType.Builder<ControlsListWidget.Entry>()
                .component(SearchableComponent.create("category", entry -> {
                    if (entry instanceof ICategoryEntry cat) {
                        return Optional.of(cat.name().getString());
                    } else if (entry instanceof IKeyEntry key) {
                        return Optional.of(key.categoryName().getString());
                    } else if (entry instanceof MultiKeyBindingEntry multiKeyBindingEntry) {
                        return Optional.of(multiKeyBindingEntry.getMultiKeyBinding().getCategory());
                    }
                    return Optional.empty();
                }))
                .component(SearchableComponent.create("key", entry -> {
                    if (entry instanceof IKeyEntry key && !key.getKey().isUnbound()) {
                        return Optional.of(key.getKey().getBoundKeyTranslationKey());
                    } else if (entry instanceof MultiKeyBindingEntry multiKeyBindingEntry
                            && multiKeyBindingEntry.getMultiKeyBinding().getKey() != InputUtil.UNKNOWN_KEY) {
                        return Optional
                                .of(multiKeyBindingEntry.getMultiKeyBinding().getKey().getTranslationKey());
                    }
                    return Optional.empty();
                }))
                .defaultComponent(SearchableComponent.create("name", entry -> {
                    if (entry instanceof IKeyEntry key) {
                        return Optional.of(key.getKeyDesc().getString());
                    } else if (entry instanceof IInputEntry input) {
                        return Optional.of(input.getInput().getTranslationKey());
                    } else if (entry instanceof MultiKeyBindingEntry multiKeyBindingEntry) {
                        return Optional.of(Text.translatable(
                                multiKeyBindingEntry.getMultiKeyBinding().getAction().replaceFirst("^multi.", ""))
                                .getString());
                    }
                    return Optional.empty();
                }))
                .build();
    }
}