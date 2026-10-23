$ErrorActionPreference = 'Stop'
$output = Join-Path $PSScriptRoot 'launcher\assets\home-ambient.wav'

# Original 48-second Indian-inspired instrumental, rendered once at build time. No downloaded music.
Add-Type -TypeDefinition @'
using System;
using System.IO;
using System.Text;
public static class SereneIndian {
 static double Frequency(int midi) { return 440.0*Math.Pow(2,(midi-69)/12.0); }
 static void Note(double[] mix,int rate,double start,double length,int midi,double gain,bool pad) {
  double freq=Frequency(midi); int count=(int)(length*rate),offset=(int)(start*rate);
  for(int i=0;i<count;i++) {
   double t=(double)i/rate,attack=pad?2.0:0.025;
   double env=Math.Min(1,t/attack)*(pad?Math.Pow(Math.Sin(Math.PI*t/length),2):Math.Exp(-t/1.5)*Math.Pow(1-t/length,2));
   double wave=Math.Sin(2*Math.PI*freq*t)+0.13*Math.Sin(2*Math.PI*freq*2*t)+0.035*Math.Sin(2*Math.PI*freq*3*t);
   mix[(offset+i)%mix.Length]+=gain*env*wave;
  }
 }
 static void Flute(double[] mix,int rate,double start,double length,int midi,double gain){
  double f=Frequency(midi);int count=(int)(length*rate),offset=(int)(start*rate);
  for(int i=0;i<count;i++){
   double t=(double)i/rate;
   double env=Math.Min(1,t/.16)*Math.Min(1,(length-t)/.3);
   double phase=2*Math.PI*f*t+2*Math.PI*f*(-.018*(1-Math.Exp(-15*t))/15-.0018*Math.Cos(2*Math.PI*4.8*t)/(2*Math.PI*4.8));
   double sample=gain*env*(Math.Sin(phase)+.16*Math.Sin(2*phase)+.025*Math.Sin(3*phase));
   int k=(offset+i)%mix.Length;mix[k]+=sample;mix[(k+(int)(rate*.21))%mix.Length]+=sample*.13;
  }
 }
 public static void Render(string path) {
  const int rate=22050; double[] mix=new double[rate*48];
  // Original Indian-inspired D pentatonic melody: flute-like lead, plucked strings, drone.
  for(int bar=0;bar<8;bar++) foreach(int note in new[]{38,45,50}) Note(mix,rate,bar*6,12,note,0.022,true);
  int[] melody={74,78,81,83,81,78,76,74,76,78,81,78,76,74,71,74,78,81,86,83,81,78,76,74};
  for(int i=0;i<melody.Length;i++) Flute(mix,rate,i*2+0.2,1.65,melody[i],0.08);
  int[] strings={50,57,62,57,54,57,59,57};
  for(int i=0;i<32;i++) Note(mix,rate,i*1.5,2.5,strings[i%strings.Length],0.028,false);
  double peak=0;foreach(double value in mix)peak=Math.Max(peak,Math.Abs(value));double scale=peak>0?0.65/peak:0;
  using(BinaryWriter writer=new BinaryWriter(File.Create(path))) {
   int bytes=mix.Length*2;writer.Write(Encoding.ASCII.GetBytes("RIFF"));writer.Write(36+bytes);writer.Write(Encoding.ASCII.GetBytes("WAVEfmt "));writer.Write(16);writer.Write((short)1);writer.Write((short)1);writer.Write(rate);writer.Write(rate*2);writer.Write((short)2);writer.Write((short)16);writer.Write(Encoding.ASCII.GetBytes("data"));writer.Write(bytes);
   foreach(double value in mix)writer.Write((short)Math.Round(Math.Max(-1,Math.Min(1,value*scale))*32767));
  }
 }
}
'@
[SereneIndian]::Render($output)
Write-Host 'Generated original Indian-inspired instrumental music.'
