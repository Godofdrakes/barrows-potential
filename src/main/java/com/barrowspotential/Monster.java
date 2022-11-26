package com.barrowspotential;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.NpcID;
import net.runelite.api.Varbits;

import java.util.*;

@RequiredArgsConstructor
@Getter
public enum Monster
{
    // Brothers
    Ahrim( "Ahrim the Blighted", 98, NpcID.AHRIM_THE_BLIGHTED, Varbits.BARROWS_KILLED_AHRIM ),
    Dharok( "Dharok the Wretched", 115, NpcID.DHAROK_THE_WRETCHED, Varbits.BARROWS_KILLED_DHAROK ),
    Guthan( "Guthan the Infested", 115, NpcID.GUTHAN_THE_INFESTED, Varbits.BARROWS_KILLED_GUTHAN ),
    Karil( "Karil the Tainted", 98, NpcID.KARIL_THE_TAINTED, Varbits.BARROWS_KILLED_KARIL ),
    Torag( "Torag the Corrupted", 115, NpcID.TORAG_THE_CORRUPTED, Varbits.BARROWS_KILLED_TORAG ),
    Verac( "Verac the Defiled", 115, NpcID.VERAC_THE_DEFILED, Varbits.BARROWS_KILLED_VERAC ),

    // Crypt Monsters
    CryptRat( "Crypt rat", 43, NpcID.CRYPT_RAT ),
    Bloodworm( "Bloodworm", 52, NpcID.BLOODWORM ),
    CryptSpider( "Crypt spider", 56, NpcID.CRYPT_SPIDER ),
    GiantCryptRat( "Giant crypt rat", 76, new int[]
        {
            NpcID.GIANT_CRYPT_RAT,
            NpcID.GIANT_CRYPT_RAT_1681,
            NpcID.GIANT_CRYPT_RAT_1682
        } ),
    Skeleton( "Skeleton", 77, new int[]
        {
            NpcID.SKELETON_1685,
            NpcID.SKELETON_1686,
            NpcID.SKELETON_1687,
            NpcID.SKELETON_1688
        } ),
    GiantCryptSpider( "Giant crypt spider", 79, NpcID.GIANT_CRYPT_SPIDER );

    Monster( String displayName, int combatLevel, int npcID )
    {
        this( displayName, combatLevel, npcID, 0 );
    }

    Monster( String displayName, int combatLevel, int[] npcIDs )
    {
        this( displayName, combatLevel, npcIDs, 0 );
    }

    Monster( String displayName, int combatLevel, int npcID, int varbit )
    {
        this( displayName, combatLevel, new int[] { npcID }, varbit );
    }

    private final String displayName;
    private final int combatLevel;
    private final int[] npcIDs;
    private final int varbit;

    public final boolean isBrother()
    {
        return getVarbit() != 0;
    }

    private static Set<Monster> getBrothers()
    {
        HashSet<Monster> set = new HashSet<>();

        for ( Monster monster : Monster.values() )
        {
            if ( monster.isBrother() )
            {
                set.add( monster );
            }
        }

        return set;
    }

    private static Set<Monster> getCryptMonsters()
    {
        HashSet<Monster> set = new HashSet<>();

        for ( Monster monster : Monster.values() )
        {
            if ( !monster.isBrother() )
            {
                set.add( monster );
            }
        }

        return set;
    }

    private static Map<Integer,Monster> getCryptMonstersByNpcID()
    {
        HashMap<Integer,Monster> map = new HashMap<>();

        for ( Monster monster : Monster.values() )
        {
            if ( monster.isBrother() )
                continue;

            for ( int npcID : monster.getNpcIDs() )
            {
                map.put( npcID, monster );
            }
        }

        return map;
    }

    private static Map<Integer,Monster> getBrothersByVarbit()
    {
        HashMap<Integer,Monster> map = new HashMap<>();

        for ( Monster monster : Monster.values() )
        {
            if ( !monster.isBrother() )
                continue;

            map.put( monster.getVarbit(), monster );
        }

        return map;
    }

    public static final Set<Monster> brothers = getBrothers();
    public static final Set<Monster> cryptMonsters = getCryptMonsters();

    public static final Map<Integer,Monster> cryptMonsterByNpcID = getCryptMonstersByNpcID();
    public static final Map<Integer,Monster> brothersByVarbit = getBrothersByVarbit();
}
