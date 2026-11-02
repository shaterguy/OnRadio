package kr.jinsu.radio;

import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.ConscryptMode;
import org.robolectric.shadows.ShadowLooper;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,manifest=Config.NONE)
@ConscryptMode(ConscryptMode.Mode.OFF)
public class RadioSessionPlayerTest {
 ExoPlayer engine;RadioSessionPlayer sessionPlayer;List<Integer> requests;java.util.concurrent.atomic.AtomicLong wallClock;
 @Before public void create(){engine=new ExoPlayer.Builder(RuntimeEnvironment.getApplication()).build();requests=new ArrayList<>();wallClock=new java.util.concurrent.atomic.AtomicLong(1801000);sessionPlayer=new RadioSessionPlayer(engine,requests::add,wallClock::get);}
 @After public void release(){sessionPlayer.release();ShadowLooper.idleMainLooper();}
 void assertNavigationAvailable(){for(int command:new int[]{Player.COMMAND_SEEK_TO_NEXT,Player.COMMAND_SEEK_TO_PREVIOUS,Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM}){assertTrue(sessionPlayer.getAvailableCommands().contains(command));assertTrue(sessionPlayer.isCommandAvailable(command));}}
 @Test public void advertisesAndHandlesAllFourCommandsWithSingleStation(){engine.setMediaItem(MediaItem.fromUri("https://example.test/live.m3u8"));ShadowLooper.idleMainLooper();assertNavigationAvailable();sessionPlayer.seekToNext();sessionPlayer.seekToPrevious();sessionPlayer.seekToNextMediaItem();sessionPlayer.seekToPreviousMediaItem();assertEquals(Arrays.asList(1,-1,1,-1),requests);}
 @Test public void commandsSurviveEmptyAndReplacedTimeline(){sessionPlayer.addListener(new Player.Listener(){});assertNavigationAvailable();engine.setMediaItem(MediaItem.fromUri("https://example.test/a.m3u8"));ShadowLooper.idleMainLooper();assertNavigationAvailable();engine.clearMediaItems();ShadowLooper.idleMainLooper();assertNavigationAvailable();sessionPlayer.seekToNext();assertEquals(Collections.singletonList(1),requests);}
 @Test public void exposesChannelsRatherThanTimeSkipping(){assertFalse(sessionPlayer.isCommandAvailable(Player.COMMAND_SEEK_BACK));assertFalse(sessionPlayer.isCommandAvailable(Player.COMMAND_SEEK_FORWARD));}
 @Test public void mediaSessionControllerDispatchesPreviousAndNextWithoutActivity() throws Exception {
  androidx.media3.session.MediaSession session=new androidx.media3.session.MediaSession.Builder(RuntimeEnvironment.getApplication(),sessionPlayer).build();
  androidx.media3.session.MediaController controller=null;
  try {
   com.google.common.util.concurrent.ListenableFuture<androidx.media3.session.MediaController> future=new androidx.media3.session.MediaController.Builder(RuntimeEnvironment.getApplication(),session.getToken()).buildAsync();
   for(int attempt=0;attempt<200&&!future.isDone();attempt++){ShadowLooper.idleMainLooper();Thread.sleep(5);}
   assertTrue("session controller connected",future.isDone());controller=future.get();
   assertTrue(controller.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT));assertTrue(controller.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS));
   controller.seekToNext();controller.seekToPrevious();ShadowLooper.idleMainLooper();
   assertEquals(Arrays.asList(1,-1),requests);
  }finally{if(controller!=null)controller.release();session.release();}
 }

 void installProgram(long start,long end){android.os.Bundle extras=new android.os.Bundle();extras.putLong(ProgramProgress.START_KEY,start);extras.putLong(ProgramProgress.END_KEY,end);engine.setMediaItem(new MediaItem.Builder().setUri("https://example.test/live.m3u8").setMediaMetadata(new MediaMetadata.Builder().setTitle("Program").setExtras(extras).build()).build());ShadowLooper.idleMainLooper();}
 @Test public void programProgressUsesWallClockWhilePaused(){installProgram(1000,3601000);assertEquals(3600000,sessionPlayer.getDuration());assertEquals(1800000,sessionPlayer.getCurrentPosition());assertFalse(engine.getPlayWhenReady());wallClock.addAndGet(30000);sessionPlayer.refreshProgress();assertEquals(1830000,sessionPlayer.getCurrentPosition());assertEquals(0,engine.getCurrentPosition());}
 @Test public void scrubbingCannotSeekStream(){installProgram(1000,3601000);for(int command:new int[]{Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,Player.COMMAND_SEEK_TO_MEDIA_ITEM,Player.COMMAND_SEEK_TO_DEFAULT_POSITION,Player.COMMAND_SEEK_BACK,Player.COMMAND_SEEK_FORWARD})assertFalse(sessionPlayer.isCommandAvailable(command));sessionPlayer.seekTo(2000);sessionPlayer.seekToDefaultPosition();sessionPlayer.seekBack();sessionPlayer.seekForward();assertEquals(0,engine.getCurrentPosition());assertEquals(1800000,sessionPlayer.getCurrentPosition());assertTrue(requests.isEmpty());}
 @Test public void missingOrExpiredProgramHasUnknownDuration(){installProgram(1000,3601000);wallClock.set(3601000);sessionPlayer.refreshProgress();assertEquals(C.TIME_UNSET,sessionPlayer.getDuration());assertEquals(0,sessionPlayer.getCurrentPosition());engine.setMediaItem(MediaItem.fromUri("https://example.test/other.m3u8"));ShadowLooper.idleMainLooper();assertEquals(C.TIME_UNSET,sessionPlayer.getDuration());}
 @Test public void progressDrawingDoesNotConsumeTouch(){ProgramProgressView view=new ProgramProgressView(RuntimeEnvironment.getApplication());view.layout(0,0,300,4);view.update(5000,true);assertFalse(view.isClickable());assertFalse(view.isFocusable());android.view.MotionEvent event=android.view.MotionEvent.obtain(0,0,android.view.MotionEvent.ACTION_DOWN,150,2,0);try{assertFalse(view.dispatchTouchEvent(event));}finally{event.recycle();}}

 @Test public void controllerReceivesProgramTimeAndCannotScrub() throws Exception {
  installProgram(1000,3601000);
  androidx.media3.session.MediaSession session=new androidx.media3.session.MediaSession.Builder(RuntimeEnvironment.getApplication(),sessionPlayer).build();
  androidx.media3.session.MediaController controller=null;
  try {
   com.google.common.util.concurrent.ListenableFuture<androidx.media3.session.MediaController> future=new androidx.media3.session.MediaController.Builder(RuntimeEnvironment.getApplication(),session.getToken()).buildAsync();
   for(int attempt=0;attempt<200&&!future.isDone();attempt++){ShadowLooper.idleMainLooper();Thread.sleep(5);}
   assertTrue(future.isDone());controller=future.get();assertEquals(3600000,controller.getDuration());assertEquals(1800000,controller.getCurrentPosition());assertFalse(controller.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM));
   wallClock.addAndGet(30000);sessionPlayer.refreshProgress();ShadowLooper.idleMainLooper();assertEquals(1830000,controller.getCurrentPosition());
   controller.seekTo(1000);ShadowLooper.idleMainLooper();assertEquals(0,engine.getCurrentPosition());assertEquals(1830000,sessionPlayer.getCurrentPosition());assertTrue(controller.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT));
  }finally{if(controller!=null)controller.release();session.release();}
 }

}
