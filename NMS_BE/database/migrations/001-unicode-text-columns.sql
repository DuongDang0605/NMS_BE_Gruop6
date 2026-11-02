-- Connect to the application database before running this script.
-- Convert legacy varchar display text to nvarchar without dropping tables or rows.
-- Fresh databases already use nvarchar through the entity mappings.
SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.SystemAccount') AND name = N'AccountName' AND system_type_id = TYPE_ID(N'varchar'))
    ALTER TABLE dbo.SystemAccount ALTER COLUMN AccountName nvarchar(100) NOT NULL;

IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.Category') AND name = N'CategoryName' AND system_type_id = TYPE_ID(N'varchar'))
    ALTER TABLE dbo.Category ALTER COLUMN CategoryName nvarchar(100) NOT NULL;

IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.Category') AND name = N'CategoryDescription' AND system_type_id = TYPE_ID(N'varchar'))
    ALTER TABLE dbo.Category ALTER COLUMN CategoryDescription nvarchar(1000) NULL;

IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.NewsArticle') AND name = N'NewsTitle' AND system_type_id = TYPE_ID(N'varchar'))
    ALTER TABLE dbo.NewsArticle ALTER COLUMN NewsTitle nvarchar(250) NOT NULL;

IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.NewsArticle') AND name = N'Headline' AND system_type_id = TYPE_ID(N'varchar'))
    ALTER TABLE dbo.NewsArticle ALTER COLUMN Headline nvarchar(500) NULL;

IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.NewsArticle') AND name = N'NewsSource' AND system_type_id = TYPE_ID(N'varchar'))
    ALTER TABLE dbo.NewsArticle ALTER COLUMN NewsSource nvarchar(250) NULL;

IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.Tag') AND name = N'TagName' AND system_type_id = TYPE_ID(N'varchar'))
    ALTER TABLE dbo.Tag ALTER COLUMN TagName nvarchar(100) NOT NULL;

IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.Tag') AND name = N'Note' AND system_type_id = TYPE_ID(N'varchar'))
    ALTER TABLE dbo.Tag ALTER COLUMN Note nvarchar(500) NULL;

COMMIT TRANSACTION;
