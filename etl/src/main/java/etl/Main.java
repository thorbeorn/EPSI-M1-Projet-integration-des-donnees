package etl;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

public class Main {

	public static void main(String[] args) 
	{
		SparkSession sparkSession = Utils.initializeSparkSession("openfoodfacts-etl");
		
		Dataset<Row> dfProducts = Extractors.extractFromCSV(sparkSession, Constants.DATA_FILE_PRODUCTS, "\t");
		Dataset<Row> dfDiets = Extractors.extractFromCSV(sparkSession, Constants.DATA_FILE_DIET, ",");
		Dataset<Row> dfUsers = Extractors.extractFromCSV(sparkSession, Constants.DATA_FILE_USERS, ",");
		
		Dataset<Row> dfProductsCleaned = Transformers.cleanProductData(dfProducts, sparkSession);
		Dataset<Row> dfDietsCleaned = Transformers.cleanDietData(dfDiets, sparkSession);
		Dataset<Row> dfUsersCleaned = Transformers.cleanUserData(dfUsers, sparkSession);
		
		Dataset<Row> weeklyMenus = Generators.generateWeeklyMenu(sparkSession, dfUsersCleaned, dfDietsCleaned, dfProductsCleaned);
		
		Loaders.loadToDatabase(dfDietsCleaned, Constants.DB_HOST, Constants.DB_USER, Constants.DB_PASSWORD, "regime");
        Loaders.loadToDatabase(dfUsersCleaned, Constants.DB_HOST, Constants.DB_USER, Constants.DB_PASSWORD, "user");
        Loaders.loadToDatabase(weeklyMenus, Constants.DB_HOST, Constants.DB_USER, Constants.DB_PASSWORD, "daily_menu");
        
		sparkSession.stop();
	}
}