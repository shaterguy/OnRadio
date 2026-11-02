package kr.jinsu.radio;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import androidx.media3.common.*;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.session.*;

@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
public class PlaybackService extends MediaSessionService {
 private ExoPlayer player;private MediaSession session;
 private final Handler handler=new Handler(Looper.getMainLooper());private final ExecutorService io=Executors.newSingleThreadExecutor();private boolean destroyed=false,fetching=false;private String cachedId="";private long fetched=0;private List<RadioData.Program> programs=Collections.emptyList();
 private final Runnable update=new Runnable(){public void run(){if(destroyed)return;refreshInfo();handler.postDelayed(this,30000);}};
 private void refreshInfo(){if(player==null||!player.getPlayWhenReady()||player.getCurrentMediaItem()==null)return;MediaItem item=player.getCurrentMediaItem();String id=item.mediaId;
  if((!id.equals(cachedId)||System.currentTimeMillis()-fetched>300000)&&!fetching){fetching=true;io.execute(()->{List<RadioData.Program> result;try{result=RadioData.schedule(RadioData.find(id),LocalDate.now(RadioData.SEOUL));}catch(Exception e){result=Collections.emptyList();}List<RadioData.Program> data=result;handler.post(()->{fetching=false;if(destroyed)return;if(player.getCurrentMediaItem()!=null&&player.getCurrentMediaItem().mediaId.equals(id)){cachedId=id;fetched=System.currentTimeMillis();programs=data;applyInfo();}});});}else if(id.equals(cachedId))applyInfo();
 }
 private void applyInfo(){MediaItem item=player.getCurrentMediaItem();if(item==null)return;RadioData.Station s=RadioData.find(item.mediaId);RadioData.Program p=RadioData.current(programs);String title=p==null?s.name:p.title;if(title.contentEquals(item.mediaMetadata.title==null?"":item.mediaMetadata.title))return;MediaMetadata meta=new MediaMetadata.Builder().setTitle(title).setArtist(s.name+" · FM "+s.fm).setAlbumTitle("온라디오").build();player.replaceMediaItem(player.getCurrentMediaItemIndex(),item.buildUpon().setMediaMetadata(meta).build());}

 @Override public void onCreate(){super.onCreate();
  DefaultHttpDataSource.Factory http=new DefaultHttpDataSource.Factory().setUserAgent("Mozilla/5.0 OnRadio/0.1").setConnectTimeoutMs(15000).setReadTimeoutMs(20000).setAllowCrossProtocolRedirects(false);
  player=new ExoPlayer.Builder(this).setMediaSourceFactory(new DefaultMediaSourceFactory(this).setDataSourceFactory(http)).build();
  player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),true);player.setHandleAudioBecomingNoisy(true);player.setWakeMode(C.WAKE_MODE_NETWORK);
  PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("player",true),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  session=new MediaSession.Builder(this,player).setSessionActivity(open).build();
  player.addListener(new Player.Listener(){@Override public void onMediaItemTransition(MediaItem item,int reason){refreshInfo();}@Override public void onIsPlayingChanged(boolean playing){if(playing)refreshInfo();}});handler.post(update);
 }
 @Override public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo){return controllerInfo.isTrusted()||getPackageName().equals(controllerInfo.getPackageName())?session:null;}
 @Override public void onTaskRemoved(Intent rootIntent){if(!player.getPlayWhenReady()||player.getPlaybackState()==Player.STATE_IDLE||player.getPlaybackState()==Player.STATE_ENDED)stopSelf();}
 @Override public void onDestroy(){destroyed=true;handler.removeCallbacksAndMessages(null);io.shutdownNow();if(session!=null)session.release();if(player!=null)player.release();super.onDestroy();}
}
