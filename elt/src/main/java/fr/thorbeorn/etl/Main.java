package fr.thorbeorn.etl;

import fr.thorbeorn.etl.spark.Master;

public class Main {
	public static void main(String[] args) {
		
		Master master01 = new Master("toto", "local", "mongodb", "", "mongodb://localhost:27017/off.products");
		master01.getData().printSchema();
        master01.getData().show(10, false);

        master01.getData().describe().show();

        master01.stopSparkSession();
	}
}
