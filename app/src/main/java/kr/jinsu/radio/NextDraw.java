package kr.jinsu.radio;

import android.view.View;
import android.view.ViewTreeObserver;

/** Runs once after measurement; detaching the view cancels the callback. */
final class NextDraw implements ViewTreeObserver.OnPreDrawListener,View.OnAttachStateChangeListener {
 private final View view;private ViewTreeObserver owner;private Runnable action;
 static void add(View view,Runnable action){new NextDraw(view,action);}
 private NextDraw(View view,Runnable action){this.view=view;this.action=action;owner=view.getViewTreeObserver();owner.addOnPreDrawListener(this);view.addOnAttachStateChangeListener(this);}
 private void cancel(){if(owner.isAlive())owner.removeOnPreDrawListener(this);else if(view.getViewTreeObserver().isAlive())view.getViewTreeObserver().removeOnPreDrawListener(this);view.removeOnAttachStateChangeListener(this);action=null;}
 @Override public boolean onPreDraw(){Runnable task=action;cancel();if(task!=null)task.run();return true;}
 @Override public void onViewAttachedToWindow(View v){owner=v.getViewTreeObserver();}
 @Override public void onViewDetachedFromWindow(View v){cancel();}
}
