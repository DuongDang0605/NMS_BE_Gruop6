-- Existing articles must receive zero counters when upgrading the Dev 3 module.
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF OBJECT_ID(N'dbo.NewsArticle', N'U') IS NOT NULL
BEGIN
    IF COL_LENGTH(N'dbo.NewsArticle', N'ViewCount') IS NULL
        ALTER TABLE dbo.NewsArticle ADD ViewCount int NOT NULL
            CONSTRAINT DF_NewsArticle_ViewCount DEFAULT (0) WITH VALUES;
    IF COL_LENGTH(N'dbo.NewsArticle', N'LikeCount') IS NULL
        ALTER TABLE dbo.NewsArticle ADD LikeCount int NOT NULL
            CONSTRAINT DF_NewsArticle_LikeCount DEFAULT (0) WITH VALUES;
END;
COMMIT TRANSACTION;
