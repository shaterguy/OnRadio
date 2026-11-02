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
 final int BG=Color.rgb(250,250,249),INK=Color.rgb(32,32,30),MUTED=Color.rgb(103,103,98),ACCENT=Color.rgb(167,71,36),LINE=Color.rgb(226,225,220),RAISED=Color.rgb(240,239,235);
 final Handler handler=new Handler(Looper.getMainLooper());final ExecutorService scheduleIo=Executors.newFixedThreadPool(3);final Map<String,TextView> rowPrograms=new HashMap<>();final Map<String,ProgramTimeRow> rowTimeProgress=new HashMap<>();
 android.content.SharedPreferences prefs;Set<String> favorites;RadioData.Station selected;
 MediaController controller;ListenableFuture<MediaController> future;boolean loading=false,dead=false,foreground=false;int generation=0;String error="",screen="list",query="",sortOrder="frequency";boolean onlyFavorites=false;
 android.animation.AnimatorSet screenTransition;int transitionGeneration;boolean closingPlayer=false;static final long PLAYER_TRANSITION_MS=420;
 FrameLayout contentFrame;LinearLayout screenPanel;Bitmap listBackdrop;int listReturnScrollY;boolean restoreListPosition;LinearLayout root,body,list,mini,playerContent;View screenHeader;ScrollView scroll;ProgramProgressView miniProgress;TextView miniTitle,miniSubtitle,playerStatus,programTitle,programTime,programDescription;IconButton miniPlay,fullPlay,favoriteButton;TextView frequencyHeader,nameHeader;boolean scheduleCenterPending;int scheduleRenderGeneration;final Map<String,List<RadioData.Program>> schedules=new HashMap<>();final Map<String,String> scheduleErrors=new HashMap<>();final Set<String> scheduleLoading=new HashSet<>();final Map<String,Long> scheduleFetched=new HashMap<>();LocalDate scheduleDate=LocalDate.now(RadioData.SEOUL);String lastProgram="";long lastRefresh=0;
 final Runnable ticker=new Runnable(){public void run(){if(!dead){refreshProgram();if(foreground&&System.currentTimeMillis()-lastRefresh>60000){lastRefresh=System.currentTimeMillis();loadListedSchedules();refreshProgram();}if(foreground)refreshRows();handler.postDelayed(this,1000);}}};
 @Override public void onCreate(Bundle state){super.onCreate(state);prefs=getSharedPreferences("radio",MODE_PRIVATE);sortOrder=prefs.getString("sort_order","frequency");favorites=new HashSet<>(prefs.getStringSet("favorites",Collections.emptySet()));selected=RadioData.find(prefs.getString("station","kbs25"));if(state!=null){screen=state.getString("screen","list");onlyFavorites=state.getBoolean("favorites");query=state.getString("query","");}if(getIntent().getBooleanExtra("player",false))screen="player";prefs.edit().putBoolean(PlaybackService.NAV_FAVORITES_KEY,onlyFavorites).apply();build();connect();loadSchedule(selected,false);handler.post(ticker);}
 @Override public void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putString("screen",screen);b.putBoolean("favorites",onlyFavorites);b.putString("query",query);}
 @Override protected void onResume(){super.onResume();foreground=true;syncStation();refreshProgram();}
 @Override protected void onPause(){foreground=false;super.onPause();}
 @Override public void onConfigurationChanged(Configuration c){super.onConfigurationChanged(c);build();}
 @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);if(i.getBooleanExtra("player",false)){screen="player";build();}}
 @Override protected void onDestroy(){dead=true;cancelScreenTransition();listBackdrop=null;generation++;handler.removeCallbacksAndMessages(null);scheduleIo.shutdownNow();if(future!=null)MediaController.releaseFuture(future);super.onDestroy();}
 void connect(){SessionToken token=new SessionToken(this,new ComponentName(this,PlaybackService.class));Bundle controllerHints=new Bundle();controllerHints.putBoolean(PlaybackService.APP_CONTROLLER_KEY,true);future=new MediaController.Builder(this,token).setConnectionHints(controllerHints).buildAsync();future.addListener(()->{try{if(dead)return;controller=future.get();MediaItem item=controller.getCurrentMediaItem();if(item!=null){selected=RadioData.find(item.mediaId);loadSchedule(selected,false);build();}controller.addListener(new Player.Listener(){@Override public void onEvents(Player p,Player.Events events){syncStation();updateState();}@Override public void onPlayerError(PlaybackException e){error="연결이 끊겼습니다. 재생 버튼을 눌러 다시 연결하세요";loading=false;updateState();}});updateState();}catch(Exception e){error="재생기를 연결하지 못했습니다. 앱을 다시 열어주세요";updateState();}},command -> handler.post(command));}
 int dp(float x){return (int)(getResources().getDisplayMetrics().density*x+.5f);}
 LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
 LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
 void pad(View v,int a,int b,int c,int d){v.setPadding(dp(a),dp(b),dp(c),dp(d));}
 TextView text(String str,int size,int color){TextView t=new TextView(this);t.setText(str);t.setTextSize(size);t.setTextColor(color);t.setIncludeFontPadding(false);t.setLineSpacing(dp(3),1);return t;}
 GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
 void touch(View v,int color,int radius){v.setBackground(new RippleDrawable(android.content.res.ColorStateList.valueOf(0x20000000),shape(color,radius),null));}
 void space(LinearLayout l,int h){l.addView(new View(this),new LinearLayout.LayoutParams(1,dp(h)));}
 TextView action(String label,Runnable run){TextView t=text(label,15,ACCENT);t.setGravity(Gravity.CENTER);t.setMinHeight(dp(48));pad(t,16,8,16,8);touch(t,RAISED,10);t.setOnClickListener(v->run.run());return t;}
 void line(LinearLayout l){View v=new View(this);v.setBackgroundColor(LINE);l.addView(v,new LinearLayout.LayoutParams(-1,dp(1)));}
 void build(){miniProgress=null;if(closingPlayer&&screen.equals("player")){screen="list";restoreListPosition=true;}if(screen.equals("list"))listBackdrop=null;cancelScreenTransition();rowPrograms.clear();rowTimeProgress.clear();miniPlay=null;fullPlay=null;playerStatus=null;programTitle=null;programTime=null;programDescription=null;miniTitle=null;miniSubtitle=null;favoriteButton=null;
  root=column();root.setBackgroundColor(BG);setContentView(root);root.setOnApplyWindowInsetsListener((v,in)->{if(Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=in.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());root.setPadding(bars.left,bars.top,bars.right,bars.bottom);}else root.setPadding(in.getSystemWindowInsetLeft(),in.getSystemWindowInsetTop(),in.getSystemWindowInsetRight(),in.getSystemWindowInsetBottom());return in;});root.requestApplyInsets();getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
  contentFrame=new FrameLayout(this);contentFrame.setClipChildren(true);root.addView(contentFrame,new LinearLayout.LayoutParams(-1,0,1));if(listBackdrop!=null&&screen.equals("player"))contentFrame.setBackground(new BitmapDrawable(getResources(),listBackdrop));screenPanel=column();screenPanel.setBackgroundColor(BG);contentFrame.addView(screenPanel,new FrameLayout.LayoutParams(-1,-1));
  LinearLayout header=row();pad(header,16,8,16,8);
  if(!screen.equals("list")){IconButton back=new IconButton("back","방송 목록으로",this::navigateBack);header.addView(back,new LinearLayout.LayoutParams(dp(48),dp(48)));}
  TextView title=text(screen.equals("list")?"온라디오":screen.equals("schedule")?"오늘의 편성표":"지금 듣는 방송",screen.equals("list")?28:20,INK);header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
  if(screen.equals("list"))header.addView(new IconButton("info","앱 정보",this::info),new LinearLayout.LayoutParams(dp(48),dp(48)));
  else if(screen.equals("player")){favoriteButton=new IconButton(favorites.contains(selected.id)?"starFilled":"star","즐겨찾기 전환",()->{toggleFavorite(selected);build();});header.addView(favoriteButton,new LinearLayout.LayoutParams(dp(48),dp(48)));}
  screenHeader=header;screenPanel.addView(header);body=column();screenPanel.addView(body,new LinearLayout.LayoutParams(-1,0,1));
  if(screen.equals("list"))buildList();else if(screen.equals("player"))buildPlayer();else buildSchedule();
  buildMini();updateState();refreshProgram();
 }
 void buildList(){
  LinearLayout tabs=row();pad(tabs,24,4,24,12);TextView all=action("전체 방송",()->{onlyFavorites=false;prefs.edit().putBoolean(PlaybackService.NAV_FAVORITES_KEY,false).apply();build();});TextView fav=action("즐겨찾기",()->{onlyFavorites=true;prefs.edit().putBoolean(PlaybackService.NAV_FAVORITES_KEY,true).apply();build();});all.setTextColor(!onlyFavorites?BG:MUTED);fav.setTextColor(onlyFavorites?BG:MUTED);touch(all,!onlyFavorites?INK:RAISED,10);touch(fav,onlyFavorites?INK:RAISED,10);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,dp(48),1);tp.setMarginEnd(dp(8));tabs.addView(all,tp);tabs.addView(fav,new LinearLayout.LayoutParams(0,dp(48),1));body.addView(tabs);
  EditText search=new EditText(this);search.setSingleLine();search.setTextSize(16);search.setTextColor(INK);search.setHintTextColor(MUTED);search.setHint("방송명 · 주파수 검색");pad(search,16,8,16,8);search.setBackground(shape(RAISED,10));search.setText(query);LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(-1,dp(48));ep.setMargins(dp(24),0,dp(24),dp(12));body.addView(search,ep);search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){query=s.toString();populate();}public void afterTextChanged(Editable e){}});
  LinearLayout sorting=row();pad(sorting,32,0,24,0);frequencyHeader=text("",13,Color.BLACK);nameHeader=text("",13,Color.BLACK);for(TextView label:new TextView[]{frequencyHeader,nameHeader}){label.setMinHeight(dp(48));label.setGravity(Gravity.CENTER_VERTICAL);touch(label,BG,0);}sorting.addView(frequencyHeader,new LinearLayout.LayoutParams(dp(76),dp(48)));sorting.addView(nameHeader,new LinearLayout.LayoutParams(0,dp(48),1));sorting.addView(new View(this),new LinearLayout.LayoutParams(dp(48),1));frequencyHeader.setOnClickListener(v->changeSort("frequency"));nameHeader.setOnClickListener(v->changeSort("name"));body.addView(sorting);updateSortLabels();

  scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);list=column();pad(list,24,0,24,16);scroll.addView(list);body.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));populate();if(restoreListPosition){restoreListPosition=false;ScrollView targetScroll=scroll;int savedY=listReturnScrollY;NextDraw.add(targetScroll,()->{if(!dead&&screen.equals("list")&&scroll==targetScroll)targetScroll.scrollTo(0,savedY);});}
 }
 void changeSort(String column){sortOrder=UiRules.nextSort(sortOrder,column);prefs.edit().putString("sort_order",sortOrder).apply();updateSortLabels();populate();scroll.scrollTo(0,0);}
 void updateSortLabels(){if(frequencyHeader==null||nameHeader==null)return;frequencyHeader.setText(UiRules.sortLabel(sortOrder,"frequency","주파수"));nameHeader.setText(UiRules.sortLabel(sortOrder,"name","방송국명"));frequencyHeader.setContentDescription("주파수 정렬"+(UiRules.active(sortOrder,"frequency")?(sortOrder.endsWith("_desc")?" 내림차순":" 오름차순"):""));nameHeader.setContentDescription("방송국명 정렬"+(UiRules.active(sortOrder,"name")?(sortOrder.endsWith("_desc")?" 내림차순":" 오름차순"):""));}
 void populate(){if(list==null||!screen.equals("list"))return;list.removeAllViews();rowPrograms.clear();rowTimeProgress.clear();int count=0;String q=query.trim().toLowerCase(Locale.ROOT);
  for(RadioData.Station s:RadioData.orderedStations(sortOrder)){if(onlyFavorites&&!favorites.contains(s.id))continue;if(!(s.name+" "+s.fm+" "+s.brand).toLowerCase(Locale.ROOT).contains(q))continue;count++;
   LinearLayout row=row();row.setMinimumHeight(dp(88));pad(row,8,12,0,12);touch(row,s.id.equals(selected.id)?RAISED:BG,10);
   TextView f=text(s.fm,22,s.id.equals(selected.id)?ACCENT:INK);f.setTypeface(android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL));f.setGravity(Gravity.CENTER_VERTICAL);row.addView(f,new LinearLayout.LayoutParams(dp(76),-2));
   LinearLayout names=column();TextView n=text(s.name,16,INK);names.addView(n);space(names,6);TextView desc=text("방송정보 확인 중",14,MUTED);desc.setMaxLines(2);desc.setEllipsize(TextUtils.TruncateAt.END);names.addView(desc);rowPrograms.put(s.id,desc);space(names,4);ProgramTimeRow timeProgress=new ProgramTimeRow(this);names.addView(timeProgress,new LinearLayout.LayoutParams(-1,-2));rowTimeProgress.put(s.id,timeProgress);row.addView(names,new LinearLayout.LayoutParams(0,-2,1));
   IconButton star=new IconButton(favorites.contains(s.id)?"starFilled":"star",s.name+" 즐겨찾기 "+(favorites.contains(s.id)?"해제":"추가"),()->{toggleFavorite(s);populate();});row.addView(star,new LinearLayout.LayoutParams(dp(48),dp(48)));row.setOnClickListener(v->{hideKeyboard();play(s);populate();});list.addView(row);line(list);
  }
  if(count==0){space(list,40);TextView empty=text(onlyFavorites&&q.isEmpty()?"즐겨찾는 방송을 담아보세요":"검색 결과가 없습니다",19,INK);list.addView(empty);space(list,12);list.addView(text(onlyFavorites?"전체 방송에서 별표를 누르면 여기에 표시됩니다":"방송명이나 FM 주파수로 다시 검색해 보세요",14,MUTED));}
  space(list,16);list.addView(text("주파수는 지역별 대표 주파수 · 인터넷 데이터 사용",12,MUTED));refreshRows();loadListedSchedules();
 }
 void toggleFavorite(RadioData.Station s){if(!favorites.add(s.id))favorites.remove(s.id);prefs.edit().putStringSet("favorites",new HashSet<>(favorites)).apply();}
 void buildMini(){line(root);LinearLayout dock=column();dock.setBackgroundColor(Color.WHITE);miniProgress=new ProgramProgressView(this);dock.addView(miniProgress,new LinearLayout.LayoutParams(-1,dp(3)));dock.setOnClickListener(v->openPlayer());mini=row();pad(mini,16,8,12,8);mini.setMinimumHeight(dp(80));touch(mini,Color.WHITE,0);
  LinearLayout title=column();miniTitle=text(selected.name,15,INK);miniSubtitle=text("재생할 방송을 선택하세요",12,MUTED);miniSubtitle.setMaxLines(2);miniSubtitle.setEllipsize(TextUtils.TruncateAt.END);title.addView(miniTitle);space(title,6);title.addView(miniSubtitle);mini.addView(title,new LinearLayout.LayoutParams(0,-2,1));title.setOnClickListener(v->openPlayer());mini.setOnClickListener(v->openPlayer());
  mini.addView(new IconButton("previous","이전 방송",()->step(-1)),new LinearLayout.LayoutParams(dp(48),dp(48)));miniPlay=new IconButton("play","재생",this::toggle);touch(miniPlay,RAISED,24);mini.addView(miniPlay,new LinearLayout.LayoutParams(dp(48),dp(48)));mini.addView(new IconButton("next","다음 방송",()->step(1)),new LinearLayout.LayoutParams(dp(48),dp(48)));
  dock.setId(R.id.playback_bar);dock.addView(mini,new LinearLayout.LayoutParams(-1,-2));root.addView(dock,new LinearLayout.LayoutParams(-1,-2));
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
 void openPlayer(){if(screen.equals("player"))return;hideKeyboard();captureListBackdrop();screen="player";build();if(android.animation.ValueAnimator.areAnimatorsEnabled())animatePlayer(true);}
 void navigateBack(){if(closingPlayer)return;if(screen.equals("player")){if(android.animation.ValueAnimator.areAnimatorsEnabled())animatePlayer(false);else{screen="list";restoreListPosition=true;build();}}else if(screen.equals("schedule")){screen="player";build();}else super.onBackPressed();}
 void buildPlayer(){scroll=new ScrollView(this);scroll.setFillViewport(true);playerContent=column();pad(playerContent,24,8,24,24);scroll.addView(playerContent);body.addView(scroll,new LinearLayout.LayoutParams(-1,-1));
  TextView brand=text(selected.brand+" RADIO",13,MUTED);playerContent.addView(brand);space(playerContent,12);
  LinearLayout dial=column();pad(dial,24,28,24,20);dial.setBackground(shape(RAISED,16));TextView freq=text(selected.fm,64,INK);freq.setTypeface(android.graphics.Typeface.create("sans-serif-light",android.graphics.Typeface.NORMAL));dial.addView(freq);space(dial,4);dial.addView(text("MHz   /   수도권 FM",13,MUTED));space(dial,16);dial.addView(new DialView(),new LinearLayout.LayoutParams(-1,dp(40)));playerContent.addView(dial);
  space(playerContent,24);playerContent.addView(text(selected.name,28,INK));space(playerContent,8);playerStatus=text("",14,ACCENT);playerStatus.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);playerContent.addView(playerStatus);

  space(playerContent,28);line(playerContent);space(playerContent,20);playerContent.addView(text("방송정보",13,MUTED));space(playerContent,12);programTitle=text("방송정보 확인 중",21,INK);playerContent.addView(programTitle);space(playerContent,8);programTime=text("",13,ACCENT);playerContent.addView(programTime);space(playerContent,12);programDescription=text(selected.description,14,MUTED);playerContent.addView(programDescription);space(playerContent,20);
  LinearLayout links=row();TextView schedule=action("오늘의 편성표",()->{screen="schedule";build();loadSchedule(selected,false);});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMarginEnd(dp(8));links.addView(schedule,p);links.addView(action("방송 홈페이지",()->browse(selected.website)),new LinearLayout.LayoutParams(0,-2,1));playerContent.addView(links);space(playerContent,20);playerContent.addView(text("실시간 인터넷 방송 · 방송사 사정에 따라 지연될 수 있습니다",12,MUTED));
 }
 void buildSchedule(){scheduleCenterPending=true;LinearLayout heading=column();pad(heading,24,8,24,16);heading.addView(text(selected.name,22,INK));space(heading,8);heading.addView(text(LocalDate.now(RadioData.SEOUL).format(DateTimeFormatter.ofPattern("M월 d일 EEEE",Locale.KOREAN))+" · 한국시간",14,MUTED));body.addView(heading);
  scroll=new ScrollView(this);list=column();pad(list,24,0,24,24);scroll.addView(list);body.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));renderSchedule();
 }
 void renderSchedule(){if(!screen.equals("schedule")||list==null)return;int oldY=scroll.getScrollY();int ticket=++scheduleRenderGeneration;View currentRow=null;list.removeAllViews();List<RadioData.Program> items=schedules.get(selected.id);
  if(items!=null&&!items.isEmpty()){for(RadioData.Program p:items){LinearLayout r=column();pad(r,16,16,16,16);boolean now=p.current(System.currentTimeMillis());if(now)currentRow=r;r.setBackground(shape(now?RAISED:BG,10));r.addView(text(p.time()+(now?"   방송 중":""),13,now?ACCENT:MUTED));space(r,8);r.addView(text(p.title,17,INK));if(!p.host.isEmpty()){space(r,6);r.addView(text(p.host,13,MUTED));}if(p.link.startsWith("https://"))r.setOnClickListener(v->browse(p.link));list.addView(r);line(list);}}
  else {space(list,20);list.addView(text(scheduleLoading.contains(selected.id)?"편성표를 가져오는 중입니다":scheduleErrors.getOrDefault(selected.id,"이 채널은 공식 편성표에서 확인할 수 있습니다"),16,MUTED));space(list,20);list.addView(action("다시 불러오기",()->loadSchedule(selected,true)));}
  space(list,20);list.addView(action("방송사 공식 편성표 열기",()->browse(selected.schedule)));space(list,12);list.addView(text("편성은 방송사 사정에 따라 변경될 수 있습니다",12,MUTED));
  if(scheduleCenterPending&&currentRow!=null)centerCurrentProgram(currentRow,ticket);else {ScrollView scheduleScroll=scroll;scheduleScroll.post(()->{if(ticket==scheduleRenderGeneration&&screen.equals("schedule")&&scroll==scheduleScroll)scheduleScroll.scrollTo(0,oldY);});}
 }
 void centerCurrentProgram(View target,int ticket){ScrollView scheduleScroll=scroll;LinearLayout scheduleList=list;NextDraw.add(scheduleScroll,()->{if(dead||ticket!=scheduleRenderGeneration||!screen.equals("schedule")||scroll!=scheduleScroll||list!=scheduleList||scheduleScroll.getHeight()<=0)return;scheduleList.setPadding(dp(24),scheduleScroll.getHeight()/2,dp(24),scheduleScroll.getHeight()/2);NextDraw.add(scheduleScroll,()->{if(dead||ticket!=scheduleRenderGeneration||!screen.equals("schedule")||scroll!=scheduleScroll||list!=scheduleList)return;scheduleScroll.scrollTo(0,UiRules.centeredScrollY(target.getTop(),target.getHeight(),scheduleScroll.getHeight()));scheduleCenterPending=false;});});}
 void loadSchedule(RadioData.Station s,boolean force){LocalDate date=LocalDate.now(RadioData.SEOUL);if(!date.equals(scheduleDate)){schedules.clear();scheduleErrors.clear();scheduleDate=date;}
  if(scheduleLoading.contains(s.id))return;if(!force&&schedules.containsKey(s.id)&&System.currentTimeMillis()-scheduleFetched.getOrDefault(s.id,0L)<300000){refreshProgram();return;}scheduleLoading.add(s.id);if(screen.equals("schedule"))renderSchedule();
  scheduleIo.execute(()->{List<RadioData.Program> result=null;String fail="";try{result=RadioData.schedule(s,date);}catch(Exception e){fail="편성정보를 가져오지 못했습니다. 공식 편성표를 확인해 주세요";}List<RadioData.Program> data=result;String reason=fail;handler.post(()->{if(dead)return;scheduleLoading.remove(s.id);if(!date.equals(scheduleDate))return;if(data!=null){schedules.put(s.id,data);scheduleFetched.put(s.id,System.currentTimeMillis());}if(!reason.isEmpty())scheduleErrors.put(s.id,reason);else scheduleErrors.remove(s.id);refreshRows();if(selected.id.equals(s.id)){refreshProgram();renderSchedule();}});});
 }
 void loadListedSchedules(){if(!screen.equals("list")){loadSchedule(selected,false);return;}for(RadioData.Station s:RadioData.STATIONS)if(rowPrograms.containsKey(s.id))loadSchedule(s,false);}
 void refreshRows(){if(!screen.equals("list"))return;long now=System.currentTimeMillis();for(RadioData.Station s:RadioData.STATIONS){TextView title=rowPrograms.get(s.id);if(title==null)continue;RadioData.Program program=null;List<RadioData.Program> items=schedules.get(s.id);if(items!=null)for(RadioData.Program item:items)if(item.current(now)){program=item;break;}String name=program!=null?program.title:scheduleLoading.contains(s.id)?"방송정보 확인 중":scheduleErrors.containsKey(s.id)?"방송정보 조회 실패":"현재 프로그램 정보 미제공";if(!TextUtils.equals(title.getText(),name))title.setText(name);ProgramTimeRow timeProgress=rowTimeProgress.get(s.id);if(timeProgress!=null)timeProgress.update(program,now);}}
 void refreshProgram(){if(selected==null)return;RadioData.Program p=RadioData.current(schedules.get(selected.id));String name=p==null?(scheduleLoading.contains(selected.id)?"방송정보 확인 중":"현재 프로그램 정보 미제공"):p.title;
  if(programTitle!=null)programTitle.setText(name);if(programTime!=null){programTime.setText(p==null?"공식 편성표에서 확인할 수 있습니다":p.time()+(p.host.isEmpty()?"":" · "+p.host));}if(programDescription!=null)programDescription.setText(p!=null&&!p.description.isEmpty()?p.description:selected.description);

  updateState();
 }
 void play(RadioData.Station s){if(controller==null){Toast.makeText(this,"재생기 연결 중입니다. 잠시 후 다시 눌러주세요",Toast.LENGTH_SHORT).show();return;}if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED&&!prefs.getBoolean("notificationAsked",false)){prefs.edit().putBoolean("notificationAsked",true).apply();requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},41);}
  final int request=++generation;selected=s;loading=true;error="";lastProgram="";if(screen.equals("player")||screen.equals("schedule"))build();updateState();loadSchedule(s,false);
  Bundle args=new Bundle();args.putString("station_id",s.id);com.google.common.util.concurrent.ListenableFuture<SessionResult> result=controller.sendCustomCommand(PlaybackService.PLAY_STATION_COMMAND,args);
  result.addListener(()->{if(dead||request!=generation)return;loading=false;try{SessionResult response=result.get();if(response.resultCode!=SessionResult.RESULT_SUCCESS)error="방송에 연결하지 못했습니다. 재생 버튼으로 다시 시도하세요";}catch(Exception failure){error="방송에 연결하지 못했습니다. 재생 버튼으로 다시 시도하세요";}syncStation();updateState();if(screen.equals("list"))populate();},command->handler.post(command));
 }
 void toggle(){if(loading){generation++;loading=false;if(controller!=null)controller.pause();updateState();return;}if(controller==null)return;if(controller.getPlayWhenReady()&&controller.getPlayerError()==null){controller.pause();updateState();}else if(controller.getPlaybackState()==Player.STATE_READY&&controller.getPlayerError()==null&&controller.getCurrentMediaItem()!=null){controller.play();}else play(selected);}
 void step(int delta){if(controller==null)return;generation++;loading=false;error="";if(delta>0)controller.seekToNext();else controller.seekToPrevious();updateState();}
 void syncStation(){if(controller==null||loading)return;MediaItem item=controller.getCurrentMediaItem();if(item!=null&&!item.mediaId.equals(selected.id)){selected=RadioData.find(item.mediaId);prefs.edit().putString("station",selected.id).apply();build();loadSchedule(selected,false);}}
 void updateState(){if(selected==null)return;boolean wanted=controller!=null&&controller.getPlayWhenReady();boolean playing=controller!=null&&controller.isPlaying();boolean buffering=loading||(wanted&&controller.getPlaybackState()==Player.STATE_BUFFERING);String state=!error.isEmpty()?error:buffering?"연결 중":playing?"실시간 재생 중":wanted?"재생 준비 중":"일시정지";boolean pause=loading||(wanted&&controller.getPlayerError()==null&&error.isEmpty());
  for(IconButton b:new IconButton[]{miniPlay,fullPlay})if(b!=null){b.icon=pause?"pause":"play";b.setContentDescription(pause?"일시정지":"재생");b.invalidate();}
  if(playerStatus!=null)playerStatus.setText(state);if(miniTitle!=null)miniTitle.setText(selected.name+"  ·  "+selected.fm);if(miniSubtitle!=null){RadioData.Program p=RadioData.current(schedules.get(selected.id));String status=playing&&error.isEmpty()&&!buffering?"":state;miniSubtitle.setText(p==null?status:status.isEmpty()?p.title:status+" · "+p.title);}updateProgramProgress();
 }
 void updateProgramProgress(){
  RadioData.Program program=RadioData.current(schedules.get(selected.id));long now=System.currentTimeMillis();
  if(program==null&&controller!=null){MediaItem item=controller.getCurrentMediaItem();Bundle extras=item==null?null:item.mediaMetadata.extras;if(item!=null&&item.mediaId.equals(selected.id)&&extras!=null){long start=extras.getLong(ProgramProgress.START_KEY,0),end=extras.getLong(ProgramProgress.END_KEY,0);if(ProgramProgress.current(start,end,now))program=new RadioData.Program(String.valueOf(item.mediaMetadata.title),"","","",start,end);}}
  boolean known=program!=null&&ProgramProgress.current(program.start,program.end,now);int value=known?ProgramProgress.scaled(program.start,program.end,now):0;
  if(miniProgress!=null)miniProgress.update(value,known);

 }
 void hideKeyboard(){View focus=getCurrentFocus();if(focus!=null)((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(focus.getWindowToken(),0);}
 void browse(String url){try{Uri u=Uri.parse(url);if(!"https".equals(u.getScheme())&&!("http".equals(u.getScheme())&&"tbs.seoul.kr".equals(u.getHost()))){Toast.makeText(this,"방송 홈페이지에서 확인해 주세요",Toast.LENGTH_SHORT).show();return;}startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(Exception e){Toast.makeText(this,"링크를 열 앱을 찾지 못했습니다",Toast.LENGTH_SHORT).show();}}
 void info(){new AlertDialog.Builder(this).setTitle("온라디오").setMessage("0.1.0-dev11\n\n국내 FM 채널을 인터넷으로 듣습니다. 주파수는 수도권 기준이며 실제 수신 주파수와 무관합니다.\n\n즐겨찾기는 기기에 저장됩니다. 녹음이나 개인정보 수집 기능은 없습니다. 방송사는 이 앱과 제휴 관계가 아닙니다.\n\n디자인 참고: Unslop · Damir Sokolovsky · CC BY 4.0").setPositiveButton("확인",null).show();}
 @Override public void onBackPressed(){navigateBack();}
 class IconButton extends View {String icon;int color=INK;Paint paint=new Paint(3);IconButton(String icon,String label,Runnable run){super(MainActivity.this);this.icon=icon;setContentDescription(label);setFocusable(true);setClickable(true);touch(this,BG,24);setOnClickListener(v->run.run());} @Override public CharSequence getAccessibilityClassName(){return "android.widget.Button";} @Override protected void onDraw(Canvas c){super.onDraw(c);c.save();float sz=dp(24);c.translate((getWidth()-sz)/2,(getHeight()-sz)/2);c.scale(sz/24,sz/24);paint.setColor(icon.equals("starFilled")?ACCENT:color);paint.setStrokeWidth(1.8f);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);paint.setStyle(Paint.Style.STROKE);Path p=new Path();
  switch(icon){case "play":paint.setStyle(Paint.Style.FILL);p.moveTo(7,4);p.lineTo(21,12);p.lineTo(7,20);p.close();c.drawPath(p,paint);break;
   case "pause":paint.setStyle(Paint.Style.FILL);c.drawRoundRect(6,4,10,20,1,1,paint);c.drawRoundRect(14,4,18,20,1,1,paint);break;
   case "previous":c.drawLine(5,5,5,19,paint);p.moveTo(19,5);p.lineTo(8,12);p.lineTo(19,19);p.close();c.drawPath(p,paint);break;
   case "next":c.drawLine(19,5,19,19,paint);p.moveTo(5,5);p.lineTo(16,12);p.lineTo(5,19);p.close();c.drawPath(p,paint);break;
   case "back":c.drawLine(5,12,21,12,paint);p.moveTo(11,5);p.lineTo(4,12);p.lineTo(11,19);c.drawPath(p,paint);break;
   case "star":case "starFilled":if(icon.equals("starFilled"))paint.setStyle(Paint.Style.FILL);for(int i=0;i<10;i++){double angle=-Math.PI/2+i*Math.PI/5;float r=i%2==0?9:4.2f;float x=12+(float)Math.cos(angle)*r,y=12+(float)Math.sin(angle)*r;if(i==0)p.moveTo(x,y);else p.lineTo(x,y);}p.close();c.drawPath(p,paint);break;
   default:c.drawCircle(12,12,9,paint);c.drawLine(12,11,12,17,paint);c.drawCircle(12,7,0.6f,paint);
  }c.restore();}}
 class DialView extends View {Paint p=new Paint(3);DialView(){super(MainActivity.this);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth();p.setStrokeWidth(dp(1));p.setColor(LINE);c.drawLine(0,dp(32),w,dp(32),p);for(int i=0;i<41;i++){float x=w*i/40;p.setColor(i==20?ACCENT:MUTED);p.setStrokeWidth(dp(i==20?2:1));c.drawLine(x,dp(i==20?0:i%5==0?12:22),x,dp(32),p);}}}
}
