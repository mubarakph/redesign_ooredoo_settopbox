$ErrorActionPreference = 'Stop'
$output = Join-Path $PSScriptRoot 'launcher\assets\home-ambient.wav'
if (Test-Path -LiteralPath $output) { return }
# Original 32-second ambient score, rendered once at build time. No downloaded music.
Add-Type -TypeDefinition @'
using System;
using System.IO;
using System.Text;
public static class SereneAmbient {
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
 public static void Render(string path) {
  const int rate=22050; double[] mix=new double[rate*32];
  int[][] chords={new[]{48,55,59,64},new[]{45,52,55,60},new[]{41,48,52,57},new[]{43,50,52,59}};
  for(int bar=0;bar<4;bar++) foreach(int note in chords[bar]) Note(mix,rate,bar*8,12,note,0.028,true);
  int[] melody={72,76,79,76,71,67,72,76,69,72,76,72,74,71,67,71};
  for(int i=0;i<melody.Length;i++) Note(mix,rate,i*2+0.5,5,melody[i],0.06,false);
  double peak=0;foreach(double value in mix)peak=Math.Max(peak,Math.Abs(value));double scale=peak>0?0.65/peak:0;
  using(BinaryWriter writer=new BinaryWriter(File.Create(path))) {
   int bytes=mix.Length*2;writer.Write(Encoding.ASCII.GetBytes("RIFF"));writer.Write(36+bytes);writer.Write(Encoding.ASCII.GetBytes("WAVEfmt "));writer.Write(16);writer.Write((short)1);writer.Write((short)1);writer.Write(rate);writer.Write(rate*2);writer.Write((short)2);writer.Write((short)16);writer.Write(Encoding.ASCII.GetBytes("data"));writer.Write(bytes);
   foreach(double value in mix)writer.Write((short)Math.Round(Math.Max(-1,Math.Min(1,value*scale))*32767));
  }
 }
}
'@
[SereneAmbient]::Render($output)
Write-Host 'Generated original offline ambient music.'
