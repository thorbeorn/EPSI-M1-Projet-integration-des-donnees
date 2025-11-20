CREATE DATABASE IF NOT EXISTS `openfoodfacts-etl`;
USE `openfoodfacts-etl`;

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

CREATE TABLE IF NOT EXISTS `daily_menu` (
  `user_id` int NOT NULL,
  `day` int NOT NULL,
  `breakfast_product_name` text NOT NULL,
  `lunch_product_name` text NOT NULL,
  `dinner_product_name` text NOT NULL,
  PRIMARY KEY (`user_id`,`day`),
  CONSTRAINT `FK_USER` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `regime` (
  `regime_id` INT PRIMARY KEY,
  `name_en` VARCHAR(100),
  `name_fr` VARCHAR(100),
  `description_en` TEXT,
  `description_fr` TEXT,
  `max_added_sugars_100g` INT,
  `max_sugars_100g` INT,
  `max_sucrose_100g` INT,
  `max_glucose_100g` INT,
  `max_fructose_100g` INT,
  PRIMARY KEY (`regime_id`),
  CONSTRAINT `FK_regime` FOREIGN KEY (`regime_id`) REFERENCES `user` (`regime_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;