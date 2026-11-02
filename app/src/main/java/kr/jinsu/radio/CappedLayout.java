package kr.jinsu.radio;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

/** A centered panel can fill available space without stretching its reading width. */
final class CappedLayout extends LinearLayout {
 private final int maximumWidth;
 CappedLayout(Context context,int maximumWidthDp){super(context);maximumWidth=Math.round(maximumWidthDp*getResources().getDisplayMetrics().density);setOrientation(VERTICAL);}
 @Override protected void onMeasure(int widthSpec,int heightSpec){int mode=View.MeasureSpec.getMode(widthSpec),width=View.MeasureSpec.getSize(widthSpec);if(mode==View.MeasureSpec.UNSPECIFIED||width>maximumWidth)widthSpec=View.MeasureSpec.makeMeasureSpec(maximumWidth,mode==View.MeasureSpec.EXACTLY?View.MeasureSpec.EXACTLY:View.MeasureSpec.AT_MOST);super.onMeasure(widthSpec,heightSpec);}
}
