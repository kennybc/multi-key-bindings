package us.kenny.mixin.controlling;

import com.blamejared.controlling.ControllingConstants;
import com.blamejared.searchables.api.SearchableComponent;
import com.blamejared.searchables.api.SearchableType;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.screen.option.ControlsListWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import us.kenny.core.MultiKeyBinding;
import us.kenny.core.MultiKeyBindingEntry;

import java.util.Optional;
import java.util.function.Function;

/**
 * Need this to filter our custom key bindings properly.
 */
@Mixin(value = ControllingConstants.class, remap = false)
public class ControllingConstantsMixin {

    @Shadow
    @Mutable
    public static SearchableType<ControlsListWidget.Entry> SEARCHABLE_KEYBINDINGS;

    @WrapOperation(method = "<clinit>", at = @At(value = "INVOKE", target = "Lcom/blamejared/searchables/api/SearchableComponent;create(Ljava/lang/String;Ljava/util/function/Function;)Lcom/blamejared/searchables/api/SearchableComponent;"))
    private static SearchableComponent<ControlsListWidget.Entry> wrapSearchableComponentCreate(
            String key,
            Function<ControlsListWidget.Entry, Optional<String>> originalFunction,
            Operation<SearchableComponent<ControlsListWidget.Entry>> original) {

        Function<ControlsListWidget.Entry, Optional<String>> enhancedFunction = (entry) -> {
            // Handle MultiKeyBindingEntry
            if (entry instanceof MultiKeyBindingEntry multiKeyBindingEntry) {
                MultiKeyBinding multiKeyBinding = multiKeyBindingEntry.getMultiKeyBinding();

                return switch (key) {
                    case "category" -> Optional.of(multiKeyBinding.getCategory());
                    case "key" -> Optional.of(multiKeyBinding.getKey().getLocalizedText().getString());
                    case "name" -> Optional.of(
                            Text.translatable(multiKeyBinding.getAction().replaceFirst("^multi.", "")).getString());
                    default -> Optional.empty();
                };
            }

            // Fall back to original function for other entry types
            return originalFunction.apply(entry);
        };

        return original.call(key, enhancedFunction);
    }
}