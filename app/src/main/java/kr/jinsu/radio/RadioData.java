package kr.jinsu.radio;

import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class RadioData {
 public static final ZoneId SEOUL=ZoneId.of("Asia/Seoul");
 public static final class Station {
  public final String id,name,brand,fm,description,kind,code,website,schedule;
  Station(String id,String name,String brand,String fm,String description,String kind,String code,String website,String schedule){this.id=id;this.name=name;this.brand=brand;this.fm=fm;this.description=description;this.kind=kind;this.code=code;this.website=website;this.schedule=schedule;}
 }
 public static final List<Station> STATIONS=Collections.unmodifiableList(Arrays.asList(
  new Station("kbs25","KBS Cool FM","KBS","89.1","음악과 이야기가 함께하는 KBS의 대중음악 채널","kbs","25","https://radio.kbs.co.kr/","https://schedule.kbs.co.kr/"),
  new Station("mbcmfm","MBC FM4U","MBC","91.9","하루를 함께하는 음악과 DJ의 이야기","mbc","mfm","https://www.imbc.com/broad/radio/","https://schedule.imbc.com/"),
  new Station("sbspower","SBS 파워FM","SBS","107.7","다양한 음악과 즐거운 이야기를 전하는 채널","sbs","Power","https://www.sbs.co.kr/radio/","https://www.sbs.co.kr/schedule/index.html?type=radio"),
  new Station("kbs24","KBS Classic FM","KBS","93.1","클래식 음악과 공연을 전하는 채널","kbs","24","https://radio.kbs.co.kr/","https://schedule.kbs.co.kr/"),
  new Station("kbs22","KBS Happy FM","KBS","106.1","친숙한 음악과 생활 속 이야기를 전하는 채널","kbs","22","https://radio.kbs.co.kr/","https://schedule.kbs.co.kr/"),
  new Station("mbcsfm","MBC 표준FM","MBC","95.9","뉴스, 시사, 음악과 생활정보를 함께하는 채널","mbc","sfm","https://www.imbc.com/broad/radio/","https://schedule.imbc.com/"),
  new Station("sbslove","SBS 러브FM","SBS","103.5","사람과 일상, 음악을 함께 나누는 채널","sbs","Love","https://www.sbs.co.kr/radio/","https://www.sbs.co.kr/schedule/index.html?type=radio"),
  new Station("kbs21","KBS 1라디오","KBS","97.3","뉴스와 시사, 재난정보를 전하는 채널","kbs","21","https://radio.kbs.co.kr/","https://schedule.kbs.co.kr/"),
  new Station("kbs23","KBS 3라디오","KBS","104.9","장애인과 사회적 약자를 위한 정보와 문화 채널","kbs","23","https://radio.kbs.co.kr/","https://schedule.kbs.co.kr/"),
  new Station("cbs939","CBS 음악FM","CBS","93.9","좋은 음악과 이야기를 함께하는 음악 전문 채널","static","https://m-aac.cbs.co.kr/mweb_cbs939/_definst_/cbs939.stream/playlist.m3u8","https://www.cbs.co.kr/radio/","https://www.cbs.co.kr/schedule?type=musicFm"),
  new Station("cbs981","CBS 표준FM","CBS","98.1","뉴스와 시사, 문화와 음악을 전하는 채널","static","https://m-aac.cbs.co.kr/mweb_cbs981/_definst_/cbs981.stream/playlist.m3u8","https://www.cbs.co.kr/radio/","https://www.cbs.co.kr/schedule?type=fm"),
  new Station("ebs","EBS FM","EBS","104.5","외국어와 교양, 배움이 있는 교육 라디오","static","https://ebsonair.ebs.co.kr/fmradiofamilypc/familypc1m/playlist.m3u8","https://home.ebs.co.kr/efm","https://www.ebs.co.kr/schedule?channel=radio")
 ));
 public static Station find(String id){for(Station s:STATIONS)if(s.id.equals(id))return s;return STATIONS.get(0);}
 public static String get(String url) throws Exception {
  URL u=new URL(url); if(!u.getProtocol().equals("https"))throw new IOException("안전하지 않은 방송 주소");
  HttpURLConnection c=(HttpURLConnection)u.openConnection();c.setConnectTimeout(12000);c.setReadTimeout(12000);c.setRequestProperty("User-Agent","Mozilla/5.0 OnRadio/0.1");c.setRequestProperty("Referer",url.contains("imbc.com")?"https://mini.imbc.com/":url.contains("sbs.co.kr")?"https://www.sbs.co.kr/":url.contains("cbs.co.kr")?"https://www.cbs.co.kr/":"https://radio.kbs.co.kr/");
  try {int status=c.getResponseCode();if(status!=200)throw new IOException("방송 서버 응답 "+status); ByteArrayOutputStream b=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){b.write(buf,0,n);if(b.size()>5_000_000)throw new IOException("응답 크기 초과");}}return b.toString(StandardCharsets.UTF_8.name()).replace("\uFEFF","");}finally{c.disconnect();}
 }
 public static String resolve(Station s) throws Exception {
  String result;
  if(s.kind.equals("kbs")){JSONObject o=new JSONObject(get("https://cfpwwwapi.kbs.co.kr/api/v1/landing/live/channel_code/"+s.code));JSONArray a=o.getJSONArray("channel_item");result="";for(int i=0;i<a.length();i++){JSONObject it=a.getJSONObject(i);if("radio".equals(it.optString("media_type"))){result=it.getString("service_url");break;}}}
  else if(s.kind.equals("mbc"))result=get("https://sminiplay.imbc.com/aacplay.ashx?agent=webapp&channel="+s.code).trim();
  else if(s.kind.equals("sbs")){String code=s.code.equals("Power")?"power":"love";result=get("https://apis.sbs.co.kr/play-api/1.0/livestream/"+code+"pc/"+code+"fm?protocol=hls&ssl=Y").trim();}
  else result=s.code;
  if(result.startsWith("\""))result=new JSONTokener(result).nextValue().toString();
  URI u=new URI(result); if(!"https".equals(u.getScheme())||u.getHost()==null)throw new IOException("방송 주소를 가져오지 못했습니다");return result;
 }
 public static class Program {
  public final String title,host,description,link;public final long start,end;
  public Program(String title,String host,String description,String link,long start,long end){this.title=clean(title);this.host=clean(host);this.description=clean(description);this.link=link;this.start=start;this.end=end;}
  public boolean current(long now){return start<=now&&now<end;}
  public String time(){DateTimeFormatter f=DateTimeFormatter.ofPattern("HH:mm");return f.format(Instant.ofEpochMilli(start).atZone(SEOUL))+" ∼ "+f.format(Instant.ofEpochMilli(end).atZone(SEOUL));}
 }
 static String clean(String s){if(s==null||s.equals("null"))return "";return s.replaceAll("<[^>]*>"," ").replace("&amp;","&").replace("&quot;","\"").replace("&#39;", "'").replace("&lt;","<").replace("&gt;",">").replaceAll("\\s+"," ").trim();}
 static long stamp(LocalDate date,String time){String t=time.replace(":","");if(t.length()<4)throw new IllegalArgumentException("시간 형식");int h=Integer.parseInt(t.substring(0,2)),m=Integer.parseInt(t.substring(2,4));return date.plusDays(h/24).atTime(h%24,m).atZone(SEOUL).toInstant().toEpochMilli();}
 public static List<Program> schedule(Station s,LocalDate date)throws Exception {
  ArrayList<Program> list=new ArrayList<>();
  if(s.kind.equals("kbs")){
   String from=date.minusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE),to=date.format(DateTimeFormatter.BASIC_ISO_DATE);
   JSONArray days=new JSONArray(get("https://static.api.kbs.co.kr/mediafactory/v1/schedule/weekly?local_station_code=00&channel_code="+s.code+"&program_planned_date_from="+from+"&program_planned_date_to="+to));
   for(int d=0;d<days.length();d++){JSONObject day=days.getJSONObject(d);if(!s.code.equals(day.optString("channel_code")))continue;JSONArray a=day.optJSONArray("schedules");if(a==null)continue;
    for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);String ds=o.optString("service_date",o.optString("program_planned_date"));LocalDate sd=LocalDate.parse(ds,DateTimeFormatter.BASIC_ISO_DATE);String st=o.optString("service_start_time",o.optString("program_planned_start_time"));String en=o.optString("service_end_time",o.optString("program_planned_end_time"));long start=stamp(sd,st),end=stamp(sd,en);if(end<=start)end+=86400000;list.add(new Program(o.optString("program_title"),o.optString("program_actor"),o.optString("program_intention"),o.optString("homepage_url"),start,end));}
   }
  }else if(s.kind.equals("mbc")){
   for(LocalDate date2:Arrays.asList(date.minusDays(1),date)){
    JSONArray a=new JSONArray(get("https://control.imbc.com/Schedule/Radio?sDate="+date2.format(DateTimeFormatter.BASIC_ISO_DATE)+"&sType="+(s.code.equals("mfm")?"FM4U":"FM")));
    for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);String st=o.getString("StartTime"),en=o.getString("EndTime");LocalDate sd=LocalDate.parse(o.getString("BroadDate"));long start=stamp(sd,st),end=stamp(sd,en);if(end<=start)end+=86400000;String link=o.optString("HomepageURL").replace("http://","https://");list.add(new Program(o.optString("Title"),o.optString("Players"),o.optString("MiniMsg"),link,start,end));}
   }
  }else if(s.brand.equals("CBS")){
   JSONArray a=new JSONObject(get("https://www.cbs.co.kr/schedule/all/ajax")).getJSONArray(s.id.equals("cbs939")?"musicfm":"fm");
   for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);LocalDate sd=LocalDate.parse(o.getString("date"));long start=stamp(sd,o.getString("startTime")),end=stamp(sd,o.getString("endTime"));if(end<=start)end+=86400000;JSONObject info=o.optJSONObject("program");String host=o.optString("mc");if(info!=null&&host.isEmpty())host=info.optString("mc");list.add(new Program(o.optString("name"),host,"",s.website,start,end));}
  }else if(s.kind.equals("sbs")){
   for(LocalDate date2:Arrays.asList(date.minusDays(1),date)){
    JSONArray a=new JSONArray(get("https://static.cloud.sbs.co.kr/schedule/"+date2.getYear()+"/"+date2.getMonthValue()+"/"+date2.getDayOfMonth()+"/"+s.code+".json"));
    for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);long start=stamp(date2,o.getString("start_time")),end=stamp(date2,o.getString("end_time"));if(end<=start)end+=86400000;list.add(new Program(o.optString("title"),o.optString("guest"),o.optString("description"),o.optString("homepage_url"),start,end));}
   }
  }
  long lo=date.atStartOfDay(SEOUL).toInstant().toEpochMilli(),hi=lo+86400000;
  list.removeIf(p->p.end<=lo||p.start>=hi||p.title.isEmpty());list.sort(Comparator.comparingLong(p->p.start));
  ArrayList<Program> merged=new ArrayList<>();
  for(Program p:list){if(!merged.isEmpty()){Program prev=merged.get(merged.size()-1);if(prev.title.equals(p.title)&&prev.end==p.start){merged.set(merged.size()-1,new Program(p.title,prev.host,prev.description,prev.link,prev.start,p.end));continue;}if(prev.start==p.start&&prev.title.equals(p.title))continue;}merged.add(p);}
  return merged;
 }
 public static Program current(List<Program> p){long now=System.currentTimeMillis();if(p!=null)for(Program a:p)if(a.current(now))return a;return null;}
}
