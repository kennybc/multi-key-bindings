package us.kenny.mixin.controlling;

import com.blamejared.controlling.ControllingConstants;
import com.blamejared.controlling.api.DisplayMode;
import com.blamejared.controlling.client.NewKeyBindsList;
import com.blamejared.searchables.api.SearchableComponent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import us.kenny.MultiKeyBindingManager;
import us.kenny.core.MultiKeyBinding;
import us.kenny.core.MultiKeyBindingEntry;
import us.kenny.core.controlling.ControllingHideableKeyEntry;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

/**
 * Need this to filter our custom key bindings properly.
 */
@Mixin(value = ControllingConstants.class, remap = false)
public class ControllingConstantsMixin {

    /**
     * Tests a custom key binding against the search term.
     *
     * @param key The search term key: category, key, or name.
     * @param multiKeyBinding The custom key binding we are testing.
     */
    @Unique
    private static Optional<String> testMultiKeyBinding(String key, MultiKeyBinding multiKeyBinding) {
        return switch (key) {
            case "category" -> Optional.of(multiKeyBinding.getCategory().label().getString());
            case "key" -> Optional.of(multiKeyBinding.getKey().getName());
            case "name" -> Optional.of(
                    Component.translatable(multiKeyBinding.getAction().replaceFirst("^multi.", "")).getString());
            default -> Optional.empty();
        };
    }

    @WrapOperation(method = "<clinit>", at = @At(value = "INVOKE", target = "Lcom/blamejared/searchables/api/SearchableComponent;create(Ljava/lang/String;Ljava/util/function/Function;)Lcom/blamejared/searchables/api/SearchableComponent;"))
    private static SearchableComponent<KeyBindsList.Entry> wrapSearchableComponentCreate(
            String key,
            Function<KeyBindsList.Entry, Optional<String>> originalFunction,
            Operation<SearchableComponent<KeyBindsList.Entry>> original) {

        Function<KeyBindsList.Entry, Optional<String>> enhancedFunction = (entry) -> {
            // Apply filter to custom MultiKeyBinding
            if (entry instanceof MultiKeyBindingEntry multiKeyBindingEntry) {
                MultiKeyBinding multiKeyBinding = multiKeyBindingEntry.getMultiKeyBinding();

                return testMultiKeyBinding(key, multiKeyBinding);
            }

            // Fall back to original function for other entry types
            return originalFunction.apply(entry);
        };

        return original.call(key, enhancedFunction);
    }
}