package kr.jinsu.radio;

import java.time.LocalDate;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk=28,manifest=org.robolectric.annotation.Config.NONE)
@org.robolectric.annotation.ConscryptMode(org.robolectric.annotation.ConscryptMode.Mode.OFF)
public class LiveAudioStationTest {
 @Test public void liveStationStaysLastInEverySort(){for(String order:new String[]{"frequency","frequency_desc","name","name_desc"}){List<RadioData.Station> rows=RadioData.orderedStations(order);assertEquals(17,rows.size());assertEquals("yonhap",rows.get(14).id);assertEquals("ytnlive",rows.get(15).id);assertEquals("mbcnews",rows.get(16).id);}}
 @Test public void liveStationHasNoInventedFrequencyOrPrograms()throws Exception{RadioData.Station s=RadioData.find("yonhap");assertFalse(s.hasPrograms());assertEquals("",s.fm);assertEquals("LIVE",s.displayFrequency());assertEquals("",s.description);assertEquals("",s.schedule);assertEquals("연합뉴스TV",s.mediaArtist());assertTrue(RadioData.schedule(s,LocalDate.now()).isEmpty());}
 @Test public void nextAndPreviousIncludeLiveStation(){assertEquals("kbs25",ChannelNavigation.next("sbspower",1,false,Collections.emptySet()).id);assertEquals("ytnlive",ChannelNavigation.next("yonhap",1,false,Collections.emptySet()).id);assertEquals("mbcnews",ChannelNavigation.next("yonhap",-1,false,Collections.emptySet()).id);}
 @Test public void liveStationUsesSameFavoriteNavigation(){Set<String> favorites=new HashSet<>(Arrays.asList("kbs25","yonhap"));assertEquals("yonhap",ChannelNavigation.next("kbs25",1,true,favorites).id);assertEquals("kbs25",ChannelNavigation.next("yonhap",1,true,favorites).id);assertEquals("yonhap",ChannelNavigation.next("yonhap",1,true,Collections.singleton("yonhap")).id);}
 @Test public void liveRecoveryIsBoundedAndNeverRestartsPausedOrFmPlayback(){RadioData.Station live=RadioData.find("yonhap"),fm=RadioData.find("kbs25");assertTrue(PlaybackService.canRecoverLive(live,true,60000,0));assertFalse(PlaybackService.canRecoverLive(live,true,59999,0));assertFalse(PlaybackService.canRecoverLive(live,false,60000,0));assertFalse(PlaybackService.canRecoverLive(fm,true,60000,0));assertTrue(PlaybackService.canRecoverLive(live,true,0,-60000));}
}

