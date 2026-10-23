package local.serene.tv;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.text.SimpleDateFormat;
import java.util.*;

/** Procedural weather artwork; no video downloads or animated bitmap allocation. */
final class WeatherCard extends View {
 final Weather weather; final boolean expanded; final Paint p=new Paint(3); final RectF r=new RectF();
 boolean running=true; int preview=-1; final long start=android.os.SystemClock.uptimeMillis();
 WeatherCard(Context c,Weather w,boolean full){super(c);weather=w;expanded=full;}
 void animateWeather(boolean value){running=value;invalidate();}
 void preview(int code){preview=code;invalidate();}
 void text(Canvas c,String s,float x,float y,float size,int color){p.setShader(null);p.setColor(color);p.setTextSize(size);p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));c.drawText(s,x,y,p);}
 void circle(Canvas c,float x,float y,float radius,int color){p.setShader(null);p.setColor(color);c.drawCircle(x,y,radius,p);}
 void cloud(Canvas c,float x,float y,float scale,boolean storm){c.save();c.translate(x,y);c.scale(scale,scale);int shade=storm?0xff8294a9:0xffedf5fa;p.setShader(new LinearGradient(0,-45,0,30,new int[]{0xfffafcff,shade,storm?0xff405368:0xff9fb5c5},null,Shader.TileMode.CLAMP));r.set(-75,-8,78,35);c.drawRoundRect(r,28,28,p);c.drawCircle(-40,-10,33,p);c.drawCircle(0,-30,45,p);c.drawCircle(40,-12,35,p);p.setShader(null);c.restore();}
 boolean wet(int code){return code>=51&&code<=67||code>=80&&code<=82||code>=95;}
 void sky(Canvas c,int code,float time,float x,float y,float scale){c.save();c.translate(x,y);c.scale(scale,scale);boolean rain=wet(code),snow=code>=71&&code<=77||code==85||code==86;
  if(code<=2){float pulse=1+.025f*(float)Math.sin(time);p.setShader(new RadialGradient(0,-20,100,new int[]{0x66ffe6a0,0x00ffe6a0},null,Shader.TileMode.CLAMP));c.drawCircle(0,-20,100,p);p.setShader(null);c.save();c.rotate(time*3,0,-20);p.setColor(0x77ffdf85);p.setStrokeWidth(3);for(int i=0;i<12;i++){double a=i*Math.PI/6;c.drawLine((float)Math.cos(a)*48,-20+(float)Math.sin(a)*48,(float)Math.cos(a)*63,-20+(float)Math.sin(a)*63,p);}c.restore();circle(c,0,-20,38*pulse,0xffffd778);}
  if(code>0){float drift=(float)Math.sin(time*.35)*8;cloud(c,drift+12,code<=2?10:0,1,rain);}
  if(rain||snow){c.save();c.clipRect(-120,24,125,145);p.setColor(snow?0xffeefaff:0xff9fdaff);p.setStrokeWidth(2);for(int i=0;i<26;i++){float xx=(i*47%240)-120, yy=(i*23+time*(snow?22:100))%160;if(snow){c.drawCircle(xx+(float)Math.sin(time+i)*7,yy,2.3f,p);}else c.drawLine(xx,yy,xx-7,yy+18,p);}c.restore();}
  if(code>=95&&((int)(time*2)%9==0)){Path bolt=new Path();bolt.moveTo(15,23);bolt.lineTo(-4,65);bolt.lineTo(13,63);bolt.lineTo(-5,101);bolt.lineTo(36,48);bolt.lineTo(18,50);bolt.close();p.setColor(0xffe5e4ff);c.drawPath(bolt,p);}
  if(code==45||code==48){p.setColor(0x889fbdcc);p.setStrokeWidth(5);for(int i=0;i<4;i++)c.drawLine(-100+(float)Math.sin(time+i)*10,35+i*15,105,35+i*15,p);}
 c.restore();}
 @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);float width=expanded?760:536,height=expanded?380:184;canvas.save();canvas.scale(getWidth()/width,getHeight()/height);r.set(0,0,width,height);Path clip=new Path();clip.addRoundRect(r,26,26,Path.Direction.CW);canvas.clipPath(clip);int code=preview>=0?preview:weather.code();float t=(android.os.SystemClock.uptimeMillis()-start)/1000f;
 p.setShader(new LinearGradient(0,0,width,height,wet(code)?new int[]{0xff172e49,0xff4c7185}:new int[]{0xff123c59,0xff538fa9},null,Shader.TileMode.CLAMP));canvas.drawRect(r,p);p.setShader(null);
 // Atmospheric hills retain a quiet nature backdrop in the reference's spirit.
 for(int i=0;i<3;i++){Path hill=new Path();hill.moveTo(0,height);hill.lineTo(0,height*(.69f+i*.07f));hill.cubicTo(width*.25f,height*.4f,width*.53f,height*.98f,width,height*(.55f+i*.12f));hill.lineTo(width,height);hill.close();p.setColor(new int[]{0x22203349,0x33203349,0x55203349}[i]);canvas.drawPath(hill,p);}
 if(expanded){text(canvas,"Doha, Qatar",28,38,23,Color.WHITE);SimpleDateFormat clock=new SimpleDateFormat("HH:mm",Locale.getDefault());clock.setTimeZone(TimeZone.getTimeZone("Asia/Qatar"));text(canvas,clock.format(new Date()),26,106,60,Color.WHITE);SimpleDateFormat date=new SimpleDateFormat("EEE · d MMM",Locale.getDefault());date.setTimeZone(TimeZone.getTimeZone("Asia/Qatar"));text(canvas,date.format(new Date()),30,137,18,0xffd6e5ed);text(canvas,weather.temperature(),28,188,40,Color.WHITE);text(canvas,preview>=0?"Preview · "+Weather.condition(code):weather.conditionLabel(),30,217,18,Color.WHITE);text(canvas,weather.status(),30,242,13,0xffd6e5ed);text(canvas,weather.metrics(),340,242,13,0xffd6e5ed);sky(canvas,code,t,540,108,1.2f);
 p.setColor(0x55203549);canvas.drawRect(0,267,760,380,p);for(int i=0;i<4;i++){float x=i*190;text(canvas,weather.day(i+1),x+25,291,16,Color.WHITE);sky(canvas,weather.dayCode(i+1),t,x+135,307,.27f);text(canvas,weather.range(i+1),x+25,352,21,Color.WHITE);} 
 }else{text(canvas,"DOHA",24,34,17,0xffd7e9f1);text(canvas,weather.temperature(),22,88,43,Color.WHITE);text(canvas,weather.conditionLabel(),24,119,19,Color.WHITE);text(canvas,weather.range(0)+"  ·  "+weather.status(),24,159,14,0xffd7e9f1);sky(canvas,code,t,412,66,.64f);}
 canvas.restore();if(running&&isShown()&&getWindowVisibility()==VISIBLE)postInvalidateDelayed(40);
 }
}
