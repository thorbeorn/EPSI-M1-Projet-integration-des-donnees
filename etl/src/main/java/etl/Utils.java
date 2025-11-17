package etl;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import static org.apache.spark.sql.functions.*;

public class Utils {
	
	public static SparkSession initializeSparkSession(String appname, String master) 
	{
        return SparkSession.builder()
                .appName(appname)
                .master(master)
                .getOrCreate();
    }
	public static Dataset<String> splitColumnIntoWords(Dataset<Row> df, String columnNameInput, String columnNameOutput ,String delimiter) {
        Dataset<Row> splitData = df.withColumn(columnNameInput, split(col(columnNameInput), delimiter));
        Dataset<Row> uniqueWords = splitData.select(explode(col(columnNameInput)).as(columnNameOutput)).distinct();
        Dataset<String> listWords = uniqueWords.select(columnNameOutput).as(Encoders.STRING());
        return listWords;
    }
}
