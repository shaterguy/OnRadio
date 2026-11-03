package kr.jinsu.radio;
final class RecordingInputStructure extends android.view.ViewStructure {
 CharSequence text,hint,description;String[] hints;int type,inputType;android.view.autofill.AutofillValue value;android.view.autofill.AutofillId id;final android.os.Bundle extras=new android.os.Bundle();
 @Override public int addChildCount(int p0){return 0;}
 @Override public void asyncCommit(){}
 @Override public android.view.ViewStructure asyncNewChild(int p0){return null;}
 @Override public android.view.autofill.AutofillId getAutofillId(){return id;}
 @Override public int getChildCount(){return 0;}
 @Override public android.os.Bundle getExtras(){return extras;}
 @Override public java.lang.CharSequence getHint(){return hint;}
 @Override public java.lang.CharSequence getText(){return text;}
 @Override public int getTextSelectionEnd(){return 0;}
 @Override public int getTextSelectionStart(){return 0;}
 @Override public boolean hasExtras(){return !extras.isEmpty();}
 @Override public android.view.ViewStructure newChild(int p0){return null;}
 @Override public android.view.ViewStructure.HtmlInfo.Builder newHtmlInfoBuilder(java.lang.String p0){return null;}
 @Override public void setAccessibilityFocused(boolean p0){}
 @Override public void setActivated(boolean p0){}
 @Override public void setAlpha(float p0){}
 @Override public void setAutofillHints(java.lang.String[] p0){hints=p0;}
 @Override public void setAutofillId(android.view.autofill.AutofillId p0){id=p0;}
 @Override public void setAutofillId(android.view.autofill.AutofillId p0,int p1){id=p0;}
 @Override public void setAutofillOptions(java.lang.CharSequence[] p0){}
 @Override public void setAutofillType(int p0){type=p0;}
 @Override public void setAutofillValue(android.view.autofill.AutofillValue p0){value=p0;}
 @Override public void setCheckable(boolean p0){}
 @Override public void setChecked(boolean p0){}
 @Override public void setChildCount(int p0){}
 @Override public void setClassName(java.lang.String p0){}
 @Override public void setClickable(boolean p0){}
 @Override public void setContentDescription(java.lang.CharSequence p0){description=p0;}
 @Override public void setContextClickable(boolean p0){}
 @Override public void setDataIsSensitive(boolean p0){}
 @Override public void setDimens(int p0,int p1,int p2,int p3,int p4,int p5){}
 @Override public void setElevation(float p0){}
 @Override public void setEnabled(boolean p0){}
 @Override public void setFocusable(boolean p0){}
 @Override public void setFocused(boolean p0){}
 @Override public void setHint(java.lang.CharSequence p0){hint=p0;}
 @Override public void setHtmlInfo(android.view.ViewStructure.HtmlInfo p0){}
 @Override public void setId(int p0,java.lang.String p1,java.lang.String p2,java.lang.String p3){}
 @Override public void setInputType(int p0){inputType=p0;}
 @Override public void setLocaleList(android.os.LocaleList p0){}
 @Override public void setLongClickable(boolean p0){}
 @Override public void setOpaque(boolean p0){}
 @Override public void setSelected(boolean p0){}
 @Override public void setText(java.lang.CharSequence p0){text=p0;}
 @Override public void setText(java.lang.CharSequence p0,int p1,int p2){text=p0;}
 @Override public void setTextLines(int[] p0,int[] p1){}
 @Override public void setTextStyle(float p0,int p1,int p2,int p3){}
 @Override public void setTransformation(android.graphics.Matrix p0){}
 @Override public void setVisibility(int p0){}
 @Override public void setWebDomain(java.lang.String p0){}
}
