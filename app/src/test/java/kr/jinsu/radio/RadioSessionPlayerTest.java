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
 ExoPlayer engine;RadioSessionPlayer sessionPlayer;List<Integer> requests;
 @Before public void create(){engine=new ExoPlayer.Builder(RuntimeEnvironment.getApplication()).build();requests=new ArrayList<>();sessionPlayer=new RadioSessionPlayer(engine,requests::add);}
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

}
