package main.foodtruckfinder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class FoodTruckCache extends TreeMap<Integer, List<FoodTruck>> {

    private static FoodTruckCache cache = new FoodTruckCache();

    private FoodTruckCache(){}

    public static FoodTruckCache getCache(){
        return cache;
    }

    public static void cleanUp(){
        cache = new FoodTruckCache();
    }

    /**
     * Food Truck Cache store the results
     * key: dow-hr-index, e.g. Monday, 14PM, index 000 --> 114000
     * value: List<FoodTruck> , and list size is 10 at most
     *
     * @param ftList: List of FootTruck object get from SODA API
     * @param dow: day of week, e.g. Monday - 1
     * @param currentHour: hour in format HH, e.g. 14 for 2PM
     */
    public void put(List<FoodTruck> ftList, int dow, String currentHour){
        for(int i=0; i < ftList.size(); i+=10){
            int page = i/10;
            List<FoodTruck> newPage = new ArrayList<>();
            for(int j=i; j < Math.min(i+10, ftList.size()); j++){
                newPage.add(ftList.get(j));
            }

            String keyStr = dow + currentHour + String.format("%03d", page);
            int key = Integer.valueOf(keyStr);
            getCache().put(key, newPage);
        }
    }

    /**
     * get next page from cache
     *
     * @param key: current key, trying to get next key which is key+1
     * @return List<FoodTruck>
     */
    public List<FoodTruck> getNext(int key){
        Map.Entry<Integer, List<FoodTruck>> entry = super.floorEntry(key+1);
        if(entry == null || entry.getKey() <= key) return null;
        List<FoodTruck> list = entry.getValue();
        print(list, key+1);
        return list;
    }

    /**
     * get previous page from cache
     *
     * @param key: current key, trying to get previous key which is key-1
     * @return List<FoodTruck>
     */
    public List<FoodTruck> getPrev(int key) {
        Map.Entry<Integer, List<FoodTruck>> entry = super.ceilingEntry(key-1);
        if(entry == null || entry.getKey() >= key) return null;
        List<FoodTruck> list = entry.getValue();
        print(list, key-1);
        return list;
    }

    /**
     * get current page from cache
     *
     * @param key: current key
     * @return List<FoodTruck>
     */
    public List<FoodTruck> get(int key){
        if(super.get(key) == null) return null;
        List<FoodTruck> list = super.get(key);
        print(list, key);
        return list;
    }

    /**
     * print out current page with index for each item
     * @param list: List<FoodTruck>
     * @param key: current key
     */
    private void print(List<FoodTruck> list, int key){
        int index = key % 1000;
        System.out.println(String.format("%-4s %-80s %-10s", "No.", "Name","Address"));
        System.out.println("---------------------------------------------------------------------------------------------");
        for(int i = 0; i < list.size(); i++){
            int count = index*10 + i + 1;
            String format = String.format("#%-3s %-80s %-10s", count, list.get(i).getApplicant(),
                    list.get(i).getLocation());
            System.out.println(format);
        }
    }
}
