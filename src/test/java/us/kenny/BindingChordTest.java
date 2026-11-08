package us.kenny;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.mojang.blaze3d.platform.InputConstants;

class BindingChordTest {
    private static final InputConstants.Key B = key(InputConstants.KEY_B);
    private static final InputConstants.Key F3 = key(InputConstants.KEY_F3);
    private static final InputConstants.Key G = key(InputConstants.KEY_G);
    private static final InputConstants.Key SHIFT = key(InputConstants.KEY_LSHIFT);

    @Test
    void bareKeyDoesNotConflictWithDebugChord() {
        BindingChord bareB = BindingChord.of(B, List.of(), false, F3);
        BindingChord debugB = BindingChord.of(B, List.of(), true, F3);

        assertFalse(bareB.conflictsWith(debugB));
    }

    @Test
    void identicalDebugChordsConflict() {
        BindingChord first = BindingChord.of(B, List.of(), true, F3);
        BindingChord second = BindingChord.of(B, List.of(), true, F3);

        assertTrue(first.conflictsWith(second));
    }

    @Test
    void reboundDebugModifierParticipatesInConflictCheck() {
        BindingChord f3DebugB = BindingChord.of(B, List.of(), true, F3);
        BindingChord gDebugB = BindingChord.of(B, List.of(), true, G);

        assertFalse(f3DebugB.conflictsWith(gDebugB));
    }

    @Test
    void configuredAndDebugModifiersAreBothRequired() {
        BindingChord shiftedDebugB = BindingChord.of(B, List.of(SHIFT), true, F3);
        BindingChord debugB = BindingChord.of(B, List.of(), true, F3);

        assertFalse(shiftedDebugB.conflictsWith(debugB));
    }

    @Test
    void modifierMatchingPrimaryKeyIsNotDoubleCounted() {
        BindingChord debugF3 = BindingChord.of(F3, List.of(), true, F3);
        BindingChord bareF3 = BindingChord.of(F3, List.of(), false, F3);

        assertTrue(debugF3.conflictsWith(bareF3));
    }

    private static InputConstants.Key key(int keyCode) {
        return InputConstants.Type.KEYBOARD.getOrCreate(keyCode);
    }
}
