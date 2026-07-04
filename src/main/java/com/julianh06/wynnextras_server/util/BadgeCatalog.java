package com.julianh06.wynnextras_server.util;

import java.util.Set;

public final class BadgeCatalog {
    public static final String DEFAULT_BADGE_ICON_ID = "spark";
    public static final String DEFAULT_BADGE_COLOR_ID = "orange";

    public static final Set<String> BADGE_ICON_IDS = Set.of(
            "spark",
            "skull",
            "star",
            "heart",
            "note",
            "notes",
            "notg",
            "hollow_spark",
            "comet",
            "void",
            "crown",
            "fire",
            "cross",
            "staff",
            "snowflake",
            "cloud",
            "umbrella",
            "snowman",
            "lightning",
            "atom",
            "shamrock",
            "check",
            "warning",
            "anchor",
            "broken_heart",
            "scales",
            "ball",
            "triangle_up",
            "triangle_down",
            "triangle_left",
            "triangle_right",
            "sum",
            "lambda",
            "omega",
            "sun",
            "tick",
            "infinity",
            "star_2",
            "flower",
            "plant",
            "pinwheel",
            "hollow_square",
            "square",
            "hollow_circle",
            "circle",
            "hollow_diamond",
            "diamond"
    );

    public static final Set<String> BADGE_COLOR_IDS = Set.of(
            "orange",
            "red",
            "pink",
            "sapphire",
            "dark_green",
            "gray",
            "white",
            "rainbow",
            "shine",
            "gradient",
            "crimson",
            "black",
            "silver",
            "rose_gold",
            "bronze",
            "gold",
            "yellow",
            "lime",
            "green",
            "mint",
            "aqua",
            "light_blue",
            "navy",
            "void",
            "indigo",
            "violet",
            "purple",
            "magenta",
            "dark_red",
            "darker_red"
    );

    private BadgeCatalog() {
    }

    public static String normalizeBadgeIconId(String badgeIconId) {
        return normalizeCatalogId(badgeIconId, BADGE_ICON_IDS, DEFAULT_BADGE_ICON_ID);
    }

    public static String normalizeBadgeColorId(String badgeColorId) {
        return normalizeCatalogId(badgeColorId, BADGE_COLOR_IDS, DEFAULT_BADGE_COLOR_ID);
    }

    private static String normalizeCatalogId(String value, Set<String> validIds, String defaultId) {
        if (value == null) {
            return defaultId;
        }

        String normalized = value.trim();
        return validIds.contains(normalized) ? normalized : defaultId;
    }
}