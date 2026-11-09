package us.kenny;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.mojang.blaze3d.platform.InputConstants;

class LegacyGlfwKeysTest {
    @Test
    void lettersDigitsAndCommonKeys() {
        assertEquals("key.keyboard.k", ConfigManager.convertKeyCodeToKeyName(75));
        assertEquals("key.keyboard.a", ConfigManager.convertKeyCodeToKeyName(65));
        assertEquals("key.keyboard.z", ConfigManager.convertKeyCodeToKeyName(90));
        assertEquals("key.keyboard.0", ConfigManager.convertKeyCodeToKeyName(48));
        assertEquals("key.keyboard.space", ConfigManager.convertKeyCodeToKeyName(32));
        assertEquals("key.keyboard.left.shift", ConfigManager.convertKeyCodeToKeyName(340));
        assertEquals("key.keyboard.f3", ConfigManager.convertKeyCodeToKeyName(292));
        assertEquals("key.keyboard.page.up", ConfigManager.convertKeyCodeToKeyName(266));
    }

    @Test
    void mouseButtons() {
        assertEquals("key.mouse.left", ConfigManager.convertKeyCodeToKeyName(0));
        assertEquals("key.mouse.right", ConfigManager.convertKeyCodeToKeyName(1));
        assertEquals("key.mouse.middle", ConfigManager.convertKeyCodeToKeyName(2));
        assertEquals("key.mouse.4", ConfigManager.convertKeyCodeToKeyName(3));
        assertEquals("key.mouse.5", ConfigManager.convertKeyCodeToKeyName(4));
    }

    @Test
    void unknownCodesBecomeUnbound() {
        assertEquals("key.keyboard.unknown", ConfigManager.convertKeyCodeToKeyName(9999));
        assertEquals("key.keyboard.unknown", ConfigManager.convertKeyCodeToKeyName(-1));
    }

    @Test
    void everyTableNameIsARealMinecraftKey() {
        java.util.List<String> bad = new java.util.ArrayList<>();
        for (Map.Entry<Integer, String> e : LegacyGlfwKeys.keyboardTable().entrySet()) {
            try {
                InputConstants.Key key = InputConstants.getKey(e.getValue());
                if (key == InputConstants.UNKNOWN || !e.getValue().equals(key.getName())) {
                    bad.add(e.getKey() + "->" + e.getValue() + " (got " + key.getName() + ")");
                }
            } catch (RuntimeException ex) {
                bad.add(e.getKey() + "->" + e.getValue() + " (" + ex.getClass().getSimpleName() + ")");
            }
        }
        for (String mouse : new String[] { "key.mouse.left", "key.mouse.right", "key.mouse.middle", "key.mouse.4" }) {
            try {
                if (!mouse.equals(InputConstants.getKey(mouse).getName())) {
                    bad.add(mouse);
                }
            } catch (RuntimeException ex) {
                bad.add(mouse + " (" + ex.getClass().getSimpleName() + ")");
            }
        }
        assertEquals(java.util.List.of(), bad);
    }
}
