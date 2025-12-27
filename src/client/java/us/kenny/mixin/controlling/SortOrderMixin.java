package us.kenny.mixin.controlling;

import com.blamejared.controlling.api.SortOrder;
import com.blamejared.controlling.api.entries.IKeyEntry;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;

@Mixin(SortOrder.class)
public abstract class SortOrderMixin {

    /*@Shadow @Final @Mutable
    private Comparator<KeyBindsList.Entry> sorter;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void fixSorter(String key, int ordinal, String par3, Comparator<IKeyEntry> comp, CallbackInfo ci) {
        this.sorter = (o1, o2) -> {
            boolean o1IsKeyEntry = o1 instanceof IKeyEntry;
            boolean o2IsKeyEntry = o2 instanceof IKeyEntry;

            // Both are IKeyEntry - use original comparator
            if (o1IsKeyEntry && o2IsKeyEntry) {
                return comp.compare((IKeyEntry) o1, (IKeyEntry) o2);
            }

            // Only one is IKeyEntry - IKeyEntry entries come first
            if (o1IsKeyEntry) {
                return -1; // o1 (IKeyEntry) comes before o2 (non-IKeyEntry)
            }
            if (o2IsKeyEntry) {
                return 1; // o2 (IKeyEntry) comes before o1 (non-IKeyEntry)
            }

            // Neither is IKeyEntry - they're equal in sort order
            return 0;
        };
    }*/
}
