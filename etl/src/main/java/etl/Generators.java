package etl;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;

import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.util.ArrayList;
import java.util.List;

public class Generators {

	private static final StructType schemaWeeklyMenus = DataTypes.createStructType(new StructField[]{
            DataTypes.createStructField("user_id", DataTypes.IntegerType, false),
            DataTypes.createStructField("day", DataTypes.IntegerType, false),
            DataTypes.createStructField("breakfast_product_name", DataTypes.StringType, false),
            DataTypes.createStructField("lunch_product_name", DataTypes.StringType, false),
            DataTypes.createStructField("dinner_product_name", DataTypes.StringType, false)
    });
	
	public static Row generateDailyMenu(Row regimePlan, Dataset<Row> dfAvailableProducts, String userCountry, String soldContriesColone) {
		double maxAddedSugarsPer100g = ((Number) regimePlan.getAs("max_added-sugars_100g")).doubleValue();
		double maxSugarPer100g = ((Number) regimePlan.getAs("max_sugars_100g")).doubleValue();
		double maxSucrosePer100g = ((Number) regimePlan.getAs("max_sucrose_100g")).doubleValue();
		double maxGlucosePer100g = ((Number) regimePlan.getAs("max_glucose_100g")).doubleValue();
		double maxFructosePer100g = ((Number) regimePlan.getAs("max_fructose_100g")).doubleValue();
	    
		Dataset<Row> dfFilteredProducts = dfAvailableProducts.filter(
			dfAvailableProducts.col("added-sugars_100g").leq(100)
                //.and(dfAvailableProducts.col("sugars_100g").leq(maxSugarPer100g))
                //.and(dfAvailableProducts.col("sucrose_100g").leq(maxSucrosePer100g))
                //.and(dfAvailableProducts.col("glucose_100g").leq(maxGlucosePer100g))
                //.and(dfAvailableProducts.col("fructose_100g").leq(maxFructosePer100g))
                //.and(dfAvailableProducts.col(soldContriesColone).contains(userCountry))
		);
	    
	    Row breakfast = dfFilteredProducts.sample(false, 0.1).first();
	    Row lunch = dfFilteredProducts.sample(false, 0.1).first();
	    Row dinner = dfFilteredProducts.sample(false, 0.1).first();

	    String breakfastProductName = breakfast.getAs("product_name");
	    String lunchProductName = lunch.getAs("product_name");
	    String dinnerProductName = dinner.getAs("product_name");

	    return RowFactory.create(breakfastProductName, lunchProductName, dinnerProductName);
	}
	
	public static Dataset<Row> generateWeeklyMenu(SparkSession sparkSession, Dataset<Row> dfUsersCleaned, Dataset<Row> dfDietsCleaned, Dataset<Row> dfProductsCleaned) {
		List<Row> menuRows = new ArrayList<>();
        List<Row> userRows = dfUsersCleaned.collectAsList();

        for (Row userRow : userRows) {
            int userId = userRow.getInt(userRow.fieldIndex("user_id"));
            int userRegimeId = userRow.getInt(userRow.fieldIndex("regime_id"));
            String userCountry = userRow.getString(userRow.fieldIndex("country"));

            List<Row> regimeList = dfDietsCleaned
                    .filter(dfDietsCleaned.col("regime_id").equalTo(userRegimeId))
                    .limit(1)
                    .collectAsList();

            if (regimeList.isEmpty()) {
                continue;
            }

            Row rowUserRegimeInfo = regimeList.get(0);
            
            for (int day = 1; day <= 7; day++) {
                Row dailyMenu = generateDailyMenu(rowUserRegimeInfo, dfProductsCleaned, userCountry, "sold_countries_fr");
                Row menuRow = RowFactory.create(userId, day, dailyMenu.getAs(0), dailyMenu.getAs(1), dailyMenu.getAs(2));
                menuRows.add(menuRow);
            }
        }
        return sparkSession.createDataFrame(menuRows, schemaWeeklyMenus);
	}

}
