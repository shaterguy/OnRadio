package kr.jinsu.radio;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.media3.common.*;
import androidx.media3.session.*;
import androidx.media3.session.MediaController;
import com.google.common.util.concurrent.ListenableFuture;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
 int BG,INK,MUTED,ACCENT,LINE,RAISED,SURFACE,RIPPLE;boolean dark;AlertDialog infoDialog,liveDialog;EditText liveNameInput,liveUrlInput;TextView liveFormStatus;String liveEditingId;int liveRequestTicket;Future<?> liveCheck;final ExecutorService liveIo=Executors.newSingleThreadExecutor();PopupMenu appMenu;IconButton menuButton;String sourceTab="radio";
 final Handler handler=new Handler(Looper.getMainLooper());final ExecutorService scheduleIo=Executors.newFixedThreadPool(3);final Map<String,TextView> rowPrograms=new HashMap<>();final Map<String,ProgramTimeRow> rowTimeProgress=new HashMap<>();
 android.content.SharedPreferences prefs;Set<String> favorites;RadioData.Station selected;
 MediaController controller;ListenableFuture<MediaController> future;boolean loading=false,dead=false,foreground=false;int generation=0;String error="",screen="list",query="",sortOrder="frequency";boolean onlyFavorites=false;
 android.animation.AnimatorSet screenTransition;int transitionGeneration;boolean closingPlayer=false;static final long PLAYER_TRANSITION_MS=420;
 boolean wide;LinearLayout stationPane,detailPane,stationList,scheduleListView;ScrollView stationScroll;FrameLayout contentFrame;LinearLayout screenPanel;Bitmap listBackdrop;int listReturnScrollY;boolean restoreListPosition;int listRenderGeneration;LinearLayout root,body,list,mini,playerContent;View screenHeader;ScrollView scroll;ProgramProgressView miniProgress;TextView miniTitle,miniSubtitle,playerStatus,programTitle,programTime,programDescription;IconButton miniPlay,fullPlay,favoriteButton;TextView frequencyHeader,nameHeader;boolean scheduleCenterPending;int scheduleRenderGeneration;final Map<String,List<RadioData.Program>> schedules=new HashMap<>();final Map<String,String> scheduleErrors=new HashMap<>();final Set<String> scheduleLoading=new HashSet<>();final Map<String,Long> scheduleFetched=new HashMap<>();LocalDate scheduleDate=LocalDate.now(RadioData.SEOUL);String lastProgram="";long lastRefresh=0;
 final Runnable ticker=new Runnable(){public void run(){if(!dead){refreshProgram();if(foreground&&System.currentTimeMillis()-lastRefresh>60000){lastRefresh=System.currentTimeMillis();loadListedSchedules();refreshProgram();}if(foreground)refreshRows();handler.postDelayed(this,1000);}}};
 @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().getDecorView().setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);prefs=getSharedPreferences("radio",MODE_PRIVATE);LiveStationStore.reload(prefs);sourceTab=prefs.getString("source_tab","radio");onlyFavorites="favorites".equals(sourceTab);sortOrder=prefs.getString("sort_order","frequency");favorites=new HashSet<>(prefs.getStringSet("favorites",Collections.emptySet()));selected=RadioData.find(prefs.getString("station","kbs25"));if(state!=null){sourceTab=state.getString("source_tab",sourceTab);screen=state.getString("screen","list");onlyFavorites=state.getBoolean("favorites");query=state.getString("query","");listReturnScrollY=state.getInt("list_scroll_y",0);restoreListPosition=true;}if(getIntent().getBooleanExtra("player",false))screen="player";prefs.edit().putBoolean(PlaybackService.NAV_FAVORITES_KEY,onlyFavorites).apply();build();connect();loadSchedule(selected,false);handler.post(ticker);if(state!=null&&state.getBoolean("live_editor",false))showLiveEditor(state.getString("live_edit_id"),state.getString("live_edit_name",""),state.getString("live_edit_url",""));}
 @Override public void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putString("source_tab",sourceTab);if(liveDialog!=null&&liveDialog.isShowing()){b.putBoolean("live_editor",true);b.putString("live_edit_id",liveEditingId);b.putString("live_edit_name",liveNameInput.getText().toString());b.putString("live_edit_url",liveUrlInput.getText().toString());}b.putString("screen",screen);b.putBoolean("favorites",onlyFavorites);b.putString("query",query);b.putInt("list_scroll_y",stationScroll!=null&&!restoreListPosition?stationScroll.getScrollY():listReturnScrollY);}
 @Override protected void onResume(){super.onResume();foreground=true;syncStation();refreshProgram();}
 @Override protected void onPause(){foreground=false;cancelLiveCheck();super.onPause();}
 @Override public void onConfigurationChanged(Configuration c){
  boolean reopenLive=liveDialog!=null&&liveDialog.isShowing();String draftId=liveEditingId,draftName=reopenLive?liveNameInput.getText().toString():"",draftUrl=reopenLive?liveUrlInput.getText().toString():"";if(reopenLive)liveDialog.dismiss();
  boolean themeChanged=dark!=((c.uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES);
  int contentY=scroll==null?0:scroll.getScrollY();boolean preserveContent=!screen.equals("list")||wide;
  boolean reopenInfo=themeChanged&&infoDialog!=null&&infoDialog.isShowing();
  if(reopenInfo)infoDialog.dismiss();
  if(stationScroll!=null&&!restoreListPosition){listReturnScrollY=stationScroll.getScrollY();restoreListPosition=true;}
  super.onConfigurationChanged(c);if(themeChanged)listBackdrop=null;build();
  if(themeChanged&&preserveContent&&scroll!=null){scheduleCenterPending=false;scheduleRenderGeneration++;ScrollView target=scroll;NextDraw.add(target,()->{if(!dead&&scroll==target)target.scrollTo(0,contentY);});}
  if(reopenInfo)info();if(reopenLive)showLiveEditor(draftId,draftName,draftUrl);
 }
 @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);if(i.getBooleanExtra("player",false)){screen="player";build();}}
 @Override protected void onDestroy(){dead=true;cancelLiveCheck();if(liveDialog!=null)liveDialog.dismiss();liveIo.shutdownNow();if(infoDialog!=null)infoDialog.dismiss();cancelScreenTransition();listBackdrop=null;generation++;handler.removeCallbacksAndMessages(null);scheduleIo.shutdownNow();if(future!=null)MediaController.releaseFuture(future);super.onDestroy();}
 void connect(){SessionToken token=new SessionToken(this,new ComponentName(this,PlaybackService.class));Bundle controllerHints=new Bundle();controllerHints.putBoolean(PlaybackService.APP_CONTROLLER_KEY,true);future=new MediaController.Builder(this,token).setConnectionHints(controllerHints).buildAsync();future.addListener(()->{try{if(dead)return;controller=future.get();MediaItem item=controller.getCurrentMediaItem();if(item!=null){selected=RadioData.find(item.mediaId);loadSchedule(selected,false);build();}controller.addListener(new Player.Listener(){@Override public void onEvents(Player p,Player.Events events){syncStation();updateState();}@Override public void onPlayerError(PlaybackException e){error="연결이 끊겼습니다. 재생 버튼을 눌러 다시 연결하세요";loading=false;updateState();}});updateState();}catch(Exception e){error="재생기를 연결하지 못했습니다. 앱을 다시 열어주세요";updateState();}},command -> handler.post(command));}
 int dp(float x){return (int)(getResources().getDisplayMetrics().density*x+.5f);}
 LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
 LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
 void pad(View v,int a,int b,int c,int d){v.setPadding(dp(a),dp(b),dp(c),dp(d));}
 TextView text(String str,int size,int color){TextView t=new TextView(this);t.setText(str);t.setTextSize(size);t.setTextColor(color);t.setIncludeFontPadding(false);t.setLineSpacing(dp(3),1);return t;}
 GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
 void touch(View v,int color,int radius){v.setBackground(new RippleDrawable(android.content.res.ColorStateList.valueOf(RIPPLE),shape(color,radius),null));}
 void space(LinearLayout l,int h){l.addView(new View(this),new LinearLayout.LayoutParams(1,dp(h)));}
 TextView action(String label,Runnable run){TextView t=text(label,15,ACCENT);t.setGravity(Gravity.CENTER);t.setMinHeight(dp(48));pad(t,16,8,16,8);touch(t,RAISED,10);t.setOnClickListener(v->run.run());return t;}
 void line(LinearLayout l){View v=new View(this);v.setBackgroundColor(LINE);l.addView(v,new LinearLayout.LayoutParams(-1,dp(1)));}
 void applySystemTheme(){
  dark=(getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;
  setTheme(R.style.AppTheme);getTheme().applyStyle(R.style.AppTheme,true);if(Build.VERSION.SDK_INT>=29)getTheme().rebase();
  BG=getColor(R.color.radio_background);INK=getColor(R.color.radio_text);MUTED=getColor(R.color.radio_secondary);ACCENT=getColor(R.color.radio_accent);LINE=getColor(R.color.radio_line);RAISED=getColor(R.color.radio_raised);SURFACE=getColor(R.color.radio_surface);RIPPLE=getColor(R.color.radio_ripple);
  Window window=getWindow();window.setBackgroundDrawable(new ColorDrawable(BG));window.setStatusBarColor(BG);window.setNavigationBarColor(SURFACE);
  int appearance=window.getDecorView().getSystemUiVisibility();int lightBars=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
  window.getDecorView().setSystemUiVisibility(dark?appearance&~lightBars:appearance|lightBars);
  if(Build.VERSION.SDK_INT>=30){WindowInsetsController bars=window.getInsetsController();if(bars!=null){int mask=WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;bars.setSystemBarsAppearance(dark?0:mask,mask);}}
 }
 void build(){applySystemTheme();if(screen.equals("schedule")&&!selected.hasPrograms())screen="player";if(stationScroll!=null&&!restoreListPosition){listReturnScrollY=stationScroll.getScrollY();restoreListPosition=true;}wide=AdaptiveLayout.twoPane(getResources().getConfiguration().screenWidthDp,getResources().getConfiguration().screenHeightDp);stationPane=null;detailPane=null;stationList=null;stationScroll=null;scheduleListView=null;list=null;scroll=null;miniProgress=null;if(closingPlayer&&screen.equals("player")){screen="list";restoreListPosition=true;}if(screen.equals("list"))listBackdrop=null;cancelScreenTransition();rowPrograms.clear();rowTimeProgress.clear();miniPlay=null;fullPlay=null;playerStatus=null;programTitle=null;programTime=null;programDescription=null;miniTitle=null;miniSubtitle=null;favoriteButton=null;
  root=column();root.setBackgroundColor(BG);setContentView(root);root.setOnApplyWindowInsetsListener((v,in)->{if(Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=in.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());root.setPadding(bars.left,bars.top,bars.right,bars.bottom);}else root.setPadding(in.getSystemWindowInsetLeft(),in.getSystemWindowInsetTop(),in.getSystemWindowInsetRight(),in.getSystemWindowInsetBottom());return in;});root.requestApplyInsets();
  contentFrame=new FrameLayout(this);contentFrame.setClipChildren(true);root.addView(contentFrame,new LinearLayout.LayoutParams(-1,0,1));if(listBackdrop!=null&&screen.equals("player"))contentFrame.setBackground(new BitmapDrawable(getResources(),listBackdrop));SwipeLayout playerGestures=new SwipeLayout(this,SwipeLayout.HORIZONTAL,direction->{if((screen.equals("player")||wide&&!screen.equals("schedule"))&&!closingPlayer)step(direction);});playerGestures.setSwipeEnabled(!wide&&screen.equals("player"));screenPanel=playerGestures;screenPanel.setBackgroundColor(BG);contentFrame.addView(screenPanel,new FrameLayout.LayoutParams(-1,-1));
  LinearLayout header=row();pad(header,16,8,16,8);
  if(!wide&&!screen.equals("list")){IconButton back=new IconButton("back","방송 목록으로",this::navigateBack);header.addView(back,new LinearLayout.LayoutParams(dp(48),dp(48)));}
  TextView title=text(wide||screen.equals("list")?"온라디오":screen.equals("schedule")?"오늘의 편성표":"지금 듣는 방송",wide||screen.equals("list")?28:20,INK);header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
  if(wide||screen.equals("list")){menuButton=new IconButton("more","메뉴",()->showAppMenu(menuButton));header.addView(menuButton,new LinearLayout.LayoutParams(dp(48),dp(48)));}
  else if(screen.equals("player")){favoriteButton=new IconButton(favorites.contains(selected.id)?"starFilled":"star","즐겨찾기 전환",()->{toggleFavorite(selected);build();});header.addView(favoriteButton,new LinearLayout.LayoutParams(dp(48),dp(48)));}
  screenHeader=header;screenPanel.addView(header);body=column();screenPanel.addView(body,new LinearLayout.LayoutParams(-1,0,1));
  if(wide)buildWide();else if(screen.equals("list"))buildList();else if(screen.equals("player"))buildPlayer();else buildSchedule();
  buildMini();updateState();refreshProgram();
 }
 boolean listVisible(){return wide||screen.equals("list");}
 void buildWide(){
  LinearLayout outer=body,panes=row();panes.setGravity(Gravity.TOP);outer.addView(panes,new LinearLayout.LayoutParams(-1,-1));
  stationPane=column();panes.addView(stationPane,new LinearLayout.LayoutParams(dp(AdaptiveLayout.stationPaneWidth(getResources().getConfiguration().screenWidthDp)),-1));
  View divider=new View(this);divider.setBackgroundColor(LINE);panes.addView(divider,new LinearLayout.LayoutParams(dp(1),-1));
  detailPane=new SwipeLayout(this,SwipeLayout.HORIZONTAL,direction->{if(!screen.equals("schedule"))step(direction);});((SwipeLayout)detailPane).setSwipeEnabled(!screen.equals("schedule"));panes.addView(detailPane,new LinearLayout.LayoutParams(0,-1,1));body=stationPane;buildList();body=detailPane;
  CappedLayout heading=new CappedLayout(this,AdaptiveLayout.DETAIL_MAX_WIDTH);LinearLayout headingRow=row();pad(headingRow,16,4,16,8);
  if(screen.equals("schedule"))headingRow.addView(new IconButton("back","재생정보로",this::navigateBack),new LinearLayout.LayoutParams(dp(48),dp(48)));
  TextView name=text(screen.equals("schedule")?"오늘의 편성표":"지금 듣는 방송",20,INK);headingRow.addView(name,new LinearLayout.LayoutParams(0,dp(48),1));name.setGravity(Gravity.CENTER_VERTICAL);
  if(!screen.equals("schedule")){favoriteButton=new IconButton(favorites.contains(selected.id)?"starFilled":"star","즐겨찾기 전환",()->{toggleFavorite(selected);build();});headingRow.addView(favoriteButton,new LinearLayout.LayoutParams(dp(48),dp(48)));}
  heading.addView(headingRow);FrameLayout headerFrame=new FrameLayout(this);headerFrame.addView(heading,new FrameLayout.LayoutParams(-1,-2,Gravity.CENTER_HORIZONTAL));detailPane.addView(headerFrame);
  if(screen.equals("schedule"))buildSchedule();else buildPlayer();body=outer;
 }
 void buildList(){
  LinearLayout tabs=row();pad(tabs,wide?12:16,4,wide?12:16,12);String active=onlyFavorites?"favorites":sourceTab;
  for(String key:new String[]{"radio","live","favorites"}){String label=key.equals("radio")?"라디오":key.equals("live")?"유튜브 라이브":"즐겨찾기";TextView tab=action(label,()->selectSourceTab(key));tab.setTextSize(14);tab.setSingleLine();tab.setEllipsize(TextUtils.TruncateAt.END);pad(tab,4,8,4,8);tab.setTextColor(key.equals(active)?BG:MUTED);touch(tab,key.equals(active)?INK:RAISED,10);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,dp(48),key.equals("live")?1.45f:1);if(!key.equals("favorites"))tp.setMarginEnd(dp(6));tabs.addView(tab,tp);}body.addView(tabs);
  EditText search=new EditText(this);search.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);search.setSingleLine();search.setTextSize(16);search.setTextColor(INK);search.setHintTextColor(MUTED);search.setHint("방송명 · 주파수 검색");pad(search,16,8,16,8);search.setBackground(shape(RAISED,10));search.setText(query);LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(-1,dp(48));ep.setMargins(dp(wide?16:24),0,dp(wide?16:24),dp(12));body.addView(search,ep);search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){query=s.toString();populate(false);}public void afterTextChanged(Editable e){}});
  LinearLayout sorting=row();pad(sorting,wide?24:32,0,wide?16:24,0);frequencyHeader=text("",13,INK);nameHeader=text("",13,INK);for(TextView label:new TextView[]{frequencyHeader,nameHeader}){label.setMinHeight(dp(48));label.setGravity(Gravity.CENTER_VERTICAL);touch(label,BG,0);}sorting.addView(frequencyHeader,new LinearLayout.LayoutParams(dp(76),dp(48)));sorting.addView(nameHeader,new LinearLayout.LayoutParams(0,dp(48),1));sorting.addView(new View(this),new LinearLayout.LayoutParams(dp(48),1));frequencyHeader.setOnClickListener(v->changeSort("frequency"));nameHeader.setOnClickListener(v->changeSort("name"));sorting.setVisibility(!onlyFavorites&&"live".equals(sourceTab)?View.GONE:View.VISIBLE);body.addView(sorting);updateSortLabels();

  scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);list=column();stationList=list;stationScroll=scroll;pad(list,wide?16:24,0,wide?16:24,16);scroll.addView(list);body.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));populate();
 }
 void changeSort(String column){sortOrder=UiRules.nextSort(sortOrder,column);prefs.edit().putString("sort_order",sortOrder).apply();updateSortLabels();populate(false);}
 void updateSortLabels(){if(frequencyHeader==null||nameHeader==null)return;frequencyHeader.setText(UiRules.sortLabel(sortOrder,"frequency","주파수"));nameHeader.setText(UiRules.sortLabel(sortOrder,"name","방송국명"));frequencyHeader.setContentDescription("주파수 정렬"+(UiRules.active(sortOrder,"frequency")?(sortOrder.endsWith("_desc")?" 내림차순":" 오름차순"):""));nameHeader.setContentDescription("방송국명 정렬"+(UiRules.active(sortOrder,"name")?(sortOrder.endsWith("_desc")?" 내림차순":" 오름차순"):""));}
 void populate(){populate(true);}
 void populate(boolean preservePosition){if(stationList==null||!listVisible())return;if(!preservePosition)listReturnScrollY=0;else if(!restoreListPosition)listReturnScrollY=stationScroll.getScrollY();restoreListPosition=true;final int render=++listRenderGeneration;final ScrollView targetScroll=stationScroll;final int savedY=listReturnScrollY;stationList.removeAllViews();rowPrograms.clear();rowTimeProgress.clear();int count=0;String q=query.trim().toLowerCase(Locale.ROOT);
  for(RadioData.Station s:RadioData.orderedStations(sortOrder)){if(onlyFavorites&&!favorites.contains(s.id))continue;if(!onlyFavorites&&(("live".equals(sourceTab))==s.hasPrograms()))continue;if(!(s.name+" "+s.fm+" "+s.brand).toLowerCase(Locale.ROOT).contains(q))continue;count++;
   LinearLayout row=row();row.setMinimumHeight(dp(88));pad(row,8,12,0,12);touch(row,s.id.equals(selected.id)?RAISED:BG,10);
   TextView f=text(s.displayFrequency(),s.hasPrograms()?22:17,s.id.equals(selected.id)?ACCENT:INK);f.setTypeface(android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL));f.setGravity(Gravity.CENTER_VERTICAL);row.addView(f,new LinearLayout.LayoutParams(dp(76),-2));
   LinearLayout names=column();TextView n=text(s.name,16,INK);names.addView(n);if(s.hasPrograms()){space(names,6);TextView desc=text("방송정보 확인 중",14,MUTED);desc.setMaxLines(2);desc.setEllipsize(TextUtils.TruncateAt.END);names.addView(desc);rowPrograms.put(s.id,desc);space(names,4);ProgramTimeRow timeProgress=new ProgramTimeRow(this);names.addView(timeProgress,new LinearLayout.LayoutParams(-1,-2));rowTimeProgress.put(s.id,timeProgress);}row.addView(names,new LinearLayout.LayoutParams(0,-2,1));
   IconButton star=new IconButton(favorites.contains(s.id)?"starFilled":"star",s.name+" 즐겨찾기 "+(favorites.contains(s.id)?"해제":"추가"),()->{toggleFavorite(s);populate();});row.addView(star,new LinearLayout.LayoutParams(dp(48),dp(48)));row.setOnClickListener(v->{hideKeyboard();play(s);populate();});stationList.addView(row);line(stationList);
  }
  if(count==0){space(stationList,40);TextView empty=text(onlyFavorites&&q.isEmpty()?"즐겨찾는 방송을 담아보세요":"검색 결과가 없습니다",19,INK);stationList.addView(empty);space(stationList,12);stationList.addView(text(onlyFavorites?"전체 방송에서 별표를 누르면 여기에 표시됩니다":"방송명이나 FM 주파수로 다시 검색해 보세요",14,MUTED));}
  space(stationList,16);stationList.addView(text("주파수는 지역별 대표 주파수 · 인터넷 데이터 사용",12,MUTED));refreshRows();loadListedSchedules();NextDraw.add(targetScroll,()->{if(!dead&&render==listRenderGeneration&&listVisible()&&stationScroll==targetScroll){targetScroll.scrollTo(0,savedY);restoreListPosition=false;}});
 }
 void toggleFavorite(RadioData.Station s){if(!favorites.add(s.id))favorites.remove(s.id);prefs.edit().putStringSet("favorites",new HashSet<>(favorites)).apply();if(favoriteButton!=null&&s.id.equals(selected.id)){favoriteButton.icon=favorites.contains(s.id)?"starFilled":"star";favoriteButton.setContentDescription(s.name+" 즐겨찾기 "+(favorites.contains(s.id)?"해제":"추가"));favoriteButton.invalidate();}}
 void buildMini(){line(root);LinearLayout dock=new SwipeLayout(this,SwipeLayout.UP,direction->openPlayer());dock.setBackgroundColor(SURFACE);miniProgress=new ProgramProgressView(this);dock.addView(miniProgress,new LinearLayout.LayoutParams(-1,dp(3)));dock.setOnClickListener(v->openPlayer());mini=row();pad(mini,16,8,12,8);mini.setMinimumHeight(dp(80));touch(mini,SURFACE,0);
  LinearLayout title=column();miniTitle=text(selected.name,15,INK);miniSubtitle=text("재생할 방송을 선택하세요",12,MUTED);miniSubtitle.setMaxLines(2);miniSubtitle.setEllipsize(TextUtils.TruncateAt.END);title.addView(miniTitle);space(title,6);title.addView(miniSubtitle);mini.addView(title,new LinearLayout.LayoutParams(0,-2,1));title.setOnClickListener(v->openPlayer());mini.setOnClickListener(v->openPlayer());
  mini.addView(new IconButton("previous","이전 방송",()->step(-1)),new LinearLayout.LayoutParams(dp(48),dp(48)));miniPlay=new IconButton("play","재생",this::toggle);touch(miniPlay,RAISED,24);mini.addView(miniPlay,new LinearLayout.LayoutParams(dp(48),dp(48)));mini.addView(new IconButton("next","다음 방송",()->step(1)),new LinearLayout.LayoutParams(dp(48),dp(48)));
  dock.setId(R.id.playback_bar);if(wide){CappedLayout dockContent=new CappedLayout(this,AdaptiveLayout.DOCK_MAX_WIDTH);dockContent.addView(mini,new LinearLayout.LayoutParams(-1,-2));FrameLayout dockFrame=new FrameLayout(this);dockFrame.addView(dockContent,new FrameLayout.LayoutParams(-1,-2,Gravity.CENTER_HORIZONTAL));dock.addView(dockFrame);}else dock.addView(mini,new LinearLayout.LayoutParams(-1,-2));root.addView(dock,new LinearLayout.LayoutParams(-1,-2));
 }
 void cancelScreenTransition(){transitionGeneration++;if(screenTransition!=null){screenTransition.removeAllListeners();screenTransition.cancel();screenTransition=null;}closingPlayer=false;}
 void animatePlayer(boolean opening){
  cancelScreenTransition();closingPlayer=!opening;final int ticket=transitionGeneration;
  final LinearLayout panel=screenPanel;final FrameLayout frame=contentFrame;
  Runnable start=()->{
   if(dead||ticket!=transitionGeneration||screenPanel!=panel||!screen.equals("player"))return;
   float distance=frame.getHeight();if(distance<=0){if(!opening){screen="list";build();}return;}
   if(opening)panel.setTranslationY(distance);
   android.animation.AnimatorSet transition=new android.animation.AnimatorSet();screenTransition=transition;
   transition.playTogether(android.animation.ObjectAnimator.ofFloat(panel,View.TRANSLATION_Y,opening?0:distance));
   float fraction=Math.abs((opening?0:distance)-panel.getTranslationY())/distance;
   transition.setDuration(Math.max(120,Math.round(PLAYER_TRANSITION_MS*fraction)));
   transition.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
   transition.addListener(new android.animation.AnimatorListenerAdapter(){@Override public void onAnimationEnd(android.animation.Animator animation){if(screenTransition!=transition||dead)return;screenTransition=null;closingPlayer=false;if(!opening&&screen.equals("player")){screen="list";restoreListPosition=true;listBackdrop=null;build();}}});
   transition.start();
  };
  if(opening)NextDraw.add(frame,start);else start.run();
 }
 void captureListBackdrop(){listReturnScrollY=screen.equals("list")&&scroll!=null?scroll.getScrollY():0;listBackdrop=null;if(!screen.equals("list")||screenPanel==null||screenPanel.getWidth()<=0||screenPanel.getHeight()<=0)return;listBackdrop=Bitmap.createBitmap(screenPanel.getWidth(),screenPanel.getHeight(),Bitmap.Config.RGB_565);screenPanel.draw(new Canvas(listBackdrop));}
 void openPlayer(){if(wide){hideKeyboard();if(screen.equals("schedule")){screen="list";build();}else if(scroll!=null)scroll.smoothScrollTo(0,0);return;}if(screen.equals("player"))return;hideKeyboard();captureListBackdrop();screen="player";build();if(android.animation.ValueAnimator.areAnimatorsEnabled())animatePlayer(true);}
 void navigateBack(){if(wide){if(screen.equals("schedule")){screen="list";build();}else super.onBackPressed();return;}if(closingPlayer)return;if(screen.equals("player")){if(android.animation.ValueAnimator.areAnimatorsEnabled())animatePlayer(false);else{screen="list";restoreListPosition=true;build();}}else if(screen.equals("schedule")){screen="player";build();}else super.onBackPressed();}
 void buildPlayer(){scroll=new ScrollView(this);scroll.setFillViewport(true);playerContent=wide?new CappedLayout(this,AdaptiveLayout.DETAIL_MAX_WIDTH):column();pad(playerContent,24,8,24,24);scroll.addView(playerContent,new ScrollView.LayoutParams(-1,-2,Gravity.CENTER_HORIZONTAL));body.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  TextView brand=text(selected.brand+(selected.hasPrograms()?" RADIO":" LIVE"),13,MUTED);playerContent.addView(brand);space(playerContent,12);
  LinearLayout dial=column();pad(dial,24,28,24,20);dial.setBackground(shape(RAISED,16));TextView freq=text(selected.displayFrequency(),64,INK);freq.setTypeface(android.graphics.Typeface.create("sans-serif-light",android.graphics.Typeface.NORMAL));dial.addView(freq);space(dial,4);dial.addView(text(selected.hasPrograms()?"MHz   /   수도권 FM":"실시간 오디오",13,MUTED));if(selected.hasPrograms()){space(dial,16);dial.addView(new DialView(),new LinearLayout.LayoutParams(-1,dp(40)));}playerContent.addView(dial);
  space(playerContent,24);playerContent.addView(text(selected.name,28,INK));space(playerContent,8);playerStatus=text("",14,ACCENT);playerStatus.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);playerContent.addView(playerStatus);

  if(selected.hasPrograms()){space(playerContent,28);line(playerContent);space(playerContent,20);playerContent.addView(text("방송정보",13,MUTED));space(playerContent,12);programTitle=text("방송정보 확인 중",21,INK);playerContent.addView(programTitle);space(playerContent,8);programTime=text("",13,ACCENT);playerContent.addView(programTime);space(playerContent,12);programDescription=text(selected.description,14,MUTED);playerContent.addView(programDescription);space(playerContent,20);}
  LinearLayout links=row();pad(links,24,12,24,16);links.setBackgroundColor(BG);TextView schedule=action("오늘의 편성표",()->{screen="schedule";build();loadSchedule(selected,false);});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMarginEnd(dp(8));if(selected.hasPrograms())links.addView(schedule,p);if(!selected.hasPrograms()){TextView edit=action("라이브 주소 수정",()->showLiveEditor(selected));links.addView(edit,p);}links.addView(action("방송 홈페이지",()->browse(selected.website)),new LinearLayout.LayoutParams(0,-2,1));if(wide){CappedLayout actions=new CappedLayout(this,AdaptiveLayout.DETAIL_MAX_WIDTH);actions.addView(links);FrameLayout actionFrame=new FrameLayout(this);actionFrame.addView(actions,new FrameLayout.LayoutParams(-1,-2,Gravity.CENTER_HORIZONTAL));body.addView(actionFrame,new LinearLayout.LayoutParams(-1,-2));}else body.addView(links,new LinearLayout.LayoutParams(-1,-2));playerContent.addView(text("실시간 인터넷 방송 · 방송사 사정에 따라 지연될 수 있습니다",12,MUTED));
 }
 void buildSchedule(){scheduleCenterPending=true;LinearLayout heading=column();pad(heading,24,8,24,16);heading.addView(text(selected.name,22,INK));space(heading,8);heading.addView(text(LocalDate.now(RadioData.SEOUL).format(DateTimeFormatter.ofPattern("M월 d일 EEEE",Locale.KOREAN))+" · 한국시간",14,MUTED));body.addView(heading);
  scroll=new ScrollView(this);list=wide?new CappedLayout(this,AdaptiveLayout.DETAIL_MAX_WIDTH):column();scheduleListView=list;pad(list,24,0,24,24);scroll.addView(list,new ScrollView.LayoutParams(-1,-2,Gravity.CENTER_HORIZONTAL));body.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));renderSchedule();
 }
 void renderSchedule(){if(!screen.equals("schedule")||list==null||list!=scheduleListView)return;int oldY=scroll.getScrollY();int ticket=++scheduleRenderGeneration;View currentRow=null;list.removeAllViews();List<RadioData.Program> items=schedules.get(selected.id);
  if(items!=null&&!items.isEmpty()){for(RadioData.Program p:items){LinearLayout r=column();pad(r,16,16,16,16);boolean now=p.current(System.currentTimeMillis());if(now)currentRow=r;r.setBackground(shape(now?RAISED:BG,10));r.addView(text(p.time()+(now?"   방송 중":""),13,now?ACCENT:MUTED));space(r,8);r.addView(text(p.title,17,INK));if(!p.host.isEmpty()){space(r,6);r.addView(text(p.host,13,MUTED));}if(p.link.startsWith("https://"))r.setOnClickListener(v->browse(p.link));list.addView(r);line(list);}}
  else {space(list,20);list.addView(text(scheduleLoading.contains(selected.id)?"편성표를 가져오는 중입니다":scheduleErrors.getOrDefault(selected.id,"이 채널은 공식 편성표에서 확인할 수 있습니다"),16,MUTED));space(list,20);list.addView(action("다시 불러오기",()->loadSchedule(selected,true)));}
  space(list,20);list.addView(action("방송사 공식 편성표 열기",()->browse(selected.schedule)));space(list,12);list.addView(text("편성은 방송사 사정에 따라 변경될 수 있습니다",12,MUTED));
  if(scheduleCenterPending&&currentRow!=null)centerCurrentProgram(currentRow,ticket);else {ScrollView scheduleScroll=scroll;scheduleScroll.post(()->{if(ticket==scheduleRenderGeneration&&screen.equals("schedule")&&scroll==scheduleScroll)scheduleScroll.scrollTo(0,oldY);});}
 }
 void centerCurrentProgram(View target,int ticket){ScrollView scheduleScroll=scroll;LinearLayout scheduleList=list;NextDraw.add(scheduleScroll,()->{if(dead||ticket!=scheduleRenderGeneration||!screen.equals("schedule")||scroll!=scheduleScroll||list!=scheduleList||scheduleScroll.getHeight()<=0)return;scheduleList.setPadding(dp(24),scheduleScroll.getHeight()/2,dp(24),scheduleScroll.getHeight()/2);NextDraw.add(scheduleScroll,()->{if(dead||ticket!=scheduleRenderGeneration||!screen.equals("schedule")||scroll!=scheduleScroll||list!=scheduleList)return;scheduleScroll.scrollTo(0,UiRules.centeredScrollY(target.getTop(),target.getHeight(),scheduleScroll.getHeight()));scheduleCenterPending=false;});});}
 void loadSchedule(RadioData.Station s,boolean force){if(!s.hasPrograms())return;LocalDate date=LocalDate.now(RadioData.SEOUL);if(!date.equals(scheduleDate)){schedules.clear();scheduleErrors.clear();scheduleDate=date;}
  if(scheduleLoading.contains(s.id))return;if(!force&&schedules.containsKey(s.id)&&System.currentTimeMillis()-scheduleFetched.getOrDefault(s.id,0L)<300000){refreshProgram();return;}scheduleLoading.add(s.id);if(screen.equals("schedule")&&selected.id.equals(s.id))renderSchedule();
  scheduleIo.execute(()->{List<RadioData.Program> result=null;String fail="";try{result=RadioData.schedule(s,date);}catch(Exception e){fail="편성정보를 가져오지 못했습니다. 공식 편성표를 확인해 주세요";}List<RadioData.Program> data=result;String reason=fail;handler.post(()->{if(dead)return;scheduleLoading.remove(s.id);if(!date.equals(scheduleDate))return;if(data!=null){schedules.put(s.id,data);scheduleFetched.put(s.id,System.currentTimeMillis());}if(!reason.isEmpty())scheduleErrors.put(s.id,reason);else scheduleErrors.remove(s.id);refreshRows();if(selected.id.equals(s.id)){refreshProgram();renderSchedule();}});});
 }
 void loadListedSchedules(){if(!listVisible()){loadSchedule(selected,false);return;}for(RadioData.Station s:RadioData.stations())if(rowPrograms.containsKey(s.id))loadSchedule(s,false);if(wide)loadSchedule(selected,false);}
 void refreshRows(){if(!listVisible())return;long now=System.currentTimeMillis();for(RadioData.Station s:RadioData.stations()){TextView title=rowPrograms.get(s.id);if(title==null)continue;RadioData.Program program=null;List<RadioData.Program> items=schedules.get(s.id);if(items!=null)for(RadioData.Program item:items)if(item.current(now)){program=item;break;}String name=program!=null?program.title:scheduleLoading.contains(s.id)?"방송정보 확인 중":scheduleErrors.containsKey(s.id)?"방송정보 조회 실패":"현재 프로그램 정보 미제공";if(!TextUtils.equals(title.getText(),name))title.setText(name);ProgramTimeRow timeProgress=rowTimeProgress.get(s.id);if(timeProgress!=null)timeProgress.update(program,now);}}
 void refreshProgram(){if(selected==null)return;RadioData.Program p=RadioData.current(schedules.get(selected.id));String name=p==null?(scheduleLoading.contains(selected.id)?"방송정보 확인 중":"현재 프로그램 정보 미제공"):p.title;
  if(programTitle!=null)programTitle.setText(name);if(programTime!=null){programTime.setText(p==null?"공식 편성표에서 확인할 수 있습니다":p.time()+(p.host.isEmpty()?"":" · "+p.host));}if(programDescription!=null)programDescription.setText(p!=null&&!p.description.isEmpty()?p.description:selected.description);

  updateState();
 }
 void play(RadioData.Station s){if(controller==null){Toast.makeText(this,"재생기 연결 중입니다. 잠시 후 다시 눌러주세요",Toast.LENGTH_SHORT).show();return;}if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED&&!prefs.getBoolean("notificationAsked",false)){prefs.edit().putBoolean("notificationAsked",true).apply();requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},41);}
  final int request=++generation;selected=s;loading=true;error="";lastProgram="";if(wide||screen.equals("player")||screen.equals("schedule"))build();updateState();loadSchedule(s,false);
  Bundle args=new Bundle();args.putString("station_id",s.id);com.google.common.util.concurrent.ListenableFuture<SessionResult> result=controller.sendCustomCommand(PlaybackService.PLAY_STATION_COMMAND,args);
  result.addListener(()->{if(dead||request!=generation)return;loading=false;try{SessionResult response=result.get();if(response.resultCode!=SessionResult.RESULT_SUCCESS)error="방송에 연결하지 못했습니다. 재생 버튼으로 다시 시도하세요";}catch(Exception failure){error="방송에 연결하지 못했습니다. 재생 버튼으로 다시 시도하세요";}syncStation();updateState();if(listVisible())populate();},command->handler.post(command));
 }
 void toggle(){if(loading){generation++;loading=false;if(controller!=null)controller.pause();updateState();return;}if(controller==null)return;if(controller.getPlayWhenReady()&&controller.getPlayerError()==null){controller.pause();updateState();}else if(controller.getPlaybackState()==Player.STATE_READY&&controller.getPlayerError()==null&&controller.getCurrentMediaItem()!=null){if(selected.hasPrograms())controller.play();else play(selected);}else play(selected);}
 void step(int delta){if(controller==null)return;generation++;loading=false;error="";if(delta>0)controller.seekToNext();else controller.seekToPrevious();updateState();}
 void syncStation(){if(controller==null||loading)return;MediaItem item=controller.getCurrentMediaItem();if(item!=null&&!item.mediaId.equals(selected.id)){selected=RadioData.find(item.mediaId);prefs.edit().putString("station",selected.id).apply();build();loadSchedule(selected,false);}}
 void updateState(){if(selected==null)return;if(controller!=null&&controller.getPlayerError()==null&&controller.isPlaying())error="";boolean wanted=controller!=null&&controller.getPlayWhenReady();boolean playing=controller!=null&&controller.isPlaying();boolean buffering=loading||(wanted&&controller.getPlaybackState()==Player.STATE_BUFFERING);String state=!error.isEmpty()?error:buffering?"연결 중":playing?"실시간 재생 중":wanted?"재생 준비 중":"일시정지";boolean pause=loading||(wanted&&controller.getPlayerError()==null&&error.isEmpty());
  for(IconButton b:new IconButton[]{miniPlay,fullPlay})if(b!=null){b.icon=pause?"pause":"play";b.setContentDescription(pause?"일시정지":"재생");b.invalidate();}
  if(playerStatus!=null)playerStatus.setText(state);if(miniTitle!=null)miniTitle.setText(selected.name+(selected.fm.isEmpty()?"":"  ·  "+selected.fm));if(miniSubtitle!=null){RadioData.Program p=RadioData.current(schedules.get(selected.id));String status=selected.hasPrograms()&&playing&&error.isEmpty()&&!buffering?"":state;miniSubtitle.setText(p==null?status:status.isEmpty()?p.title:status+" · "+p.title);}updateProgramProgress();
 }
 void updateProgramProgress(){
  if(!selected.hasPrograms()){if(miniProgress!=null)miniProgress.setVisibility(View.GONE);return;}
  RadioData.Program program=RadioData.current(schedules.get(selected.id));long now=System.currentTimeMillis();
  if(program==null&&controller!=null){MediaItem item=controller.getCurrentMediaItem();Bundle extras=item==null?null:item.mediaMetadata.extras;if(item!=null&&item.mediaId.equals(selected.id)&&extras!=null){long start=extras.getLong(ProgramProgress.START_KEY,0),end=extras.getLong(ProgramProgress.END_KEY,0);if(ProgramProgress.current(start,end,now))program=new RadioData.Program(String.valueOf(item.mediaMetadata.title),"","","",start,end);}}
  boolean known=program!=null&&ProgramProgress.current(program.start,program.end,now);int value=known?ProgramProgress.scaled(program.start,program.end,now):0;
  if(miniProgress!=null)miniProgress.update(value,known);

 }
 void hideKeyboard(){View focus=getCurrentFocus();if(focus!=null)((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(focus.getWindowToken(),0);}
 void browse(String url){try{Uri u=Uri.parse(url);if(!"https".equals(u.getScheme())&&!("http".equals(u.getScheme())&&"tbs.seoul.kr".equals(u.getHost()))){Toast.makeText(this,"방송 홈페이지에서 확인해 주세요",Toast.LENGTH_SHORT).show();return;}startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(Exception e){Toast.makeText(this,"링크를 열 앱을 찾지 못했습니다",Toast.LENGTH_SHORT).show();}}
 void selectSourceTab(String tab){sourceTab=tab;onlyFavorites="favorites".equals(tab);listReturnScrollY=0;restoreListPosition=true;query="";prefs.edit().putString("source_tab",tab).putBoolean(PlaybackService.NAV_FAVORITES_KEY,onlyFavorites).apply();build();}
 void showAppMenu(View anchor){appMenu=new PopupMenu(this,anchor);appMenu.getMenu().add(0,1,0,"유튜브 라이브 추가");appMenu.getMenu().add(0,2,1,"앱 정보");appMenu.setOnMenuItemClickListener(item->{if(item.getItemId()==1)showLiveEditor(null);else info();return true;});appMenu.show();}
 YoutubeLiveAudio.LiveInfo inspectLive(String url)throws Exception{return YoutubeLiveAudio.inspect(url);}
 void cancelLiveCheck(){liveRequestTicket++;if(liveCheck!=null){liveCheck.cancel(true);liveCheck=null;}if(liveDialog!=null&&liveDialog.isShowing()){if(liveNameInput!=null)liveNameInput.setEnabled(true);if(liveUrlInput!=null)liveUrlInput.setEnabled(true);android.widget.Button button=liveDialog.getButton(AlertDialog.BUTTON_POSITIVE);if(button!=null){button.setEnabled(true);button.setText(liveEditingId==null?"추가":"저장");}}}
 void showLiveEditor(RadioData.Station station){showLiveEditor(station==null?null:station.id,station==null?"":station.name,station==null?"":station.website);}
 void showLiveEditor(String editingId,String name,String url){
  if(liveDialog!=null)liveDialog.dismiss();cancelLiveCheck();liveEditingId=editingId;
  LinearLayout form=column();pad(form,24,8,24,8);form.addView(text("채널명",14,MUTED));space(form,8);
  liveNameInput=new EditText(this);liveNameInput.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);liveNameInput.setSingleLine();liveNameInput.setTextSize(16);liveNameInput.setTextColor(INK);liveNameInput.setHintTextColor(MUTED);liveNameInput.setHint("목록에 표시할 이름");liveNameInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(80)});liveNameInput.setText(name);form.addView(liveNameInput,new LinearLayout.LayoutParams(-1,dp(48)));space(form,16);
  form.addView(text("유튜브 라이브 주소",14,MUTED));space(form,8);liveUrlInput=new EditText(this);liveUrlInput.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);liveUrlInput.setSingleLine();liveUrlInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);liveUrlInput.setTextSize(15);liveUrlInput.setTextColor(INK);liveUrlInput.setHintTextColor(MUTED);liveUrlInput.setHint("https://youtu.be/…");liveUrlInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2048)});liveUrlInput.setText(url);form.addView(liveUrlInput,new LinearLayout.LayoutParams(-1,dp(48)));space(form,12);
  liveFormStatus=text("공개 방송 중인 라이브를 확인한 뒤 저장합니다",13,MUTED);liveFormStatus.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);form.addView(liveFormStatus);
  ScrollView formScroll=new ScrollView(this);formScroll.addView(form);
  AlertDialog.Builder builder=new AlertDialog.Builder(this).setTitle(editingId==null?"유튜브 라이브 추가":"라이브 주소 수정").setView(formScroll).setNegativeButton("취소",null).setPositiveButton(editingId==null?"추가":"저장",null);
  if(editingId!=null&&editingId.startsWith("user_"))builder.setNeutralButton("삭제",(d,w)->confirmRemoveLive(editingId));
  final AlertDialog dialog=builder.create();dialog.getWindow().getDecorView().setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);liveDialog=dialog;dialog.setOnDismissListener(d->{if(liveDialog==dialog){cancelLiveCheck();liveDialog=null;}});dialog.show();
  dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
   final String submittedName=liveNameInput.getText().toString().trim();final String page;
   if(!LiveStationStore.validName(submittedName)){liveNameInput.setError("채널명을 1∼80자로 입력해 주세요");return;}
   try{page=YoutubeLiveAudio.canonicalPage(liveUrlInput.getText().toString());}catch(Exception invalid){liveUrlInput.setError(invalid.getMessage());return;}
   cancelLiveCheck();final int ticket=++liveRequestTicket;dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);liveNameInput.setEnabled(false);liveUrlInput.setEnabled(false);liveFormStatus.setText("라이브 확인 중…");
   liveCheck=liveIo.submit(()->{YoutubeLiveAudio.LiveInfo checked=null;String failure="";try{checked=inspectLive(page);}catch(Exception unavailable){failure="라이브를 확인하지 못했습니다. 공개 방송 중인 주소인지 확인해 주세요";}YoutubeLiveAudio.LiveInfo result=checked;String message=failure;handler.post(()->finishLiveRegistration(dialog,ticket,editingId,submittedName,result,message));});
  });
 }
 void finishLiveRegistration(AlertDialog dialog,int ticket,String editingId,String name,YoutubeLiveAudio.LiveInfo result,String failure){
  if(dead||!foreground||ticket!=liveRequestTicket||liveDialog!=dialog||!dialog.isShowing())return;
  liveCheck=null;dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);liveNameInput.setEnabled(true);liveUrlInput.setEnabled(true);
  if(result==null){liveFormStatus.setText(failure);return;}
  try{
   RadioData.Station saved=LiveStationStore.save(prefs,editingId,name,result);
   if(editingId!=null&&selected.id.equals(saved.id)){generation++;loading=false;error="";pauseAndClear(controller);selected=saved;prefs.edit().putString("station",saved.id).apply();}
   dialog.dismiss();
   if(editingId==null){screen="list";sourceTab="live";onlyFavorites=false;query="";listReturnScrollY=0;restoreListPosition=true;prefs.edit().putString("source_tab","live").putBoolean(PlaybackService.NAV_FAVORITES_KEY,false).apply();}
   build();if(editingId==null&&stationScroll!=null){ScrollView target=stationScroll;NextDraw.add(target,()->{if(stationScroll==target)target.fullScroll(View.FOCUS_DOWN);});}
   Toast.makeText(this,editingId==null?"라이브를 추가했습니다":"저장했습니다. 재생을 누르면 변경한 주소로 연결합니다",Toast.LENGTH_SHORT).show();
  }catch(Exception invalid){liveFormStatus.setText(invalid.getMessage()==null?"라이브를 저장하지 못했습니다":invalid.getMessage());}
 }
 static void pauseAndClear(Player player){if(player!=null){player.pause();player.stop();player.clearMediaItems();}}
 void confirmRemoveLive(String id){new AlertDialog.Builder(this).setTitle("등록한 라이브 삭제").setMessage("이 라이브를 목록과 즐겨찾기에서 삭제합니다").setNegativeButton("취소",null).setPositiveButton("삭제",(d,w)->{try{LiveStationStore.remove(prefs,id);favorites=new HashSet<>(prefs.getStringSet("favorites",Collections.emptySet()));if(selected.id.equals(id)){generation++;loading=false;error="";pauseAndClear(controller);selected=RadioData.STATIONS.get(0);prefs.edit().putString("station",selected.id).apply();}build();}catch(Exception failure){Toast.makeText(this,"라이브를 삭제하지 못했습니다",Toast.LENGTH_SHORT).show();}}).show();}
 void info(){infoDialog=new AlertDialog.Builder(this).setTitle("온라디오").setMessage("0.1.0-dev19\n\n국내 FM 채널과 유튜브 라이브 오디오를 인터넷으로 듣습니다. 주파수는 수도권 기준이며 실제 수신 주파수와 무관합니다.\n\n즐겨찾기는 기기에 저장됩니다. 녹음이나 개인정보 수집 기능은 없습니다. 방송사는 이 앱과 제휴 관계가 아닙니다.\n\n디자인 참고: Unslop · Damir Sokolovsky · CC BY 4.0").setPositiveButton("확인",null).show();}
 @Override public void onBackPressed(){navigateBack();}
 class IconButton extends View {String icon;int color=INK;Paint paint=new Paint(3);IconButton(String icon,String label,Runnable run){super(MainActivity.this);this.icon=icon;setContentDescription(label);setFocusable(true);setClickable(true);touch(this,BG,24);setOnClickListener(v->run.run());} @Override public CharSequence getAccessibilityClassName(){return "android.widget.Button";} @Override protected void onDraw(Canvas c){super.onDraw(c);c.save();float sz=dp(24);c.translate((getWidth()-sz)/2,(getHeight()-sz)/2);c.scale(sz/24,sz/24);paint.setColor(icon.equals("starFilled")?ACCENT:color);paint.setStrokeWidth(1.8f);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);paint.setStyle(Paint.Style.STROKE);Path p=new Path();
  switch(icon){case "more":paint.setStyle(Paint.Style.FILL);for(int y:new int[]{5,12,19})c.drawCircle(12,y,1.5f,paint);break;case "play":paint.setStyle(Paint.Style.FILL);p.moveTo(7,4);p.lineTo(21,12);p.lineTo(7,20);p.close();c.drawPath(p,paint);break;
   case "pause":paint.setStyle(Paint.Style.FILL);c.drawRoundRect(6,4,10,20,1,1,paint);c.drawRoundRect(14,4,18,20,1,1,paint);break;
   case "previous":c.drawLine(5,5,5,19,paint);p.moveTo(19,5);p.lineTo(8,12);p.lineTo(19,19);p.close();c.drawPath(p,paint);break;
   case "next":c.drawLine(19,5,19,19,paint);p.moveTo(5,5);p.lineTo(16,12);p.lineTo(5,19);p.close();c.drawPath(p,paint);break;
   case "back":c.drawLine(5,12,21,12,paint);p.moveTo(11,5);p.lineTo(4,12);p.lineTo(11,19);c.drawPath(p,paint);break;
   case "star":case "starFilled":if(icon.equals("starFilled"))paint.setStyle(Paint.Style.FILL);for(int i=0;i<10;i++){double angle=-Math.PI/2+i*Math.PI/5;float r=i%2==0?9:4.2f;float x=12+(float)Math.cos(angle)*r,y=12+(float)Math.sin(angle)*r;if(i==0)p.moveTo(x,y);else p.lineTo(x,y);}p.close();c.drawPath(p,paint);break;
   default:c.drawCircle(12,12,9,paint);c.drawLine(12,11,12,17,paint);c.drawCircle(12,7,0.6f,paint);
  }c.restore();}}
 class DialView extends View {Paint p=new Paint(3);DialView(){super(MainActivity.this);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth();p.setStrokeWidth(dp(1));p.setColor(LINE);c.drawLine(0,dp(32),w,dp(32),p);for(int i=0;i<41;i++){float x=w*i/40;p.setColor(i==20?ACCENT:MUTED);p.setStrokeWidth(dp(i==20?2:1));c.drawLine(x,dp(i==20?0:i%5==0?12:22),x,dp(32),p);}}}
}
