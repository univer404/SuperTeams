package com.superteams.team;

import org.junit.jupiter.api.Test;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
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

    @Test void bankBalanceRejectsNegativeAndNonFiniteValues() {
        TeamData team=new TeamData(UUID.randomUUID(),"Guild",UUID.randomUUID(),1L);
        assertThrows(IllegalArgumentException.class,()->team.bankBalance(-1));
        assertThrows(IllegalArgumentException.class,()->team.bankBalance(Double.NaN));
        assertThrows(IllegalArgumentException.class,()->team.bankBalance(Double.POSITIVE_INFINITY));
        team.bankBalance(125.5);
        assertEquals(125.5,team.bankBalance());
    }
}