package kr.jinsu.radio;

import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,manifest=Config.NONE)
@ConscryptMode(ConscryptMode.Mode.OFF)
public class LiveAudioLayoutTest {
 ActivityController<PlayerLayoutTest.OfflineActivity> lifecycle;PlayerLayoutTest.OfflineActivity activity;
 @After public void destroy(){if(lifecycle!=null)lifecycle.destroy();RuntimeEnvironment.setQualifiers("");}
 void create(int w,boolean dark){RuntimeEnvironment.setQualifiers("w"+w+"dp-h800dp-"+(dark?"night":"notnight"));lifecycle=Robolectric.buildActivity(PlayerLayoutTest.OfflineActivity.class).create();activity=lifecycle.get();activity.selected=RadioData.find("yonhap");activity.build();}
 boolean contains(View root,String text){if(root instanceof TextView&&((TextView)root).getText().toString().contains(text))return true;if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++)if(contains(group.getChildAt(i),text))return true;}return false;}
 @Test public void playerHasSameControlsWithoutProgramPlaceholders(){create(412,false);activity.screen="player";activity.build();assertNotNull(activity.miniPlay);assertNull(activity.programTitle);assertNull(activity.programTime);assertNull(activity.programDescription);assertEquals(View.GONE,activity.miniProgress.getVisibility());assertEquals("연합뉴스TV",activity.miniTitle.getText().toString());assertTrue(contains(activity.root,"연합뉴스TV"));assertFalse(contains(activity.root,"MHz"));assertFalse(contains(activity.root,"오늘의 편성표"));assertFalse(contains(activity.root,"방송정보"));assertTrue(contains(activity.root,"방송 홈페이지"));}
 @Test public void listRowHasNoScheduleLookupOrProgress(){create(412,true);assertFalse(activity.rowPrograms.containsKey("yonhap"));assertFalse(activity.rowTimeProgress.containsKey("yonhap"));View row=activity.stationList.getChildAt(activity.stationList.getChildCount()-4);assertTrue(contains(row,"연합뉴스TV"));activity.toggleFavorite(activity.selected);activity.onlyFavorites=true;activity.build();assertTrue(contains(activity.stationList,"연합뉴스TV"));assertFalse(contains(activity.stationList,"현재 프로그램 정보 미제공"));}
 @Test public void tabletDetailKeepsAudioControlsAndDarkTheme(){create(1000,true);assertTrue(activity.wide);assertTrue(activity.dark);assertNotNull(activity.detailPane);assertNotNull(activity.miniPlay);assertNull(activity.programTitle);assertFalse(contains(activity.detailPane,"오늘의 편성표"));assertFalse(contains(activity.detailPane,"방송정보"));assertEquals(View.GONE,activity.miniProgress.getVisibility());}
 @Test public void staleScheduleScreenReturnsToAudioPlayer(){create(412,false);activity.screen="schedule";activity.build();assertEquals("player",activity.screen);assertNull(activity.scheduleListView);}
 @Test public void returningToRadioRestoresProgramsAndProgress(){create(412,false);activity.screen="player";activity.build();activity.selected=RadioData.find("kbs25");activity.build();assertNotNull(activity.programTitle);assertNotNull(activity.programDescription);assertTrue(contains(activity.root,"오늘의 편성표"));assertTrue(contains(activity.root,"MHz"));assertNotEquals(View.GONE,activity.miniProgress.getVisibility());}
}
