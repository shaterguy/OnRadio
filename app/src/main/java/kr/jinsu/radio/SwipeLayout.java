package kr.jinsu.radio;

import android.content.Context;
import android.view.*;
import android.widget.LinearLayout;
import java.util.function.IntConsumer;

/** Intercepts only a deliberate gesture on its axis, preserving child taps and vertical scrolling. */
final class SwipeLayout extends LinearLayout {
 static final int UP=0,HORIZONTAL=1;
 private final int mode,slop;private final float minimumDistance;private final IntConsumer action;
 private boolean enabled=true,tracking,dragging,rejected;private float startX,startY;private int pointerId;
 SwipeLayout(Context context,int mode,IntConsumer action){super(context);setOrientation(VERTICAL);this.mode=mode;this.action=action;slop=ViewConfiguration.get(context).getScaledTouchSlop();minimumDistance=Math.max(48*getResources().getDisplayMetrics().density,slop*3);}
 void setSwipeEnabled(boolean value){enabled=value;if(!value)reset();}
 private void reset(){tracking=false;dragging=false;rejected=false;setPressed(false);}
 private void begin(MotionEvent event){reset();tracking=enabled&&event.getPointerCount()==1;startX=event.getX();startY=event.getY();pointerId=event.getPointerId(0);}
 private void track(MotionEvent event){if(!tracking||rejected||dragging)return;if(event.getPointerCount()!=1||event.findPointerIndex(pointerId)<0){rejected=true;return;}float dx=event.getX()-startX,dy=event.getY()-startY,ax=Math.abs(dx),ay=Math.abs(dy);if(Math.max(ax,ay)<=slop)return;
  boolean matches=mode==UP?dy<0&&ay>ax*1.4f:ax>ay*1.4f;
  if(matches||mode==UP){if(!matches)rejected=true;dragging=true;setPressed(false);if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);}else rejected=true;
 }
 @Override public boolean onInterceptTouchEvent(MotionEvent event){int type=event.getActionMasked();if(type==MotionEvent.ACTION_DOWN){begin(event);return super.onInterceptTouchEvent(event);}if(enabled&&type==MotionEvent.ACTION_POINTER_DOWN){rejected=true;tracking=false;dragging=true;}if(dragging)return true;if(type==MotionEvent.ACTION_MOVE)track(event);if(dragging)return true;if(type==MotionEvent.ACTION_UP||type==MotionEvent.ACTION_CANCEL){tracking=false;rejected=false;}return super.onInterceptTouchEvent(event);}
 @Override public boolean onTouchEvent(MotionEvent event){int type=event.getActionMasked();if(type==MotionEvent.ACTION_DOWN&&!tracking)begin(event);if(enabled&&type==MotionEvent.ACTION_POINTER_DOWN){rejected=true;tracking=false;dragging=true;}if(type==MotionEvent.ACTION_MOVE)track(event);
  if(dragging){if(type==MotionEvent.ACTION_UP){float dx=event.getX()-startX,dy=event.getY()-startY;boolean valid=tracking&&!rejected&&event.getPointerCount()==1&&(mode==UP?-dy>=minimumDistance&&-dy>Math.abs(dx)*1.4f:Math.abs(dx)>=minimumDistance&&Math.abs(dx)>Math.abs(dy)*1.4f);int direction=mode==UP?0:dx<0?1:-1;reset();if(valid)action.accept(direction);}else if(type==MotionEvent.ACTION_CANCEL)reset();return true;}
  boolean eligible=tracking;boolean handled=super.onTouchEvent(event);if(type==MotionEvent.ACTION_UP||type==MotionEvent.ACTION_CANCEL)reset();return handled||eligible;
 }
 @Override public boolean performClick(){return super.performClick();}
}
