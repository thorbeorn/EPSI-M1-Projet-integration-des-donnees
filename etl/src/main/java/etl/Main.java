package etl;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

/**
 * Classe principale du processus ETL (Extract, Transform, Load)
 * pour OpenFoodFacts - Génération de menus personnalisés
 * 
 * Ce programme implémente un pipeline complet qui :
 * 1. EXTRACT : Charge les données sources depuis des fichiers CSV
 * 2. TRANSFORM : Nettoie, valide et transforme les données
 * 3. GENERATE : Crée des menus hebdomadaires personnalisés
 * 4. LOAD : Insère les résultats dans une base de données MySQL
 */
public class Main {

	/**
	 * Point d'entrée principal de l'application ETL
	 * 
	 * Workflow complet du processus :
	 * - Initialisation de Spark
	 * - Extraction des données brutes (produits, régimes, utilisateurs)
	 * - Nettoyage et validation de chaque jeu de données
	 * - Génération de menus hebdomadaires personnalisés
	 * - Chargement des données dans la base de données
	 * - Arrêt propre de Spark
	 * 
	 * @param args Arguments de ligne de commande (non utilisés)
	 */
	public static void main(String[] args)
	{
	    // ===== ÉTAPE 0 : INITIALISATION =====
	    // Crée ou récupère une session Spark avec le nom "openfoodfacts-etl"
	    // Configure automatiquement la mémoire et le mode d'exécution
	    SparkSession sparkSession = Utils.initializeSparkSession("openfoodfacts-etl");

	    // ===== ÉTAPE 1 : EXTRACTION (Extract) =====
	    // Charge les données brutes depuis les fichiers CSV sources
	    
	    // Extraction des produits alimentaires
	    // Utilise la tabulation (\t) comme délimiteur
	    Dataset<Row> dfProducts = Extractors.extractFromCSV(
	    		sparkSession, 
	    		Constants.DATA_FILE_PRODUCTS,  // "/data/products.csv"
	    		"\t"                           // Délimiteur : tabulation
	    );
	    
	    // Extraction des régimes alimentaires
	    // Utilise la virgule (,) comme délimiteur
	    Dataset<Row> dfDiets = Extractors.extractFromCSV(
	    		sparkSession, 
	    		Constants.DATA_FILE_DIET,      // "/data/diets.csv"
	    		","                            // Délimiteur : virgule
	    );
	    
	    // Extraction des utilisateurs
	    // Utilise la virgule (,) comme délimiteur
	    Dataset<Row> dfUsers = Extractors.extractFromCSV(
	    		sparkSession, 
	    		Constants.DATA_FILE_USERS,     // "/data/users.csv"
	    		","                            // Délimiteur : virgule
	    );

	    // ===== ÉTAPE 2 : TRANSFORMATION (Transform) =====
	    // Nettoie, valide et transforme chaque jeu de données
	    // L'ordre est important : les régimes doivent être nettoyés avant les utilisateurs
	    
	    // Nettoyage des produits :
	    // - Sélection et casting des colonnes
	    // - Suppression des doublons et valeurs manquantes
	    // - Validation des valeurs nutritionnelles (0-100g)
	    // - Validation des noms de pays
	    Dataset<Row> dfProductsCleaned = Transformers.cleanProductData(dfProducts, sparkSession);
	    
	    // Nettoyage des régimes :
	    // - Suppression des chaînes vides
	    // - Validation des caractères (ASCII + accents français)
	    // - Validation des limites nutritionnelles
	    Dataset<Row> dfDietsCleaned = Transformers.cleanDietData(dfDiets, sparkSession);
	    
	    // Nettoyage des utilisateurs :
	    // - Suppression des chaînes vides
	    // - Validation des caractères ASCII
	    // - Validation de l'intégrité référentielle (regime_id doit exister)
	    // NOTE : Utilise dfDietsCleaned pour valider les clés étrangères
	    Dataset<Row> dfUsersCleaned = Transformers.cleanUserData(dfUsers, dfDietsCleaned, sparkSession);

	    // ===== ÉTAPE 3 : GÉNÉRATION (Generate) =====
	    // Crée des menus hebdomadaires personnalisés pour chaque utilisateur
	    // en fonction de :
	    // - Leur régime alimentaire (contraintes nutritionnelles)
	    // - Leur pays (disponibilité des produits)
	    // - Les produits disponibles
	    Dataset<Row> weeklyMenus = Generators.generateWeeklyMenu(
	    		sparkSession, 
	    		dfUsersCleaned, 
	    		dfDietsCleaned, 
	    		dfProductsCleaned
	    );

	    // ===== ÉTAPE 4 : CHARGEMENT (Load) =====
	    // Insère les données nettoyées et générées dans la base de données MySQL
	    // Mode : Append (ajoute aux données existantes sans écraser)
	    
	    // Chargement de la table "regime" (régimes alimentaires)
	    Loaders.loadToDatabase(
	    		dfDietsCleaned,          // Dataset source
	    		Constants.DB_HOST,       // jdbc:mysql://mysql:3306/openfoodfacts-etl
	    		Constants.DB_USER,       // root
	    		Constants.DB_PASSWORD,   // ""
	    		"regime"                 // Nom de la table de destination
	    );
	    
	    // Chargement de la table "user" (utilisateurs)
	    Loaders.loadToDatabase(
	    		dfUsersCleaned, 
	    		Constants.DB_HOST, 
	    		Constants.DB_USER, 
	    		Constants.DB_PASSWORD, 
	    		"user"                   // Nom de la table de destination
	    );
	    
	    // Chargement de la table "daily_menu" (menus quotidiens)
	    Loaders.loadToDatabase(
	    		weeklyMenus, 
	    		Constants.DB_HOST, 
	    		Constants.DB_USER, 
	    		Constants.DB_PASSWORD, 
	    		"daily_menu"             // Nom de la table de destination
	    );

	    // ===== ÉTAPE 5 : FINALISATION =====
	    // Arrête proprement la session Spark
	    // Libère les ressources (mémoire, threads, connexions)
	    sparkSession.stop();
	    
	    // Le processus ETL est terminé
	    // Les données sont maintenant dans la base de données et prêtes à être utilisées
	}
}