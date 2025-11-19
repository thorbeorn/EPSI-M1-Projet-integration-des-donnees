package etl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import static org.apache.spark.sql.functions.expr;
import static org.apache.spark.sql.functions.when;

public class Transformers {

	private static Dataset<Row> selectAndCastColumns(Dataset<Row> df) {
	    return df.select(
	                    df.col("product_name").cast("string"),
	                    df.col("categories_en").cast("string"),
	                    df.col("countries_en").cast("string"),
	                    df.col("added-sugars_100g").cast("float"),
	                    df.col("sugars_100g").cast("float"),
	                    df.col("sucrose_100g").cast("float"),
	                    df.col("glucose_100g").cast("float"),
	                    df.col("fructose_100g").cast("float")
	            ).withColumnRenamed("countries_en", "sold_countries")
	            .withColumnRenamed("categories_en", "categories");
	}
	private static Dataset<Row> removeDuplicates(Dataset<Row> df) {
	    return df.dropDuplicates();
	}
	private static Dataset<Row> removeNegativeValues(Dataset<Row> df, String columnName) {
	    return df.filter(
	            df.col(columnName).geq(0)
	    );
	}
	private static Dataset<Row> removeEmptyStrings(Dataset<Row> df, String columnName) {
	    return df.filter(
	            df.col("product_name").notEqual("")
	    );
	}
	private static Dataset<Row> removeMissingValues(Dataset<Row> df) {
	    return df.na().drop();
	}
	
	private static Dataset<Row> removeNonASCIICharacters(Dataset<Row> df, String columnName, String regex) {
        return df.filter(
                df.col(columnName).rlike("^[\\x00-\\x7F]*$")
        );
    }
	private static Dataset<Row> removeOutOfRangeValues(Dataset<Row> df, String columnName, float min, float max) {
        return df.filter(
        		df.col(columnName).geq(min)
                .and(df.col(columnName).leq(max))
        );
    }
	private static Dataset<Row> cleanCountryNames(Dataset<Row> dfSelectedColumns, SparkSession sparkSession) {
	    Dataset<String> listCountry = Utils.splitColumnIntoWords(dfSelectedColumns, "sold_countries", "country", ",");
	    Dataset<Row> dfCountries = Extractors.extractFromCSV(sparkSession, Constants.DATA_FILE_COUNTRIES, ",", new String[] {
	        "index",
	        "country_id",
	        "country_code_2",
	        "country_code_3",
	        "country_fr",
	        "country_en"
	    });
	    Dataset<Row> dfMergedData = listCountry.join(dfCountries, listCountry.col("country").equalTo(dfCountries.col("country_en")), "left_outer");
	    Dataset<Row> dfFalseCountry = dfMergedData.filter(dfMergedData.col("country_en").isNull()).select("country");
	    List<String> falseCountryList = dfFalseCountry.as(Encoders.STRING()).collectAsList();
	    Map<String, String> countryMap = dfCountries.select("country_en", "country_fr")
	        .as(Encoders.tuple(Encoders.STRING(), Encoders.STRING()))
	        .collectAsList()
	        .stream()
	        .collect(Collectors.toMap(t -> t._1(), t -> t._2()));
	    
	    Dataset<Row> dfCleaned = dfSelectedColumns;
	    dfCleaned = dfCleaned.withColumn("sold_countries_en",
	        expr("concat_ws(',', filter(split(sold_countries, ','), country -> !array_contains(array('" + 
	            String.join("','", falseCountryList) + "'), country)))"));
	    dfCleaned = dfCleaned.withColumn("sold_countries_fr",
	        expr("concat_ws(',', transform(filter(split(sold_countries, ','), country -> !array_contains(array('" + 
	            String.join("','", falseCountryList) + "'), country)), country -> " +
	            "CASE " + 
	            countryMap.entrySet().stream()
	                .map(e -> "WHEN country = '" + e.getKey() + "' THEN '" + e.getValue() + "'")
	                .collect(Collectors.joining(" ")) + 
	            " END))"));
	    dfCleaned = dfCleaned.withColumn("sold_countries_en",
	        when(dfCleaned.col("sold_countries_en").equalTo(""), null)
	            .otherwise(dfCleaned.col("sold_countries_en")));
	    
	    dfCleaned = dfCleaned.withColumn("sold_countries_fr",
	        when(dfCleaned.col("sold_countries_fr").equalTo(""), null)
	            .otherwise(dfCleaned.col("sold_countries_fr")));
	    
	    dfCleaned = dfCleaned.na().drop(new String[]{"sold_countries_en", "sold_countries_fr"});
	    dfCleaned = dfCleaned.drop("sold_countries");
	    
	    return dfCleaned;
	}
	
	public static Dataset<Row> cleanProductData(Dataset<Row> dfProducts, SparkSession sparkSession) {
		dfProducts = selectAndCastColumns(dfProducts);
		dfProducts = removeMissingValues(dfProducts);
		dfProducts = removeDuplicates(dfProducts);
		
		dfProducts = removeNegativeValues(dfProducts, "added-sugars_100g");
		dfProducts = removeNegativeValues(dfProducts, "sugars_100g");
		dfProducts = removeNegativeValues(dfProducts, "sucrose_100g");
		dfProducts = removeNegativeValues(dfProducts, "glucose_100g");
		dfProducts = removeNegativeValues(dfProducts, "fructose_100g");
		
		dfProducts = removeEmptyStrings(dfProducts, "product_name");
		dfProducts = removeEmptyStrings(dfProducts, "categories");
		dfProducts = removeEmptyStrings(dfProducts, "sold_countries");
		
		dfProducts = removeNonASCIICharacters(dfProducts, "product_name", "^[\\x00-\\x7F]*$");
		dfProducts = removeNonASCIICharacters(dfProducts, "categories", "^[\\x00-\\x7F]*$");
		dfProducts = removeNonASCIICharacters(dfProducts, "sold_countries", "^[\\x00-\\x7F]*$");
		
		dfProducts = removeOutOfRangeValues(dfProducts, "added-sugars_100g", 0, 100);
		dfProducts = removeOutOfRangeValues(dfProducts, "sugars_100g", 0, 100);
		dfProducts = removeOutOfRangeValues(dfProducts, "sucrose_100g", 0, 100);
		dfProducts = removeOutOfRangeValues(dfProducts, "glucose_100g", 0, 100);
		dfProducts = removeOutOfRangeValues(dfProducts, "fructose_100g", 0, 100);
		
		dfProducts = cleanCountryNames(dfProducts, sparkSession);
		return dfProducts;
	}
	public static Dataset<Row> cleanUserData(Dataset<Row> dfUsers, SparkSession sparkSession) {
		dfUsers = removeMissingValues(dfUsers);
		dfUsers = removeDuplicates(dfUsers);
		
		dfUsers = removeEmptyStrings(dfUsers, "first_name");
		dfUsers = removeEmptyStrings(dfUsers, "last_name");
		dfUsers = removeEmptyStrings(dfUsers, "country");
		
		dfUsers = removeNonASCIICharacters(dfUsers, "first_name", "^[\\x00-\\x7F]*$");
		dfUsers = removeNonASCIICharacters(dfUsers, "last_name", "^[\\x00-\\x7F]*$");
		dfUsers = removeNonASCIICharacters(dfUsers, "country", "^[\\x00-\\x7F]*$");
		return dfUsers;
	}
	public static Dataset<Row> cleanDietData(Dataset<Row> dfDiets, SparkSession sparkSession) {
		dfDiets = removeMissingValues(dfDiets);
		dfDiets = removeDuplicates(dfDiets);
		
		dfDiets = removeEmptyStrings(dfDiets, "name_en");
		dfDiets = removeEmptyStrings(dfDiets, "name_fr");
		dfDiets = removeEmptyStrings(dfDiets, "description_en");
		dfDiets = removeEmptyStrings(dfDiets, "description_fr");
		
		dfDiets = removeNonASCIICharacters(dfDiets, "name_en", "^[\\x00-\\x7F]*$");
		dfDiets = removeNonASCIICharacters(dfDiets, "name_fr", "^[\\x00-\\x7F]*$");
		dfDiets = removeNonASCIICharacters(dfDiets, "description_en", "^[\\x00-\\x7F]*$");
		dfDiets = removeNonASCIICharacters(dfDiets, "description_fr", "^[\\x00-\\x7F]*$");
		
		dfDiets = removeOutOfRangeValues(dfDiets, "max_added-sugars_100g", 0, 100);
		dfDiets = removeOutOfRangeValues(dfDiets, "max_sugars_100g", 0, 100);
		dfDiets = removeOutOfRangeValues(dfDiets, "max_sucrose_100g", 0, 100);
		dfDiets = removeOutOfRangeValues(dfDiets, "max_glucose_100g", 0, 100);
		dfDiets = removeOutOfRangeValues(dfDiets, "max_fructose_100g", 0, 100);
		return dfDiets;
	}
}
