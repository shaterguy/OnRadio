package kr.jinsu.radio;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class ChannelNavigationTest {
 @Test public void nextUsesFrequencyRatherThanStationName(){assertEquals("mbcmfm",ChannelNavigation.next("kbs25",1,false,Collections.emptySet()).id);assertEquals("kbs24",ChannelNavigation.next("mbcmfm",1,false,Collections.emptySet()).id);}
 @Test public void previousMovesLower(){assertEquals("kbs25",ChannelNavigation.next("mbcmfm",-1,false,Collections.emptySet()).id);}
 @Test public void endsWrap(){List<RadioData.Station> all=RadioData.orderedStations("frequency");assertEquals(all.get(0).id,ChannelNavigation.next(all.get(all.size()-1).id,1,false,Collections.emptySet()).id);assertEquals(all.get(all.size()-1).id,ChannelNavigation.next(all.get(0).id,-1,false,Collections.emptySet()).id);}
 @Test public void repeatedInputAccumulatesFromPendingTarget(){String next=ChannelNavigation.next("kbs25",1,false,Collections.emptySet()).id;assertEquals("kbs24",ChannelNavigation.next(next,1,false,Collections.emptySet()).id);}
 @Test public void favoritesUseFrequencyAndWrap(){Set<String> favorites=new HashSet<>(Arrays.asList("sbspower","kbs25","ytn"));assertEquals("ytn",ChannelNavigation.next("kbs25",1,true,favorites).id);assertEquals("sbspower",ChannelNavigation.next("kbs25",-1,true,favorites).id);}
 @Test public void emptyFavoritesFallBackToAll(){assertEquals("mbcmfm",ChannelNavigation.next("kbs25",1,true,Collections.emptySet()).id);}
}
