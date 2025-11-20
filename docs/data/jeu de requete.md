# 🍭 **1. SQL — Contrôles Qualité Produits**

## 🔹 1.1. Détection des valeurs manquantes

```sql
SELECT *
FROM products
WHERE product_name IS NULL
   OR categories IS NULL
   OR sold_countries IS NULL
   OR added_sugars_100g IS NULL
   OR sugars_100g IS NULL
   OR sucrose_100g IS NULL
   OR glucose_100g IS NULL
   OR fructose_100g IS NULL;
```

---

## 🔹 1.2. Détection des valeurs négatives

```sql
SELECT *
FROM products
WHERE added_sugars_100g < 0
   OR sugars_100g < 0
   OR sucrose_100g < 0
   OR glucose_100g < 0
   OR fructose_100g < 0;
```

---

## 🔹 1.3. Détection des valeurs hors bornes (0–100)

```sql
SELECT *
FROM products
WHERE added_sugars_100g > 100
   OR sugars_100g > 100
   OR sucrose_100g > 100
   OR glucose_100g > 100
   OR fructose_100g > 100;
```

---

## 🔹 1.4. Détection des chaînes vides

```sql
SELECT *
FROM products
WHERE TRIM(product_name) = ''
   OR TRIM(categories) = ''
   OR TRIM(sold_countries) = '';
```

---

## 🔹 1.5. Détection des caractères non ASCII

```sql
SELECT *
FROM products
WHERE product_name !~ '^[\x00-\x7F]*$'
   OR categories !~ '^[\x00-\x7F]*$'
   OR sold_countries !~ '^[\x00-\x7F]*$';
```

---

## 🔹 1.6. Détection des doublons

```sql
SELECT product_name, categories, sold_countries,
       added_sugars_100g, sugars_100g, sucrose_100g, glucose_100g, fructose_100g,
       COUNT(*) AS nb
FROM products
GROUP BY product_name, categories, sold_countries,
         added_sugars_100g, sugars_100g, sucrose_100g, glucose_100g, fructose_100g
HAVING COUNT(*) > 1;
```

---

## 🔹 1.7. Détection des pays invalides (non présents dans la table officielle)

```sql
SELECT DISTINCT c.country AS invalid_country
FROM (
    SELECT UNNEST(STRING_TO_ARRAY(sold_countries, ',')) AS country
    FROM products
) c
LEFT JOIN countries ct ON c.country = ct.country_en
WHERE ct.country_en IS NULL;
```

---

# 👤 **2. SQL — Contrôles Qualité Users**

## 🔹 2.1. Valeurs manquantes

```sql
SELECT *
FROM users
WHERE first_name IS NULL
   OR last_name IS NULL
   OR country IS NULL;
```

---

## 🔹 2.2. Chaînes vides

```sql
SELECT *
FROM users
WHERE TRIM(first_name) = ''
   OR TRIM(last_name) = ''
   OR TRIM(country) = '';
```

---

## 🔹 2.3. Caractères non ASCII

```sql
SELECT *
FROM users
WHERE first_name !~ '^[\x00-\x7F]*$'
   OR last_name !~ '^[\x00-\x7F]*$'
   OR country !~ '^[\x00-\x7F]*$';
```

---

## 🔹 2.4. Doublons

```sql
SELECT first_name, last_name, country, COUNT(*) AS nb
FROM users
GROUP BY first_name, last_name, country
HAVING COUNT(*) > 1;
```

---

# 🥗 **3. SQL — Contrôles Qualité Diets**

## 🔹 3.1. Valeurs manquantes

```sql
SELECT *
FROM diets
WHERE name_en IS NULL
   OR name_fr IS NULL
   OR description_en IS NULL
   OR description_fr IS NULL;
```

---

## 🔹 3.2. Chaînes vides

```sql
SELECT *
FROM diets
WHERE TRIM(name_en) = ''
   OR TRIM(name_fr) = ''
   OR TRIM(description_en) = ''
   OR TRIM(description_fr) = '';
```

---

## 🔹 3.3. Caractères non ASCII

```sql
SELECT *
FROM diets
WHERE name_en !~ '^[\x00-\x7F]*$'
   OR name_fr !~ '^[\x00-\x7F]*$'
   OR description_en !~ '^[\x00-\x7F]*$'
   OR description_fr !~ '^[\x00-\x7F]*$';
```

---

## 🔹 3.4. Valeurs hors bornes (0–100)

```sql
SELECT *
FROM diets
WHERE max_added_sugars_100g < 0 OR max_added_sugars_100g > 100
   OR max_sugars_100g < 0 OR max_sugars_100g > 100
   OR max_sucrose_100g < 0 OR max_sucrose_100g > 100
   OR max_glucose_100g < 0 OR max_glucose_100g > 100
   OR max_fructose_100g < 0 OR max_fructose_100g > 100;
```

---

# 🧹 4. SQL — Version “Nettoyage” (équivalent transformation Spark)

## 🔹 4.1. Nettoyage produits complet (pipeline SQL)

```sql
SELECT DISTINCT
    product_name,
    categories,
    sold_countries,
    added_sugars_100g,
    sugars_100g,
    sucrose_100g,
    glucose_100g,
    fructose_100g
FROM products
WHERE product_name IS NOT NULL
  AND categories IS NOT NULL
  AND sold_countries IS NOT NULL

  AND TRIM(product_name) <> ''
  AND TRIM(categories) <> ''
  AND TRIM(sold_countries) <> ''

  AND product_name ~ '^[\x00-\x7F]*$'
  AND categories ~ '^[\x00-\x7F]*$'
  AND sold_countries ~ '^[\x00-\x7F]*$'

  AND added_sugars_100g BETWEEN 0 AND 100
  AND sugars_100g BETWEEN 0 AND 100
  AND sucrose_100g BETWEEN 0 AND 100
  AND glucose_100g BETWEEN 0 AND 100
  AND fructose_100g BETWEEN 0 AND 100;
```