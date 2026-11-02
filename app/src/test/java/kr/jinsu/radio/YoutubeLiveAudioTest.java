package kr.jinsu.radio;

import org.json.JSONObject;
import org.junit.*;
import static org.junit.Assert.*;
import java.io.IOException;
import java.nio.file.*;

public class YoutubeLiveAudioTest {
 static final String BASE="https://manifest.googlevideo.com/master.m3u8";
 static final String AUDIO="#EXTM3U\n#EXT-X-MEDIA:URI=\"/audio-low.m3u8\",TYPE=AUDIO,GROUP-ID=\"233\",NAME=\"Default\",DEFAULT=YES,AUTOSELECT=YES\n#EXT-X-MEDIA:URI=\"/audio-high.m3u8\",TYPE=AUDIO,GROUP-ID=\"234\",NAME=\"Default\",DEFAULT=YES,AUTOSELECT=YES\n#EXT-X-STREAM-INF:BANDWIDTH=591418,CODECS=\"avc1.4D4015,mp4a.40.2\",RESOLUTION=426x240,AUDIO=\"234\"\n/video.m3u8\n";
 @Test public void choosesActualAudioRenditionInsteadOfMixedVideo()throws Exception{assertEquals("https://manifest.googlevideo.com/audio-high.m3u8",YoutubeLiveAudio.audioRendition(AUDIO,BASE));}
 @Test public void acceptsExplicitAudioOnlyVariants()throws Exception{String m="#EXTM3U\n#EXT-X-STREAM-INF:BANDWIDTH=64000,CODECS=\"mp4a.40.5\"\n/a.m3u8\n#EXT-X-STREAM-INF:BANDWIDTH=128000,CODECS=\"mp4a.40.2\"\n/b.m3u8";assertEquals("https://manifest.googlevideo.com/b.m3u8",YoutubeLiveAudio.audioRendition(m,BASE));}
 @Test(expected=IOException.class)public void neverFallsBackToVideoWhenAudioMissing()throws Exception{YoutubeLiveAudio.audioRendition("#EXTM3U\n#EXT-X-STREAM-INF:BANDWIDTH=500000,CODECS=\"avc1.42C00B,mp4a.40.5\",RESOLUTION=256x144\n/video.m3u8",BASE);}
 @Test(expected=IOException.class)public void rejectsNonManifestResponse()throws Exception{YoutubeLiveAudio.audioRendition("<html>no stream</html>",BASE);}
 @Test public void validatesHttpsAndGoogleMediaHost()throws Exception{YoutubeLiveAudio.validateMediaUrl("https://rr1---sn-test.googlevideo.com/live.m3u8");for(String value:new String[]{"http://manifest.googlevideo.com/x","https://googlevideo.com.attacker.invalid/x","https://www.youtube.com/x","https://user@manifest.googlevideo.com/x","https://manifest.googlevideo.com:444/x","file:///tmp/audio"}){try{YoutubeLiveAudio.validateMediaUrl(value);fail(value);}catch(IOException expected){}}}
 JSONObject player(boolean live,String channel,String state)throws Exception{return new JSONObject().put("playabilityStatus",new JSONObject().put("status",state)).put("videoDetails",new JSONObject().put("channelId",channel).put("isLive",live));}
 @Test public void acceptsOnlyOfficialCurrentlyLivePlayer()throws Exception{YoutubeLiveAudio.validateLivePlayer(player(true,YoutubeLiveAudio.CHANNEL_ID,"OK"));for(JSONObject p:new JSONObject[]{player(false,YoutubeLiveAudio.CHANNEL_ID,"OK"),player(true,"other","OK"),player(true,YoutubeLiveAudio.CHANNEL_ID,"LOGIN_REQUIRED")}){try{YoutubeLiveAudio.validateLivePlayer(p);fail();}catch(IOException expected){}}}
 @Test public void playerJsonIsParsedWithoutExecutingJavascript()throws Exception{JSONObject p=YoutubeLiveAudio.playerFromPage("<script>var ytInitialPlayerResponse = {\"videoDetails\":{\"channelId\":\"test\"}}; alert('unused');</script>");assertEquals("test",p.getJSONObject("videoDetails").getString("channelId"));}
 @Test(expected=IOException.class)public void rejectsArbitraryInputUrl()throws Exception{YoutubeLiveAudio.resolve("https://other.invalid/");}
 @Test public void actualPublicLiveProducesAudioMediaPlaylist()throws Exception{
  Assume.assumeTrue(Boolean.getBoolean("radioYoutubeLiveCheck"));
  for(RadioData.Station station:RadioData.STATIONS)if(!station.hasPrograms()){
   YoutubeLiveAudio.LiveInfo info=YoutubeLiveAudio.inspect(station.website);assertEquals(station.channelId,info.channelId);
   String media=YoutubeLiveAudio.request(info.audioUrl,null,null);assertTrue(media.startsWith("#EXTM3U"));assertTrue(media.contains("#EXTINF:"));assertFalse(media.contains("#EXT-X-STREAM-INF:"));
   Path out=Paths.get(System.getProperty("radioYoutubeEvidencePath","build/yonhap-live"),station.id);Files.createDirectories(out);Files.write(out.resolve("stream.url"),info.audioUrl.getBytes(java.nio.charset.StandardCharsets.UTF_8));Files.write(out.resolve("media.m3u8"),media.getBytes(java.nio.charset.StandardCharsets.UTF_8));
   System.out.println("PUBLIC_AUDIO_PLAYLIST_OK "+station.id);
  }
 }
}
