package etl;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

public class Extractors {
	
	public static Dataset<Row> extractFromCSV(SparkSession sparkSession, String filePath, String delimiter) 
	{
		return sparkSession.read()
                .format("csv")
                .option("header", true)
                .option("delimiter", delimiter)
                .option("inferSchema", true)
                .load(filePath);
	}
	public static Dataset<Row> extractFromCSV(SparkSession sparkSession, String filePath, String delimiter, String[] header) 
	{
		return sparkSession.read()
                .format("csv")
                .option("header", false)
                .option("delimiter", delimiter)
                .load(filePath)
                .toDF(header);
	}
	
	public static Dataset<Row> extractFromJSON(SparkSession sparkSession, String filePath) 
	{
		return sparkSession.read()
                .json(filePath);
	}
	
	public static Dataset<Row> extractFromDatabase(SparkSession sparkSession, String dbHost, String dbUser, String dbPassword, String dbTable)
    {
        return sparkSession.read()
                .format("jdbc")
                .option("url", dbHost)
                .option("dbtable", dbTable)
                .option("user", dbUser)
                .option("password", dbPassword)
                .load();
    }
}