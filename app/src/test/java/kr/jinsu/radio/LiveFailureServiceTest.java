package kr.jinsu.radio;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.shadows.ShadowToast;
import android.os.*;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.*;
import java.io.IOException;
import java.util.concurrent.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,manifest=Config.NONE) @ConscryptMode(ConscryptMode.Mode.OFF)
public class LiveFailureServiceTest {
 public static class OfflineService extends PlaybackService {
  boolean hold;final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
  @Override String resolveStation(RadioData.Station target)throws Exception{entered.countDown();if(hold)release.await();if(target.id.equals("yonhap"))throw new YoutubeLiveAudio.LiveUnavailableException(YoutubeLiveAudio.LiveState.ENDED);throw new IOException("offline");}
 }
 ServiceController<OfflineService> lifecycle;OfflineService service;
 @Before public void create(){ShadowToast.reset();lifecycle=Robolectric.buildService(OfflineService.class).create();service=lifecycle.get();}
 @After public void destroy(){service.release.countDown();lifecycle.destroy();Shadows.shadowOf(Looper.getMainLooper()).idle();}
 SessionResult await(com.google.common.util.concurrent.ListenableFuture<SessionResult> future)throws Exception{long end=System.currentTimeMillis()+3000;while(!future.isDone()&&System.currentTimeMillis()<end){Shadows.shadowOf(Looper.getMainLooper()).idle();Thread.sleep(5);}assertTrue(future.isDone());return future.get();}
 ExoPlayer engine()throws Exception{java.lang.reflect.Field field=PlaybackService.class.getDeclaredField("player");field.setAccessible(true);return (ExoPlayer)field.get(service);}
 @Test public void appRequestReceivesTypedTargetSnapshotWithoutPlayingArchive()throws Exception{RadioData.Station target=RadioData.find("yonhap");SessionResult result=await(service.requestStation(target,true));assertNotEquals(SessionResult.RESULT_SUCCESS,result.resultCode);assertEquals(YoutubeLiveAudio.LiveState.ENDED,PlaybackService.failureState(result.extras,target));assertEquals(0,engine().getMediaItemCount());assertFalse(engine().getPlayWhenReady());assertNull(ShadowToast.getTextOfLatestToast());}
 @Test public void systemPlayGetsClearNamedNoticeWhileOtherLiveRemainsUntouched()throws Exception{engine().setMediaItem(new MediaItem.Builder().setMediaId("ytnlive").setUri("https://example.test/live.m3u8").build());engine().play();await(service.requestStation(RadioData.find("yonhap")));assertEquals("ytnlive",engine().getCurrentMediaItem().mediaId);assertTrue(engine().getPlayWhenReady());assertTrue(ShadowToast.getTextOfLatestToast().contains("연합뉴스TV"));assertTrue(ShadowToast.getTextOfLatestToast().contains("라이브가 종료"));}
 @Test public void endedCurrentItemClearsPlayIntent()throws Exception{engine().setMediaItem(new MediaItem.Builder().setMediaId("yonhap").setUri("https://example.test/live.m3u8").build());engine().play();await(service.requestStation(RadioData.find("yonhap"),true));assertFalse(engine().getPlayWhenReady());assertEquals(Player.STATE_IDLE,engine().getPlaybackState());}
 @Test public void networkFailureAndWrongSnapshotAreNotEnded()throws Exception{RadioData.Station target=RadioData.find("ytnlive");SessionResult result=await(service.requestStation(target,true));assertEquals(YoutubeLiveAudio.LiveState.UNKNOWN,PlaybackService.failureState(result.extras,target));Bundle extras=PlaybackService.failureExtras(RadioData.find("yonhap"),new YoutubeLiveAudio.LiveUnavailableException(YoutubeLiveAudio.LiveState.ENDED));assertEquals(YoutubeLiveAudio.LiveState.UNKNOWN,PlaybackService.failureState(extras,target));extras.putString(PlaybackService.LIVE_FAILURE_VIDEO,"abcdefghijk");assertEquals(YoutubeLiveAudio.LiveState.UNKNOWN,PlaybackService.failureState(extras,RadioData.find("yonhap")));}
 @Test public void laterRequestCancelsLateEndedNotice()throws Exception{service.hold=true;com.google.common.util.concurrent.ListenableFuture<SessionResult> old=service.requestStation(RadioData.find("yonhap"),true);assertTrue(service.entered.await(2,TimeUnit.SECONDS));com.google.common.util.concurrent.ListenableFuture<SessionResult> newer=service.requestStation(RadioData.find("ytnlive"),true);service.release.countDown();assertTrue(await(old).extras.getBoolean("cancelled"));assertEquals(YoutubeLiveAudio.LiveState.UNKNOWN,PlaybackService.failureState(await(newer).extras,RadioData.find("ytnlive")));assertNotNull(ShadowToast.getTextOfLatestToast());assertFalse(ShadowToast.getTextOfLatestToast().contains("라이브가 종료"));}

 @Test public void temporaryNetworkFailureKeepsExistingPlaybackAndVisibleNotice()throws Exception{engine().setMediaItem(new MediaItem.Builder().setMediaId("ytnlive").setUri("https://example.test/live.m3u8").build());engine().play();SessionResult result=await(service.requestStation(RadioData.find("ytnlive"),true));assertEquals(YoutubeLiveAudio.LiveState.UNKNOWN,PlaybackService.failureState(result.extras,RadioData.find("ytnlive")));assertTrue(engine().getPlayWhenReady());assertTrue(ShadowToast.getTextOfLatestToast().contains("YTN"));assertTrue(ShadowToast.getTextOfLatestToast().contains("연결하지 못했습니다"));assertFalse(ShadowToast.getTextOfLatestToast().contains("종료"));}
}
