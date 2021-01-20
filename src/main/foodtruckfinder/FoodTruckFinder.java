package main.foodtruckfinder;

import java.util.List;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Date;
import java.util.Locale;
import java.util.Scanner;

/**
 * Main class of Food Truck Finder
 */
public class FoodTruckFinder {

    public static void main(String[] args){

        LocalDate date = LocalDate.now();
        DayOfWeek dowStr = date.getDayOfWeek();
        Integer dow = dowStr.getValue();

        String currentHour = new SimpleDateFormat("HH").format(new Date());
        String ampm = new SimpleDateFormat("a").format(new Date());

        Scanner scan = new Scanner(System.in);

        System.out.println("============================= Hi! Welcome to Food Truck Finder =============================");
        System.out.println("current Day Of Week is: " + dowStr);
        System.out.println("current Time is: " + currentHour + " " + ampm);
        try{
            SODAClient client = new SODAClient();
            int index = 0;
            List<FoodTruck> res = client.getFoodTruck(dow, currentHour, index);
            if(res == null){
                return;
            }

            int currentInd = Integer.valueOf(dow + currentHour + String.format("%03d", index));
            FoodTruckCache cache = FoodTruckCache.getCache();
            System.out.print("Options: n - next page, p - prev page, q - quit:  ");
            while(scan.hasNext()){
                String input = scan.next();
                if(input.equals("n")){
                    res = cache.getNext(currentInd++);
                    if(currentInd > cache.lastKey()){
                        System.out.println("This is the last list, no more list");
                        currentInd--;
                    }
                    System.out.print("Options: n - next page, p - prev page, q - quit:  ");
                }
                else if(input.equals("p")){
                    res = cache.getPrev(currentInd--);
                    if(currentInd < cache.firstKey()){
                        System.out.println("This is the first list, no more previous list");
                        currentInd++;
                    }
                    System.out.print("Options: n - next page, p - prev page, q - quit:  ");
                }
                else if(input.equals("q")){
                    break;
                }
                else{
                    System.out.println("Illegal input. Please try again.");
                    System.out.print("Options: n - next page, p - prev page, q - quit:  ");
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally{
            System.out.println("================= Thank you for using Food Truck Finder, See you next time! =================");
            scan.close();
        }
    }


}
