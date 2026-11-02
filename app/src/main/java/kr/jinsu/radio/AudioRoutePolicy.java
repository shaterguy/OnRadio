package kr.jinsu.radio;

import android.media.AudioDeviceInfo;

final class AudioRoutePolicy {
 static boolean bluetoothOutput(int type){return type==AudioDeviceInfo.TYPE_BLUETOOTH_A2DP||type==AudioDeviceInfo.TYPE_BLUETOOTH_SCO||type==AudioDeviceInfo.TYPE_BLE_HEADSET||type==AudioDeviceInfo.TYPE_BLE_SPEAKER||type==AudioDeviceInfo.TYPE_HEARING_AID;}
 static boolean requestIsCurrent(long requestGeneration,long currentGeneration){return requestGeneration==currentGeneration;}
 static boolean shouldStop(boolean bluetoothRemoved,boolean bluetoothStillConnected){return bluetoothRemoved&&!bluetoothStillConnected;}
}
