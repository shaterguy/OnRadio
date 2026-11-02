package kr.jinsu.radio;

import android.view.View;
import android.widget.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,manifest=Config.NONE)
@ConscryptMode(ConscryptMode.Mode.OFF)
public class StationListPositionTest {
 public static class SelectingActivity extends PlayerLayoutTest.OfflineActivity {
  @Override void play(RadioData.Station station){selected=station;if(wide)build();}
 }
 ActivityController<SelectingActivity> lifecycle;SelectingActivity activity;int width;
 @After public void destroy(){if(lifecycle!=null)lifecycle.destroy();RuntimeEnvironment.setQualifiers("");}
 void create(int w,boolean favorites){width=w;RuntimeEnvironment.setQualifiers("w"+w+"dp-h700dp");lifecycle=Robolectric.buildActivity(SelectingActivity.class).create();activity=lifecycle.get();activity.onlyFavorites=favorites;activity.favorites.clear();for(RadioData.Station s:RadioData.STATIONS)activity.favorites.add(s.id);activity.build();frame();}
 void frame(){int w=activity.dp(width),h=activity.dp(700);activity.root.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));activity.root.layout(0,0,w,h);activity.stationScroll.getViewTreeObserver().dispatchOnPreDraw();}
 void selectAndRefresh(){activity.stationScroll.scrollTo(0,activity.dp(240));int before=activity.stationScroll.getScrollY();assertTrue(before>0);View row=(View)((View)activity.rowPrograms.get("ytn").getParent()).getParent();assertTrue(row.performClick());frame();assertEquals("ytn",activity.selected.id);assertEquals(before,activity.stationScroll.getScrollY());activity.populate();activity.populate();frame();assertEquals(before,activity.stationScroll.getScrollY());activity.build();frame();assertEquals(before,activity.stationScroll.getScrollY());}
 @Test public void phoneAllStationsSelectionAndConnectionRefreshKeepPosition(){create(412,false);selectAndRefresh();}
 @Test public void phoneFavoritesSelectionAndConnectionRefreshKeepPosition(){create(412,true);selectAndRefresh();}
 @Test public void tabletAllStationsSelectionAndConnectionRefreshKeepPosition(){create(1000,false);selectAndRefresh();}
 @Test public void tabletFavoritesSelectionAndConnectionRefreshKeepPosition(){create(1000,true);selectAndRefresh();}
 @Test public void explicitSortResetsInsteadOfApplyingAnOldPendingRestore(){create(412,false);activity.stationScroll.scrollTo(0,activity.dp(240));activity.populate();activity.changeSort("name");frame();assertEquals(0,activity.stationScroll.getScrollY());assertFalse(activity.restoreListPosition);}
}
