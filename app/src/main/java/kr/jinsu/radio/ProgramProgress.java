package kr.jinsu.radio;

final class ProgramProgress {
 static final String START_KEY="onradio.program_start",END_KEY="onradio.program_end";
 static boolean valid(long start,long end){return end>start&&end-start>0;}
 static long duration(long start,long end){return valid(start,end)?end-start:0;}
 static long elapsed(long start,long end,long now){return valid(start,end)?Math.max(0,Math.min(end-start,now-start)):0;}
 static int scaled(long start,long end,long now){long duration=duration(start,end);return duration==0?0:(int)Math.round(elapsed(start,end,now)*10000.0/duration);}
 static boolean current(long start,long end,long now){return valid(start,end)&&start<=now&&now<end;}
}
