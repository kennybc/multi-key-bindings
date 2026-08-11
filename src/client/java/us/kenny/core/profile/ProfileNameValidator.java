package us.kenny.core.profile;

import java.util.regex.Pattern;

/**
 * Validates profile names. Names must match [A-Za-z0-9_-]{1,32} and be
 * unique case-insensitively.
 */
public final class ProfileNameValidator {
    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9_-]{1,32}");

    private ProfileNameValidator() {
    }

    public static boolean isValid(String name) {
        return name != null && VALID.matcher(name).matches();
    }

    public static boolean isTaken(String name, Iterable<String> existing) {
        for (String e : existing) {
            if (e.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }
}
