package etl;

import org.apache.spark.sql.SparkSession;

public class Utils {
	
	public static SparkSession initializeSparkSession(String appname, String master) 
	{
        return SparkSession.builder()
                .appName(appname)
                .master(master)
                .getOrCreate();
    }
}
