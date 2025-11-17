package etl;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

public class Main {

	public static void main(String[] args) 
	{
		SparkSession sparkSession = Utils.initializeSparkSession("openfoodfacts-etl", "local[4]");
		
		Dataset<Row> test;
	}
}