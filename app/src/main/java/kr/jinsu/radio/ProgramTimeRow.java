package kr.jinsu.radio;

import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;

/** Schedule time and read-only progress on one line. */
final class ProgramTimeRow extends LinearLayout {
 private final TextView time;private final ProgramProgressView progress;
 ProgramTimeRow(Context context){super(context);setOrientation(HORIZONTAL);setGravity(Gravity.CENTER_VERTICAL);setClickable(false);setFocusable(false);
  time=new TextView(context);time.setTextColor(0xff676762);time.setTextSize(11);time.setIncludeFontPadding(false);time.setSingleLine();time.setEllipsize(TextUtils.TruncateAt.END);time.setAutoSizeTextTypeUniformWithConfiguration(8,11,1,TypedValue.COMPLEX_UNIT_SP);addView(time,new LayoutParams(0,LayoutParams.WRAP_CONTENT,1));
  progress=new ProgramProgressView(context);LayoutParams bar=new LayoutParams(dp(24),dp(3));bar.setMarginStart(dp(6));bar.setMarginEnd(dp(4));addView(progress,bar);
  for(View child:new View[]{time,progress})child.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
 }
 private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
 @Override protected void onMeasure(int widthSpec,int heightSpec){int width=MeasureSpec.getSize(widthSpec);LayoutParams bar=(LayoutParams)progress.getLayoutParams();bar.width=Math.max(dp(16),Math.min(dp(56),Math.round(width*.16f)));super.onMeasure(widthSpec,heightSpec);}
 void update(RadioData.Program program,long now){boolean known=program!=null&&ProgramProgress.current(program.start,program.end,now);setVisibility(known?VISIBLE:GONE);if(!known){time.setText("");progress.update(0,false);setContentDescription(null);return;}
  int value=ProgramProgress.scaled(program.start,program.end,now);String when=program.time();if(!TextUtils.equals(time.getText(),when))time.setText(when);progress.update(value,true);setContentDescription(when+" · 방송 진행상태");
 }
}
