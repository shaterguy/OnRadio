package kr.jinsu.radio;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.*;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import java.util.concurrent.atomic.AtomicLong;
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
 static final String APP_CONTROLLER_KEY="onradio.app_controller";
 private int externalResumeTicket;
 static final String OUTPUT_GENERATION_KEY="onradio.output_generation";
 static final AtomicLong outputLossGeneration=new AtomicLong();
 private ExoPlayer player;private MediaSession session;private AudioManager audioManager;
 private final AudioDeviceCallback outputCallback=new AudioDeviceCallback(){@Override public void onAudioDevicesRemoved(AudioDeviceInfo[] removed){if(destroyed)return;boolean bluetoothRemoved=false;for(AudioDeviceInfo d:removed)if(d.isSink()&&AudioRoutePolicy.bluetoothOutput(d.getType()))bluetoothRemoved=true;boolean stillConnected=false;if(audioManager!=null)for(AudioDeviceInfo d:audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS))if(d.isSink()&&AudioRoutePolicy.bluetoothOutput(d.getType()))stillConnected=true;if(AudioRoutePolicy.shouldStop(bluetoothRemoved,stillConnected)){outputLossGeneration.incrementAndGet();if(player!=null){player.pause();player.stop();}}}};

 private final Handler handler=new Handler(Looper.getMainLooper());private final ExecutorService io=Executors.newSingleThreadExecutor();private boolean destroyed=false,fetching=false;private String cachedId="";private long fetched=0;private List<RadioData.Program> programs=Collections.emptyList();
 private final Runnable update=new Runnable(){public void run(){if(destroyed)return;refreshInfo();handler.postDelayed(this,30000);}};
 private void refreshInfo(){if(player==null||!player.getPlayWhenReady()||player.getCurrentMediaItem()==null)return;MediaItem item=player.getCurrentMediaItem();String id=item.mediaId;
  if((!id.equals(cachedId)||System.currentTimeMillis()-fetched>300000)&&!fetching){fetching=true;io.execute(()->{List<RadioData.Program> result;try{result=RadioData.schedule(RadioData.find(id),LocalDate.now(RadioData.SEOUL));}catch(Exception e){result=Collections.emptyList();}List<RadioData.Program> data=result;handler.post(()->{fetching=false;if(destroyed)return;if(player.getCurrentMediaItem()!=null&&player.getCurrentMediaItem().mediaId.equals(id)){cachedId=id;fetched=System.currentTimeMillis();programs=data;applyInfo();}});});}else if(id.equals(cachedId))applyInfo();
 }
 private void applyInfo(){MediaItem item=player.getCurrentMediaItem();if(item==null)return;RadioData.Station s=RadioData.find(item.mediaId);RadioData.Program p=RadioData.current(programs);String title=p==null?s.name:p.title;if(title.contentEquals(item.mediaMetadata.title==null?"":item.mediaMetadata.title))return;MediaMetadata meta=new MediaMetadata.Builder().setTitle(title).setArtist(s.name+" · FM "+s.fm).setAlbumTitle("온라디오").setExtras(item.mediaMetadata.extras).build();player.replaceMediaItem(player.getCurrentMediaItemIndex(),item.buildUpon().setMediaMetadata(meta).build());}

 @Override public void onCreate(){super.onCreate();
  DefaultHttpDataSource.Factory http=new DefaultHttpDataSource.Factory().setUserAgent("Mozilla/5.0 OnRadio/0.1").setConnectTimeoutMs(15000).setReadTimeoutMs(20000).setAllowCrossProtocolRedirects(false);
  player=new ExoPlayer.Builder(this).setMediaSourceFactory(new DefaultMediaSourceFactory(this).setDataSourceFactory(http)).build();
  player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),true);player.setHandleAudioBecomingNoisy(true);player.setWakeMode(C.WAKE_MODE_NETWORK);
  audioManager=(AudioManager)getSystemService(AUDIO_SERVICE);if(audioManager!=null)audioManager.registerAudioDeviceCallback(outputCallback,handler);
  PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("player",true),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  Player guardedPlayer=new ForwardingPlayer(player){private boolean allowed(){MediaItem item=player.getCurrentMediaItem();Bundle extras=item==null?null:item.mediaMetadata.extras;return extras!=null&&AudioRoutePolicy.requestIsCurrent(extras.getLong(OUTPUT_GENERATION_KEY,-1),outputLossGeneration.get());}private void requestPlay(){if(allowed()){if(player.getPlaybackState()==Player.STATE_IDLE)player.prepare();super.play();}else if(!resumeExternalRequest()){player.pause();player.stop();}}@Override public void play(){requestPlay();}@Override public void setPlayWhenReady(boolean ready){if(ready)requestPlay();else {externalResumeTicket++;super.setPlayWhenReady(false);}}@Override public void pause(){externalResumeTicket++;super.pause();}@Override public void stop(){externalResumeTicket++;super.stop();}};
  session=new MediaSession.Builder(this,guardedPlayer).setSessionActivity(open).build();
  player.addListener(new Player.Listener(){@Override public void onMediaItemTransition(MediaItem item,int reason){refreshInfo();}@Override public void onIsPlayingChanged(boolean playing){if(playing)refreshInfo();}});handler.post(update);
 }
 private boolean resumeExternalRequest(){MediaSession.ControllerInfo caller=session==null?null:session.getControllerForCurrentRequest();MediaItem old=player.getCurrentMediaItem();if(caller==null||caller.getConnectionHints().getBoolean(APP_CONTROLLER_KEY,false)||old==null)return false;int ticket=++externalResumeTicket;long outputGeneration=outputLossGeneration.get();long oldGeneration=old.mediaMetadata.extras==null?-1:old.mediaMetadata.extras.getLong(OUTPUT_GENERATION_KEY,-1);player.pause();player.stop();io.execute(()->{try{String url=RadioData.resolve(RadioData.find(old.mediaId));handler.post(()->{if(destroyed||ticket!=externalResumeTicket||outputGeneration!=outputLossGeneration.get())return;MediaItem current=player.getCurrentMediaItem();if(current==null||!current.mediaId.equals(old.mediaId))return;long currentGeneration=current.mediaMetadata.extras==null?-1:current.mediaMetadata.extras.getLong(OUTPUT_GENERATION_KEY,-1);if(currentGeneration!=oldGeneration)return;Bundle extras=current.mediaMetadata.extras==null?new Bundle():new Bundle(current.mediaMetadata.extras);extras.putLong(OUTPUT_GENERATION_KEY,outputGeneration);MediaMetadata meta=current.mediaMetadata.buildUpon().setExtras(extras).build();player.setMediaItem(current.buildUpon().setUri(url).setMediaMetadata(meta).build());player.prepare();player.play();});}catch(Exception ignored){/* Remain stopped; a later explicit play can retry. */}});return true;}
 @Override public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo){return controllerInfo.isTrusted()||getPackageName().equals(controllerInfo.getPackageName())?session:null;}
 @Override public void onTaskRemoved(Intent rootIntent){if(!player.getPlayWhenReady()||player.getPlaybackState()==Player.STATE_IDLE||player.getPlaybackState()==Player.STATE_ENDED)stopSelf();}
 @Override public void onDestroy(){destroyed=true;if(audioManager!=null)audioManager.unregisterAudioDeviceCallback(outputCallback);handler.removeCallbacksAndMessages(null);io.shutdownNow();if(session!=null)session.release();if(player!=null)player.release();super.onDestroy();}
}
