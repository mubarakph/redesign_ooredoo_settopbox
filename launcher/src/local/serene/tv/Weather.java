package local.serene.tv;
import android.content.*;
import android.os.*;
import org.json.*;
import java.net.*;
import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/** Small city-level weather cache; no location permission or background service. */
final class Weather {
 private final SharedPreferences prefs;
 private final Handler main=new Handler(Looper.getMainLooper());
 private boolean loading; private long attempt;
 Weather(Context context){prefs=context.getSharedPreferences("weather",0);}
 boolean hasForecast(){try{return new JSONObject(prefs.getString("data","")).getJSONObject("daily").getJSONArray("time").length()>=5;}catch(Exception e){return false;}}
 int code(){try{return new JSONObject(prefs.getString("data","")).getJSONObject("current").getInt("weather_code");}catch(Exception e){return -1;}}
 String metrics(){try{JSONObject c=new JSONObject(prefs.getString("data","")).getJSONObject("current");return "Feels "+Math.round(c.getDouble("apparent_temperature"))+"° · Humidity "+c.getInt("relative_humidity_2m")+"% · Wind "+Math.round(c.getDouble("wind_speed_10m"))+" km/h";}catch(Exception e){return "";}}
 String temperature(){try{return Math.round(new JSONObject(prefs.getString("data","")).getJSONObject("current").getDouble("temperature_2m"))+"°C";}catch(Exception e){return "—°C";}}
 String conditionLabel(){return code()<0?"Weather unavailable":condition(code());}
 int dayCode(int day){try{return new JSONObject(prefs.getString("data","")).getJSONObject("daily").getJSONArray("weather_code").getInt(day);}catch(Exception e){return -1;}}
 String range(int day){try{JSONObject d=new JSONObject(prefs.getString("data","")).getJSONObject("daily");return Math.round(d.getJSONArray("temperature_2m_max").getDouble(day))+"° / "+Math.round(d.getJSONArray("temperature_2m_min").getDouble(day))+"°";}catch(Exception e){return "— / —";}}
 String day(int day){try{String value=new JSONObject(prefs.getString("data","")).getJSONObject("daily").getJSONArray("time").getString(day);return new SimpleDateFormat("EEE",Locale.getDefault()).format(new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(value)).toUpperCase(Locale.getDefault());}catch(Exception e){return "—";}}
 String summary(){try{JSONObject j=new JSONObject(prefs.getString("data",""));JSONObject c=j.getJSONObject("current"),d=j.getJSONObject("daily");return "Doha  "+Math.round(c.getDouble("temperature_2m"))+"°C  ·  "+condition(c.getInt("weather_code"))+"\nH "+Math.round(d.getJSONArray("temperature_2m_max").getDouble(0))+"°  L "+Math.round(d.getJSONArray("temperature_2m_min").getDouble(0))+"°  ·  "+status();}catch(Exception e){return "Doha, Qatar\n"+(loading?"Loading weather…":"Weather unavailable · Select to retry");}}
 String status(){long time=prefs.getLong("updated",0);if(time==0)return "Not updated yet";SimpleDateFormat f=new SimpleDateFormat("h:mm a",Locale.getDefault());f.setTimeZone(TimeZone.getTimeZone("Asia/Qatar"));return (System.currentTimeMillis()-time>3600000?"Last updated ":"Updated ")+f.format(new Date(time));}
 String details(){try{JSONObject c=new JSONObject(prefs.getString("data","")).getJSONObject("current");return summary()+"\nFeels like "+Math.round(c.getDouble("apparent_temperature"))+"°C · Humidity "+c.getInt("relative_humidity_2m")+"%\nWind "+Math.round(c.getDouble("wind_speed_10m"))+" km/h\n\nWeather data: Open-Meteo (CC BY 4.0)\nopen-meteo.com\nCity: Doha, Qatar";}catch(Exception e){return summary()+"\n\nWeather data: Open-Meteo · open-meteo.com";}}
 void refresh(boolean force,Runnable update){long now=System.currentTimeMillis();if(loading||(!force&&(now-prefs.getLong("updated",0)<1800000&&hasForecast()||now-attempt<300000)))return;loading=true;attempt=now;new Thread(()->{HttpsURLConnection connection=null;try{
  URL url=new URL("https://api.open-meteo.com/v1/forecast?latitude=25.2854&longitude=51.5310&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m&daily=weather_code,temperature_2m_max,temperature_2m_min&timezone=Asia%2FQatar&forecast_days=5");
  connection=(HttpsURLConnection)url.openConnection();connection.setConnectTimeout(10000);connection.setReadTimeout(10000);if(connection.getResponseCode()!=200)throw new IOException("HTTP error");ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(InputStream in=connection.getInputStream()){byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1){if(bytes.size()+n>65536)throw new IOException("Response too large");bytes.write(buffer,0,n);}}
  String body=new String(bytes.toByteArray(),StandardCharsets.UTF_8);JSONObject j=new JSONObject(body);double temperature=j.getJSONObject("current").getDouble("temperature_2m");if(!Double.isFinite(temperature)||temperature< -100||temperature>70)throw new IOException("Invalid temperature");j.getJSONObject("daily").getJSONArray("temperature_2m_max").getDouble(0);j.getJSONObject("daily").getJSONArray("temperature_2m_min").getDouble(0);prefs.edit().putString("data",body).putLong("updated",System.currentTimeMillis()).apply();
 }catch(Exception ignored){}finally{if(connection!=null)connection.disconnect();main.post(()->{loading=false;update.run();});}},"Doha weather").start();}
 static String condition(int code){if(code==0)return "Clear";if(code==1)return "Mostly clear";if(code==2)return "Partly cloudy";if(code==3)return "Overcast";if(code==45||code==48)return "Fog";if(code>=51&&code<=57)return "Drizzle";if(code>=61&&code<=67)return "Rain";if(code>=71&&code<=77)return "Snow";if(code>=80&&code<=82)return "Showers";if(code==85||code==86)return "Snow showers";if(code>=95&&code<=99)return "Thunderstorms";return "Conditions unavailable";}
}
