package etl;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import static org.apache.spark.sql.functions.*;

public class Utils {
	
	public static SparkSession initializeSparkSession(String appname) 
	{
		String master = System.getProperty("spark.master", "local[4]");
	    String driverMemory = System.getProperty("spark.driver.memory", "2g");
	    String executorMemory = System.getProperty("spark.executor.memory", "2g");
	    
        return SparkSession.builder()
                .appName(appname)
                .master(master)
                .config("spark.driver.memory", driverMemory)
                .config("spark.executor.memory", executorMemory)
                .getOrCreate();
    }
	public static Dataset<String> splitColumnIntoWords(Dataset<Row> df, String columnNameInput, String columnNameOutput ,String delimiter) {
        Dataset<Row> splitData = df.withColumn(columnNameInput, split(col(columnNameInput), delimiter));
        Dataset<Row> uniqueWords = splitData.select(explode(col(columnNameInput)).as(columnNameOutput)).distinct();
        Dataset<String> listWords = uniqueWords.select(columnNameOutput).as(Encoders.STRING());
        return listWords;
    }
}
