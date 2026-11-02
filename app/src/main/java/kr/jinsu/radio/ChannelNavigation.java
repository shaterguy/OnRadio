package kr.jinsu.radio;

import java.util.*;

final class ChannelNavigation {
 static RadioData.Station next(String currentId,int delta,boolean favoritesOnly,Set<String> favorites){
  List<RadioData.Station> ordered=RadioData.orderedStations("frequency"),pool=new ArrayList<>();
  if(favoritesOnly)for(RadioData.Station station:ordered)if(favorites.contains(station.id))pool.add(station);
  if(pool.isEmpty())pool.addAll(ordered);
  int index=-1;for(int i=0;i<pool.size();i++)if(pool.get(i).id.equals(currentId)){index=i;break;}
  return pool.get(index<0?(delta>0?0:pool.size()-1):Math.floorMod(index+delta,pool.size()));
 }
}
