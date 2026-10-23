package com.superteams.team;

import java.util.*;
import org.bukkit.inventory.ItemStack;

public final class TeamData {
    private final UUID id;
    private String name;
    private UUID owner;
    private final long createdAt;
    private final Map<UUID, MemberData> members = new LinkedHashMap<>();
    private String homeWorld;
    private double homeX, homeY, homeZ;
    private float homeYaw, homePitch;
    private boolean homeSet;
    private boolean friendlyFire;
    private double bankBalance;
    private final ItemStack[] enderChest = new ItemStack[27];

    public TeamData(UUID id, String name, UUID owner, long createdAt) {
        this.id=id; this.name=name; this.owner=owner; this.createdAt=createdAt;
        members.put(owner, new MemberData(owner, "unknown", createdAt, EnumSet.allOf(TeamRight.class)));
    }
    public UUID id(){return id;} public String name(){return name;} public void name(String v){name=v;}
    public UUID owner(){return owner;} public void owner(UUID v){owner=v;}
    public long createdAt(){return createdAt;} public Map<UUID,MemberData> members(){return members;}
    public boolean friendlyFire(){return friendlyFire;} public void friendlyFire(boolean v){friendlyFire=v;}
    public double bankBalance(){return bankBalance;} public void bankBalance(double v){if(v<0 || !Double.isFinite(v)) throw new IllegalArgumentException("Invalid bank balance"); bankBalance=v;}
    public boolean homeSet(){return homeSet;}
    public void home(String world,double x,double y,double z,float yaw,float pitch){homeWorld=world;homeX=x;homeY=y;homeZ=z;homeYaw=yaw;homePitch=pitch;homeSet=true;}
    public String homeWorld(){return homeWorld;} public double homeX(){return homeX;} public double homeY(){return homeY;} public double homeZ(){return homeZ;} public float homeYaw(){return homeYaw;} public float homePitch(){return homePitch;}
    public MemberData member(UUID uuid){return members.get(uuid);}
    public ItemStack[] enderChestContents(){ItemStack[] copy=new ItemStack[enderChest.length];for(int i=0;i<enderChest.length;i++)copy[i]=enderChest[i]==null?null:enderChest[i].clone();return copy;}
    public void enderChestContents(ItemStack[] contents){Arrays.fill(enderChest,null);if(contents!=null)for(int i=0;i<Math.min(enderChest.length,contents.length);i++)enderChest[i]=contents[i]==null?null:contents[i].clone();}
    public boolean addMember(MemberData member){return members.putIfAbsent(member.uuid(),member)==null;}
    public MemberData removeMember(UUID uuid){if(uuid.equals(owner)) throw new IllegalArgumentException("Cannot remove team owner");return members.remove(uuid);}
    public void transferOwner(UUID next){if(!members.containsKey(next)) throw new IllegalArgumentException("New owner must be a member");if(next.equals(owner))return;UUID previous=owner;owner=next;members.get(next).rights().addAll(EnumSet.allOf(TeamRight.class));members.get(previous).rights().clear();}
}