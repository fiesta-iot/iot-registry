DROP DATABASE IF EXISTS fiesta_iot;

CREATE DATABASE fiesta_iot;

USE fiesta_iot;

SET time_zone = "+00:00";
#SET time_zone = "Europe/Madrid";

CREATE TABLE resource_id_mapper (
  hash VARCHAR(44),
  url VARCHAR(2000) NOT NULL,
  CONSTRAINT PK_resource PRIMARY KEY (hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE endpoint_id_mapper (
  hash VARCHAR(44),
  url VARCHAR(2000) NOT NULL,
  CONSTRAINT PK_endpoint PRIMARY KEY (hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE observation_id_mapper (
  hash VARCHAR(44),
  url VARCHAR(2000) NOT NULL,
  CONSTRAINT PK_observation PRIMARY KEY (hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE sparql_query (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `value` TEXT NOT NULL,
  `name` VARCHAR(256) NOT NULL,
  `description` text,
  `scope` ENUM ('GLOBAL', 'RESOURCES', 'OBSERVATIONS') NOT NULL,
  `created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `modified` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT PK_sparql_query PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE sparql_query_log (
  `hash` CHAR(44) NOT NULL,
  `query` TEXT NOT NULL,
  CONSTRAINT PK_sparql_query_log PRIMARY KEY (hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE sparql_query_log ROW_FORMAT=compressed; 
-- SHOW VARIABLES LIKE 'innodb_file_format'; -- Barracuda
-- SHOW VARIABLES LIKE 'innodb_file_per_table'; -- ON

CREATE TABLE sparql_query_execution_log (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `query_hash` CHAR(44) NOT NULL,
  `exec_time` INT NULL,
  `ip_address` BIGINT NOT NULL,
  `user` VARCHAR(256),
  `user_agent` VARCHAR(256),
  `component` VARCHAR(256),
  `aborted` BOOLEAN DEFAULT TRUE,
  `created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT PK_sparql_query_execution_log PRIMARY KEY (id), 
  CONSTRAINT FK_query_hash FOREIGN KEY (query_hash) REFERENCES sparql_query_log(hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE sparql_query_execution_log ROW_FORMAT=compressed;

-- Retrieve list of IP addresses in string format
-- SELECT INET_NTOA(ip_address) FROM sparql_query_execution_log;

CREATE TABLE semantic_storage_log (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `entity` ENUM ('RESOURCE', 'OBSERVATION', 'TESTBED') NOT NULL,
  `exec_time` INT NULL,
  `ip_address` BIGINT NOT NULL,
  `user` VARCHAR(256),
  `user_agent` VARCHAR(256),
  `component` VARCHAR(256),
  `aborted` BOOLEAN DEFAULT TRUE,
  `created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT semantic_storage_log PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE semantic_storage_log ROW_FORMAT=compressed;

CREATE USER IF NOT EXISTS 'USERNAME'@'localhost' IDENTIFIED BY 'PASSWORD';
GRANT ALL PRIVILEGES ON fiesta_iot . * TO 'USERNAME'@'localhost';
FLUSH PRIVILEGES;

