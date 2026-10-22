package local.serene.tv;

import android.app.job.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.text.Html;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Personal, bounded offline cache of landscape photos from Commons nature categories. */
public class WallpaperSync extends JobService {
 static final int DAILY=6101,NOW=6102,MAX_PHOTOS=14;
 static final long HOUR=3600000L;
 static final String UPDATED="local.serene.tv.WALLPAPERS_UPDATED";
 static final String[] CATEGORIES={"Featured pictures of forests","Featured pictures of mountains","Featured pictures of waterfalls"};
 static final AtomicBoolean RUNNING=new AtomicBoolean(false);
 Thread worker;
 static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("wallpaper-sync",0);}
 static File directory(Context c){File d=new File(c.getFilesDir(),"nature-wallpapers");d.mkdirs();return d;}
 static AtomicFile index(Context c){return new AtomicFile(new File(directory(c),"photos.json"));}
 static boolean enabled(Context c){return prefs(c).getBoolean("enabled",true);}
 static void schedule(Context c){
  JobScheduler scheduler=(JobScheduler)c.getSystemService(JOB_SCHEDULER_SERVICE);
  if(!enabled(c)){scheduler.cancel(DAILY);scheduler.cancel(NOW);return;}
  ComponentName service=new ComponentName(c,WallpaperSync.class);
  if(scheduler.getPendingJob(DAILY)==null){JobInfo job=new JobInfo.Builder(DAILY,service).setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED).setRequiresStorageNotLow(true).setPersisted(true).setPeriodic(24*HOUR,4*HOUR).setBackoffCriteria(2*HOUR,JobInfo.BACKOFF_POLICY_EXPONENTIAL).build();scheduler.schedule(job);}
  if(prefs(c).getLong("last_success",0)==0&&scheduler.getPendingJob(NOW)==null)requestNow(c,false);
 }
 static void requestNow(Context c,boolean manual){JobScheduler scheduler=(JobScheduler)c.getSystemService(JOB_SCHEDULER_SERVICE);PersistableBundle extra=new PersistableBundle();extra.putBoolean("manual",manual);scheduler.schedule(new JobInfo.Builder(NOW,new ComponentName(c,WallpaperSync.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED).setRequiresStorageNotLow(true).setExtras(extra).setBackoffCriteria(30*60*1000L,JobInfo.BACKOFF_POLICY_EXPONENTIAL).build());}
 static class Photo{String file,title,author,license,source;}
 static synchronized ArrayList<Photo> photos(Context c){
  ArrayList<Photo> result=new ArrayList<>();try{JSONArray array=new JSONArray(new String(index(c).readFully(),StandardCharsets.UTF_8));for(int i=0;i<array.length();i++){JSONObject j=array.getJSONObject(i);String filename=j.getString("file");if(!filename.matches("commons-[0-9]+\\.jpg")||!new File(directory(c),filename).isFile())continue;Photo p=new Photo();p.file=filename;p.title=j.optString("title");p.author=j.optString("author");p.license=j.optString("license");p.source=j.optString("source");result.add(p);}}catch(Exception ignored){}return result;
 }
 static synchronized void save(Context c,ArrayList<Photo> list)throws Exception{
  JSONArray array=new JSONArray();for(Photo p:list){JSONObject j=new JSONObject();j.put("file",p.file);j.put("title",p.title);j.put("author",p.author);j.put("license",p.license);j.put("source",p.source);array.put(j);}
  AtomicFile af=index(c);FileOutputStream out=null;try{out=af.startWrite();out.write(array.toString().getBytes(StandardCharsets.UTF_8));af.finishWrite(out);}catch(Exception e){if(out!=null)af.failWrite(out);throw e;}
 }
 static String clean(String value){String s=Html.fromHtml(value,Html.FROM_HTML_MODE_LEGACY).toString().replaceAll("\\s+"," ").trim();return s.length()>250?s.substring(0,250):s;}
 static String meta(JSONObject metadata,String name){JSONObject field=metadata.optJSONObject(name);return field==null?"":clean(field.optString("value"));}
 static byte[] fetch(String address,int maxBytes)throws Exception{
  for(int redirect=0;redirect<4;redirect++){
   URL url=new URL(address);String host=url.getHost();if(!url.getProtocol().equals("https")||!(host.equals("commons.wikimedia.org")||host.equals("upload.wikimedia.org")||host.equals("thumb.wikimedia.org"))||(url.getPort()!=-1&&url.getPort()!=443))throw new IOException("Photo host rejected");
   HttpURLConnection conn=(HttpURLConnection)url.openConnection();conn.setConnectTimeout(15000);conn.setReadTimeout(20000);conn.setInstanceFollowRedirects(false);conn.setRequestProperty("User-Agent","SereneHome/1.0 (personal nature wallpaper; https://github.com/mubarakph/redesign_ooredoo_settopbox)");
   try{int code=conn.getResponseCode();if(code>=300&&code<400){String location=conn.getHeaderField("Location");if(location==null)throw new IOException("Missing redirect");address=new URL(url,location).toString();continue;}if(code!=200)throw new IOException("Photo source HTTP "+code);if(conn.getContentLengthLong()>maxBytes)throw new IOException("Photo response too large");try(InputStream in=conn.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int read;while((read=in.read(buffer))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();if(out.size()+read>maxBytes)throw new IOException("Photo response too large");out.write(buffer,0,read);}return out.toByteArray();}}
   finally{conn.disconnect();}
  }throw new IOException("Too many redirects");
 }
 static boolean sync(Context c,boolean manual){
  if(!enabled(c))return true;
  if(!manual&&System.currentTimeMillis()-prefs(c).getLong("last_success",0)<20*HOUR)return true;
  ArrayList<Photo> stored=photos(c);Set<String> known=new HashSet<>();for(Photo p:stored)known.add(p.file);
  int start=prefs(c).getInt("category",0)%CATEGORIES.length;
  try{for(int attempt=0;attempt<CATEGORIES.length;attempt++){
   int cat=(start+attempt)%CATEGORIES.length;String cursor=prefs(c).getString("cursor_"+cat,"");
   String url="https://commons.wikimedia.org/w/api.php?action=query&generator=categorymembers&gcmtype=file&gcmlimit=50&prop=imageinfo&iiprop=url%7Csize%7Cextmetadata&iiurlwidth=1280&format=json&gcmtitle="+URLEncoder.encode("Category:"+CATEGORIES[cat],"UTF-8");if(!cursor.isEmpty())url+="&gcmcontinue="+URLEncoder.encode(cursor,"UTF-8");
   JSONObject response=new JSONObject(new String(fetch(url,1024*1024),StandardCharsets.UTF_8));JSONObject continuation=response.optJSONObject("continue");prefs(c).edit().putString("cursor_"+cat,continuation==null?"":continuation.optString("gcmcontinue")).apply();
   JSONObject query=response.optJSONObject("query"),pages=query==null?null:query.optJSONObject("pages");if(pages==null)continue;ArrayList<String> keys=new ArrayList<>();Iterator<String> iterator=pages.keys();while(iterator.hasNext())keys.add(iterator.next());Collections.shuffle(keys);
   for(String key:keys){JSONObject page=pages.getJSONObject(key);String title=page.optString("title"),filename="commons-"+page.optLong("pageid")+".jpg";
    if(known.contains(filename)||!title.toLowerCase(Locale.ROOT).matches(".*\\.(jpg|jpeg|png)$")||title.toLowerCase(Locale.ROOT).matches(".*(portrait|people|city|building|temple|bridge|train|road|vehicle|painting|illustration).*"))continue;
    JSONArray infos=page.optJSONArray("imageinfo");if(infos==null||infos.length()==0)continue;JSONObject info=infos.getJSONObject(0);if(info.optInt("width")<1000||info.optInt("height")<600||info.optDouble("width")/Math.max(1,info.optInt("height"))<1.4)continue;
    JSONObject metadata=info.optJSONObject("extmetadata");if(metadata==null)continue;String license=meta(metadata,"LicenseShortName");if(!(license.startsWith("CC BY")||license.contains("CC0")||license.toLowerCase(Locale.ROOT).contains("public domain")))continue;
    byte[] bytes=fetch(info.getString("thumburl"),8*1024*1024);BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,bounds);if(bounds.outWidth<1||bounds.outHeight<1||bounds.outWidth>4096||bounds.outHeight>4096)continue;
    BitmapFactory.Options options=new BitmapFactory.Options();options.inPreferredConfig=Bitmap.Config.RGB_565;Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);if(bitmap==null)continue;
    Bitmap scaled=Bitmap.createScaledBitmap(bitmap,1280,Math.max(1,Math.round(bitmap.getHeight()*1280f/bitmap.getWidth())),true);AtomicFile output=new AtomicFile(new File(directory(c),filename));FileOutputStream stream=null;
    try{stream=output.startWrite();if(!scaled.compress(Bitmap.CompressFormat.JPEG,72,stream))throw new IOException("JPEG encoding failed");output.finishWrite(stream);}catch(Exception e){if(stream!=null)output.failWrite(stream);throw e;}finally{if(scaled!=bitmap)scaled.recycle();bitmap.recycle();}
    Photo photo=new Photo();photo.file=filename;photo.title=clean(title.replaceFirst("^File:","").replaceFirst("(?i)\\.(jpg|jpeg|png)$",""));photo.author=meta(metadata,"Artist");String licenseUrl=meta(metadata,"LicenseUrl");photo.license=license+(licenseUrl.isEmpty()?"":"\n"+licenseUrl);photo.source=info.optString("descriptionurl");stored.add(photo);
    ArrayList<Photo> retired=new ArrayList<>();while(stored.size()>MAX_PHOTOS)retired.add(stored.remove(0));save(c,stored);for(Photo old:retired)new File(directory(c),old.file).delete();
    prefs(c).edit().putLong("last_success",System.currentTimeMillis()).putInt("category",(cat+1)%CATEGORIES.length).putString("status","Added "+photo.title).apply();c.sendBroadcast(new Intent(UPDATED).setPackage(c.getPackageName()));return true;
   }
  }throw new IOException("No new landscape photo found; keeping offline collection");}
  catch(Exception e){prefs(c).edit().putString("status",e.getMessage()==null?"Photo source unavailable; keeping offline collection":e.getMessage()).apply();return false;}
 }
 @Override public boolean onStartJob(JobParameters params){if(!RUNNING.compareAndSet(false,true))return false;worker=new Thread(()->{boolean ok;try{ok=sync(this,params.getExtras().getBoolean("manual",false));}finally{RUNNING.set(false);}if(!Thread.currentThread().isInterrupted())jobFinished(params,!ok&&enabled(this));},"Daily nature sync");worker.start();return true;}
 @Override public boolean onStopJob(JobParameters params){if(worker!=null)worker.interrupt();return enabled(this);}
}
