package kr.jinsu.radio;

import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,manifest=Config.NONE)
@ConscryptMode(ConscryptMode.Mode.OFF)
public class PlayerLayoutTest {
 public static class OfflineActivity extends MainActivity {
  int steps,lastDirection;
  @Override void step(int direction){steps++;lastDirection=direction;}
  @Override void connect(){}
  @Override void loadSchedule(RadioData.Station station,boolean force){}
  @Override void loadListedSchedules(){}
 }
 ActivityController<OfflineActivity> lifecycle;OfflineActivity activity;
 @Before public void create(){lifecycle=Robolectric.buildActivity(OfflineActivity.class).create();activity=lifecycle.get();activity.screen="player";activity.build();}
 @After public void destroy(){lifecycle.destroy();}
 void layout(int width,int height){int w=activity.dp(width),h=activity.dp(height);activity.root.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));activity.root.layout(0,0,w,h);}
 TextView find(View view,String text){if(view instanceof TextView&&((TextView)view).getText().toString().equals(text))return (TextView)view;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){TextView match=find(group.getChildAt(i),text);if(match!=null)return match;}}return null;}
 int y(View view){int result=0;while(view!=null){result+=view.getTop()-view.getScrollY();view=view.getParent() instanceof View?(View)view.getParent():null;}return result;}
 @Test public void buttonPositionsDoNotDependOnDescriptionLength(){activity.programDescription.setText("짧은 설명");layout(412,820);TextView schedule=find(activity.root,"오늘의 편성표"),website=find(activity.root,"방송 홈페이지");assertNotNull(schedule);assertNotNull(website);int initial=y(schedule);String full="전체 설명을 유지하고 길이에 상관없이 버튼은 고정합니다. ".repeat(80);activity.programDescription.setText(full);layout(412,820);assertEquals(full,activity.programDescription.getText().toString());assertEquals(initial,y(schedule));assertEquals(y(schedule),y(website));View footer=(View)schedule.getParent();assertSame(activity.body,footer.getParent());assertEquals(activity.body.getHeight(),footer.getBottom());}
 @Test public void longDescriptionScrollDoesNotMoveButtonsOrDock(){for(int height:new int[]{560,820}){activity.programDescription.setText("생략하지 않는 긴 프로그램 설명입니다. ".repeat(100));layout(360,height);TextView schedule=find(activity.root,"오늘의 편성표");View dock=activity.root.findViewById(R.id.playback_bar);int buttonY=y(schedule),dockY=y(dock);activity.scroll.scrollTo(0,activity.playerContent.getHeight());assertTrue(activity.scroll.getScrollY()>0);assertEquals(buttonY,y(schedule));assertEquals(dockY,y(dock));assertTrue(buttonY+schedule.getHeight()<=dockY);assertNull(activity.programDescription.getEllipsize());}}
 void gesture(View target,float x,float y,float endX,float endY){int[] types={MotionEvent.ACTION_DOWN,MotionEvent.ACTION_MOVE,MotionEvent.ACTION_MOVE,MotionEvent.ACTION_UP};for(int i=0;i<types.length;i++){float fraction=i==0?0:i==1?.5f:1;MotionEvent event=MotionEvent.obtain(0,i*50,types[i],activity.dp(x+(endX-x)*fraction),activity.dp(y+(endY-y)*fraction),0);try{target.dispatchTouchEvent(event);}finally{event.recycle();}}}
 @Test public void playerSwipeCallsSameChannelStepPath(){layout(360,820);gesture(activity.screenPanel,260,180,80,180);assertEquals(1,activity.steps);assertEquals(1,activity.lastDirection);gesture(activity.screenPanel,80,180,260,180);assertEquals(2,activity.steps);assertEquals(-1,activity.lastDirection);}
 @Test public void upwardDockSwipeOpensPlayer(){activity.screen="list";activity.build();layout(360,820);View dock=activity.root.findViewById(R.id.playback_bar);gesture(dock,100,65,100,0);assertEquals("player",activity.screen);assertEquals(0,activity.steps);}

}
