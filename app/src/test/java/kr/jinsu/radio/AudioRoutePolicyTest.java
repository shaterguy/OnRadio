package kr.jinsu.radio;
import org.junit.Test;
import static org.junit.Assert.*;
public class AudioRoutePolicyTest {
 @Test public void lastBluetoothOutputDisconnectStopsPlayback(){assertTrue(AudioRoutePolicy.shouldStop(true,false));}
 @Test public void profileSwitchWithAnotherOutputDoesNotStop(){assertFalse(AudioRoutePolicy.shouldStop(true,true));}
 @Test public void RemovingMicrophoneOrSpeakerDoesNotStop(){assertFalse(AudioRoutePolicy.shouldStop(false,false));assertFalse(AudioRoutePolicy.bluetoothOutput(2));assertFalse(AudioRoutePolicy.bluetoothOutput(15));}
 @Test public void classicBluetoothAndLeAudioAreHandled(){assertTrue(AudioRoutePolicy.bluetoothOutput(8));assertTrue(AudioRoutePolicy.bluetoothOutput(7));assertTrue(AudioRoutePolicy.bluetoothOutput(26));assertTrue(AudioRoutePolicy.bluetoothOutput(27));assertTrue(AudioRoutePolicy.bluetoothOutput(23));}
 @Test public void playQueuedBeforeDisconnectIsRejected(){assertTrue(AudioRoutePolicy.requestIsCurrent(0,0));assertFalse(AudioRoutePolicy.requestIsCurrent(0,1));assertTrue(AudioRoutePolicy.requestIsCurrent(1,1));}
}
