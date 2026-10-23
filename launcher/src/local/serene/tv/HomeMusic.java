package local.serene.tv;

import android.content.*;
import android.content.res.AssetFileDescriptor;
import android.media.*;
import android.os.*;
import android.util.Log;

/** Own-player volume only. Never changes system/stream volume or mute state. */
final class HomeMusic {
 private final Context context;
 private final AudioManager audio;
 private final Handler handler=new Handler(Looper.getMainLooper());
 private final AudioAttributes attributes=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
 private final AudioFocusRequest focus;
 private MediaPlayer player;
 private boolean visible,muted,prepared,hasFocus,released,focusRequested;
 private final BroadcastReceiver screenOff=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){setVisible(false);}};
 HomeMusic(Context c){
  context=c;audio=(AudioManager)c.getSystemService(Context.AUDIO_SERVICE);
  focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(attributes).setWillPauseWhenDucked(true).setOnAudioFocusChangeListener(change->{
   if(change==AudioManager.AUDIOFOCUS_GAIN){hasFocus=true;playIfReady();}
   else{hasFocus=false;pausePlayer();}
  },handler).build();
  if(Build.VERSION.SDK_INT>=33)c.registerReceiver(screenOff,new IntentFilter(Intent.ACTION_SCREEN_OFF),Context.RECEIVER_NOT_EXPORTED);else c.registerReceiver(screenOff,new IntentFilter(Intent.ACTION_SCREEN_OFF));
 }
 void setMuted(boolean value){muted=value;if(muted)stop();else start();}
 void setVisible(boolean value){visible=value;if(visible)start();else stop();}
 private void start(){
  if(released||!visible||muted||!((PowerManager)context.getSystemService(Context.POWER_SERVICE)).isInteractive())return;
  if(player!=null&&prepared&&player.isPlaying())return;
  // Preserve music/casting/PiP audio that is already playing from another app.
  if(!hasFocus&&audio.isMusicActive())return;
  if(!hasFocus){focusRequested=true;hasFocus=audio.requestAudioFocus(focus)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;if(!hasFocus){stop();return;}}
  if(player==null){
   MediaPlayer created=new MediaPlayer();player=created;prepared=false;
   try(AssetFileDescriptor asset=context.getAssets().openFd("home-ambient.wav")){
    created.setAudioAttributes(attributes);created.setDataSource(asset.getFileDescriptor(),asset.getStartOffset(),asset.getLength());created.setLooping(true);created.setVolume(0.14f,0.14f);
    created.setOnPreparedListener(p->{if(player!=p)return;prepared=true;playIfReady();});
    created.setOnErrorListener((p,what,extra)->{stop();return true;});created.prepareAsync();
   }catch(Exception e){Log.w("HomeMusic","Ambient track unavailable",e);stop();}
  }else playIfReady();
 }
 private void playIfReady(){if(player!=null&&prepared&&visible&&!muted&&hasFocus&&!released){player.setVolume(0.14f,0.14f);player.start();}}
 private void pausePlayer(){if(player!=null&&prepared&&player.isPlaying())player.pause();}
 private void stop(){
  if(player!=null){player.setOnPreparedListener(null);player.setOnErrorListener(null);player.release();player=null;}prepared=false;
  hasFocus=false;if(focusRequested){focusRequested=false;audio.abandonAudioFocusRequest(focus); }
 }
 void release(){released=true;visible=false;stop();try{context.unregisterReceiver(screenOff);}catch(Exception ignored){}}
}
