package us.kenny;

import java.util.HashMap;
import java.util.Map;

/**
 * Translates the GLFW key and mouse-button codes stored by v1 configs into
 * Minecraft key names. Minecraft 26.3 no longer uses GLFW codes internally,
 * so these numbers can't go through InputConstants; the GLFW values are fixed
 * forever, so a static table is safe.
 */
final class LegacyGlfwKeys {
    private static final String UNKNOWN = "key.keyboard.unknown";
    private static final Map<Integer, String> KEYS = new HashMap<>();

    static {
        KEYS.put(32, "key.keyboard.space");
        KEYS.put(39, "key.keyboard.apostrophe");
        KEYS.put(44, "key.keyboard.comma");
        KEYS.put(45, "key.keyboard.minus");
        KEYS.put(46, "key.keyboard.period");
        KEYS.put(47, "key.keyboard.slash");
        for (int i = 0; i <= 9; i++) {
            KEYS.put(48 + i, "key.keyboard." + i);
        }
        KEYS.put(59, "key.keyboard.semicolon");
        KEYS.put(61, "key.keyboard.equal");
        for (char c = 'a'; c <= 'z'; c++) {
            KEYS.put(65 + (c - 'a'), "key.keyboard." + c);
        }
        KEYS.put(91, "key.keyboard.left.bracket");
        KEYS.put(92, "key.keyboard.backslash");
        KEYS.put(93, "key.keyboard.right.bracket");
        KEYS.put(96, "key.keyboard.grave.accent");
        KEYS.put(161, "key.keyboard.world.1");
        KEYS.put(162, "key.keyboard.world.2");
        KEYS.put(256, "key.keyboard.escape");
        KEYS.put(257, "key.keyboard.enter");
        KEYS.put(258, "key.keyboard.tab");
        KEYS.put(259, "key.keyboard.backspace");
        KEYS.put(260, "key.keyboard.insert");
        KEYS.put(261, "key.keyboard.delete");
        KEYS.put(262, "key.keyboard.right");
        KEYS.put(263, "key.keyboard.left");
        KEYS.put(264, "key.keyboard.down");
        KEYS.put(265, "key.keyboard.up");
        KEYS.put(266, "key.keyboard.page.up");
        KEYS.put(267, "key.keyboard.page.down");
        KEYS.put(268, "key.keyboard.home");
        KEYS.put(269, "key.keyboard.end");
        KEYS.put(280, "key.keyboard.caps.lock");
        KEYS.put(281, "key.keyboard.scroll.lock");
        KEYS.put(282, "key.keyboard.num.lock");
        KEYS.put(283, "key.keyboard.print.screen");
        KEYS.put(284, "key.keyboard.pause");
        // GLFW also has F25 (314), but Minecraft 26.3 has no key for it; it maps to unbound.
        for (int i = 1; i <= 24; i++) {
            KEYS.put(289 + i, "key.keyboard.f" + i);
        }
        for (int i = 0; i <= 9; i++) {
            KEYS.put(320 + i, "key.keyboard.keypad." + i);
        }
        KEYS.put(330, "key.keyboard.keypad.decimal");
        KEYS.put(331, "key.keyboard.keypad.divide");
        KEYS.put(332, "key.keyboard.keypad.multiply");
        KEYS.put(333, "key.keyboard.keypad.subtract");
        KEYS.put(334, "key.keyboard.keypad.add");
        KEYS.put(335, "key.keyboard.keypad.enter");
        KEYS.put(336, "key.keyboard.keypad.equal");
        KEYS.put(340, "key.keyboard.left.shift");
        KEYS.put(341, "key.keyboard.left.control");
        KEYS.put(342, "key.keyboard.left.alt");
        KEYS.put(343, "key.keyboard.left.win");
        KEYS.put(344, "key.keyboard.right.shift");
        KEYS.put(345, "key.keyboard.right.control");
        KEYS.put(346, "key.keyboard.right.alt");
        KEYS.put(347, "key.keyboard.right.win");
        KEYS.put(348, "key.keyboard.menu");
    }

    private LegacyGlfwKeys() {
    }

    /**
     * Key name for a v1 keyCode. Codes 0-10 were GLFW mouse buttons
     * (0 = left, 1 = right, 2 = middle, 3+ = extra buttons); anything else is
     * a GLFW keyboard key.
     *
     * @param keyCode The GLFW code stored in a v1 config.
     */
    static String nameOf(int keyCode) {
        if (keyCode >= 0 && keyCode <= 10) {
            return switch (keyCode) {
                case 0 -> "key.mouse.left";
                case 1 -> "key.mouse.right";
                case 2 -> "key.mouse.middle";
                default -> "key.mouse." + (keyCode + 1);
            };
        }
        return KEYS.getOrDefault(keyCode, UNKNOWN);
    }

    /**
     * All keyboard entries, for tests.
     */
    static Map<Integer, String> keyboardTable() {
        return Map.copyOf(KEYS);
    }
}
