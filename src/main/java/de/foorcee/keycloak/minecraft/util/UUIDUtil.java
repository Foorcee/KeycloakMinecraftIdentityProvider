package de.foorcee.keycloak.minecraft.util;

public class UUIDUtil {

    private static final String DASHED_UUID_REGEX = "(\\p{XDigit}{8})-(\\p{XDigit}{4})-(\\p{XDigit}{4})-(\\p{XDigit}{4})-(\\p{XDigit}+)";

    public static boolean isDashedUuid(final String s) {
        return s.matches(DASHED_UUID_REGEX);
    }
}
