package etl;

/**
 * Classe finale contenant toutes les constantes de configuration
 * utilisées dans le processus ETL OpenFoodFacts
 * 
 * Cette classe ne peut pas être instanciée ni héritée (final)
 */
public final class Constants {
	
	// ===== CHEMINS DES FICHIERS DE DONNÉES =====
	
	/**
	 * Chemin vers le fichier CSV contenant les données des produits alimentaires
	 */
	public static final String DATA_FILE_PRODUCTS = "/data/products.csv";
	
	/**
	 * Chemin vers le fichier CSV contenant les informations sur les pays
	 */
	public static final String DATA_FILE_COUNTRIES = "/data/countries.csv";
	
	/**
	 * Chemin vers le fichier CSV contenant les types de régimes alimentaires
	 */
	public static final String DATA_FILE_DIET = "/data/diets.csv";
	
	/**
	 * Chemin vers le fichier CSV contenant les données des utilisateurs
	 */
	public static final String DATA_FILE_USERS = "/data/users.csv";

	// ===== PARAMÈTRES DE CONNEXION À LA BASE DE DONNÉES =====
	
	/**
	 * URL de connexion JDBC à la base de données MySQL
	 * Format : jdbc:mysql://hôte:port/nom_base
	 * Ici : serveur "mysql" sur le port 3306, base "openfoodfacts-etl"
	 */
	public static final String DB_HOST = "jdbc:mysql://mysql:3306/openfoodfacts-etl";
	
	/**
	 * Nom d'utilisateur pour la connexion à la base de données
	 */
    public static final String DB_USER = "root";
    
    /**
     * Mot de passe pour la connexion à la base de données
     * ATTENTION : mot de passe vide - non sécurisé pour un environnement de production !
     * Cette configuration est acceptable uniquement pour le développement/test
     */
    public static final String DB_PASSWORD = "";
}