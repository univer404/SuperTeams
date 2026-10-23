package com.superteams.team;

import java.util.*;

public final class MemberData {
    private final UUID uuid;
    private String lastKnownName;
    private final long joinedAt;
    private final EnumSet<TeamRight> rights;
    public MemberData(UUID uuid,String name,long joinedAt,Collection<TeamRight> rights){this.uuid=uuid;this.lastKnownName=name;this.joinedAt=joinedAt;this.rights=rights.isEmpty()?EnumSet.noneOf(TeamRight.class):EnumSet.copyOf(rights);}
    public UUID uuid(){return uuid;} public String lastKnownName(){return lastKnownName;} public void lastKnownName(String n){lastKnownName=n;}
    public long joinedAt(){return joinedAt;} public EnumSet<TeamRight> rights(){return rights;}
    public boolean has(TeamRight right){return rights.contains(right);}
}