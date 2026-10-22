package local.serene.tv;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.*;
import android.net.wifi.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.io.InputStream;

public class HomeActivity extends Activity {
 final Handler handler=new Handler();
 final ArrayList<App> apps=new ArrayList<>();
 final LinkedHashSet<String> favorites=new LinkedHashSet<>();
 static final String[] FILES={"alpine.jpg","forest.jpg","peaks.jpg","coast.jpg","kerala-munnar.jpg","pine-forest.jpg","kerala-backwaters.jpg","woodland.jpg","kerala-waterfalls.jpg","kerala-palms.jpg"};
 static final String[] SCENES={"ALPINE LAKE","FOREST SUNLIGHT","MOUNTAIN VALLEY","COASTAL SUNRISE","MUNNAR · KERALA","PINE FOREST","BACKWATERS · KERALA","WOODLAND LIGHT","ATHIRAPPILLY · KERALA","PALM BACKWATERS · KERALA"};
 LinearLayout dock,grid;TextView clock,sceneTitle,wifiLabel;WifiSymbol wifiSymbol;Landscape landscape;FrameLayout root;Bitmap photo;
 int scene=0,uiTheme=0,ink,muted,cardColor,focusedColor;long lastScene=0;boolean active=false,dark=false,allAppsPage=false,loadingApps=false,appsDirty=true;String focusTag=null;
 static class App{String pkg,label;Intent intent;Drawable icon,banner;}
 final BroadcastReceiver packageChanges=new BroadcastReceiver(){@Override public void onReceive(Context context,Intent intent){appsDirty=true;if(active)refreshApps();}};
 int dp(float n){return (int)(getResources().getDisplayMetrics().density*n+.5f);}
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setFlags(1024,1024);getWindow().getDecorView().setSystemUiVisibility(5894);dark=getPreferences(0).getBoolean("dark",false);uiTheme=getPreferences(0).getInt("ui_theme",0);scene=getPreferences(0).getInt("scene",0)%FILES.length;loadFavorites();loadPhoto();build();IntentFilter filter=new IntentFilter();filter.addAction(Intent.ACTION_PACKAGE_ADDED);filter.addAction(Intent.ACTION_PACKAGE_REMOVED);filter.addAction(Intent.ACTION_PACKAGE_CHANGED);filter.addDataScheme("package");if(Build.VERSION.SDK_INT>=33)registerReceiver(packageChanges,filter,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(packageChanges,filter);root.post(()->refreshApps());}
 ArrayList<App> discoverApps(){
  ArrayList<App> found=new ArrayList<>();PackageManager pm=getPackageManager();Set<String> seen=new HashSet<>();
  for(String category:new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER,Intent.CATEGORY_LAUNCHER})for(ResolveInfo ri:pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(category),0)){
   String pkg=ri.activityInfo.packageName;if(pkg.equals(getPackageName())||!seen.add(pkg))continue;
   App a=new App();a.pkg=pkg;a.label=ri.loadLabel(pm).toString();a.icon=ri.loadIcon(pm);try{a.banner=ri.activityInfo.loadBanner(pm);if(a.banner==null)a.banner=pm.getApplicationBanner(pkg);}catch(Exception ignored){}a.intent=new Intent(Intent.ACTION_MAIN).addCategory(category).setComponent(new ComponentName(pkg,ri.activityInfo.name)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);found.add(a);
  }
  Collections.sort(found,(a,b)->a.label.compareToIgnoreCase(b.label));return found;
 }
 void loadFavorites(){favorites.clear();String saved=getPreferences(0).getString("dock",null);
  if(saved!=null){for(String s:saved.split(","))if(!s.isEmpty())favorites.add(s);}else favorites.addAll(Arrays.asList("com.netflix.ninja","com.google.android.youtube.tv","com.amazon.amazonvideo.livingroom","com.apple.atve.androidtv.appletv","com.stremio.one","org.videolan.vlc"));
 }
 void refreshApps(){if(loadingApps||!appsDirty)return;loadingApps=true;appsDirty=false;new Thread(()->{ArrayList<App> found;try{found=discoverApps();}catch(Exception e){handler.post(()->{loadingApps=false;appsDirty=true;});return;}handler.post(()->{if(isDestroyed())return;loadingApps=false;View current=getCurrentFocus();if(current!=null&&current.getTag() instanceof String)focusTag=(String)current.getTag();apps.clear();apps.addAll(found);build();if(appsDirty)refreshApps();});},"App discovery").start();}
 App findApp(String pkg){for(App a:apps)if(a.pkg.equals(pkg))return a;return null;}
 void saveDock(){getPreferences(0).edit().putString("dock",android.text.TextUtils.join(",",favorites)).apply();}
 TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
 TextView photoText(String s,int size){TextView t=text(s,size,Color.WHITE);t.setShadowLayer(dp(5),0,dp(1),0x99000000);return t;}
 GradientDrawable shape(int color,int stroke){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(14));if(stroke!=0)g.setStroke(dp(2),stroke);return g;}
 void focusStyle(View v,boolean f){v.setBackground(shape(f?focusedColor:cardColor,f?(dark?0xffc5e6ff:0xff2d628a):0));v.setElevation(f?dp(8):0);if(uiTheme==1&&v instanceof LinearLayout){v.setScaleX(f?1.035f:1);v.setScaleY(f?1.035f:1);}}
 TextView action(String title,String tag,Runnable click){TextView v=text(title,11,ink);v.setTag(tag);v.setGravity(Gravity.CENTER);v.setPadding(dp(14),dp(9),dp(14),dp(9));v.setFocusable(true);v.setClickable(true);focusStyle(v,false);v.setOnFocusChangeListener((w,f)->focusStyle(v,f));v.setOnClickListener(w->click.run());return v;}
 LinearLayout.LayoutParams margin(int width,int height,int right){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(width,height);p.rightMargin=dp(right);return p;}
 void build(){
  ink=dark?0xfff0f5fb:0xff182b3d;muted=dark?0xffafbecd:0xff4b6072;cardColor=dark?0xe5152232:0xeaf4f8fb;focusedColor=dark?0xff334e69:0xffffffff;
  root=new FrameLayout(this);root.setClipChildren(false);root.setClipToPadding(false);landscape=new Landscape();root.addView(landscape,new FrameLayout.LayoutParams(-1,-1));LinearLayout body=new LinearLayout(this);body.setClipChildren(false);body.setClipToPadding(false);body.setOrientation(1);body.setPadding(dp(42),dp(22),dp(42),dp(24));root.addView(body,new FrameLayout.LayoutParams(-1,-1));
  LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.addView(new View(this),new LinearLayout.LayoutParams(0,1,1));
  wifiSymbol=new WifiSymbol();top.addView(wifiSymbol,margin(dp(26),dp(26),6));wifiLabel=photoText("Wi-Fi",10);top.addView(wifiLabel,margin(dp(66),-2,13));clock=photoText("",13);top.addView(clock,margin(-2,-2,18));
  Switch theme=new Switch(this);theme.setTag("dark-mode");theme.setText("Dark mode  ");theme.setTextSize(11);theme.setTextColor(ink);theme.setChecked(dark);theme.setContentDescription("Dark mode");theme.setPadding(dp(12),dp(5),dp(12),dp(5));theme.setFocusable(true);focusStyle(theme,false);theme.setOnFocusChangeListener((v,f)->focusStyle(v,f));theme.setOnCheckedChangeListener((button,checked)->{dark=checked;getPreferences(0).edit().putBoolean("dark",dark).apply();focusTag="dark-mode";build();});top.addView(theme,new LinearLayout.LayoutParams(-2,dp(36)));body.addView(top);
  if(allAppsPage)buildAllApps(body);else buildHome(body);setContentView(root);tick.run();View target=focusTag==null?null:root.findViewWithTag(focusTag);
  if(target!=null)target.requestFocus();else if(allAppsPage&&grid.getChildCount()>0)((LinearLayout)grid.getChildAt(0)).getChildAt(0).requestFocus();else if(dock!=null&&dock.getChildCount()>0)dock.getChildAt(0).requestFocus();focusTag=null;
 }
 void buildHome(LinearLayout body){
  if(uiTheme==1){buildAppleHome(body);return;}
  LinearLayout hero=new LinearLayout(this);hero.setOrientation(1);hero.setGravity(Gravity.BOTTOM);hero.setPadding(0,0,0,dp(26));sceneTitle=photoText(SCENES[scene],11);sceneTitle.setLetterSpacing(.23f);hero.addView(sceneTitle);body.addView(hero,new LinearLayout.LayoutParams(-1,0,1));
  LinearLayout heading=new LinearLayout(this);heading.setGravity(Gravity.CENTER_VERTICAL);heading.addView(text("Dock",uiTheme==1?21:17,ink),margin(-2,-2,16));heading.addView(text("Hold OK to move apps or edit the dock",11,muted),new LinearLayout.LayoutParams(0,-2,1));heading.addView(action("Settings","settings",()->startActivity(new Intent("android.settings.SETTINGS"))),margin(-2,-2,9));heading.addView(action("Theme","theme-selector",()->chooseTheme()),margin(-2,-2,9));heading.addView(action("Change wallpaper","wallpaper",()->nextScene()));body.addView(heading);
  HorizontalScrollView scroll=new HorizontalScrollView(this);scroll.setHorizontalScrollBarEnabled(false);scroll.setClipChildren(false);scroll.setClipToPadding(false);dock=new LinearLayout(this);dock.setClipChildren(false);dock.setPadding(dp(7),dp(10),dp(7),dp(8));scroll.addView(dock);
  int cardWidth=uiTheme==1?152:108,cardHeight=uiTheme==1?116:91;
  if(uiTheme==1){dock.setPadding(dp(15),dp(15),dp(15),dp(15));dock.setBackground(shape(dark?0x99101b2b:0x99f2f6fa,0));}
  for(String pkg:favorites){App a=findApp(pkg);if(a!=null)dock.addView(card(a),margin(dp(cardWidth),dp(cardHeight),uiTheme==1?18:10));else if(loadingApps||appsDirty){View placeholder=new View(this);placeholder.setBackground(shape(cardColor,0));dock.addView(placeholder,margin(dp(cardWidth),dp(cardHeight),uiTheme==1?18:10));}}
  LinearLayout all=new LinearLayout(this);all.setTag("all-apps");all.setOrientation(1);all.setGravity(Gravity.CENTER);all.setFocusable(true);all.setClickable(true);focusStyle(all,false);TextView symbol=text("\u283f",uiTheme==1?44:34,ink);symbol.setGravity(Gravity.CENTER);all.addView(symbol,new LinearLayout.LayoutParams(-1,dp(uiTheme==1?62:48)));TextView label=text("All apps",uiTheme==1?14:11,ink);label.setGravity(Gravity.CENTER);all.addView(label);all.setOnFocusChangeListener((v,f)->focusStyle(v,f));all.setOnClickListener(v->{allAppsPage=true;build();});dock.addView(all,margin(dp(cardWidth),dp(cardHeight),0));LinearLayout.LayoutParams scrollParams=new LinearLayout.LayoutParams(-1,dp(uiTheme==1?154:114));scrollParams.topMargin=dp(uiTheme==1?9:0);body.addView(scroll,scrollParams);
 }
 void buildAppleHome(LinearLayout body){
  LinearLayout preview=new LinearLayout(this);preview.setOrientation(1);preview.setGravity(Gravity.BOTTOM);preview.setPadding(dp(10),0,0,dp(20));sceneTitle=photoText(SCENES[scene],10);sceneTitle.setLetterSpacing(.2f);preview.addView(sceneTitle);body.addView(preview,new LinearLayout.LayoutParams(-1,0,1));
  HorizontalScrollView shelf=new HorizontalScrollView(this);shelf.setHorizontalScrollBarEnabled(false);shelf.setClipChildren(false);shelf.setClipToPadding(false);dock=new LinearLayout(this);dock.setClipChildren(false);dock.setClipToPadding(false);dock.setPadding(dp(20),dp(24),dp(20),dp(12));shelf.addView(dock);
  for(String pkg:favorites){App app=findApp(pkg);if(app!=null)dock.addView(appleCard(app),margin(dp(148),dp(111),24));else if(loadingApps||appsDirty){View pending=new View(this);pending.setBackground(shape(dark?0xff252a33:0xffdce2ea,0));dock.addView(pending,margin(dp(148),dp(83),24));}}
  dock.addView(appleAllApps(),margin(dp(148),dp(111),0));body.addView(shelf,new LinearLayout.LayoutParams(-1,dp(151)));
  LinearLayout footer=new LinearLayout(this);footer.setGravity(Gravity.CENTER_VERTICAL);footer.setPadding(dp(10),dp(8),0,0);footer.addView(text("Dock",12,muted),new LinearLayout.LayoutParams(0,-2,1));footer.addView(action("Settings","settings",()->startActivity(new Intent("android.settings.SETTINGS"))),margin(-2,-2,9));footer.addView(action("Theme","theme-selector",()->chooseTheme()),margin(-2,-2,9));footer.addView(action("Change wallpaper","wallpaper",()->nextScene()));body.addView(footer,new LinearLayout.LayoutParams(-1,dp(44)));
 }
 Drawable copyDrawable(Drawable drawable){Drawable.ConstantState state=drawable.getConstantState();return state==null?drawable:state.newDrawable(getResources()).mutate();}
 View appleCard(App app){
  LinearLayout outer=new LinearLayout(this);outer.setOrientation(1);outer.setGravity(Gravity.TOP);outer.setTag("app:"+app.pkg);outer.setContentDescription(app.label);outer.setFocusable(true);outer.setClickable(true);outer.setClipChildren(false);outer.setClipToPadding(false);
  FrameLayout artwork=new FrameLayout(this);artwork.setBackground(shape(0xfff5f5f7,0));artwork.setClipToOutline(true);
  ImageView image=new ImageView(this);if(app.banner!=null){image.setImageDrawable(copyDrawable(app.banner));image.setScaleType(ImageView.ScaleType.CENTER_CROP);artwork.addView(image,new FrameLayout.LayoutParams(-1,-1));}else{image.setImageDrawable(copyDrawable(app.icon));image.setScaleType(ImageView.ScaleType.FIT_CENTER);FrameLayout.LayoutParams ip=new FrameLayout.LayoutParams(dp(53),dp(53),Gravity.CENTER);artwork.addView(image,ip);}
  // Real Android TV banner artwork provides tvOS-like edge-to-edge 16:9 tiles.
  outer.addView(artwork,new LinearLayout.LayoutParams(-1,dp(83)));TextView label=text(app.label,12,ink);label.setGravity(Gravity.CENTER);label.setSingleLine(true);label.setEllipsize(android.text.TextUtils.TruncateAt.END);label.setAlpha(0);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(24));lp.topMargin=dp(5);outer.addView(label,lp);
  outer.setOnFocusChangeListener((v,focused)->appleFocus(artwork,label,focused));outer.setOnClickListener(v->{try{startActivity(app.intent);}catch(Exception e){Toast.makeText(this,"Cannot open "+app.label,0).show();}});outer.setOnLongClickListener(v->{showDockOptions(app);return true;});return outer;
 }
 View appleAllApps(){
  LinearLayout outer=new LinearLayout(this);outer.setOrientation(1);outer.setTag("all-apps");outer.setContentDescription("All apps");outer.setFocusable(true);outer.setClickable(true);outer.setClipChildren(false);
  FrameLayout tile=new FrameLayout(this);GradientDrawable fill=new GradientDrawable(GradientDrawable.Orientation.TL_BR,dark?new int[]{0xff42516b,0xff202a3b}:new int[]{0xffdce8f3,0xffafc3da});fill.setCornerRadius(dp(12));tile.setBackground(fill);tile.setClipToOutline(true);tile.addView(new AppsGridSymbol(),new FrameLayout.LayoutParams(dp(47),dp(47),Gravity.CENTER));outer.addView(tile,new LinearLayout.LayoutParams(-1,dp(83)));
  TextView label=text("All apps",12,ink);label.setGravity(Gravity.CENTER);label.setAlpha(0);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(24));lp.topMargin=dp(5);outer.addView(label,lp);outer.setOnFocusChangeListener((v,focused)->appleFocus(tile,label,focused));outer.setOnClickListener(v->{allAppsPage=true;build();});return outer;
 }
 void appleFocus(View artwork,TextView label,boolean focused){artwork.animate().cancel();artwork.animate().scaleX(focused?1.09f:1).scaleY(focused?1.09f:1).translationY(focused?-dp(3):0).setDuration(110).start();artwork.setElevation(focused?dp(16):dp(2));label.setAlpha(focused?1:0);}
 class AppsGridSymbol extends View{final Paint paint=new Paint(3);AppsGridSymbol(){super(HomeActivity.this);}@Override protected void onDraw(Canvas c){paint.setColor(dark?0xfff3f7ff:0xff273d59);float unit=getWidth()/5f;for(int y=0;y<3;y++)for(int x=0;x<3;x++){float left=x*unit*1.8f,top=y*unit*1.8f;c.drawRoundRect(left,top,left+unit,top+unit,unit*.2f,unit*.2f,paint);}}}
 void buildAllApps(LinearLayout body){
  sceneTitle=null;dock=null;LinearLayout titleRow=new LinearLayout(this);titleRow.setGravity(Gravity.CENTER_VERTICAL);titleRow.setPadding(0,dp(24),0,dp(15));titleRow.addView(photoText("All apps",30),new LinearLayout.LayoutParams(0,-2,1));titleRow.addView(action("Back to Dock","back",()->closeApps()));body.addView(titleRow);body.addView(photoText("Select an app to open it. Hold OK for dock options.",12));
  ScrollView scroller=new ScrollView(this);scroller.setClipChildren(false);scroller.setClipToPadding(false);grid=new LinearLayout(this);grid.setClipChildren(false);grid.setClipToPadding(false);grid.setOrientation(1);grid.setPadding(dp(7),dp(20),dp(7),dp(10));scroller.addView(grid);body.addView(scroller,new LinearLayout.LayoutParams(-1,0,1));LinearLayout row=null;int col=0;
  int columns=uiTheme==1?5:6;for(App a:apps){if(col%columns==0){row=new LinearLayout(this);row.setClipChildren(false);grid.addView(row);}LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(uiTheme==1?111:91),1);p.setMargins(dp(uiTheme==1?6:0),dp(uiTheme==1?7:0),dp(uiTheme==1?18:10),dp(uiTheme==1?16:12));row.addView(uiTheme==1?appleCard(a):card(a),p);col++;}if(row!=null)while(col%columns!=0){row.addView(new View(this),new LinearLayout.LayoutParams(0,1,1));col++;}
 }
 void closeApps(){allAppsPage=false;focusTag="all-apps";build();}
 View card(App a){
  LinearLayout card=new LinearLayout(this);card.setTag("app:"+a.pkg);card.setContentDescription(a.label);card.setOrientation(1);card.setGravity(Gravity.CENTER);card.setPadding(dp(7),dp(9),dp(7),dp(7));card.setFocusable(true);card.setClickable(true);focusStyle(card,false);
  ImageView icon=new ImageView(this);Drawable.ConstantState cs=a.icon.getConstantState();icon.setImageDrawable(cs==null?a.icon:cs.newDrawable(getResources()).mutate());icon.setScaleType(ImageView.ScaleType.FIT_CENTER);card.addView(icon,new LinearLayout.LayoutParams(dp(uiTheme==1?62:44),dp(uiTheme==1?62:44)));TextView label=text(a.label,uiTheme==1?14:11,ink);label.setMaxLines(1);label.setEllipsize(android.text.TextUtils.TruncateAt.END);label.setGravity(Gravity.CENTER);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(6);card.addView(label,lp);
  card.setOnFocusChangeListener((v,f)->focusStyle(v,f));card.setOnClickListener(v->{try{startActivity(a.intent);}catch(Exception e){Toast.makeText(this,"Cannot open "+a.label,0).show();}});card.setOnLongClickListener(v->{showDockOptions(a);return true;});return card;
 }
 AlertDialog.Builder dialog(String title){return new AlertDialog.Builder(new ContextThemeWrapper(this,dark?android.R.style.Theme_Material_Dialog_Alert:android.R.style.Theme_Material_Light_Dialog_Alert)).setTitle(title);}
 void chooseTheme(){final int[] choice={uiTheme};dialog("Choose UI theme").setSingleChoiceItems(new String[]{"Scenic — compact dock","Apple TV — tvOS-style artwork grid"},uiTheme,(d,index)->choice[0]=index).setPositiveButton("Apply theme",(d,which)->{uiTheme=choice[0];getPreferences(0).edit().putInt("ui_theme",uiTheme).apply();focusTag="theme-selector";build();}).setNegativeButton("Cancel",null).show();}
 void showDockOptions(App a){
  if(!favorites.contains(a.pkg)){dialog(a.label).setItems(new String[]{"Add to Dock"},(d,which)->{favorites.add(a.pkg);saveDock();focusTag="app:"+a.pkg;build();}).setNegativeButton("Cancel",null).show();return;}
  dialog(a.label+" — Dock").setItems(new String[]{"Move left","Move right","Move to position\u2026","Remove from Dock"},(d,which)->{ArrayList<String> order=new ArrayList<>(favorites);int at=order.indexOf(a.pkg);
   if(which==0)moveApp(a.pkg,Math.max(0,at-1));else if(which==1)moveApp(a.pkg,Math.min(order.size()-1,at+1));else if(which==2){String[] positions=new String[order.size()];for(int i=0;i<positions.length;i++){App item=findApp(order.get(i));positions[i]=(i+1)+"  ·  "+(item==null?order.get(i):item.label);}dialog("Move "+a.label+" to position").setSingleChoiceItems(positions,at,(target,pos)->{target.dismiss();moveApp(a.pkg,pos);}).setNegativeButton("Cancel",null).show();}else{favorites.remove(a.pkg);saveDock();build();}
  }).setNegativeButton("Cancel",null).show();
 }
 void moveApp(String pkg,int position){ArrayList<String> order=new ArrayList<>(favorites);if(!order.remove(pkg))return;order.add(Math.max(0,Math.min(position,order.size())),pkg);favorites.clear();favorites.addAll(order);saveDock();focusTag="app:"+pkg;build();}
 void loadPhoto(){Bitmap replacement=null;try(InputStream in=getAssets().open(FILES[scene])){BitmapFactory.Options options=new BitmapFactory.Options();options.inPreferredConfig=Bitmap.Config.RGB_565;replacement=BitmapFactory.decodeStream(in,null,options);}catch(Exception ignored){}Bitmap old=photo;photo=replacement;if(old!=null&&old!=replacement)old.recycle();}
 void nextScene(){scene=(scene+1)%FILES.length;lastScene=System.currentTimeMillis();getPreferences(0).edit().putInt("scene",scene).apply();loadPhoto();if(landscape!=null)landscape.invalidate();if(sceneTitle!=null)sceneTitle.setText(SCENES[scene]);}
 void updateWifi(){
  int level=-1;String label="Offline";try{ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);NetworkCapabilities caps=cm.getNetworkCapabilities(cm.getActiveNetwork());if(caps!=null&&caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)){WifiManager wm=(WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);int rssi=wm.getConnectionInfo().getRssi();if(rssi>-127&&rssi<0){level=WifiManager.calculateSignalLevel(rssi,5);label=new String[]{"Weak","Fair","Good","Strong","Excellent"}[level];wifiSymbol.setContentDescription("Wi-Fi "+label+", "+rssi+" dBm");}else{label="Wi-Fi";wifiSymbol.setContentDescription("Wi-Fi connected; signal unavailable");}}else if(caps!=null&&caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))label="Ethernet";}catch(SecurityException ignored){label="Wi-Fi";}wifiSymbol.level=level;wifiSymbol.invalidate();wifiLabel.setText(label);
 }
 final Runnable tick=new Runnable(){public void run(){if(clock!=null)clock.setText(new SimpleDateFormat("EEE, d MMM   h:mm a",Locale.getDefault()).format(new Date()));if(lastScene==0)lastScene=System.currentTimeMillis();if(System.currentTimeMillis()-lastScene>=300000)nextScene();if(wifiSymbol!=null)updateWifi();handler.removeCallbacks(this);if(active)handler.postDelayed(this,15000);}};
 @Override protected void onResume(){super.onResume();active=true;if(appsDirty)refreshApps();tick.run();}
 @Override protected void onPause(){active=false;handler.removeCallbacks(tick);super.onPause();}
 @Override protected void onDestroy(){handler.removeCallbacks(tick);try{unregisterReceiver(packageChanges);}catch(Exception ignored){}if(photo!=null){photo.recycle();photo=null;}super.onDestroy();}
 @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);if(allAppsPage){allAppsPage=false;build();}}
 @Override public void onBackPressed(){if(allAppsPage)closeApps();else if(dock!=null&&dock.getChildCount()>0)dock.getChildAt(0).requestFocus();}
 class WifiSymbol extends View{
  final Paint p=new Paint(3);int level=-1;WifiSymbol(){super(HomeActivity.this);}
  @Override protected void onDraw(Canvas c){float w=getWidth(),h=getHeight(),cx=w/2,cy=h*.83f;p.setStrokeCap(Paint.Cap.ROUND);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1.7f));for(int ring=1;ring<=3;ring++){float r=w*(.14f+ring*.115f);p.setColor(level>=ring?Color.WHITE:0x55ffffff);c.drawArc(new RectF(cx-r,cy-r,cx+r,cy+r),225,90,false,p);}p.setStyle(Paint.Style.FILL);p.setColor(level>=0?Color.WHITE:0x66ffffff);c.drawCircle(cx,cy,dp(1.7f),p);if(level<0){p.setStrokeWidth(dp(1.5f));c.drawLine(w*.2f,h*.2f,w*.8f,h*.8f,p);}}
 }
 class Landscape extends View{
  final Paint p=new Paint(3);Landscape(){super(HomeActivity.this);}
  @Override protected void onDraw(Canvas c){float w=getWidth(),h=getHeight();c.drawColor(0xff20394b);if(photo!=null&&!photo.isRecycled()){float scale=Math.max(w/photo.getWidth(),h/photo.getHeight());float sw=photo.getWidth()*scale,sh=photo.getHeight()*scale;p.setShader(null);p.setColor(Color.WHITE);p.setFilterBitmap(true);c.drawBitmap(photo,null,new RectF((w-sw)/2,(h-sh)/2,(w+sw)/2,(h+sh)/2),p);}if(dark)c.drawColor(0x6e020914);p.setShader(new LinearGradient(0,0,0,h*.28f,new int[]{0x85040b13,0x00040b13},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h*.28f,p);int[] bottom=dark?new int[]{0x0007101b,0xc007101b,0xf507101b}:new int[]{0x00edf3f6,0x85edf3f6,0xf0edf3f6};p.setShader(new LinearGradient(0,h*(allAppsPage?.08f:.58f),0,h,bottom,new float[]{0,.63f,1},Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);}
 }
}
