package com.skilllock;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SkillLocation
{
    public final String name;
    public final int x;
    public final int y;
    public final int level;
    public final boolean isLocked;
}
