CREATE DATABASE  IF NOT EXISTS `bank` /*!40100 DEFAULT CHARACTER SET utf8mb3 */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `bank`;
-- MySQL dump 10.13  Distrib 8.0.31, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: bank
-- ------------------------------------------------------
-- Server version	8.0.31

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `klant`
--

DROP TABLE IF EXISTS `klant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `klant` (
  `klantID` varchar(60) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `klantNaam` varchar(45) NOT NULL,
  `klantGeboorte` date NOT NULL,
  `klantPin` int NOT NULL,
  `klantEmail` varchar(45) NOT NULL,
  `klantLand` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`klantID`),
  UNIQUE KEY `klantID_UNIQUE` (`klantID`),
  UNIQUE KEY `klantPin_UNIQUE` (`klantPin`),
  UNIQUE KEY `klantEmail_UNIQUE` (`klantEmail`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `klant`
--

LOCK TABLES `klant` WRITE;
/*!40000 ALTER TABLE `klant` DISABLE KEYS */;
INSERT INTO `klant` VALUES ('2A35CA17','Long Vo','2004-03-20',5555,'longvo00@gmail.com','Netherlands'),('B12F5B1D','Kristien Verlinden','1973-10-27',4763,'kristienverlinden73@gmail.com','Netherlands'),('F9299DD4','Xander Bos','2004-08-25',6969,'xanderbos04@gmail.com','Netherlands');
/*!40000 ALTER TABLE `klant` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rekening`
--

DROP TABLE IF EXISTS `rekening`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rekening` (
  `rekeningID` int NOT NULL,
  `rekeningNaam` varchar(45) DEFAULT NULL,
  `rekeningTotaal` double DEFAULT NULL,
  `klant_klantID` varchar(60) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `dagGepind` int NOT NULL,
  PRIMARY KEY (`rekeningID`,`klant_klantID`),
  UNIQUE KEY `rekeningID_UNIQUE` (`rekeningID`),
  UNIQUE KEY `klant_klantID_UNIQUE` (`klant_klantID`),
  KEY `fk_rekening_klant_idx` (`klant_klantID`),
  CONSTRAINT `fk_rekening_klant` FOREIGN KEY (`klant_klantID`) REFERENCES `klant` (`klantID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rekening`
--

LOCK TABLES `rekening` WRITE;
/*!40000 ALTER TABLE `rekening` DISABLE KEYS */;
INSERT INTO `rekening` VALUES (1053488,'Spaarrekening',80,'F9299DD4',2095),(1234567,'Spaarrekening',32157,'2A35CA17',2755),(1435742,'Spaarrekening',45363,'B12F5B1D',340);
/*!40000 ALTER TABLE `rekening` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `transactie`
--

DROP TABLE IF EXISTS `transactie`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `transactie` (
  `transactieID` int NOT NULL AUTO_INCREMENT,
  `rekening_rekeningID` int NOT NULL,
  `transactieHoeveelheid` int NOT NULL,
  `transactieDate` timestamp NULL DEFAULT NULL,
  UNIQUE KEY `transactieID_UNIQUE` (`transactieID`),
  KEY `fk_transactie_rekening1_idx` (`rekening_rekeningID`),
  CONSTRAINT `fk_transactie_rekening1` FOREIGN KEY (`rekening_rekeningID`) REFERENCES `rekening` (`rekeningID`)
) ENGINE=InnoDB AUTO_INCREMENT=38 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `transactie`
--

LOCK TABLES `transactie` WRITE;
/*!40000 ALTER TABLE `transactie` DISABLE KEYS */;
INSERT INTO `transactie` VALUES (1,1234567,70,'2023-05-23 21:17:39'),(2,1053488,70,'2023-05-24 09:51:14'),(3,1053488,555,'2023-05-24 10:39:21'),(4,1053488,70,'2023-05-24 11:43:20'),(5,1053488,70,'2023-05-24 11:44:31'),(6,1234567,70,'2023-05-24 11:49:28'),(7,1234567,70,'2023-05-24 11:49:59'),(8,1234567,70,'2023-05-24 11:51:25'),(9,1234567,70,'2023-05-24 11:53:36'),(10,1053488,70,'2023-05-24 11:56:45'),(11,1053488,70,'2023-05-24 12:00:38'),(12,1053488,70,'2023-05-24 12:02:38'),(13,1053488,70,'2023-05-24 12:04:29'),(14,1053488,70,'2023-05-24 12:04:56'),(15,1053488,70,'2023-05-24 12:05:29'),(16,1053488,70,'2023-05-24 12:09:08'),(17,1053488,70,'2023-05-24 12:10:32'),(18,1053488,70,'2023-05-24 12:11:16'),(19,1053488,70,'2023-05-24 12:13:22'),(20,1053488,70,'2023-05-24 12:14:54'),(21,1053488,70,'2023-05-24 12:17:34'),(22,1053488,45,'2023-05-24 18:29:57'),(23,1053488,70,'2023-05-24 18:30:57'),(24,1053488,70,'2023-05-24 18:32:21'),(25,1053488,70,'2023-05-24 18:33:27'),(26,1053488,70,'2023-05-24 18:35:40'),(27,1234567,70,'2023-05-24 18:36:42'),(28,1053488,70,'2023-05-24 18:38:08'),(29,1053488,70,'2023-05-24 18:40:50'),(30,1234567,25,'2023-05-24 18:45:03'),(31,1234567,25,'2023-05-24 18:46:39'),(32,1435742,70,'2023-05-25 09:29:15'),(33,1435742,25,'2023-05-25 13:09:58'),(34,1435742,70,'2023-05-25 16:09:14'),(35,1435742,70,'2023-05-25 16:09:40'),(36,1435742,10,'2023-05-25 16:12:22'),(37,1435742,70,'2023-05-25 16:13:56');
/*!40000 ALTER TABLE `transactie` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2023-05-26 11:17:21
