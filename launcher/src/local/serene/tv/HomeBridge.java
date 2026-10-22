package local.serene.tv;
import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.content.Intent;
public class HomeBridge extends AccessibilityService {
    long lastLaunch;
    @Override protected void onServiceConnected(){
        startActivity(new Intent(this,HomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType()!=AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        if (!"com.ooredoo.tv.stb".contentEquals(event.getPackageName()==null?"":event.getPackageName()))return;
        String name=String.valueOf(event.getClassName());
        if (!name.equals("com.ifeelsmart.VoltStb.MainActivity"))return;
        long now=android.os.SystemClock.elapsedRealtime();
        if(now-lastLaunch<800)return;
        lastLaunch=now;
        startActivity(new Intent(this,HomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
    }
    @Override public void onInterrupt(){}
}
