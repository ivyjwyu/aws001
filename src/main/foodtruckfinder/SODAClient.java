package main.foodtruckfinder;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SODAClient {

    /**
     * get food truck list if the cache is ready, read from it directly, else prepare the cache by sending request to SODA API
     * @param dow: day of week
     * @param currentTime: current hour in format HH
     * @param index: current page index
     * @return List<FoodTruck>
     * @throws Exception
     */
    public List<FoodTruck> getFoodTruck(int dow, String currentTime, int index) throws Exception{
        FoodTruckCache cache = getCache(dow, currentTime);
        int key = Integer.valueOf(dow + currentTime + String.format("%03d", index));
        if(cache == null){
            cache = prepareCache(dow, currentTime+":00");
            if(cache.size()==0){
                System.out.println("Sorry, we've found 0 food truck at this time. Please try later");
                return null;
            }
        }
        return cache.get(key);
    }

    /**
     * prepare food truck cache when it's not ready for current request
     * @param dow: day of week
     * @param currentTime: current hour in format HH
     * @return FoodTruckCache: for current request
     * @throws Exception
     */
    public FoodTruckCache prepareCache(int dow, String currentTime) throws Exception {
        FoodTruckCache.cleanUp();
        FoodTruckCache cache = FoodTruckCache.getCache();

        List<FoodTruck> fts = new ArrayList<>();
        try{
            StringBuilder query = new StringBuilder();
            String clause = String.format( "start24 < '%1$s' AND end24 > '%1$s'", currentTime);
            query.append("&$where=").append(URLEncoder.encode(clause, "UTF-8"));
            query.append("&dayorder=").append(dow);
            query.append("&$select=applicant,location");
            query.append("&$order=applicant");

            URL url = new URL(EnvConf.BASE_URL + "?" + query.toString());
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("X-App-Token", EnvConf.app_tokan);
            conn.setReadTimeout(15000);
            conn.setConnectTimeout(15000);

            int response = conn.getResponseCode();
            switch (response){
                case 200:
                    BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String line;
                    StringBuilder result = new StringBuilder();
                    while ((line = rd.readLine()) != null) {
                        result.append(line);
                    }
                    rd.close();
                    ObjectMapper objectMapper = new ObjectMapper();
                    List<FoodTruck> foodTrucks = objectMapper.readValue(result.toString(), new TypeReference<List<FoodTruck>>() {});
                    System.out.println("total food truck opening: " + foodTrucks.size());
                    cache.put(foodTrucks, dow, currentTime.substring(0,2));
                    return cache;
                case 202:
                    throw new Exception("Please try again");
                case 403:
                    throw new Exception("You're not authorized to get data, please contact support");
                case 429:
                    throw new Exception("You've sent too many requests, please try later");
                case 500:
                    throw new Exception("Socrata Server may be experiencing problems. Please try later.");
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * check if the cache contains the data requsted for, if not, return null
     * @param dow: day of week
     * @param currentTime: current hour in format HH
     * @return FoodTruckCache
     */
    public FoodTruckCache getCache(int dow, String currentTime){
        FoodTruckCache cache = FoodTruckCache.getCache();
        int currentKey = Integer.valueOf(dow + currentTime + 000);
        if(cache.containsKey(currentKey)){
            return cache;
        }else{
            return null;
        }
    }

}
