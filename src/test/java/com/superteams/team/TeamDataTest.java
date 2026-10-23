package com.superteams.team;

import org.junit.jupiter.api.Test;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import static org.junit.jupiter.api.Assertions.*;

class TeamDataTest {
    @Test void ownerStartsWithEveryRight() {
        UUID owner=UUID.randomUUID();
        TeamData team=new TeamData(UUID.randomUUID(),"Guild",owner,1L);
        assertEquals(owner,team.owner());
        assertTrue(team.member(owner).rights().containsAll(EnumSet.allOf(TeamRight.class)));
    }

    @Test void memberRightsAreIndependent() {
        UUID owner=UUID.randomUUID(), member=UUID.randomUUID();
        TeamData team=new TeamData(UUID.randomUUID(),"Guild",owner,1L);
        MemberData data=new MemberData(member,"Member",2L,Set.of(TeamRight.INVITE));
        assertTrue(team.addMember(data));
        assertTrue(data.has(TeamRight.INVITE));
        assertFalse(data.has(TeamRight.KICK));
        data.rights().add(TeamRight.HOME_USE);
        assertTrue(data.has(TeamRight.HOME_USE));
        assertTrue(team.member(owner).has(TeamRight.HOME_USE));
    }

    @Test void ownershipTransferRequiresExistingMemberAndRevokesOldRights() {
        UUID owner=UUID.randomUUID(), member=UUID.randomUUID();
        TeamData team=new TeamData(UUID.randomUUID(),"Guild",owner,1L);
        team.addMember(new MemberData(member,"Member",2L,Set.of(TeamRight.CHAT)));
        assertThrows(IllegalArgumentException.class,()->team.transferOwner(UUID.randomUUID()));
        team.transferOwner(member);
        assertEquals(member,team.owner());
        assertTrue(team.member(member).rights().containsAll(EnumSet.allOf(TeamRight.class)));
        assertTrue(team.member(owner).rights().isEmpty());
    }

    @Test void teamEnderChestContentsAreDefensiveCopies() {
        TeamData team=new TeamData(UUID.randomUUID(),"Guild",UUID.randomUUID(),1L);
        ItemStack[] contents=new ItemStack[27];contents[0]=new ItemStack(Material.DIAMOND,3);
        team.enderChestContents(contents);
        contents[0].setAmount(1);
        assertEquals(3,team.enderChestContents()[0].getAmount());
        ItemStack[] read=team.enderChestContents();read[0].setAmount(2);
        assertEquals(3,team.enderChestContents()[0].getAmount());
        assertEquals(27,team.enderChestContents().length);
    }

    @Test void bankBalanceRejectsNegativeAndNonFiniteValues() {
        TeamData team=new TeamData(UUID.randomUUID(),"Guild",UUID.randomUUID(),1L);
        assertThrows(IllegalArgumentException.class,()->team.bankBalance(-1));
        assertThrows(IllegalArgumentException.class,()->team.bankBalance(Double.NaN));
        assertThrows(IllegalArgumentException.class,()->team.bankBalance(Double.POSITIVE_INFINITY));
        team.bankBalance(125.5);
        assertEquals(125.5,team.bankBalance());
    }
}