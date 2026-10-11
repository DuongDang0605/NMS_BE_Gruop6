-- Run on SQL Server before starting Spring Boot.
-- This script preserves an existing database; Hibernate creates the five tables.
USE [master];
GO
IF DB_ID(N'FUNewsManagementSystem') IS NULL
BEGIN
    CREATE DATABASE [FUNewsManagementSystem];
END;
GO
