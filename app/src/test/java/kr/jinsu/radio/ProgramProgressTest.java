package kr.jinsu.radio;

import org.junit.Test;
import static org.junit.Assert.*;

public class ProgramProgressTest {
 @Test public void ratioUsesProgramStartRatherThanListeningStart(){assertEquals(5000,ProgramProgress.scaled(1000,61000,31000));assertEquals(30000,ProgramProgress.elapsed(1000,61000,31000));}
 @Test public void boundariesAreClamped(){assertEquals(0,ProgramProgress.scaled(1000,61000,0));assertEquals(0,ProgramProgress.scaled(1000,61000,1000));assertEquals(10000,ProgramProgress.scaled(1000,61000,61000));assertEquals(10000,ProgramProgress.scaled(1000,61000,90000));}
 @Test public void invalidScheduleHasNoDuration(){assertFalse(ProgramProgress.valid(1000,1000));assertFalse(ProgramProgress.valid(61000,1000));assertEquals(0,ProgramProgress.scaled(61000,1000,30000));}
 @Test public void midnightProgramUsesAbsoluteTimestamps(){long start=RadioData.stamp(java.time.LocalDate.of(2026,11,2),"2330"),end=RadioData.stamp(java.time.LocalDate.of(2026,11,3),"0030"),now=RadioData.stamp(java.time.LocalDate.of(2026,11,3),"0000");assertEquals(5000,ProgramProgress.scaled(start,end,now));}
 @Test public void endedProgramIsNotCurrent(){assertTrue(ProgramProgress.current(1000,61000,1000));assertFalse(ProgramProgress.current(1000,61000,61000));}
}
