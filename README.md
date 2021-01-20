-------------------------
How to build & run
-------------------------
Dependencies: <br />
1) java-sdk 8
2) jackson-annotations-2.12.0<br />
3) jackson-core-2.12.0<br />
4) jackson-databind-2.12.0<br />

Java requirements: <br />
Java 10 runtime environments, both 32-bit and 64-bit versions are supported <br />

Build steps:
on a machine with maven installed (Apache Maven 3.6.3), run command in project dir:
<pre> mvn clean compile assembly:single </pre>
Jar file should be created under target dir.<br />
In case the jar file can't be generated, I've attached a pre-built jar in /out/artifacts/Redfin_far/Redfin.jar<br />

Run the command in the dir where jar file located: <br />

<pre>java -jar Redfin-1.0-SNAPSHOT-jar-with-dependencies.jar<br /></pre>

Output of the program including food truck name, address. Each page size is 10 as required: <br />

example:<br />
<pre>
============================= Hi! Welcome to Food Truck Finder =============================<br />
current Day Of Week is: SUNDAY<br />
current Time is: 14 PM<br />
total food truck opening: 111<br />
No.  Name                                                                             Address<br />
---------------------------------------------------------------------------------------------<br />
#1   Athena SF Gyro                                                                   699 08TH ST<br />
#2   Authentic India                                                                  1355 MARKET ST<br />
#3   Bay Area Dots, LLC                                                               900 BEACH ST<br />
#4   Bay Area Dots, LLC                                                               567 BAY ST<br />
#5   Bay Area Mobile Catering, Inc. dba. Taqueria Angelica's                          1301 CESAR CHAVEZ ST<br />
#6   Bay Area Mobile Catering, Inc. dba. Taqueria Angelica's                          1455 MARKET ST<br />
#7   BH & MT LLC                                                                      170 OTIS ST<br />
#8   BOWL'D ACAI, LLC.                                                                451 MONTGOMERY ST<br />
#9   Buenafe                                                                          220 RANKIN ST<br />
#10  Buenafe                                                                          901 16TH ST<br />
</pre>

User could have 3 options to view the food truck list:
1) n - view the next page
2) p - view the previous page
3) q - quit

example:<br />
<pre>
Options: n - next page, p - prev page, q - quit:  n<br />
No.  Name                                                                             Address<br />
---------------------------------------------------------------------------------------------<br />
#11  California Kahve                                                                 1234 GREAT HWY<br />
#12  CC Acquisition LLC                                                               525 MARKET ST<br />
#13  CC Acquisition LLC                                                               298 MARKET ST<br />
#14  Chairman SF, LLC                                                                 625 02ND ST<br />
#15  Chairman SF, LLC                                                                 34 ELLIS ST<br />
#16  Chairman SF, LLC                                                                 536 MISSION ST<br />
#17  Cochinita                                                                        290 TOWNSEND ST<br />
#18  Cochinita                                                                        2601 24TH ST<br />
#19  Cochinita                                                                        490 BRANNAN ST<br />
#20  Cochinita                                                                        999 DIVISADERO ST<br />
Options: n - next page, p - prev page, q - quit:  q<br />
================= Thank you for using Food Truck Finder, See you next time! =================<br />
</pre>

----------------------
Issue & Tips
----------------------
The application using an application token to request data from Socrata Open Data API. <br />
Please do not use it abusively, otherwise monopolize the use of our API may be throttled. <br />
Socrata Open Data API platform doesn't specify the expired date of the application token. If user got error message as : <br />
"You're not authorized to get data, please contact support" <br />
please try to renew the application token. <br />

----------------------
Discussion
----------------------
In the application, I use a TreeMap instance for Food Truck Cache to store the current opening food truck list get from SODA.<br />
Once trigger the application, it will get the full list from SODA and store each page list in the cache which will benefit the user to view more list quickly without another HTTPS request.<br />
In the cache, the key is an int composed by "Day of week - Hour - Index" <br />
e.g. for Monday, 14 PM, index starts with 000, so the first key is 114000<br />
the value is a list of Food Truck order by their name, the size of list is 10 at most.<br />

If we have 25 opening food truck at Monday 14 PM, there'll be 3 list in cache such as:<br />
- Key: 114000  Value: List of FoodTruck, size 10<br />
- Key: 114001  Value: List of FoodTruck, size 10<br />
- Key: 114002  Value: List of FoodTruck, size 5<br />

User could view next or previous page easily by get next or previous entry from this cache. <br />
<br />
If we plan to scale up this application to a web service. There're few things should be different from this simple command line application. <br />

**System API** 
- we can have SOAP or REST APIs to expose the functionality of service. the definition of the API could be:
  - getFoodTruck(api_dev_key, day_of_week, hour, user_location, maximum_results_to_return);
    - api_dev_key (String): The API developer key of a registered account. This will be used to throttle users based on their allocated quota.
    - day_of_week (int): indicate day of week of the query date 
    - hour (int): indicate hour of the query time, in format HH, 0~24
    - user_location (String): optional location (longitude, latitude) user intend to query for
    - maximum_results_to_return (int): optional max limit to specify return results size
    
    
**High Level System Design** 
- At a high level, we need multiple application servers to serve all these requests with load balancers in front of them for traffic distributions. 
  On the backend, we need an efficient database that can store all food truck information and support a huge number of reads. In addition, we could have a food truck cache to store query results.
  - Load Balancer layer between clients nad application servers adopted Round Robin approach could help to distribute requests equally among backend servers.
  - The query results won't change frequently for a specific where clause. We could store the query results in a cache. Application servers can quickly check if the cache has the results before hitting backend database.
    For cache eviction policy, LRU seems suitable for the requirement.
  - A separate Update service can run periodically to remove expired food truck from backend database and cache. This service should be very lightweight and can be scheduled to run only when the user traffic is expected to be low.
    

**Database Schema**
- Store data of food truck name, address, city, state, longitude, latitude, zipcode, open hours in a table:
    - Table Name: Food Truck
    - PK: FoodTruckID: int 
    - FoodTruckName: varchar(20)
    - FoodTruckAddress: varchar(256)
    - City: varchar(8)
    - State: varchar(2)
    - longitude: varchar(8)
    - latitude: varchar(8)
    - zipcode: varchar(6)
    - hours: varchar(10)
- Database partitioning
    - If the food truck table has huge number of rows, the index doesn't fit into a single machine's memory. We can sharding based on state, city or zipcode. Then all food truck belonging to a state, city or zipcode will be stored on a fixed node.
    While storing, we will find for example city to find the server and store the food truck there. Similarly, we can ask for the city server while querying the city.
    - Issues: some city has much more food trucks than others which lead to uniform distribution of data. For that, we could have to repartition or use consistent hashing.
- Replication and Fault Tolerance
    - A replica of Database server can provide an alternate to data partitioning. A master-agent (aka master-slave) configuration, and agents will only server read traffic and all write traffic will first go to master and then applied to agents.
    - In case both primary and secondary servers die, we have to reload the Food Truck database and indexes.
    
----------------------
About
----------------------
developed by Jiwei Yu, Jan 2021