package etl;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

/**
 * Classe utilitaire pour extraire des données depuis différentes sources
 * dans le cadre d'un processus ETL (Extract, Transform, Load)
 */
public class Extractors {

	/**
	 * Extrait des données depuis un fichier CSV avec en-tête automatique
	 * 
	 * @param sparkSession La session Spark active
	 * @param filePath Le chemin vers le fichier CSV
	 * @param delimiter Le délimiteur utilisé dans le CSV (ex: "," ou ";")
	 * @return Un Dataset contenant les données du CSV
	 */
	public static Dataset<Row> extractFromCSV(SparkSession sparkSession, String filePath, String delimiter)
	{
		return sparkSession.read()
                .format("csv")                      // Spécifie le format de lecture
                .option("header", true)             // Indique que la première ligne contient les en-têtes
                .option("delimiter", delimiter)     // Définit le séparateur de colonnes
                .option("inferSchema", true)        // Détecte automatiquement les types de données
                .load(filePath);                    // Charge le fichier
	}
	
	/**
	 * Extrait des données depuis un fichier CSV avec en-têtes personnalisés
	 * 
	 * @param sparkSession La session Spark active
	 * @param filePath Le chemin vers le fichier CSV
	 * @param delimiter Le délimiteur utilisé dans le CSV
	 * @param header Un tableau contenant les noms des colonnes à appliquer
	 * @return Un Dataset contenant les données du CSV avec les en-têtes fournis
	 */
	public static Dataset<Row> extractFromCSV(SparkSession sparkSession, String filePath, String delimiter, String[] header)
	{
		return sparkSession.read()
                .format("csv")                      // Spécifie le format de lecture
                .option("header", false)            // Indique qu'il n'y a pas d'en-tête dans le fichier
                .option("delimiter", delimiter)     // Définit le séparateur de colonnes
                .load(filePath)                     // Charge le fichier
                .toDF(header);                      // Applique les noms de colonnes personnalisés
	}

	/**
	 * Extrait des données depuis un fichier JSON
	 * 
	 * @param sparkSession La session Spark active
	 * @param filePath Le chemin vers le fichier JSON
	 * @return Un Dataset contenant les données du JSON
	 */
	public static Dataset<Row> extractFromJSON(SparkSession sparkSession, String filePath)
	{
		return sparkSession.read()
                .json(filePath);                    // Lit directement le fichier JSON
	}

	/**
	 * Extrait des données depuis une base de données relationnelle via JDBC
	 * 
	 * @param sparkSession La session Spark active
	 * @param dbHost L'URL de connexion à la base de données (ex: "jdbc:mysql://localhost:3306/mabase")
	 * @param dbUser Le nom d'utilisateur pour la connexion
	 * @param dbPassword Le mot de passe pour la connexion
	 * @param dbTable Le nom de la table à extraire
	 * @return Un Dataset contenant les données de la table
	 */
	public static Dataset<Row> extractFromDatabase(SparkSession sparkSession, String dbHost, String dbUser, String dbPassword, String dbTable)
    {
        return sparkSession.read()
                .format("jdbc")                     // Spécifie le format JDBC pour les bases de données
                .option("url", dbHost)              // URL de connexion à la base
                .option("dbtable", dbTable)         // Nom de la table à lire
                .option("user", dbUser)             // Identifiant utilisateur
                .option("password", dbPassword)     // Mot de passe
                .load();                            // Charge les données
    }
}