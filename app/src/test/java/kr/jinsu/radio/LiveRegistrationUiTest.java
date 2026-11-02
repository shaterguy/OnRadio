package kr.jinsu.radio;

import android.app.AlertDialog;
import android.content.res.Configuration;
import android.os.*;
import android.view.*;
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
public class LiveRegistrationUiTest {
 public static class Offline extends PlayerLayoutTest.OfflineActivity {
  int inspections,plays;boolean hold,fail;final java.util.concurrent.CountDownLatch entered=new java.util.concurrent.CountDownLatch(1),release=new java.util.concurrent.CountDownLatch(1);
  @Override YoutubeLiveAudio.LiveInfo inspectLive(String url)throws Exception{inspections++;entered.countDown();if(hold)release.await();if(fail)throw new java.io.IOException("test unavailable");return new YoutubeLiveAudio.LiveInfo("abcdefghijk","UCaaaaaaaaaaaaaaaaaaaaaa","https://manifest.googlevideo.com/audio.m3u8");}
  @Override void play(RadioData.Station station){plays++;}
 }
 ActivityController<Offline> lifecycle;Offline activity;
 @Before public void create(){RuntimeEnvironment.setQualifiers("w360dp-h800dp");lifecycle=Robolectric.buildActivity(Offline.class).create().start().resume();activity=lifecycle.get();}
 @After public void destroy(){lifecycle.pause().stop().destroy();RadioData.setLiveOverrides(Collections.emptyList());RuntimeEnvironment.setQualifiers("");}
 boolean contains(View view,String value){if(view instanceof TextView&&((TextView)view).getText().toString().equals(value))return true;if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++)if(contains(g.getChildAt(i),value))return true;}return false;}
 void show(){activity.showLiveEditor(null);activity.liveNameInput.setText("내 라이브");activity.liveUrlInput.setText("https://youtu.be/abcdefghijk");}
 YoutubeLiveAudio.LiveInfo result(){return new YoutubeLiveAudio.LiveInfo("abcdefghijk","UCaaaaaaaaaaaaaaaaaaaaaa","https://manifest.googlevideo.com/audio.m3u8");}
 @Test public void sourceTabsSeparateListsAndFavoritesRemainShared(){assertTrue(contains(activity.stationList,"KBS Cool FM"));assertFalse(contains(activity.stationList,"연합뉴스TV"));String selected=activity.selected.id;activity.selectSourceTab("live");assertEquals(View.GONE,((View)activity.frequencyHeader.getParent()).getVisibility());assertFalse(contains(activity.stationList,"KBS Cool FM"));assertTrue(contains(activity.stationList,"연합뉴스TV"));assertTrue(contains(activity.stationList,"YTN"));assertTrue(contains(activity.stationList,"MBC뉴스"));assertEquals(selected,activity.selected.id);activity.toggleFavorite(RadioData.find("yonhap"));activity.toggleFavorite(RadioData.find("kbs25"));activity.selectSourceTab("favorites");assertEquals(View.VISIBLE,((View)activity.frequencyHeader.getParent()).getVisibility());assertTrue(contains(activity.stationList,"KBS Cool FM"));assertTrue(contains(activity.stationList,"연합뉴스TV"));assertEquals(0,activity.plays);}
 @Test public void menuOpensAddFormAndRealSavePathPersistsWithoutAutoplay()throws Exception{activity.showAppMenu(activity.menuButton);assertTrue(activity.appMenu.getMenu().performIdentifierAction(1,0));assertNotNull(activity.liveDialog);activity.liveNameInput.setText("내 라이브");activity.liveUrlInput.setText("https://youtu.be/abcdefghijk");activity.liveDialog.getButton(-1).performClick();long end=System.currentTimeMillis()+3000;while(activity.liveDialog!=null&&System.currentTimeMillis()<end){Shadows.shadowOf(Looper.getMainLooper()).idle();Thread.sleep(5);}assertNull(activity.liveDialog);assertEquals(1,activity.inspections);assertEquals(0,activity.plays);assertTrue(contains(activity.stationList,"내 라이브"));assertEquals("live",activity.sourceTab);RadioData.setLiveOverrides(Collections.emptyList());LiveStationStore.reload(activity.prefs);assertEquals(18,RadioData.stations().size());}
 @Test public void cancelPauseAndNewDialogRejectLateResults(){show();AlertDialog old=activity.liveDialog;int ticket=activity.liveRequestTicket;old.dismiss();activity.finishLiveRegistration(old,ticket,null,"잘못된 저장",result(),"");assertEquals(17,RadioData.stations().size());show();old=activity.liveDialog;ticket=activity.liveRequestTicket;lifecycle.pause();activity.finishLiveRegistration(old,ticket,null,"백그라운드 저장",result(),"");assertEquals(17,RadioData.stations().size());lifecycle.resume();activity.showLiveEditor(null);activity.finishLiveRegistration(old,ticket,null,"오래된 창",result(),"");assertEquals(17,RadioData.stations().size());}
 @Test public void rotationPreservesDraftAndInvalidatesOldVerification(){show();AlertDialog old=activity.liveDialog;int ticket=activity.liveRequestTicket;activity.onConfigurationChanged(new Configuration(activity.getResources().getConfiguration()));assertNotSame(old,activity.liveDialog);assertEquals("내 라이브",activity.liveNameInput.getText().toString());assertEquals("https://youtu.be/abcdefghijk",activity.liveUrlInput.getText().toString());activity.finishLiveRegistration(old,ticket,null,"늦은 저장",result(),"");assertEquals(17,RadioData.stations().size());}
 @Test public void failedValidationPreservesExistingEntryFavoriteAndForm(){activity.selected=RadioData.find("yonhap");activity.favorites.add("yonhap");activity.showLiveEditor(activity.selected);AlertDialog form=activity.liveDialog;activity.liveUrlInput.setText("https://youtu.be/abcdefghijk");activity.finishLiveRegistration(form,activity.liveRequestTicket,"yonhap","변경 이름",null,"라이브 확인 실패");assertEquals("6QZ_qc75ihU",RadioData.find("yonhap").code);assertTrue(activity.favorites.contains("yonhap"));assertSame(form,activity.liveDialog);assertEquals("https://youtu.be/abcdefghijk",activity.liveUrlInput.getText().toString());}
 @Test public void playerEditButtonOpensExistingSourceAndSaveKeepsFavorite(){activity.selected=RadioData.find("yonhap");activity.prefs.edit().putStringSet("favorites",new HashSet<>(Arrays.asList("yonhap"))).commit();activity.favorites.add("yonhap");activity.screen="player";activity.build();assertTrue(contains(activity.body,"라이브 주소 수정"));activity.showLiveEditor(activity.selected);AlertDialog form=activity.liveDialog;assertEquals("https://www.youtube.com/watch?v=6QZ_qc75ihU",activity.liveUrlInput.getText().toString());activity.finishLiveRegistration(form,activity.liveRequestTicket,"yonhap","내 뉴스",result(),"");assertEquals("yonhap",activity.selected.id);assertEquals("abcdefghijk",activity.selected.code);assertTrue(activity.prefs.getStringSet("favorites",Collections.emptySet()).contains("yonhap"));assertEquals(0,activity.plays);}
 @Test public void narrowTabsKeepSingleLineAndTouchHeight(){int w=activity.dp(360),h=activity.dp(800);activity.root.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));activity.root.layout(0,0,w,h);LinearLayout tabs=(LinearLayout)activity.body.getChildAt(0);assertEquals(3,tabs.getChildCount());for(int i=0;i<3;i++){TextView tab=(TextView)tabs.getChildAt(i);assertTrue(tab.getHeight()>=activity.dp(48));assertEquals(1,tab.getLineCount());assertEquals(0,tab.getLayout().getEllipsisCount(0));}}
 @Test public void pendingCheckLocksInputsAndCancellationRestoresEditing()throws Exception{show();activity.hold=true;activity.liveDialog.getButton(-1).performClick();assertTrue(activity.entered.await(2,java.util.concurrent.TimeUnit.SECONDS));assertFalse(activity.liveNameInput.isEnabled());assertFalse(activity.liveUrlInput.isEnabled());assertFalse(activity.liveDialog.getButton(-1).isEnabled());activity.cancelLiveCheck();assertTrue(activity.liveNameInput.isEnabled());assertTrue(activity.liveUrlInput.isEnabled());assertTrue(activity.liveDialog.getButton(-1).isEnabled());activity.release.countDown();assertEquals(17,RadioData.stations().size());}
 @Test public void asyncFailureReenablesFormWithoutSavingOrAutoplay()throws Exception{show();activity.fail=true;activity.liveDialog.getButton(-1).performClick();long end=System.currentTimeMillis()+3000;while(!activity.liveDialog.getButton(-1).isEnabled()&&System.currentTimeMillis()<end){Shadows.shadowOf(Looper.getMainLooper()).idle();Thread.sleep(5);}assertNotNull(activity.liveDialog);assertTrue(activity.liveNameInput.isEnabled());assertTrue(activity.liveUrlInput.isEnabled());assertEquals("내 라이브",activity.liveNameInput.getText().toString());assertEquals(17,RadioData.stations().size());assertEquals(0,activity.plays);}

 @Test public void sourceEditClearsActivePlayIntentAndPlaylist(){androidx.media3.exoplayer.ExoPlayer engine=new androidx.media3.exoplayer.ExoPlayer.Builder(activity).build();try{engine.setMediaItem(androidx.media3.common.MediaItem.fromUri("https://example.invalid/live.m3u8"));engine.setPlayWhenReady(true);assertTrue(engine.getPlayWhenReady());assertEquals(1,engine.getMediaItemCount());MainActivity.pauseAndClear(engine);assertFalse(engine.getPlayWhenReady());assertEquals(0,engine.getMediaItemCount());assertEquals(androidx.media3.common.Player.STATE_IDLE,engine.getPlaybackState());}finally{engine.release();}}


 @Test @Config(sdk={26,28}) public void ordinaryInputsStayOutsideAutofillAcrossAddEditAndRebuild(){
  assertEquals(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS,activity.getWindow().getDecorView().getImportantForAutofill());
  assertExcludedInputs(activity.getWindow().getDecorView());
  activity.showLiveEditor(null);assertLiveFormExcluded();
  activity.liveNameInput.setText("내 뉴스");activity.liveUrlInput.setText("https://youtu.be/abcdefghijk");
  activity.onConfigurationChanged(new Configuration(activity.getResources().getConfiguration()));
  assertLiveFormExcluded();assertEquals("내 뉴스",activity.liveNameInput.getText().toString());assertEquals("https://youtu.be/abcdefghijk",activity.liveUrlInput.getText().toString());
  activity.showLiveEditor(RadioData.find("yonhap"));assertLiveFormExcluded();assertEquals("연합뉴스TV",activity.liveNameInput.getText().toString());
  activity.liveNameInput.requestFocus();activity.liveUrlInput.requestFocus();assertLiveFormExcluded();
 }
 void assertLiveFormExcluded(){
  assertEquals(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS,activity.liveDialog.getWindow().getDecorView().getImportantForAutofill());
  assertExcludedInputs(activity.liveDialog.getWindow().getDecorView());
  assertFalse(activity.liveNameInput.getTransformationMethod() instanceof android.text.method.PasswordTransformationMethod);assertFalse(activity.liveUrlInput.getTransformationMethod() instanceof android.text.method.PasswordTransformationMethod);
  assertEquals(android.text.InputType.TYPE_TEXT_VARIATION_URI,activity.liveUrlInput.getInputType()&android.text.InputType.TYPE_MASK_VARIATION);
 }
 void assertExcludedInputs(View view){
  if(view instanceof EditText){assertFalse(view.isImportantForAutofill());assertNull(view.getAutofillHints());assertTrue(view.isFocusable());}
  if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)assertExcludedInputs(group.getChildAt(i));}
 }
}
