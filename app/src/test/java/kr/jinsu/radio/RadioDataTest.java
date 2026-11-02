package kr.jinsu.radio;
import org.junit.Test;
import static org.junit.Assert.*;
import java.time.*;
public class RadioDataTest {
 @Test public void extendedHourBelongsToNextDate(){LocalDate day=LocalDate.of(2026,11,2);assertEquals(RadioData.stamp(day.plusDays(1),"010000"),RadioData.stamp(day,"250000"));}
 @Test public void kstTimezoneDoesNotDependOnDeviceTimezone(){assertEquals(Instant.parse("2026-11-01T15:00:00Z").toEpochMilli(),RadioData.stamp(LocalDate.of(2026,11,2),"00:00"));}
 @Test public void boundariesDoNotOverlap(){RadioData.Program p=new RadioData.Program("program","","","",100,200);assertFalse(p.current(99));assertTrue(p.current(100));assertTrue(p.current(199));assertFalse(p.current(200));}
 @Test public void htmlAndNullMetadataAreSafe(){assertEquals("음악 & 이야기",RadioData.clean("<b>음악</b> &amp; 이야기"));assertEquals("",RadioData.clean("null"));assertEquals("",RadioData.clean(null));}
 @Test public void stationIdsAreUnique(){java.util.Set<String> ids=new java.util.HashSet<>();for(RadioData.Station s:RadioData.STATIONS){assertTrue(ids.add(s.id));assertTrue(s.schedule.startsWith("https://"));}}
}
