package com.company.hunttech.web.screens.fragments;

import java.util.Objects;

/**
 * Immutable data class holding a parsed skill label result.
 * Safe to pass between threads (no UI references).
 */
public class SkillLabelData {
    public final String skillName;
    public final int counter;
    public final String styleName;
    public final String description;
    public final boolean isKeySkill;

    public SkillLabelData(String skillName, int counter, String styleName,
                          String description, boolean isKeySkill) {
        this.skillName = Objects.requireNonNull(skillName);
        this.counter = counter;
        this.styleName = styleName;
        this.description = description;
        this.isKeySkill = isKeySkill;
    }
}
