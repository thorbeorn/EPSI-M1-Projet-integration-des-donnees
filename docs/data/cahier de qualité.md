# 📘 **Cahier de Qualité — ETL Spark**

## 1. 🎯 **Objectif du module**

Assurer la **qualité**, la **cohérence**, et la **nettoyage** des données produits / utilisateurs / régimes alimentaires avant leur insertion dans la base analytique.

---

# 2. ✔️ **Règles de Qualité et de Validation**

## 🔹 2.1. Règles Générales

| Règle                               | Description                                                                                             | Méthode appliquée           |
| ----------------------------------- | ------------------------------------------------------------------------------------------------------- | --------------------------- |
| **Aucune valeur manquante**         | Les lignes contenant des `null` sont supprimées                                                         | `na().drop()`               |
| **Suppression des doublons**        | Unicité totale des lignes                                                                               | `dropDuplicates()`          |
| **Suppression des chaînes vides**   | Aucun champ textuel ne doit être vide (`""`)                                                            | `removeEmptyStrings()`      |
| **Caractères ASCII uniquement**     | Pas de caractères spéciaux/UTF-8 dans noms, catégories, pays                                            | `rlike("^[\\x00-\\x7F]*$")` |
| **Valeurs numériques positives**    | Aucune valeur négative pour les nutriments                                                              | `geq(0)`                    |
| **Valeurs dans l’intervalle 0–100** | Les nutriments sont exprimés en g/100g → bornes strictes                                                | `removeOutOfRangeValues()`  |
| **Qualité des pays**                | - suppression des faux pays<br>- création des colonnes propres `sold_countries_en`, `sold_countries_fr` | `cleanCountryNames()`       |

---

## 🔹 2.2. Règles Spécifiques « produits »

| Champ                 | Règle                                    |
| --------------------- | ---------------------------------------- |
| `product_name`        | non vide, ASCII                          |
| `categories`          | non vide, ASCII                          |
| `sold_countries`      | non vide, ASCII, pays valides uniquement |
| Nutriments (`*_100g`) | >=0 et <=100                             |

---

## 🔹 2.3. Règles Spécifiques « users »

| Champ                     | Règle           |
| ------------------------- | --------------- |
| `first_name`, `last_name` | non vide, ASCII |
| `country`                 | non vide, ASCII |

---

## 🔹 2.4. Règles Spécifiques « diets »

| Champ                              | Règle           |
| ---------------------------------- | --------------- |
| `name_en`, `name_fr`               | non vide, ASCII |
| `description_en`, `description_fr` | non vide, ASCII |
| `max_*_100g`                       | 0 ≤ x ≤ 100     |

---

# 3. 🧪 **Coverage de Tests Attendus**

## 🔹 3.1. Tests unitaires (UDF et fonctions privées)

| Fonction                     | Cas à tester                                                                        |
| ---------------------------- | ----------------------------------------------------------------------------------- |
| `selectAndCastColumns()`     | - colonnes manquantes → exception<br>- cast correct des types                       |
| `removeMissingValues()`      | - suppression d’une ligne contenant un null                                         |
| `removeDuplicates()`         | - deux lignes identiques → 1 ligne restante                                         |
| `removeNegativeValues()`     | - valeurs < 0 supprimées<br>- valeurs ≥ 0 conservées                                |
| `removeEmptyStrings()`       | - chaîne "" supprimée<br>- chaîne " " conservée                                     |
| `removeNonASCIICharacters()` | - caractère non-ASCII supprimé<br>- ASCII conservé                                  |
| `removeOutOfRangeValues()`   | - valeurs hors bornes supprimées<br>- min/max inclus                                |
| `cleanCountryNames()`        | - faux pays supprimés<br>- traduction FR correcte<br>- pays dupliqués dans la liste |
| `cleanProductData()`         | - pipeline complet sur dataset mixte                                                |

---

## 🔹 3.2. Tests d’intégration

| Cas                       | Description                                                    |
| ------------------------- | -------------------------------------------------------------- |
| Pipeline complet produits | dataset contenant : valeurs négatives, nulls, UTF-8, faux pays |
| Pipeline utilisateurs     | mélange de données propres/sales                               |
| Pipeline régimes          | mauvaises bornes, descriptions vides                           |

---

# 4. ⚠️ **Anomalies possibles détectées par le pipeline**

## 🔹 4.1. Anomalies syntaxiques / structurelles

* colonnes manquantes dans le CSV source
* type incorrect (ex. "ten" dans un champ float)
* valeurs séparées par `;` au lieu de `,` dans les pays

## 🔹 4.2. Anomalies de contenu

| Anomalie               | Exemple                           |
| ---------------------- | --------------------------------- |
| Valeurs négatives      | `added-sugars_100g = -5`          |
| Valeurs hors 0–100     | `glucose_100g = 250`              |
| Pays invalides         | `sold_countries = "Mars, France"` |
| Caractères non ASCII   | `“Chocolaté”`                     |
| Chaîne vide            | `"product_name" = ""`             |
| Multitude de faux pays | `Italieee`, `Germania`            |

## 🔹 4.3. Anomalies de cohérence

* produit vendu dans 0 pays après nettoyage
* produit dont toutes les catégories sont invalides
* conflits de cases (`france`, `France`, `FRANCE`) → filtrés comme faux pays si absent de la liste officielle

---

# 5. 🔄 **Exemples Before / After Nettoyage**

## 🧁 **Example 1 : Produit**

### **Before**

```json
{
  "product_name": "Chocolaté Noir 70%",
  "categories": "Desserts",
  "sold_countries": "France, Belgique, xxUnknown",
  "added-sugars_100g": -2,
  "sucrose_100g": 105,
  "glucose_100g": "10",
  "fructose_100g": 5
}
```

### **After**

```json
{
  "product_name": "Chocolat Noir 70%",
  "categories": "Desserts",
  "sold_countries_en": "France,Belgium",
  "sold_countries_fr": "France,Belgique",
  "added-sugars_100g": 0,
  "sucrose_100g": null,        // ligne supprimée si hors 0–100
  "glucose_100g": 10.0,
  "fructose_100g": 5.0
}
```

---

## 👤 **Example 2 : User**

### Before

```json
{
  "first_name": "José",
  "last_name": "",
  "country": "Italiaaa"
}
```

### After

→ **ligne supprimée** (UTF-8 + chaîne vide + pays invalides)

---

## 🥗 **Example 3 : Diet**

### Before

```json
{
  "name_en": "Sugar Free",
  "description_fr": "Régime sans sucre ajouté",
  "max_sugars_100g": 150,
  "max_glucose_100g": -1
}
```

### After

→ **ligne supprimée** (glucose < 0 + sucre > 100)
