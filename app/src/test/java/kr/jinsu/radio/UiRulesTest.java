package kr.jinsu.radio;
import org.junit.Test;
import static org.junit.Assert.*;
public class UiRulesTest {
 @Test public void activeColumnTogglesDirection(){assertEquals("frequency_desc",UiRules.nextSort("frequency","frequency"));assertEquals("frequency",UiRules.nextSort("frequency_desc","frequency"));}
 @Test public void inactiveColumnStartsAscending(){assertEquals("name",UiRules.nextSort("frequency_desc","name"));assertEquals("frequency",UiRules.nextSort("name_desc","frequency"));}
 @Test public void onlyActiveColumnHasMonochromeArrow(){assertEquals("주파수 ▲",UiRules.sortLabel("frequency","frequency","주파수"));assertEquals("방송국명",UiRules.sortLabel("frequency","name","방송국명"));assertEquals("방송국명 ▼",UiRules.sortLabel("name_desc","name","방송국명"));}
 @Test public void rowCenterMatchesViewportCenter(){assertEquals(850,UiRules.centeredScrollY(1000,100,400));assertEquals(0,UiRules.centeredScrollY(0,100,400));}
 @Test public void descendingStationOrderReversesAscending(){for(String key:new String[]{"frequency","name"}){java.util.List<RadioData.Station> a=RadioData.orderedStations(key),d=RadioData.orderedStations(key+"_desc");int count=0;for(RadioData.Station s:a)if(s.hasPrograms())count++;for(int i=0;i<count;i++)assertEquals(a.get(i).id,d.get(count-1-i).id);for(int i=count;i<a.size();i++)assertEquals(a.get(i).id,d.get(i).id);}}
}
