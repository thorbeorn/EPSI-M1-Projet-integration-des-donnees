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
        double maxAddedSugarsPerDay = regimePlan.getAs("max_added-sugars_g_day");
        double maxSugarPerDay = regimePlan.getAs("max_sugars_g_day");
        double maxSucrosePerDay = regimePlan.getAs("max_sucrose_g_day");
        double maxGlucosePerDay = regimePlan.getAs("max_glucose_g_day");
        double maxFructosePerDay = regimePlan.getAs("max_fructose_g_day");

        Dataset<Row> dfFilteredProducts = dfAvailableProducts.filter(
                dfAvailableProducts.col("max_added-sugars_100g").leq(maxAddedSugarsPerDay)
                        .and(dfAvailableProducts.col("max_sugars_100g").leq(maxSugarPerDay))
                        .and(dfAvailableProducts.col("max_sucrose_100g").leq(maxSucrosePerDay))
                        .and(dfAvailableProducts.col("max_glucose_100g").leq(maxGlucosePerDay))
                        .and(dfAvailableProducts.col("max_fructose_100g").leq(maxFructosePerDay)
                        .and(dfAvailableProducts.col(soldContriesColone).contains(userCountry)))
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

            Row rowUserRegimeInfo = dfDietsCleaned.filter(dfDietsCleaned.col("regime_id").equalTo(userRegimeId)).first();
            for (int day = 1; day <= 7; day++) {
                Row dailyMenu = generateDailyMenu(rowUserRegimeInfo, dfProductsCleaned, userCountry, "sold_countries_fr");
                Row menuRow = RowFactory.create(userId, day, dailyMenu.getAs(0), dailyMenu.getAs(1), dailyMenu.getAs(2));
                menuRows.add(menuRow);
            }
        }
        return sparkSession.createDataFrame(menuRows, schemaWeeklyMenus);
	}

}
