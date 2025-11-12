package fr.thorbeorn.ProjetIntegrationDesDonnees;

import fr.thorbeorn.ProjetIntegrationDesDonnees.spark.Master;

public class Main {

	public static void main(String[] args) {
		Master master01 = new Master("openfoodfacts", "local", "json", "");
		master01.getData().show();
		master01.getData().describe();

	}

}
