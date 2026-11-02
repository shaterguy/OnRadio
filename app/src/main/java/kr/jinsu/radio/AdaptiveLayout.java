package kr.jinsu.radio;

final class AdaptiveLayout {
 static boolean twoPane(int widthDp,int heightDp){return widthDp>=600&&heightDp>=480;}
 static int stationPaneWidth(int widthDp){return Math.max(280,Math.min(420,Math.round(widthDp*.40f)));}
 static final int DETAIL_MAX_WIDTH=680,DOCK_MAX_WIDTH=1120;
}
