package kr.jinsu.radio;

import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

/** Resolves a validated public YouTube live broadcast to an audio-only HLS rendition. */
final class YoutubeLiveAudio {
 static final String CHANNEL_ID="UCTHCOPwqNfZ0uiKOvFyhGwg";
 static final String WEB_AGENT="Mozilla/5.0 OnRadio/0.1";
 static final String CLIENT_AGENT="Mozilla/5.0 (Macintosh; Intel Mac OS X 15_7_3) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.0 Safari/605.1.15";
 private static final Pattern PLAYER=Pattern.compile("(?:var\\s+)?ytInitialPlayerResponse\\s*=\\s*");
 private static final Pattern VISITOR=Pattern.compile("\"VISITOR_DATA\"\\s*:\\s*\"([^\"]+)\"");
 private static final Pattern ATTRIBUTE=Pattern.compile("(?:^|,)([A-Z0-9-]+)=(\"[^\"]*\"|[^,]*)");
 private YoutubeLiveAudio(){}
 static final class LiveInfo {final String videoId,channelId,audioUrl;LiveInfo(String videoId,String channelId,String audioUrl){this.videoId=videoId;this.channelId=channelId;this.audioUrl=audioUrl;}}
 static String canonicalPage(String value)throws IOException{
  try{
   String input=value.trim();if(input.length()>2048)throw new IOException("라이브 주소가 너무 깁니다");
   if(input.startsWith("youtu.be/")||input.startsWith("youtube.com/")||input.startsWith("www.youtube.com/"))input="https://"+input;
   URI uri=new URI(input);String host=uri.getHost()==null?"":uri.getHost().toLowerCase(Locale.ROOT);
   if(!"https".equalsIgnoreCase(uri.getScheme())||uri.getUserInfo()!=null||uri.getPort()!=-1&&uri.getPort()!=443||!Arrays.asList("youtube.com","www.youtube.com","m.youtube.com","youtu.be","www.youtu.be").contains(host))throw new IOException("유튜브 라이브의 HTTPS 주소를 입력해 주세요");
   String path=uri.getPath(),id="";
   if(host.endsWith("youtu.be"))id=path.replaceFirst("^/","").replaceFirst("/$","");
   else if("/watch".equals(path)){String query=uri.getRawQuery();if(query!=null)for(String part:query.split("&")){String[] pair=part.split("=",2);if("v".equals(pair[0])&&pair.length==2){id=URLDecoder.decode(pair[1],"UTF-8");break;}}}
   else if(path.matches("/(?:live|shorts)/[A-Za-z0-9_-]{11}/?"))id=path.split("/")[2];
   else if(path.matches("/channel/UC[A-Za-z0-9_-]{22}/live/?")||path.matches("/@[^/\\s]{1,100}/live/?"))return new URI("https","www.youtube.com",path.replaceFirst("/$",""),null).toASCIIString();
   if(!id.matches("[A-Za-z0-9_-]{11}"))throw new IOException("라이브 영상의 공유 주소를 입력해 주세요");
   return "https://www.youtube.com/watch?v="+id;
  }catch(URISyntaxException|IllegalArgumentException error){throw new IOException("라이브 주소 형식을 확인해 주세요");}
 }
 enum LiveState {LIVE,ENDED,UPCOMING,NOT_LIVE,UNKNOWN}
 static final class LiveUnavailableException extends IOException {
  final LiveState state;LiveUnavailableException(LiveState state){super(notice(state));this.state=state;}
 }
 interface Fetcher {String request(String url,String body,String visitorId)throws Exception;}
 static String notice(LiveState state){
  switch(state){
   case ENDED:return "등록한 라이브가 종료되었습니다. 새 라이브 주소를 확인해 주세요.";
   case UPCOMING:return "아직 시작하지 않은 라이브입니다. 방송 시작 후 다시 재생해 주세요.";
   case NOT_LIVE:return "이 주소는 현재 라이브 방송이 아닙니다. 라이브 주소를 확인해 주세요.";
   default:return "라이브 상태를 확인하지 못했습니다. 잠시 후 다시 시도해 주세요.";
  }
 }
 static LiveInfo inspect(String value)throws Exception{
  String page=canonicalPage(value);return resolvePage(page,"",page.startsWith("https://www.youtube.com/watch?v=")?page.substring(page.indexOf('=')+1):"",YoutubeLiveAudio::request);
 }
 static String resolve(String videoId)throws Exception{return resolve(videoId,CHANNEL_ID);}
 static String resolve(String videoId,String channelId)throws Exception{return resolve(videoId,channelId,YoutubeLiveAudio::request);}
 static String resolve(String videoId,String channelId,Fetcher fetcher)throws Exception{
  if(!videoId.matches("[A-Za-z0-9_-]{11}")||!channelId.matches("UC[A-Za-z0-9_-]{22}"))throw new IOException("라이브 주소 형식 오류");
  return resolvePage("https://www.youtube.com/watch?v="+videoId,channelId,videoId,fetcher).audioUrl;
 }
 static void validateIdentity(JSONObject response,String expectedChannel,String expectedVideo)throws IOException{
  JSONObject details=response.optJSONObject("videoDetails");
  if(details==null)throw new IOException("공개 라이브를 확인하지 못했습니다");
  String id=details.optString("videoId"),channel=details.optString("channelId");
  if(!id.matches("[A-Za-z0-9_-]{11}")||!channel.matches("UC[A-Za-z0-9_-]{22}")||!expectedVideo.isEmpty()&&!expectedVideo.equals(id)||!expectedChannel.isEmpty()&&!expectedChannel.equals(channel))throw new IOException("등록한 라이브의 재생 정보가 일치하지 않습니다");
 }
 static LiveState liveState(JSONObject response){
  JSONObject status=response.optJSONObject("playabilityStatus"),details=response.optJSONObject("videoDetails");
  if(status==null||details==null)return LiveState.UNKNOWN;
  String code=status.optString("status");
  JSONObject format=response.optJSONObject("microformat"),renderer=format==null?null:format.optJSONObject("playerMicroformatRenderer"),broadcast=renderer==null?null:renderer.optJSONObject("liveBroadcastDetails");
  boolean live=details.optBoolean("isLive")||broadcast!=null&&broadcast.optBoolean("isLiveNow");
  if("OK".equals(code)&&live)return LiveState.LIVE;
  if(details.optBoolean("isUpcoming")&&("OK".equals(code)||"LIVE_STREAM_OFFLINE".equals(code)))return LiveState.UPCOMING;
  if(!"OK".equals(code))return LiveState.UNKNOWN;
  if(broadcast!=null){try{long end=java.time.OffsetDateTime.parse(broadcast.optString("endTimestamp")).toInstant().toEpochMilli();if(end>0&&end<=System.currentTimeMillis())return LiveState.ENDED;}catch(java.time.DateTimeException ignored){}}
  if(details.has("isLiveContent")&&!details.optBoolean("isLiveContent"))return LiveState.NOT_LIVE;
  return LiveState.UNKNOWN;
 }
 private static LiveInfo resolvePage(String pageUrl,String expectedChannel,String expectedVideo,Fetcher fetcher)throws Exception{
  String page=fetcher.request(pageUrl,null,null);JSONObject initial=playerFromPage(page);validateIdentity(initial,expectedChannel,expectedVideo);JSONObject details=initial.getJSONObject("videoDetails");
  String channelId=details.getString("channelId"),id=details.getString("videoId");LiveState initialState=liveState(initial);
  if(initialState==LiveState.ENDED||initialState==LiveState.UPCOMING||initialState==LiveState.NOT_LIVE)throw new LiveUnavailableException(initialState);
  Matcher visitor=VISITOR.matcher(page);String visitorId=visitor.find()?new JSONTokener("\""+visitor.group(1)+"\"").nextValue().toString():"";
  if(visitorId.isEmpty())throw new IOException("라이브 접속 정보를 가져오지 못했습니다");
  JSONObject client=new JSONObject().put("clientName","VISIONOS").put("clientVersion","1.02").put("deviceMake","Apple").put("deviceModel","RealityDevice17,1").put("userAgent",CLIENT_AGENT).put("osName","visionOS").put("osVersion","26.5.23O471").put("hl","en").put("timeZone","UTC").put("utcOffsetMinutes",0);
  JSONObject playback=new JSONObject().put("html5Preference","HTML5_PREF_WANTS");
  Matcher timestamp=Pattern.compile("\"(?:STS|signatureTimestamp)\"\\s*:\\s*(\\d+)").matcher(page);if(timestamp.find())playback.put("signatureTimestamp",Long.parseLong(timestamp.group(1)));
  JSONObject body=new JSONObject().put("context",new JSONObject().put("client",client)).put("videoId",id).put("playbackContext",new JSONObject().put("contentPlaybackContext",playback)).put("contentCheckOk",true).put("racyCheckOk",true);
  JSONObject response=new JSONObject(fetcher.request("https://www.youtube.com/youtubei/v1/player?prettyPrint=false",body.toString(),visitorId));
  validateIdentity(response,channelId,id);validateLivePlayer(response,channelId);
  JSONObject streams=response.optJSONObject("streamingData");String master=streams==null?"":streams.optString("hlsManifestUrl");validateMediaUrl(master);
  return new LiveInfo(id,channelId,audioRendition(fetcher.request(master,null,null),master));
 }
 static JSONObject playerFromPage(String html)throws Exception{
  Matcher match=PLAYER.matcher(html);if(!match.find())throw new IOException("라이브 재생 정보를 찾지 못했습니다");
  Object value=new JSONTokener(html.substring(match.end())).nextValue();if(!(value instanceof JSONObject))throw new IOException("라이브 재생 정보 형식 오류");return (JSONObject)value;
 }
 static void validateLivePlayer(JSONObject response)throws Exception{validateLivePlayer(response,CHANNEL_ID);}
 static void validateLivePlayer(JSONObject response,String channelId)throws Exception{
  JSONObject details=response.optJSONObject("videoDetails");
  if(details==null||!channelId.equals(details.optString("channelId")))throw new IOException("등록한 유튜브 채널의 라이브가 아닙니다");
  LiveState state=liveState(response);if(state!=LiveState.LIVE)throw new LiveUnavailableException(state);
 }
 static Map<String,String> attributes(String line){
  Map<String,String> result=new HashMap<>();Matcher match=ATTRIBUTE.matcher(line.substring(line.indexOf(':')+1));
  while(match.find()){String value=match.group(2);if(value.startsWith("\""))value=value.substring(1,value.length()-1);result.put(match.group(1),value);}return result;
 }
 static String audioRendition(String manifest,String base)throws Exception{
  if(!manifest.trim().startsWith("#EXTM3U"))throw new IOException("라이브 재생목록 형식 오류");
  String best="",fallback="";long highest=-1;String[] lines=manifest.split("\\r?\\n");
  for(int i=0;i<lines.length;i++){
   String line=lines[i].trim();
   if(line.startsWith("#EXT-X-MEDIA:")){
    Map<String,String> a=attributes(line);if("AUDIO".equals(a.get("TYPE"))&&a.containsKey("URI")){
     String url=new URI(base).resolve(a.get("URI")).toString();validateMediaUrl(url);
     if(fallback.isEmpty()||"234".equals(a.get("GROUP-ID")))fallback=url;
    }
   }else if(line.startsWith("#EXT-X-STREAM-INF:")){
    Map<String,String> a=attributes(line);String codecs=a.getOrDefault("CODECS","").toLowerCase(Locale.ROOT);
    boolean audioOnly=!a.containsKey("RESOLUTION")&&!a.containsKey("VIDEO")&&codecs.matches("(?:mp4a\\.[a-z0-9.]+|opus)(?:\\s*,\\s*(?:mp4a\\.[a-z0-9.]+|opus))*");
    if(!audioOnly)continue;
    for(int j=i+1;j<lines.length;j++){String next=lines[j].trim();if(next.isEmpty())continue;if(next.startsWith("#"))break;String url=new URI(base).resolve(next).toString();validateMediaUrl(url);long bitrate;try{bitrate=Long.parseLong(a.getOrDefault("BANDWIDTH","0"));}catch(NumberFormatException invalid){bitrate=0;}if(bitrate>highest){highest=bitrate;best=url;}break;}
   }
  }
  if(!best.isEmpty())return best;if(!fallback.isEmpty())return fallback;throw new IOException("오디오 전용 라이브 스트림이 없습니다");
 }
 static void validateMediaUrl(String value)throws IOException{
  try{URI uri=new URI(value);String host=uri.getHost();if(!"https".equals(uri.getScheme())||host==null||!(host.equals("googlevideo.com")||host.endsWith(".googlevideo.com"))||uri.getUserInfo()!=null||uri.getPort()!=-1&&uri.getPort()!=443)throw new IOException("라이브 미디어 주소 형식 오류");}catch(URISyntaxException error){throw new IOException("라이브 미디어 주소 형식 오류");}
 }
 private static void validateRequestUrl(URL url)throws IOException{
  String host=url.getHost();if(!"https".equals(url.getProtocol())||!(host.equals("www.youtube.com")||host.equals("youtube.com")||host.endsWith(".googlevideo.com"))||url.getUserInfo()!=null||url.getPort()!=-1&&url.getPort()!=443)throw new IOException("허용되지 않은 라이브 서버");
 }
 static String request(String value,String body,String visitorId)throws Exception{
  URL url=new URL(value);
  for(int redirects=0;redirects<=3;redirects++){
   validateRequestUrl(url);HttpURLConnection connection=(HttpURLConnection)url.openConnection();connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(12000);connection.setReadTimeout(12000);connection.setRequestProperty("User-Agent",body==null?WEB_AGENT:CLIENT_AGENT);
   if(body!=null){if(!"www.youtube.com".equals(url.getHost()))throw new IOException("라이브 API 주소 오류");connection.setRequestMethod("POST");connection.setDoOutput(true);connection.setRequestProperty("Content-Type","application/json");connection.setRequestProperty("Origin","https://www.youtube.com");connection.setRequestProperty("X-YouTube-Client-Name","101");connection.setRequestProperty("X-YouTube-Client-Version","1.02");connection.setRequestProperty("X-Goog-Visitor-Id",visitorId);}
   try{
    if(body!=null)try(OutputStream out=connection.getOutputStream()){out.write(body.getBytes(StandardCharsets.UTF_8));}
    int status=connection.getResponseCode();
    if(status>=300&&status<400){String location=connection.getHeaderField("Location");if(location==null||body!=null)throw new IOException("라이브 서버 주소 변경 오류");url=new URL(url,location);continue;}
    if(status!=200)throw new IOException("라이브 서버 응답 "+status);
    try(InputStream in=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){out.write(buffer,0,n);if(out.size()>5_000_000)throw new IOException("라이브 응답 크기 초과");}return out.toString(StandardCharsets.UTF_8.name());}
   }finally{connection.disconnect();}
  }
  throw new IOException("라이브 서버 주소 변경 횟수 초과");
 }
}
