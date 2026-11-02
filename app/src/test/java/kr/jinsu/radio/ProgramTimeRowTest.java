package kr.jinsu.radio;

import android.view.*;
import android.widget.TextView;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,manifest=Config.NONE)
@ConscryptMode(ConscryptMode.Mode.OFF)
public class ProgramTimeRowTest {
 RadioData.Program program(long start,long end){return new RadioData.Program("Program","","","",start,end);}
 @Test public void timesAndBarsAppearWithoutNumericPercentage(){ProgramTimeRow row=new ProgramTimeRow(RuntimeEnvironment.getApplication());RadioData.Program program=program(1000,61000);row.update(program,31000);assertEquals(2,row.getChildCount());assertEquals(program.time(),((TextView)row.getChildAt(0)).getText().toString());assertEquals(View.VISIBLE,row.getChildAt(1).getVisibility());assertFalse(row.getContentDescription().toString().contains("%"));}
 @Test public void missingAndEndedProgramsHideProgress(){ProgramTimeRow row=new ProgramTimeRow(RuntimeEnvironment.getApplication());row.update(program(1000,61000),31000);row.update(null,31000);assertEquals(View.GONE,row.getVisibility());assertEquals("",((TextView)row.getChildAt(0)).getText().toString());row.update(program(1000,61000),61000);assertEquals(View.GONE,row.getVisibility());}
 @Test public void fitsNarrowStationColumnAndDoesNotConsumeTouch(){ProgramTimeRow row=new ProgramTimeRow(RuntimeEnvironment.getApplication());row.update(program(1000,61000),31000);int width=Math.round(140*row.getResources().getDisplayMetrics().density);row.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1000,View.MeasureSpec.AT_MOST));row.layout(0,0,width,row.getMeasuredHeight());assertTrue(row.getChildAt(0).getWidth()>0);assertTrue(row.getChildAt(1).getWidth()>0);assertTrue(row.getChildAt(1).getRight()<=width);assertFalse(row.isClickable());android.view.MotionEvent event=android.view.MotionEvent.obtain(0,0,android.view.MotionEvent.ACTION_DOWN,width/2f,1,0);try{assertFalse(row.dispatchTouchEvent(event));}finally{event.recycle();}}
}
