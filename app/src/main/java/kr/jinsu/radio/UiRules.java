package kr.jinsu.radio;

final class UiRules {
 static String nextSort(String current,String tapped){return current.equals(tapped)?tapped+"_desc":tapped;}
 static boolean active(String order,String column){return order.equals(column)||order.equals(column+"_desc");}
 static String sortLabel(String order,String column,String label){return label+(active(order,column)?(order.endsWith("_desc")?" ▼":" ▲"):"");}
 static int centeredScrollY(int rowTop,int rowHeight,int viewportHeight){return Math.max(0,rowTop+(rowHeight-viewportHeight)/2);}
}
