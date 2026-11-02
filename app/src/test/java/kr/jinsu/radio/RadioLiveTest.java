package kr.jinsu.radio;
import org.junit.Test;
import org.junit.Assume;
import static org.junit.Assert.*;
import java.time.LocalDate;
import java.util.List;
public class RadioLiveTest {
 @Test public void officialSchedulesParseAndContainCurrentProgram() throws Exception {
  Assume.assumeTrue(Boolean.getBoolean("radioLiveCheck"));
  for(RadioData.Station s:RadioData.STATIONS){if(s.id.equals("ebs"))continue;List<RadioData.Program> p=RadioData.schedule(s,LocalDate.now(RadioData.SEOUL));assertFalse(s.id+" empty",p.isEmpty());RadioData.Program now=RadioData.current(p);assertNotNull(s.id+" current",now);System.out.println(s.id+" programs="+p.size()+" current="+now.title+" time="+now.time());}
 }
}
