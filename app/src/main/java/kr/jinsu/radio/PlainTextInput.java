package kr.jinsu.radio;

import android.content.Context;
import android.text.InputType;
import android.view.ViewStructure;
import android.view.autofill.AutofillManager;
import android.view.autofill.AutofillValue;
import android.widget.EditText;

/** Ordinary editable text that does not export content to the Autofill framework. */
final class PlainTextInput extends EditText {
 PlainTextInput(Context context){super(context);setImportantForAutofill(IMPORTANT_FOR_AUTOFILL_NO);setAutofillHints((String[])null);}
 @Override public int getAutofillType(){return AUTOFILL_TYPE_NONE;}
 @Override public AutofillValue getAutofillValue(){return null;}
 @Override public void autofill(AutofillValue value){/* User text is entered through the normal editor and clipboard. */}
 @Override public void onProvideAutofillStructure(ViewStructure structure,int flags){
  // TextView's implementation exports text even when importance is NO.
  // Only the Autofill representation is empty; normal assist and accessibility are unchanged.
  structure.setAutofillId(getAutofillId());
  structure.setAutofillType(AUTOFILL_TYPE_NONE);
  structure.setAutofillValue(null);structure.setAutofillHints(null);structure.setAutofillOptions(null);
  structure.setText(null);structure.setHint(null);structure.setContentDescription(null);
  structure.setClassName("android.view.View");structure.setInputType(InputType.TYPE_NULL);
  structure.setDataIsSensitive(true);
 }
 static void cancelAutofillSession(Context context){AutofillManager manager=context.getSystemService(AutofillManager.class);if(manager!=null)manager.cancel();}
}
