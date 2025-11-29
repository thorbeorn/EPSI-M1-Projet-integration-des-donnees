package etl;

import java.util.List;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import static org.apache.spark.sql.functions.expr;
import static org.apache.spark.sql.functions.when;

/**
 * Classe contenant les méthodes de transformation (Transform) du processus ETL
 * Responsable du nettoyage, de la validation et de la transformation des données
 */
public class Transformers {

	/**
	 * Sélectionne les colonnes nécessaires et applique le casting de types approprié
	 * Renomme également certaines colonnes pour plus de clarté
	 * 
	 * @param df Le Dataset source contenant les données brutes des produits
	 * @return Un Dataset avec les colonnes sélectionnées, typées et renommées
	 */
	private static Dataset<Row> selectAndCastColumns(Dataset<Row> df) {
	    return df.select(
                df.col("product_name").cast("string"),           // Nom du produit en chaîne
                df.col("categories_en").cast("string"),          // Catégories en anglais
                df.col("countries_en").cast("string"),           // Pays en anglais
                df.col("proteins_100g").cast("float"),           // Protéines pour 100g en nombre flottant
                df.col("carbohydrates_100g").cast("float"),      // Glucides pour 100g
                df.col("fat_100g").cast("float")                 // Graisses pour 100g
        ).withColumnRenamed("countries_en", "sold_countries")    // Renomme pour clarté
        .withColumnRenamed("categories_en", "categories");
	}
	
	/**
	 * Supprime les lignes en double dans le Dataset
	 * Conserve uniquement la première occurrence de chaque ligne identique
	 * 
	 * @param df Le Dataset pouvant contenir des doublons
	 * @return Un Dataset sans doublons
	 */
	private static Dataset<Row> removeDuplicates(Dataset<Row> df) {
	    return df.dropDuplicates();
	}
	
	/**
	 * Supprime les lignes contenant des valeurs négatives dans une colonne spécifique
	 * Utile pour les données nutritionnelles qui ne peuvent pas être négatives
	 * 
	 * @param df Le Dataset à filtrer
	 * @param columnName Le nom de la colonne à vérifier
	 * @return Un Dataset ne contenant que des valeurs >= 0 pour la colonne spécifiée
	 */
	private static Dataset<Row> removeNegativeValues(Dataset<Row> df, String columnName) {
	    return df.filter(
	            df.col(columnName).geq(0)  // Greater or Equal (>=) à 0
	    );
	}
	
	/**
	 * Supprime les lignes où une colonne contient une chaîne vide
	 * 
	 * @param df Le Dataset à filtrer
	 * @param columnName Le nom de la colonne à vérifier
	 * @return Un Dataset sans chaînes vides dans la colonne spécifiée
	 */
	private static Dataset<Row> removeEmptyStrings(Dataset<Row> df, String columnName) {
	    return df.filter(
	            df.col(columnName).notEqual("")  // Filtre les chaînes vides
	    );
	}
	
	/**
	 * Supprime toutes les lignes contenant au moins une valeur manquante (null)
	 * dans n'importe quelle colonne
	 * 
	 * @param df Le Dataset pouvant contenir des valeurs nulles
	 * @return Un Dataset sans aucune valeur nulle
	 */
	private static Dataset<Row> removeMissingValues(Dataset<Row> df) {
	    return df.na().drop();  // na() = gestion des valeurs nulles/manquantes
	}
	
	/**
	 * Supprime les lignes contenant des caractères non-ASCII dans une colonne
	 * Utilise une expression régulière pour valider que tous les caractères sont ASCII
	 * 
	 * @param df Le Dataset à filtrer
	 * @param columnName Le nom de la colonne à vérifier
	 * @param regex L'expression régulière définissant les caractères autorisés
	 * @return Un Dataset ne contenant que des caractères correspondant au regex
	 */
	private static Dataset<Row> removeNonASCIICharacters(Dataset<Row> df, String columnName, String regex) {
        return df.filter(
                df.col(columnName).rlike(regex)  // rlike = regex like (correspond au pattern)
        );
    }
	
	/**
	 * Supprime les lignes dont les valeurs d'une colonne sont hors d'une plage définie
	 * Utile pour valider les valeurs nutritionnelles (ex: 0-100g pour 100g de produit)
	 * 
	 * @param df Le Dataset à filtrer
	 * @param columnName Le nom de la colonne à vérifier
	 * @param min La valeur minimale acceptable (incluse)
	 * @param max La valeur maximale acceptable (incluse)
	 * @return Un Dataset avec des valeurs dans la plage [min, max]
	 */
	private static Dataset<Row> removeOutOfRangeValues(Dataset<Row> df, String columnName, float min, float max) {
        return df.filter(
        		df.col(columnName).geq(min)          // >= min
                .and(df.col(columnName).leq(max))    // ET <= max
        );
    }
	
	/**
	 * Nettoie et valide les noms de pays dans les données de produits
	 * 
	 * Processus :
	 * 1. Extrait tous les noms de pays uniques depuis la colonne "sold_countries"
	 * 2. Charge un référentiel de pays valides depuis un fichier CSV
	 * 3. Identifie les noms de pays invalides (non présents dans le référentiel)
	 * 4. Supprime ces pays invalides de la colonne "sold_countries"
	 * 5. Renomme la colonne nettoyée en "sold_countries_en"
	 * 6. Supprime les lignes où aucun pays valide ne reste
	 * 
	 * @param dfSelectedColumns Le Dataset contenant les produits avec leurs pays
	 * @param sparkSession La session Spark pour charger le référentiel de pays
	 * @return Un Dataset avec des noms de pays validés et nettoyés
	 */
	private static Dataset<Row> cleanCountryNames(Dataset<Row> dfSelectedColumns, SparkSession sparkSession) {
	    // Étape 1 : Extrait tous les pays uniques depuis la colonne (séparés par virgules)
		Dataset<String> listCountry = Utils.splitColumnIntoWords(dfSelectedColumns, "sold_countries", "country", ",");
	    
		// Étape 2 : Charge le fichier de référence contenant les pays valides
		Dataset<Row> dfCountries = Extractors.extractFromCSV(sparkSession, Constants.DATA_FILE_COUNTRIES, ",", new String[] {
	        "index",
	        "country_id",
	        "country_code_2",      // Code ISO à 2 lettres (ex: FR)
	        "country_code_3",      // Code ISO à 3 lettres (ex: FRA)
	        "country_fr",          // Nom en français
	        "country_en"           // Nom en anglais
	    });
	    
		// Étape 3 : Jointure externe gauche pour identifier les pays invalides
		// Les pays non présents dans le référentiel auront country_en = null
		Dataset<Row> dfMergedData = listCountry.join(dfCountries, 
				listCountry.col("country").equalTo(dfCountries.col("country_en")), 
				"left_outer");
	    
		// Étape 4 : Sélectionne uniquement les pays qui n'existent pas dans le référentiel
		Dataset<Row> dfFalseCountry = dfMergedData
				.filter(dfMergedData.col("country_en").isNull())
				.select("country");
	    
		// Convertit la liste des faux pays en liste Java pour traitement
		List<String> falseCountryList = dfFalseCountry.as(Encoders.STRING()).collectAsList();

	    Dataset<Row> dfCleaned = dfSelectedColumns;
	    
	    // Étape 5 : Crée une nouvelle colonne "sold_countries_en" en filtrant les pays invalides
	    // Utilise une expression SQL complexe pour :
	    // - Diviser la chaîne de pays par virgules (split)
	    // - Filtrer pour exclure les pays de la liste des faux pays (filter + !array_contains)
	    // - Rejoindre les pays valides avec des virgules (concat_ws)
	    dfCleaned = dfCleaned.withColumn(
	        "sold_countries_en",
	        expr(
	            "concat_ws(',', " +                                          // Concatène avec virgule
	                "filter(split(sold_countries, ','), country -> " +       // Filtre les pays
	                    "!array_contains(array('" +                          // Vérifie si pays n'est PAS dans la liste
	                        String.join("','", falseCountryList.stream()
	                        		.map(Utils::escape)                      // Échappe les apostrophes
	                        		.toList()) +
	                    "'), country)" +
	                ")" +
	            ")"
	        )
	    );

	    // Étape 6 : Remplace les chaînes vides par null (produits sans pays valide)
	    dfCleaned = dfCleaned.withColumn("sold_countries_en",
	        when(dfCleaned.col("sold_countries_en").equalTo(""), null)
	            .otherwise(dfCleaned.col("sold_countries_en")));

	    // Étape 7 : Supprime les lignes où sold_countries_en est null
	    dfCleaned = dfCleaned.na().drop(new String[]{"sold_countries_en"});
	    
	    // Étape 8 : Supprime l'ancienne colonne non nettoyée
	    dfCleaned = dfCleaned.drop("sold_countries");

	    return dfCleaned;
	}
	
	/**
	 * Pipeline complet de nettoyage des données de produits
	 * 
	 * Applique dans l'ordre :
	 * 1. Sélection et casting des colonnes
	 * 2. Suppression des valeurs manquantes
	 * 3. Suppression des doublons
	 * 4. Suppression des valeurs négatives (protéines, glucides, graisses)
	 * 5. Suppression des chaînes vides (nom, catégories, pays)
	 * 6. Suppression des caractères non-ASCII
	 * 7. Validation des plages de valeurs nutritionnelles (0-100g)
	 * 8. Nettoyage et validation des noms de pays
	 * 
	 * @param dfProducts Le Dataset brut des produits
	 * @param sparkSession La session Spark nécessaire pour le nettoyage des pays
	 * @return Un Dataset de produits nettoyé et validé
	 */
	public static Dataset<Row> cleanProductData(Dataset<Row> dfProducts, SparkSession sparkSession) {
		// Sélection et typage des colonnes
		dfProducts = selectAndCastColumns(dfProducts);
		dfProducts = removeMissingValues(dfProducts);
		dfProducts = removeDuplicates(dfProducts);
		
		// Validation des valeurs nutritionnelles (pas de valeurs négatives)
		dfProducts = removeNegativeValues(dfProducts, "proteins_100g");
		dfProducts = removeNegativeValues(dfProducts, "carbohydrates_100g");
		dfProducts = removeNegativeValues(dfProducts, "fat_100g");
		
		// Suppression des chaînes vides
		dfProducts = removeEmptyStrings(dfProducts, "product_name");
		dfProducts = removeEmptyStrings(dfProducts, "categories");
		dfProducts = removeEmptyStrings(dfProducts, "sold_countries");
		
		// Validation ASCII (caractères entre 0x00 et 0x7F)
		dfProducts = removeNonASCIICharacters(dfProducts, "product_name", "^[\\x00-\\x7F]*$");
		dfProducts = removeNonASCIICharacters(dfProducts, "categories", "^[\\x00-\\x7F]*$");
		dfProducts = removeNonASCIICharacters(dfProducts, "sold_countries", "^[\\x00-\\x7F]*$");
		
		// Validation des plages de valeurs (0-100g pour 100g de produit)
		dfProducts = removeOutOfRangeValues(dfProducts, "proteins_100g", 0, 100);
		dfProducts = removeOutOfRangeValues(dfProducts, "carbohydrates_100g", 0, 100);
		dfProducts = removeOutOfRangeValues(dfProducts, "fat_100g", 0, 100);
		
		// Nettoyage des noms de pays
		dfProducts = cleanCountryNames(dfProducts, sparkSession);
		
		return dfProducts;
	}
	
	/**
	 * Pipeline de nettoyage des données utilisateurs
	 * 
	 * Applique :
	 * 1. Suppression des chaînes vides (prénom, nom, pays)
	 * 2. Validation des caractères ASCII
	 * 3. Validation de l'existence du regime_id dans la table des régimes (jointure inner)
	 * 
	 * @param dfUsers Le Dataset brut des utilisateurs
	 * @param dfDiets Le Dataset des régimes alimentaires pour validation
	 * @param sparkSession La session Spark
	 * @return Un Dataset d'utilisateurs nettoyé avec des regime_id valides
	 */
	public static Dataset<Row> cleanUserData(Dataset<Row> dfUsers, Dataset<Row> dfDiets, SparkSession sparkSession) {
	    // Suppression des chaînes vides
		dfUsers = removeEmptyStrings(dfUsers, "first_name");
	    dfUsers = removeEmptyStrings(dfUsers, "last_name");
	    dfUsers = removeEmptyStrings(dfUsers, "country");
	    
	    // Validation ASCII uniquement (0x00-0x7F)
	    dfUsers = removeNonASCIICharacters(dfUsers, "first_name", "^[\\x00-\\x7F]*$");
	    dfUsers = removeNonASCIICharacters(dfUsers, "last_name", "^[\\x00-\\x7F]*$");
	    dfUsers = removeNonASCIICharacters(dfUsers, "country", "^[\\x00-\\x7F]*$");
	    
	    // Validation par jointure : ne garde que les utilisateurs ayant un regime_id valide
	    // La jointure "inner" élimine automatiquement les utilisateurs avec regime_id inexistant
	    dfUsers = dfUsers.join(
	        dfDiets.select("regime_id"),                                    // Sélectionne uniquement les ID de régimes
	        dfUsers.col("regime_id").equalTo(dfDiets.col("regime_id")),    // Condition de jointure
	        "inner"                                                         // Jointure interne (élimine les non-correspondances)
	    ).select(dfUsers.col("*"));                                         // Conserve toutes les colonnes des utilisateurs
	    
	    return dfUsers;
	}
	
	/**
	 * Pipeline de nettoyage des données de régimes alimentaires
	 * 
	 * Applique :
	 * 1. Suppression des chaînes vides (nom, description)
	 * 2. Validation des caractères ASCII et Latin-1 (pour accents français)
	 * 3. Validation des plages de valeurs nutritionnelles maximales (0-100g)
	 * 
	 * @param dfDiets Le Dataset brut des régimes
	 * @param sparkSession La session Spark
	 * @return Un Dataset de régimes nettoyé et validé
	 */
	public static Dataset<Row> cleanDietData(Dataset<Row> dfDiets, SparkSession sparkSession) {
		// Suppression des chaînes vides
		dfDiets = removeEmptyStrings(dfDiets, "name");
		dfDiets = removeEmptyStrings(dfDiets, "description");
		
		// Validation ASCII + caractères accentués Latin-1 (0xC0-0xFF pour é, è, à, etc.)
		// Permet les caractères français dans les noms et descriptions de régimes
		dfDiets = removeNonASCIICharacters(dfDiets, "name", "^[\\x00-\\x7F\\u00C0-\\u00FF]*$");
		dfDiets = removeNonASCIICharacters(dfDiets, "description", "^[\\x00-\\x7F\\u00C0-\\u00FF]*$");
		
		// Validation des limites nutritionnelles maximales par jour (0-100g)
		dfDiets = removeOutOfRangeValues(dfDiets, "max_proteins_g_day", 0, 100);
		dfDiets = removeOutOfRangeValues(dfDiets, "max_carbohydrates_g_day", 0, 100);
		dfDiets = removeOutOfRangeValues(dfDiets, "max_fat_g_day", 0, 100);
		
		return dfDiets;
	}
}