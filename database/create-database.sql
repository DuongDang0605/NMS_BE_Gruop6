-- Run on SQL Server before starting Spring Boot.
-- This script preserves an existing database; Hibernate creates the entity tables.
USE [master];
GO
IF DB_ID(N'FUNewsManagementSystem') IS NULL
BEGIN
    CREATE DATABASE [FUNewsManagementSystem];
END;
GO

-- Category was first created with varchar columns; ddl-auto=update does not change
-- existing column types, so convert them to nvarchar once to store Vietnamese text.
USE [FUNewsManagementSystem];
GO
IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
           WHERE TABLE_NAME = 'Category' AND COLUMN_NAME = 'CategoryName' AND DATA_TYPE = 'varchar')
BEGIN
    ALTER TABLE [Category] ALTER COLUMN [CategoryName] nvarchar(100) NOT NULL;
    ALTER TABLE [Category] ALTER COLUMN [CategoryDescription] nvarchar(1000) NULL;
END;
GO
