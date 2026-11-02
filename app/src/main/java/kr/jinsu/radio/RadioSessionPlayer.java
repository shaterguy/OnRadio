package kr.jinsu.radio;

import androidx.media3.common.*;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.function.IntConsumer;

/** Keeps system command availability consistent with channel navigation, even with one stream. */
@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
final class RadioSessionPlayer extends ForwardingSimpleBasePlayer {
 private final IntConsumer navigate;
 RadioSessionPlayer(Player player,IntConsumer navigate){super(player);this.navigate=navigate;}
 @Override protected State getState(){
  State state=super.getState();
  return state.buildUpon().setAvailableCommands(state.availableCommands.buildUpon()
   .add(Player.COMMAND_SEEK_TO_NEXT).add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
   .add(Player.COMMAND_SEEK_TO_PREVIOUS).add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
   .remove(Player.COMMAND_SEEK_BACK).remove(Player.COMMAND_SEEK_FORWARD).build()).build();
 }
 @Override protected ListenableFuture<?> handleSeek(int index,long positionMs,int command){
  if(command==Player.COMMAND_SEEK_TO_NEXT||command==Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM){navigate.accept(1);return Futures.immediateVoidFuture();}
  if(command==Player.COMMAND_SEEK_TO_PREVIOUS||command==Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM){navigate.accept(-1);return Futures.immediateVoidFuture();}
  return super.handleSeek(index,positionMs,command);
 }
}
