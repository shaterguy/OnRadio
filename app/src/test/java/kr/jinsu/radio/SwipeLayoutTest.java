package kr.jinsu.radio;

import android.view.*;
import android.widget.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,manifest=Config.NONE)
@ConscryptMode(ConscryptMode.Mode.OFF)
public class SwipeLayoutTest {
 List<Integer> actions;int clicks;SwipeLayout layout;org.robolectric.android.controller.ActivityController<android.app.Activity> host;
 @After public void destroy(){if(host!=null)host.destroy();}
 int dp(float value){return Math.round(value*RuntimeEnvironment.getApplication().getResources().getDisplayMetrics().density);}
 void create(int mode){actions=new ArrayList<>();clicks=0;layout=new SwipeLayout(RuntimeEnvironment.getApplication(),mode,actions::add);Button button=new Button(RuntimeEnvironment.getApplication());button.setText("control");button.setOnClickListener(v->clicks++);layout.addView(button,new LinearLayout.LayoutParams(-1,-1));host=Robolectric.buildActivity(android.app.Activity.class).setup().visible();host.get().setContentView(layout);layout.measure(View.MeasureSpec.makeMeasureSpec(dp(360),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(dp(240),View.MeasureSpec.EXACTLY));layout.layout(0,0,dp(360),dp(240));}
 void event(int action,float x,float y,long time){MotionEvent e=MotionEvent.obtain(0,time,action,dp(x),dp(y),0);try{layout.dispatchTouchEvent(e);}finally{e.recycle();}if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)org.robolectric.shadows.ShadowLooper.idleMainLooper();}
 void swipe(float x,float y,float endX,float endY){event(MotionEvent.ACTION_DOWN,x,y,0);event(MotionEvent.ACTION_MOVE,(x+endX)/2,(y+endY)/2,50);event(MotionEvent.ACTION_MOVE,endX,endY,100);event(MotionEvent.ACTION_UP,endX,endY,150);}
 @Test public void upSwipeOverControlOpensOnceWithoutClick(){create(SwipeLayout.UP);swipe(100,180,100,60);assertEquals(Collections.singletonList(0),actions);assertEquals(0,clicks);}
 @Test public void ordinaryControlTapRemainsClickable(){create(SwipeLayout.UP);event(MotionEvent.ACTION_DOWN,100,120,0);event(MotionEvent.ACTION_UP,100,120,50);org.robolectric.shadows.ShadowLooper.idleMainLooper();assertEquals(1,clicks);assertTrue(actions.isEmpty());}
 @Test public void leftAndRightMapToNextAndPrevious(){create(SwipeLayout.HORIZONTAL);swipe(260,100,80,100);swipe(80,100,260,100);assertEquals(Arrays.asList(1,-1),actions);assertEquals(0,clicks);}
 @Test public void shortDragAndCancelDoNotSwitch(){create(SwipeLayout.HORIZONTAL);swipe(160,100,135,100);assertTrue(actions.isEmpty());event(MotionEvent.ACTION_DOWN,250,100,200);event(MotionEvent.ACTION_MOVE,100,100,250);event(MotionEvent.ACTION_CANCEL,100,100,300);assertTrue(actions.isEmpty());}
 @Test public void downwardOrSidewaysDockDragDoesNotOpenOrClick(){create(SwipeLayout.UP);swipe(100,60,100,180);swipe(80,100,260,100);assertTrue(actions.isEmpty());assertEquals(0,clicks);}
 @Test public void playerVerticalScrollIsNotIntercepted(){create(SwipeLayout.HORIZONTAL);ScrollView scroll=new ScrollView(RuntimeEnvironment.getApplication());TextView text=new TextView(RuntimeEnvironment.getApplication());text.setText("설명을 끝까지 읽을 수 있습니다.\n".repeat(200));scroll.addView(text);layout.removeAllViews();layout.addView(scroll,new LinearLayout.LayoutParams(-1,-1));layout.measure(View.MeasureSpec.makeMeasureSpec(dp(360),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(dp(240),View.MeasureSpec.EXACTLY));layout.layout(0,0,dp(360),dp(240));swipe(180,200,180,40);assertTrue(actions.isEmpty());assertTrue(scroll.getScrollY()>0);}
 @Test public void secondFingerCancelsChildClickAndSwipe(){create(SwipeLayout.UP);event(MotionEvent.ACTION_DOWN,100,120,0);MotionEvent.PointerProperties[] properties={new MotionEvent.PointerProperties(),new MotionEvent.PointerProperties()};MotionEvent.PointerCoords[] coords={new MotionEvent.PointerCoords(),new MotionEvent.PointerCoords()};for(int i=0;i<2;i++){properties[i].id=i;properties[i].toolType=MotionEvent.TOOL_TYPE_FINGER;coords[i].x=dp(100+i*20);coords[i].y=dp(120);coords[i].pressure=1;coords[i].size=1;}for(int type:new int[]{MotionEvent.ACTION_POINTER_DOWN,MotionEvent.ACTION_POINTER_UP}){MotionEvent e=MotionEvent.obtain(0,50,type|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),2,properties,coords,0,0,1,1,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0);try{layout.dispatchTouchEvent(e);}finally{e.recycle();}}event(MotionEvent.ACTION_UP,100,120,100);assertTrue(actions.isEmpty());assertEquals(0,clicks);}

}
