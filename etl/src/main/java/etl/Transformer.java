package etl;

import java.util.List;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import static org.apache.spark.sql.functions.expr;
import static org.apache.spark.sql.functions.when;

public class Transformer {

	private static Dataset<Row> selectAndCastColumns(Dataset<Row> df) {
	    return df.select(
	                    df.col("product_name").cast("string"),
	                    df.col("categories_en").cast("string"),
	                    df.col("countries_en").cast("string"),
	                    df.col("added-sugars_100g").cast("float"),
	                    df.col("sugars_100g").cast("float"),
	                    df.col("lactose_100g").cast("float")
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
                df.col(columnName).leq(100).geq(0)
        );
    }
	private static Dataset<Row> cleanCountryNames(Dataset<Row> dfSelectedColumns, SparkSession sparkSession) {
        Dataset<String> listCountry = Utils.splitColumnIntoWords(dfSelectedColumns, "sold_countries", "country", ",");
        Dataset<Row> dfCountries = sparkSession.read()
                .format("csv")
                .option("header", false)
                .option("delimiter", ",")
                .load(Constants.DATA_FILE_COUNTRIES)
                .toDF("index", "country_id", "country_code_2", "country_code_3", "country_fr", "country_en");
        Dataset<Row> dfMergedData = listCountry.join(dfCountries, listCountry.col("country").equalTo(dfCountries.col("country_en")), "left_outer");
        Dataset<Row> dfFalseCountry = dfMergedData.filter(dfMergedData.col("country_en").isNull()).select("country");
        List<String> falseCountryList = dfFalseCountry.as(Encoders.STRING()).collectAsList();
        Dataset<Row> dfCleaned = dfSelectedColumns.withColumn("cleaned_sold_countries",
                expr("concat_ws(',', filter(split(sold_countries, ','), country -> !array_contains(array('" + String.join("','", falseCountryList) + "'), country)))"));
        dfCleaned = dfCleaned.withColumn("cleaned_sold_countries",
                when(dfCleaned.col("cleaned_sold_countries").equalTo(""), null)
                        .otherwise(dfCleaned.col("cleaned_sold_countries")));
        dfCleaned = dfCleaned.na().drop(new String[]{"cleaned_sold_countries"});
        dfCleaned = dfCleaned.drop("sold_countries");
        dfCleaned = dfCleaned.withColumnRenamed("cleaned_sold_countries", "sold_countries");
        return dfCleaned;
    }
	
	public static Dataset<Row> cleanData(Dataset<Row> dfProducts, SparkSession sparkSession) {
		dfProducts = selectAndCastColumns(dfProducts);
		dfProducts = removeMissingValues(dfProducts);
		dfProducts = removeDuplicates(dfProducts);
		
		dfProducts = removeNegativeValues(dfProducts, "added-sugars_100g");
		dfProducts = removeNegativeValues(dfProducts, "sugars_100g");
		dfProducts = removeNegativeValues(dfProducts, "lactose_100g");
		
		dfProducts = removeEmptyStrings(dfProducts, "product_name");
		dfProducts = removeEmptyStrings(dfProducts, "categories");
		dfProducts = removeEmptyStrings(dfProducts, "sold_countries");
		
		dfProducts = removeNonASCIICharacters(dfProducts, "product_name", "^[\\x00-\\x7F]*$");
		dfProducts = removeNonASCIICharacters(dfProducts, "categories", "^[\\x00-\\x7F]*$");
		dfProducts = removeNonASCIICharacters(dfProducts, "sold_countries", "^[\\x00-\\x7F]*$");
		
		dfProducts = removeOutOfRangeValues(dfProducts, "added-sugars_100g", 0, 100);
		dfProducts = removeOutOfRangeValues(dfProducts, "sugars_100g", 0, 100);
		dfProducts = removeOutOfRangeValues(dfProducts, "lactose_100g", 0, 100);
		dfProducts = cleanCountryNames(dfProducts, sparkSession);
		return dfProducts;
	}

}
