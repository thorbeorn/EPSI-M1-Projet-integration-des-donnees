# 📘 README — Initialisation de l’environnement Docker + Restauration du dump MongoDB + Création des tables SQL

Ce document explique comment initier l'environnement Docker nécessaire pour utiliser le *dump MongoDB* d'OpenFoodFacts, ainsi que comment restaurer la base et créer les tables SQL utilisées par l’ETL.

---

# En cas d'utilisation de la backup mongoDB

## 🚀 1. Modification du `docker-compose.yml`

Ajouter les éléments suivants dans votre fichier `docker-compose.yml` :

```yml
volumes:
  elt_mongo_data: {}

services:
  mongo:
    image: mongo:bionic
    container_name: mongo
    restart: always
    ports:
      - "27017:27017"
    volumes:
      - elt_mongo_data:/data/db
      - ${PWD}/openfoodfacts-mongodbdump.gz:/openfoodfacts-mongodbdump.gz

  mongo-express:
    image: mongo-express:latest
    container_name: mongo-express
    restart: always
    ports:
      - "8081:8081"
    environment:
      ME_CONFIG_MONGODB_ADMINUSERNAME: ""
      ME_CONFIG_MONGODB_ADMINPASSWORD: ""
      ME_CONFIG_MONGODB_SERVER: mongo
```

---

## ▶️ 2. Lancer l’environnement Docker

Dans le dossier contenant votre `docker-compose.yml`, exécutez :

```bash
docker-compose up -d
```

Cela démarre :

* un conteneur **MongoDB**
* un conteneur **Mongo Express** accessible via `http://localhost:8081`

---

## 💾 3. Restaurer le dump MongoDB

```bash
docker exec -it mongo bash
mongorestore --gzip --archive=/openfoodfacts-mongodbdump.gz
exit
```

👉 Cette commande restaure le dump dans MongoDB **uniquement lorsqu’il est vide** (premier démarrage ou perte du volume).

---

# Initialisation de la Database pour le stockage du warehouse

## 🗄️ 1. Installation de la base SQL `openfoodfacts-etl`

Les requêtes suivantes permettent de créer la base et les tables nécessaires pour l’ETL.

> Disponible dans le dossier /docs/database/init.sql

### 📌 Création de la base

```sql
CREATE DATABASE IF NOT EXISTS `openfoodfacts-etl`;
USE `openfoodfacts-etl`;
```

---

## 👥 TABLE `user`

```sql
CREATE TABLE IF NOT EXISTS `user` (
  `user_id` int NOT NULL,
  `first_name` text,
  `last_name` text,
  `age` int DEFAULT NULL,
  `gender` text,
  `weight` double DEFAULT NULL,
  `country` text,
  `regime_id` int DEFAULT NULL,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```

---

## 🍽️ TABLE `daily_menu`

```sql
CREATE TABLE IF NOT EXISTS `daily_menu` (
  `user_id` int NOT NULL,
  `day` int NOT NULL,
  `breakfast_product_name` text NOT NULL,
  `lunch_product_name` text NOT NULL,
  `dinner_product_name` text NOT NULL,
  PRIMARY KEY (`user_id`,`day`),
  CONSTRAINT `FK_USER` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```

---

## 🏷️ TABLE `products_cleaned`

```sql
CREATE TABLE `regime` (
  `regime_id` INT PRIMARY KEY,
  `name` VARCHAR(100),
  `description` TEXT,
  `max_proteins_g_day` INT,
  `max_fat_g_day` INT,
  `max_carbohydrates_g_day` INT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```