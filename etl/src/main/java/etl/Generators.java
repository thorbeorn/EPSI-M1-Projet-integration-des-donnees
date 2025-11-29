package etl;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe utilitaire pour générer des menus alimentaires personnalisés
 * en fonction des régimes des utilisateurs et des produits disponibles
 */
public class Generators {
	
	/**
	 * Schéma de données pour les menus hebdomadaires
	 * Définit la structure des colonnes :
	 * - user_id : identifiant de l'utilisateur (entier, non nul)
	 * - day : numéro du jour de la semaine (1-7, entier, non nul)
	 * - breakfast_product_name : nom du produit pour le petit-déjeuner (chaîne, non nul)
	 * - lunch_product_name : nom du produit pour le déjeuner (chaîne, non nul)
	 * - dinner_product_name : nom du produit pour le dîner (chaîne, non nul)
	 */
	private static final StructType schemaWeeklyMenus = DataTypes.createStructType(new StructField[]{
            DataTypes.createStructField("user_id", DataTypes.IntegerType, false),
            DataTypes.createStructField("day", DataTypes.IntegerType, false),
            DataTypes.createStructField("breakfast_product_name", DataTypes.StringType, false),
            DataTypes.createStructField("lunch_product_name", DataTypes.StringType, false),
            DataTypes.createStructField("dinner_product_name", DataTypes.StringType, false)
    });
	
	/**
	 * Génère un menu journalier (3 repas) pour un utilisateur en respectant
	 * les contraintes nutritionnelles de son régime alimentaire
	 * 
	 * Le processus :
	 * 1. Extrait les limites nutritionnelles maximales du régime
	 * 2. Filtre les produits disponibles selon ces contraintes et le pays de l'utilisateur
	 * 3. Sélectionne aléatoirement 3 produits (petit-déjeuner, déjeuner, dîner)
	 * 
	 * @param regimePlan Une ligne contenant les informations du régime alimentaire de l'utilisateur
	 *                   (doit contenir : max_proteins_g_day, max_fat_g_day, max_carbohydrates_g_day)
	 * @param dfAvailableProducts Dataset contenant tous les produits alimentaires disponibles
	 * @param userCountry Le pays de l'utilisateur (pour filtrer les produits vendus dans ce pays)
	 * @param soldContriesColone Le nom de la colonne contenant les pays où le produit est vendu
	 * @return Une Row contenant les 3 noms de produits (petit-déjeuner, déjeuner, dîner)
	 */
	public static Row generateDailyMenu(Row regimePlan, Dataset<Row> dfAvailableProducts, String userCountry, String soldContriesColone) {
		// Extraction des limites nutritionnelles maximales autorisées par jour (conversion en double)
		double maxProtein = ((Number) regimePlan.getAs("max_proteins_g_day")).doubleValue();
		double maxFat = ((Number) regimePlan.getAs("max_fat_g_day")).doubleValue();
		double maxCarbohydrates = ((Number) regimePlan.getAs("max_carbohydrates_g_day")).doubleValue();
		
		// Filtrage des produits respectant toutes les contraintes nutritionnelles et géographiques
		Dataset<Row> dfFilteredProducts = dfAvailableProducts.filter(
			dfAvailableProducts.col("proteins_100g").leq(maxProtein)              // Protéines <= max
                .and(dfAvailableProducts.col("fat_100g").leq(maxFat))                 // Graisses <= max
                .and(dfAvailableProducts.col("carbohydrates_100g").leq(maxCarbohydrates))  // Glucides <= max
                .and(dfAvailableProducts.col(soldContriesColone).contains(userCountry))    // Vendu dans le pays
		);
		
		// Sélection aléatoire de 3 produits pour les 3 repas de la journée
		// sample(false, 0.1) : échantillonne 10% des données sans remplacement, puis prend le premier élément
	    Row breakfast = dfFilteredProducts.sample(false, 0.1).first();  // Petit-déjeuner
	    Row lunch = dfFilteredProducts.sample(false, 0.1).first();      // Déjeuner
	    Row dinner = dfFilteredProducts.sample(false, 0.1).first();     // Dîner
	    
	    // Extraction des noms de produits
	    String breakfastProductName = breakfast.getAs("product_name");
	    String lunchProductName = lunch.getAs("product_name");
	    String dinnerProductName = dinner.getAs("product_name");
	    
	    // Création et retour d'une Row contenant les 3 noms de produits
	    return RowFactory.create(breakfastProductName, lunchProductName, dinnerProductName);
	}
	
	/**
	 * Génère des menus hebdomadaires personnalisés pour tous les utilisateurs
	 * 
	 * Pour chaque utilisateur :
	 * 1. Récupère ses informations (ID, régime, pays)
	 * 2. Trouve les contraintes de son régime alimentaire
	 * 3. Génère un menu pour chaque jour de la semaine (actuellement limité à 1 jour)
	 * 4. Compile tous les menus dans un Dataset
	 * 
	 * @param sparkSession La session Spark active
	 * @param dfUsersCleaned Dataset contenant les utilisateurs nettoyés
	 *                       (colonnes attendues : user_id, regime_id, country)
	 * @param dfDietsCleaned Dataset contenant les régimes alimentaires
	 *                       (colonnes attendues : regime_id, max_proteins_g_day, max_fat_g_day, max_carbohydrates_g_day)
	 * @param dfProductsCleaned Dataset contenant les produits nettoyés
	 *                          (colonnes attendues : product_name, proteins_100g, fat_100g, carbohydrates_100g, sold_countries_en)
	 * @return Un Dataset contenant tous les menus générés selon le schéma schemaWeeklyMenus
	 */
	public static Dataset<Row> generateWeeklyMenu(SparkSession sparkSession, Dataset<Row> dfUsersCleaned, Dataset<Row> dfDietsCleaned, Dataset<Row> dfProductsCleaned) {
		// Liste pour accumuler toutes les lignes de menus générés
		List<Row> menuRows = new ArrayList<>(); 
		
		// Collecte tous les utilisateurs en mémoire (attention : peut être coûteux si beaucoup d'utilisateurs)
		List<Row> userRows = dfUsersCleaned.collectAsList();
		
		// Boucle sur chaque utilisateur
		for (Row userRow : userRows) { 
			// Extraction des informations de l'utilisateur
			int userId = userRow.getInt(userRow.fieldIndex("user_id")); 
			int userRegimeId = userRow.getInt(userRow.fieldIndex("regime_id")); 
			String userCountry = userRow.getString(userRow.fieldIndex("country"));
			
			// Recherche du régime alimentaire correspondant à l'utilisateur
			List<Row> regimeList = dfDietsCleaned
			    .filter(dfDietsCleaned.col("regime_id").equalTo(userRegimeId))  // Filtre par ID de régime
			    .limit(1)                                                        // Prend seulement le premier résultat
				.collectAsList();                                                // Collecte en liste
			
			// Si aucun régime trouvé, passe à l'utilisateur suivant
			if (regimeList.isEmpty()) { 
				continue; 
			}
			
			// Récupère les informations du régime
			Row rowUserRegimeInfo = regimeList.get(0);
			
			// Génération du menu pour chaque jour de la semaine
			// NOTE : Actuellement limité à 1 jour (day <= 1) au lieu de 7 jours complets
			// Pour générer une semaine complète, décommenter la ligne suivante :
			//for (int day = 1; day <= 7; day++) { 
			for (int day = 1; day <= 1; day++) { 
				// Génère un menu journalier
				Row dailyMenu = generateDailyMenu(rowUserRegimeInfo, dfProductsCleaned, userCountry, "sold_countries_en");
				
				// Crée une ligne complète avec user_id, day et les 3 repas
				Row menuRow = RowFactory.create(
					userId,                  // ID utilisateur
					day,                     // Numéro du jour
					dailyMenu.getAs(0),     // Petit-déjeuner
					dailyMenu.getAs(1),     // Déjeuner
					dailyMenu.getAs(2)      // Dîner
				);
				
				// Ajoute le menu à la liste
				menuRows.add(menuRow); 
			}
		} 
		
		// Crée et retourne un Dataset à partir de la liste de menus avec le schéma défini
		return sparkSession.createDataFrame(menuRows, schemaWeeklyMenus);
	}
}