package fr.thorbeorn.etl.spark;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;

public class Master {

	private SparkSession sparksession;
	private Dataset<Row> data;
	

	public Master(String AppName, String MasterName, String Format, String FilePath, String MongoURI) {
		SparkSession sp = null;
		Dataset<Row> dt = null;
		switch (Format.toLowerCase()) {
	        case "csv":
	        	sp = SparkSession
						.builder()
						.appName(AppName)
						.master(MasterName)
						.getOrCreate();
	            dt = sp.read()
	                   .format("csv")
	                   .option("header", true)
	                   .option("delimiter", ";")
	                   .load(FilePath);
	            break;
	        case "json":
	        	sp = SparkSession
						.builder()
						.appName(AppName)
						.master(MasterName)
						.getOrCreate();
	            dt = sp.read()
	                   .format("json")
	                   .load(FilePath);
	            break;
	        case "mongodb":
	        	sp = SparkSession
						.builder()
						.appName(AppName)
						.master(MasterName)
						.config("spark.mongodb.read.connection.uri", MongoURI)
						.getOrCreate();
	        	StructType schema = new StructType()
	                    .add("_id", DataTypes.StringType)
	                    .add("name", DataTypes.StringType)
	                    .add("age", DataTypes.IntegerType)
	                    .add("attributes", DataTypes.createMapType(DataTypes.StringType, DataTypes.StringType));
	            dt = sp.read()
	                   .format("mongodb")
	                   .schema(schema)
	                   .load();
	            break;
	        default:
	            throw new IllegalArgumentException("Format non supporté : " + Format);
	    }
		
		setSparkSession(sp);
		setData(dt);
	}



	public SparkSession getSparkSession() {
		return sparksession;
	}
	public void setSparkSession(SparkSession sparksession) {
		this.sparksession = sparksession;
	}
	public Dataset<Row> getData() {
		return data;
	}
	public void setData(Dataset<Row> data) {
		this.data = data;
	}
	public void stopSparkSession() {
		this.sparksession.stop();
	}
}
