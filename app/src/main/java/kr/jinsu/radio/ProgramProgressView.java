package kr.jinsu.radio;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Read-only drawing: no seek thumb, touch handling, or player reference. */
final class ProgramProgressView extends View {
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private int progress;
 ProgramProgressView(Context context){super(context);setClickable(false);setFocusable(false);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
 void update(int value,boolean known){value=Math.max(0,Math.min(10000,value));if(progress!=value){progress=value;invalidate();}setVisibility(known?VISIBLE:INVISIBLE);}
 @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);paint.setColor(getContext().getColor(R.color.radio_line));canvas.drawRect(0,0,getWidth(),getHeight(),paint);paint.setColor(getContext().getColor(R.color.radio_text));canvas.drawRect(0,0,getWidth()*progress/10000f,getHeight(),paint);}
}
