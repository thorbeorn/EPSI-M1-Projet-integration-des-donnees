package etl;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;

public class Loaders {

	public static void loadToDatabase(Dataset<Row> df, String dbHost, String dbUser, String dbPassword, String dbTable) {
		df.write()
        .format("jdbc")
        .option("url", dbHost)
        .option("dbtable", dbTable)
        .option("user", dbUser)
        .option("password", dbPassword)
        .option("driver", "com.mysql.cj.jdbc.Driver")
        .mode(SaveMode.Overwrite)
        .option("truncate", "true")
        .save();
	}

}
