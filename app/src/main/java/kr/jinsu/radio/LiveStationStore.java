package kr.jinsu.radio;

import android.content.SharedPreferences;
import org.json.*;
import java.io.IOException;
import java.util.*;

/** Persists public video/channel identifiers only; never signed stream URLs or visitor data. */
final class LiveStationStore {
 static final String KEY="youtube_live_channels_v1";
 private LiveStationStore(){}
 static boolean validName(String value){return value!=null&&!value.trim().isEmpty()&&value.trim().length()<=80&&!value.matches("(?s).*[\\p{Cntrl}].*");}
 static boolean builtinLive(String id){for(RadioData.Station station:RadioData.STATIONS)if(station.id.equals(id))return !station.hasPrograms();return false;}
 static List<RadioData.Station> decode(String json)throws Exception{
  if(json.length()>100_000)throw new IOException("저장된 라이브 목록 크기 오류");
  JSONArray array=new JSONArray(json);if(array.length()>100)throw new IOException("등록 가능한 라이브 수를 초과했습니다");
  List<RadioData.Station> rows=new ArrayList<>();Set<String> ids=new HashSet<>(),videos=new HashSet<>();
  for(int i=0;i<array.length();i++){
   JSONObject item=array.getJSONObject(i);String id=item.getString("id"),name=item.getString("name"),video=item.getString("videoId"),channel=item.getString("channelId");
   if(!(builtinLive(id)||id.matches("user_[0-9a-f-]{36}"))||!validName(name)||!video.matches("[A-Za-z0-9_-]{11}")||!channel.matches("UC[A-Za-z0-9_-]{22}")||!ids.add(id)||!videos.add(video))throw new IOException("저장된 라이브 항목 형식 오류");
   rows.add(RadioData.youtube(id,name.trim(),video,channel));
  }return rows;
 }
 static String encode(List<RadioData.Station> rows)throws Exception{JSONArray array=new JSONArray();for(RadioData.Station row:rows)array.put(new JSONObject().put("id",row.id).put("name",row.name).put("videoId",row.code).put("channelId",row.channelId));return array.toString();}
 static void reload(SharedPreferences prefs){try{RadioData.setLiveOverrides(decode(prefs.getString(KEY,"[]")));}catch(Exception invalid){RadioData.setLiveOverrides(Collections.emptyList());}}
 static synchronized RadioData.Station save(SharedPreferences prefs,String editingId,String name,YoutubeLiveAudio.LiveInfo info)throws Exception{
  if(!validName(name))throw new IOException("채널명을 1∼80자로 입력해 주세요");
  if(info==null||!info.videoId.matches("[A-Za-z0-9_-]{11}")||!info.channelId.matches("UC[A-Za-z0-9_-]{22}"))throw new IOException("라이브 확인 결과가 올바르지 않습니다");
  List<RadioData.Station> rows;try{rows=decode(prefs.getString(KEY,"[]"));}catch(Exception invalid){throw new IOException("저장된 라이브 목록을 읽지 못했습니다. 기존 목록은 유지됩니다");}
  boolean adding=editingId==null;String id=adding?"user_"+UUID.randomUUID():editingId;boolean exists=builtinLive(id);
  for(RadioData.Station row:rows)if(row.id.equals(id))exists=true;
  if(!adding&&!exists)throw new IOException("수정할 라이브 항목이 없습니다");
  Set<String> overridden=new HashSet<>();for(RadioData.Station row:rows)overridden.add(row.id);
  for(RadioData.Station row:rows)if(!row.id.equals(id)&&row.code.equals(info.videoId))throw new IOException("이미 등록된 라이브 주소입니다");
  for(RadioData.Station row:RadioData.STATIONS)if(!row.hasPrograms()&&!overridden.contains(row.id)&&!row.id.equals(id)&&row.code.equals(info.videoId))throw new IOException("이미 등록된 라이브 주소입니다");
  RadioData.Station saved=RadioData.youtube(id,name.trim(),info.videoId,info.channelId);boolean replaced=false;
  for(int i=0;i<rows.size();i++)if(rows.get(i).id.equals(id)){rows.set(i,saved);replaced=true;break;}
  if(!replaced){if(rows.size()>=100)throw new IOException("라이브는 최대 100개까지 등록할 수 있습니다");rows.add(saved);}
  if(!prefs.edit().putString(KEY,encode(rows)).commit())throw new IOException("라이브를 저장하지 못했습니다. 다시 시도해 주세요");
  RadioData.setLiveOverrides(rows);return saved;
 }
 static synchronized void remove(SharedPreferences prefs,String id)throws Exception{
  if(builtinLive(id))throw new IOException("기본 라이브는 주소 수정만 지원합니다");
  List<RadioData.Station> rows=decode(prefs.getString(KEY,"[]"));rows.removeIf(row->row.id.equals(id));Set<String> favorites=new HashSet<>(prefs.getStringSet("favorites",Collections.emptySet()));favorites.remove(id);
  if(!prefs.edit().putString(KEY,encode(rows)).putStringSet("favorites",favorites).commit())throw new IOException("라이브를 삭제하지 못했습니다");
  RadioData.setLiveOverrides(rows);
 }
}
