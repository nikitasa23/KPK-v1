package com.example.examplemod;

public enum Gender {
    MALE("Мужской"),
    FEMALE("Женский");

    private final String displayName;

    Gender(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Gender fromString(String text) {
        if (text != null) {
            for (Gender g : Gender.values()) {
                if (text.equalsIgnoreCase(g.name()) || text.equalsIgnoreCase("м") && g == MALE ||
                        text.equalsIgnoreCase("ж") && g == FEMALE) {
                    return g;
                }
            }
        }
        return null;
    }
}