package kr.jinsu.radio;

import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
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
public class SystemThemeTest {
 public static class OfflineActivity extends PlayerLayoutTest.OfflineActivity {
  int connects;
  @Override void connect(){connects++;}
 }
 ActivityController<OfflineActivity> lifecycle;OfflineActivity activity;int width=412;
 @After public void destroy(){if(lifecycle!=null)lifecycle.destroy();RuntimeEnvironment.setQualifiers("");}
 void create(boolean night,int w){width=w;RuntimeEnvironment.setQualifiers("w"+w+"dp-h700dp-"+(night?"night":"notnight"));lifecycle=Robolectric.buildActivity(OfflineActivity.class).create();activity=lifecycle.get();frame();}
 void frame(){int w=activity.dp(width),h=activity.dp(700);activity.root.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));activity.root.layout(0,0,w,h);if(activity.stationScroll!=null)activity.stationScroll.getViewTreeObserver().dispatchOnPreDraw();if(activity.scroll!=null)activity.scroll.getViewTreeObserver().dispatchOnPreDraw();}
 void switchTheme(boolean night){RuntimeEnvironment.setQualifiers("w"+width+"dp-h700dp-"+(night?"night":"notnight"));activity.onConfigurationChanged(new Configuration(activity.getResources().getConfiguration()));frame();}
 int background(View view){return ((ColorDrawable)view.getBackground()).getColor();}
 @Test public void coldStartUsesSystemNightPaletteThroughoutScreens(){
  create(true,412);assertTrue(activity.dark);assertEquals(0xff161513,background(activity.root));assertEquals(0xffefe9de,activity.frequencyHeader.getCurrentTextColor());
  assertEquals(0xff1e1c19,background(activity.root.findViewById(R.id.playback_bar)));assertEquals(activity.INK,activity.miniTitle.getCurrentTextColor());
  for(String screen:new String[]{"player","schedule","list"}){activity.screen=screen;activity.build();frame();assertEquals(0xff161513,background(activity.root));if(activity.programTitle!=null)assertEquals(activity.INK,activity.programTitle.getCurrentTextColor());}
  assertEquals(0,activity.getWindow().getDecorView().getSystemUiVisibility()&View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
 }
 @Test public void dayNightDayKeepsSelectionFavoritesQueryAndConnection(){
  create(false,412);activity.selected=RadioData.find("ytn");activity.favorites.add("ytn");activity.onlyFavorites=true;activity.query="YTN";activity.sortOrder="name_desc";activity.build();frame();int generation=activity.generation;Object executor=activity.scheduleIo;
  switchTheme(true);assertTrue(activity.dark);assertEquals("ytn",activity.selected.id);assertTrue(activity.favorites.contains("ytn"));assertTrue(activity.onlyFavorites);assertEquals("YTN",activity.query);assertEquals("name_desc",activity.sortOrder);assertEquals(generation,activity.generation);assertSame(executor,activity.scheduleIo);assertEquals(1,activity.connects);
  switchTheme(false);assertFalse(activity.dark);assertEquals(0xfffafaf9,background(activity.root));assertEquals(0xff20201e,activity.frequencyHeader.getCurrentTextColor());assertEquals(0xffffffff,background(activity.root.findViewById(R.id.playback_bar)));assertTrue((activity.getWindow().getDecorView().getSystemUiVisibility()&View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)!=0);assertEquals(1,activity.connects);
 }
 void verifyListPosition(int w,boolean favorites){
  create(false,w);activity.onlyFavorites=favorites;for(RadioData.Station station:RadioData.STATIONS)activity.favorites.add(station.id);activity.build();frame();activity.stationScroll.scrollTo(0,activity.dp(240));int y=activity.stationScroll.getScrollY();assertTrue(y>0);
  switchTheme(true);assertEquals(y,activity.stationScroll.getScrollY());assertEquals(w>=600,activity.wide);switchTheme(false);assertEquals(y,activity.stationScroll.getScrollY());
 }
 @Test public void phoneListPositionSurvivesThemeChange(){verifyListPosition(412,false);}
 @Test public void tabletFavoritesPositionSurvivesThemeChange(){verifyListPosition(1000,true);}
 @Test public void infoDialogFollowsLiveThemeChange(){
  create(false,412);activity.info();android.app.AlertDialog old=activity.infoDialog;assertTrue(old.isShowing());switchTheme(true);assertFalse(old.isShowing());assertNotSame(old,activity.infoDialog);assertTrue(activity.infoDialog.isShowing());assertEquals(activity.ACCENT,activity.infoDialog.getButton(-1).getCurrentTextColor());
 }
 @Test public void timeLabelsAndProgressRemainReadableInBothModes(){
  create(false,412);for(boolean night:new boolean[]{false,true}){if(night)switchTheme(true);ProgramTimeRow row=new ProgramTimeRow(activity);assertEquals(activity.MUTED,((TextView)row.getChildAt(0)).getCurrentTextColor());assertTrue(contrast(activity.INK,activity.BG)>=4.5);assertTrue(contrast(activity.MUTED,activity.RAISED)>=4.5);assertTrue(contrast(activity.ACCENT,activity.RAISED)>=4.5);assertTrue(contrast(activity.getColor(R.color.radio_text),activity.getColor(R.color.radio_line))>=3);}
 }
 @Test public void themeChangeDropsOldBackdropAndKeepsPlayer(){
  create(false,412);activity.screen="player";activity.build();activity.listBackdrop=android.graphics.Bitmap.createBitmap(2,2,android.graphics.Bitmap.Config.RGB_565);switchTheme(true);assertEquals("player",activity.screen);assertNull(activity.listBackdrop);assertEquals(1,activity.connects);assertNotNull(activity.miniPlay);
 }
 static double luminance(int c){double[] v={((c>>16)&255)/255.0,((c>>8)&255)/255.0,(c&255)/255.0};for(int i=0;i<3;i++)v[i]=v[i]<=.04045?v[i]/12.92:Math.pow((v[i]+.055)/1.055,2.4);return .2126*v[0]+.7152*v[1]+.0722*v[2];}
 static double contrast(int a,int b){double x=luminance(a),y=luminance(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);}
}
