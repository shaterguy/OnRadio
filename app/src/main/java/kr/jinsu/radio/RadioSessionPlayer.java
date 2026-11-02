package kr.jinsu.radio;

import androidx.media3.common.*;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.function.IntConsumer;

/** Keeps system command availability consistent with channel navigation, even with one stream. */
@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
final class RadioSessionPlayer extends ForwardingSimpleBasePlayer {
 private final Player engine;private final IntConsumer navigate;private final java.util.function.LongSupplier clock;private boolean progressRefresh;
 RadioSessionPlayer(Player player,IntConsumer navigate){this(player,navigate,System::currentTimeMillis);}
 RadioSessionPlayer(Player player,IntConsumer navigate,java.util.function.LongSupplier clock){super(player);this.engine=player;this.navigate=navigate;this.clock=clock;}
 void refreshProgress(){progressRefresh=true;try{invalidateState();}finally{progressRefresh=false;}}
 @Override protected State getState(){
  State state=super.getState();
  State.Builder result=state.buildUpon().setAvailableCommands(state.availableCommands.buildUpon()
   .add(Player.COMMAND_SEEK_TO_NEXT).add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
   .add(Player.COMMAND_SEEK_TO_PREVIOUS).add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
   .remove(Player.COMMAND_SEEK_BACK).remove(Player.COMMAND_SEEK_FORWARD)
   .remove(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM).remove(Player.COMMAND_SEEK_TO_MEDIA_ITEM).remove(Player.COMMAND_SEEK_TO_DEFAULT_POSITION).build());
  MediaItem item=engine.getCurrentMediaItem();
  if(item!=null&&!state.timeline.isEmpty()){
   android.os.Bundle extras=item.mediaMetadata.extras;long start=extras==null?0:extras.getLong(ProgramProgress.START_KEY,0),end=extras==null?0:extras.getLong(ProgramProgress.END_KEY,0);
   boolean known=ProgramProgress.current(start,end,clock.getAsLong());long duration=known?ProgramProgress.duration(start,end):C.TIME_UNSET;
   Timeline.Window window=state.timeline.getWindow(0,new Timeline.Window());
   MediaItemData presentation=new MediaItemData.Builder(window.uid).setMediaItem(item).setMediaMetadata(item.mediaMetadata).setTracks(engine.getCurrentTracks()).setDurationUs(known?duration*1000:C.TIME_UNSET).setDefaultPositionUs(0).setIsSeekable(false).build();
   PositionSupplier position=()->known?ProgramProgress.elapsed(start,end,clock.getAsLong()):0;
   result.setPlaylist(java.util.Collections.singletonList(presentation)).setCurrentMediaItemIndex(0).setCurrentAd(C.INDEX_UNSET,C.INDEX_UNSET).setContentPositionMs(position).setContentBufferedPositionMs(position);
   if(progressRefresh&&known)result.setPositionDiscontinuity(Player.DISCONTINUITY_REASON_INTERNAL,position.get());else result.clearPositionDiscontinuity();
  }
  return result.build();
 }
 @Override protected ListenableFuture<?> handleSeek(int index,long positionMs,int command){
  if(command==Player.COMMAND_SEEK_TO_NEXT||command==Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM){navigate.accept(1);return Futures.immediateVoidFuture();}
  if(command==Player.COMMAND_SEEK_TO_PREVIOUS||command==Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM){navigate.accept(-1);return Futures.immediateVoidFuture();}
  return Futures.immediateVoidFuture(); // This is a live station: time seeking is never forwarded.
 }
}
