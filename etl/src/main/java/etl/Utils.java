package etl;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import static org.apache.spark.sql.functions.*;

/**
 * Classe utilitaire contenant des fonctions helper
 * pour les opérations courantes du processus ETL
 */
public class Utils {
	
	/**
	 * Initialise et configure une session Spark avec des paramètres personnalisables
	 * 
	 * Les paramètres peuvent être fournis via les propriétés système (-D),
	 * sinon des valeurs par défaut sont utilisées :
	 * - master : "local[4]" (exécution locale avec 4 threads)
	 * - driver memory : "2g" (2 Go de RAM pour le driver)
	 * - executor memory : "2g" (2 Go de RAM par executor)
	 * 
	 * @param appname Le nom de l'application Spark (apparaît dans l'interface Spark UI)
	 * @return Une instance de SparkSession configurée et prête à l'emploi
	 */
	public static SparkSession initializeSparkSession(String appname) 
	{
		// Récupère le mode d'exécution depuis les propriétés système, sinon utilise "local[4]"
		String master = System.getProperty("spark.master", "local[4]");
		
		// Récupère la mémoire allouée au driver, sinon utilise 2 Go
	    String driverMemory = System.getProperty("spark.driver.memory", "2g");
	    
	    // Récupère la mémoire allouée aux executors, sinon utilise 2 Go
	    String executorMemory = System.getProperty("spark.executor.memory", "2g");
	    
	    // Construit et retourne la session Spark avec la configuration spécifiée
        return SparkSession.builder()
                .appName(appname)                                   // Définit le nom de l'application
                .master(master)                                     // Définit le mode d'exécution (local, cluster, etc.)
                .config("spark.driver.memory", driverMemory)        // Configure la mémoire du driver
                .config("spark.executor.memory", executorMemory)    // Configure la mémoire des executors
                .getOrCreate();                                     // Crée ou récupère une session existante
    }
	
	/**
	 * Divise une colonne contenant du texte en mots individuels uniques
	 * 
	 * Processus :
	 * 1. Divise le contenu de la colonne selon un délimiteur
	 * 2. "Explose" chaque liste de mots en lignes séparées
	 * 3. Élimine les doublons pour ne garder que les mots uniques
	 * 4. Retourne un Dataset de chaînes de caractères
	 * 
	 * Exemple : "pomme,banane,pomme" avec délimiteur "," -> ["pomme", "banane"]
	 * 
	 * @param df Le Dataset source contenant la colonne à traiter
	 * @param columnNameInput Le nom de la colonne contenant le texte à diviser
	 * @param columnNameOutput Le nom de la colonne de sortie contenant les mots
	 * @param delimiter Le délimiteur utilisé pour séparer les mots (ex: ",", ";", " ")
	 * @return Un Dataset de String contenant tous les mots uniques trouvés
	 */
	public static Dataset<String> splitColumnIntoWords(Dataset<Row> df, String columnNameInput, String columnNameOutput, String delimiter) {
		// Étape 1 : Divise la colonne en tableau de mots selon le délimiteur
        Dataset<Row> splitData = df.withColumn(columnNameInput, split(col(columnNameInput), delimiter));
        
        // Étape 2 : Transforme chaque tableau en lignes individuelles et élimine les doublons
        Dataset<Row> uniqueWords = splitData.select(explode(col(columnNameInput)).as(columnNameOutput)).distinct();
        
        // Étape 3 : Convertit le Dataset<Row> en Dataset<String> pour faciliter l'utilisation
        Dataset<String> listWords = uniqueWords.select(columnNameOutput).as(Encoders.STRING());
        
        return listWords;
    }
	
	/**
	 * Échappe les apostrophes dans une chaîne de caractères
	 * pour la rendre utilisable dans des requêtes SQL
	 * 
	 * Transforme les apostrophes simples (') en apostrophes échappées (\')
	 * pour éviter les erreurs de syntaxe SQL et les injections SQL
	 * 
	 * Exemple : "l'orange" devient "l\'orange"
	 * 
	 * @param s La chaîne de caractères à échapper
	 * @return La chaîne avec les apostrophes échappées
	 */
	public static String escape(String s) {
	    return s.replace("'", "\\'");
	}
}