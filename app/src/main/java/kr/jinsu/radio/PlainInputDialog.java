package kr.jinsu.radio;

import android.app.AlertDialog;
import android.content.Context;

/** Cancels the input form's Autofill context before any dialog window removal. */
final class PlainInputDialog extends AlertDialog {
 PlainInputDialog(Context context){super(context);}
 @Override public void dismiss(){PlainTextInput.cancelAutofillSession(getContext());super.dismiss();}
}
