CREATE DATABASE IF NOT EXISTS `openfoodfacts-etl`;
USE `openfoodfacts-etl`;

CREATE TABLE `regime` (
  `regime_id` INT PRIMARY KEY,
  `name` VARCHAR(100),
  `description` TEXT,
  `max_proteins_g_day` INT,
  `max_fat_g_day` INT,
  `max_carbohydrates_g_day` INT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user` (
  `user_id` INT PRIMARY KEY,
  `first_name` TEXT,
  `last_name` TEXT,
  `age` INT,
  `gender` TEXT,
  `weight` DOUBLE,
  `country` TEXT,
  `regime_id` INT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `daily_menu` (
  `user_id` INT NOT NULL,
  `day` INT NOT NULL,
  `breakfast_product_name` TEXT NOT NULL,
  `lunch_product_name` TEXT NOT NULL,
  `dinner_product_name` TEXT NOT NULL,
  PRIMARY KEY (`user_id`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
