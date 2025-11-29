package etl;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;

/**
 * Classe utilitaire pour charger (Load) des données vers différentes destinations
 * dans le cadre d'un processus ETL (Extract, Transform, Load)
 */
public class Loaders {
	
	/**
	 * Charge un Dataset vers une table de base de données relationnelle via JDBC
	 * Les données sont ajoutées à la table existante (mode Append)
	 * 
	 * @param df Le Dataset contenant les données à charger
	 * @param dbHost L'URL de connexion JDBC à la base de données (ex: "jdbc:mysql://localhost:3306/mabase")
	 * @param dbUser Le nom d'utilisateur pour la connexion à la base
	 * @param dbPassword Le mot de passe pour la connexion à la base
	 * @param dbTable Le nom de la table de destination dans la base de données
	 */
	public static void loadToDatabase(Dataset<Row> df, String dbHost, String dbUser, String dbPassword, String dbTable) {
		df.write()                                          // Prépare l'écriture du Dataset
        .format("jdbc")                                     // Spécifie le format JDBC pour bases de données
        .option("url", dbHost)                              // URL de connexion à la base
        .option("dbtable", dbTable)                         // Nom de la table de destination
        .option("user", dbUser)                             // Identifiant utilisateur
        .option("password", dbPassword)                     // Mot de passe
        .option("driver", "com.mysql.cj.jdbc.Driver")      // Driver JDBC MySQL (version récente)
        .mode(SaveMode.Append)                              // Mode d'écriture : ajoute les données sans écraser
        .save();                                            // Exécute l'opération de sauvegarde
	}
}