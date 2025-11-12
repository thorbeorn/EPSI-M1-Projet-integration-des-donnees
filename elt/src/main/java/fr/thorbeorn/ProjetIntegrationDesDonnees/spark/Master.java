package fr.thorbeorn.ProjetIntegrationDesDonnees.spark;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

public class Master {
	
	private SparkSession sparksession;
	private Dataset<Row> data;
	

	public Master(String AppName, String MasterName, String Format, String FilePath) {
		SparkSession sp = SparkSession.builder().appName(AppName).master(MasterName).getOrCreate();
		
		Dataset<Row> dt = sp.read()
				.format(Format)
				.option("header", true)
				.load(FilePath);
		
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