package kr.jinsu.radio;
import android.app.AlertDialog;
import android.content.*;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.autofill.*;
import android.view.inputmethod.*;
import android.widget.EditText;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowAutofillManager;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={26,28},manifest=Config.NONE,shadows=PlainTextInputTest.RecordingManager.class)
@ConscryptMode(ConscryptMode.Mode.OFF)
public class PlainTextInputTest {
 @Implements(AutofillManager.class) public static class RecordingManager extends ShadowAutofillManager {
  static int cancelled;
  @Implementation protected void cancel(){cancelled++;}
 }
 ActivityController<LiveRegistrationUiTest.Offline> lifecycle;LiveRegistrationUiTest.Offline activity;
 @Before public void create(){lifecycle=Robolectric.buildActivity(LiveRegistrationUiTest.Offline.class).create().start().resume().visible();activity=lifecycle.get();Shadows.shadowOf(Looper.getMainLooper()).idle();RecordingManager.cancelled=0;}
 @After public void destroy(){lifecycle.pause().stop().destroy();RadioData.setLiveOverrides(Collections.emptyList());}
 PlainTextInput input(){PlainTextInput view=new PlainTextInput(activity);view.setSingleLine();view.setText("내 채널과 주소");view.setHint("입력 힌트");mount(view);return view;}
 void mount(EditText view){activity.root.addView(view,new android.widget.LinearLayout.LayoutParams(500,80));view.measure(View.MeasureSpec.makeMeasureSpec(500,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(80,View.MeasureSpec.EXACTLY));view.layout(0,0,500,80);Shadows.shadowOf(Looper.getMainLooper()).idle();}
 void showForm(){activity.showLiveEditor(null);Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue("dialog input attached before removal test",activity.liveNameInput.isAttachedToWindow());}
 @Test public void oldImportanceFlagsAloneStillExportOrdinaryTextWhenRequested(){EditText old=new EditText(activity);old.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);old.setText("저장되면 안 되는 글");mount(old);RecordingInputStructure output=new RecordingInputStructure();old.dispatchProvideAutofillStructure(output,1);assertEquals("저장되면 안 되는 글",output.getText().toString());assertEquals(View.AUTOFILL_TYPE_TEXT,old.getAutofillType());assertNotNull(old.getAutofillValue());}
 @Test public void newInputExportsNeitherTextNorValueEvenWhenImportanceIsIgnored(){PlainTextInput view=input();assertEquals(View.AUTOFILL_TYPE_NONE,view.getAutofillType());assertNull(view.getAutofillValue());for(int flags:new int[]{0,1}){RecordingInputStructure output=new RecordingInputStructure();view.dispatchProvideAutofillStructure(output,flags);assertNull(output.text);assertNull(output.hint);assertNull(output.description);assertNull(output.value);assertNull(output.hints);assertEquals(View.AUTOFILL_TYPE_NONE,output.type);assertEquals(0,output.inputType);}view.autofill(AutofillValue.forText("패스 값"));assertEquals("내 채널과 주소",view.getText().toString());}
 @Test public void regularAssistAccessibilityKeyboardAndPasteKeepNormalText(){PlainTextInput view=input();RecordingInputStructure assist=new RecordingInputStructure();view.onProvideStructure(assist);assertEquals("내 채널과 주소",assist.text.toString());AccessibilityNodeInfo node=AccessibilityNodeInfo.obtain();view.onInitializeAccessibilityNodeInfo(node);assertEquals("내 채널과 주소",node.getText().toString());node.recycle();EditorInfo info=new EditorInfo();InputConnection connection=view.onCreateInputConnection(info);assertNotNull(connection);view.setSelection(view.length());connection.commitText(" 한글",1);assertTrue(view.getText().toString().endsWith(" 한글"));ClipboardManager clipboard=(ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);clipboard.setPrimaryClip(ClipData.newPlainText("test","붙여넣기"));view.setSelection(0,view.length());assertTrue(view.onTextContextMenuItem(android.R.id.paste));assertEquals("붙여넣기",view.getText().toString());}
 @Test public void allThreeRealInputsUseExportFreeComponent(){int found=countPlainInputs(activity.getWindow().getDecorView());showForm();assertTrue(activity.liveDialog instanceof PlainInputDialog);found+=countPlainInputs(activity.liveDialog.getWindow().getDecorView());assertEquals(3,found);}
 int countPlainInputs(View view){int count=0;if(view instanceof EditText){assertTrue(view instanceof PlainTextInput);assertEquals(View.AUTOFILL_TYPE_NONE,view.getAutofillType());assertNull(view.getAutofillValue());count++;}if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)count+=countPlainInputs(group.getChildAt(i));}return count;}
 void trackDetach(View view,int before,List<Integer> seen){view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){public void onViewAttachedToWindow(View v){}public void onViewDetachedFromWindow(View v){seen.add(RecordingManager.cancelled-before);}});}
 @Test public void dialogCancelsBeforeRemovalForCancelButtonBackAndProgrammaticClose(){for(int mode=0;mode<3;mode++){showForm();int before=RecordingManager.cancelled;List<Integer> seen=new ArrayList<>();trackDetach(activity.liveNameInput,before,seen);AlertDialog dialog=activity.liveDialog;if(mode==0)dialog.cancel();else if(mode==1)dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();else dialog.dismiss();Shadows.shadowOf(Looper.getMainLooper()).idle();assertFalse(dialog.isShowing());assertFalse("a real attached input must detach",seen.isEmpty());assertTrue(seen.get(0)>0);}}
 @Test public void successfulSaveCancelsBeforeFieldRemovalAndPreservesRegistry(){showForm();activity.liveNameInput.setText("내 방송");activity.liveUrlInput.setText("https://youtu.be/abcdefghijk");AlertDialog dialog=activity.liveDialog;int before=RecordingManager.cancelled;List<Integer> seen=new ArrayList<>();trackDetach(activity.liveNameInput,before,seen);activity.finishLiveRegistration(dialog,activity.liveRequestTicket,null,"내 방송",new YoutubeLiveAudio.LiveInfo("abcdefghijk","UCaaaaaaaaaaaaaaaaaaaaaa","https://manifest.googlevideo.com/audio.m3u8"),"");assertFalse(dialog.isShowing());assertFalse("a real attached input must detach",seen.isEmpty());assertTrue(seen.get(0)>0);assertEquals(18,RadioData.stations().size());}
 @Test public void pauseAndScreenReplacementCancelExistingContext(){int before=RecordingManager.cancelled;lifecycle.pause();assertTrue(RecordingManager.cancelled>before);lifecycle.resume();before=RecordingManager.cancelled;List<Integer> seen=new ArrayList<>();View search=findInput(activity.getWindow().getDecorView());assertNotNull(search);trackDetach(search,before,seen);activity.build();assertFalse("a real attached input must detach",seen.isEmpty());assertTrue(seen.get(0)>0);}
 View findInput(View v){if(v instanceof EditText)return v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){View result=findInput(g.getChildAt(i));if(result!=null)return result;}}return null;}
}
