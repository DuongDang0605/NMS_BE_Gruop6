-- Run before Hibernate updates an existing Report 1 database.
-- Preserve articles: 1 (active) -> PUBLISHED, 0 (inactive) -> ARCHIVED.
-- varchar(30) matches the NewsArticle.NewsStatus entity mapping.
SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF OBJECT_ID(N'dbo.NewsArticle', N'U') IS NOT NULL
BEGIN
    -- Reject unexpected values before changing constraints or narrowing the column.
    IF EXISTS (
        SELECT 1 FROM dbo.NewsArticle
        WHERE NewsStatus IS NULL
           OR UPPER(LTRIM(RTRIM(CONVERT(nvarchar(max), NewsStatus)))) NOT IN
              (N'0', N'1', N'DRAFT', N'PENDING_REVIEW', N'CHANGES_REQUESTED',
               N'APPROVED', N'SCHEDULED', N'PUBLISHED', N'ARCHIVED')
    )
        THROW 50006, 'NewsArticle contains unsupported NewsStatus values; migration cancelled.', 1;

    IF EXISTS (SELECT 1 FROM sys.check_constraints
               WHERE parent_object_id = OBJECT_ID(N'dbo.NewsArticle')
                 AND name = N'CK_NewsArticle_Status')
        ALTER TABLE dbo.NewsArticle DROP CONSTRAINT CK_NewsArticle_Status;

    IF EXISTS (SELECT 1 FROM sys.columns
               WHERE object_id = OBJECT_ID(N'dbo.NewsArticle') AND name = N'NewsStatus'
                 AND (system_type_id <> TYPE_ID(N'varchar') OR max_length <> 30 OR is_nullable = 1))
    BEGIN
        -- A legacy default is also bound to the old column type.
        DECLARE @defaultName sysname;
        SELECT @defaultName = dc.name
        FROM sys.default_constraints dc
        JOIN sys.columns c ON c.object_id = dc.parent_object_id AND c.column_id = dc.parent_column_id
        WHERE c.object_id = OBJECT_ID(N'dbo.NewsArticle') AND c.name = N'NewsStatus';
        IF @defaultName IS NOT NULL
        BEGIN
            DECLARE @dropDefault nvarchar(max) =
                N'ALTER TABLE dbo.NewsArticle DROP CONSTRAINT ' + QUOTENAME(@defaultName);
            EXEC sys.sp_executesql @dropDefault;
        END;

        ALTER TABLE dbo.NewsArticle ALTER COLUMN NewsStatus varchar(30) NOT NULL;

        IF @defaultName IS NOT NULL
            ALTER TABLE dbo.NewsArticle ADD CONSTRAINT DF_NewsArticle_Status DEFAULT ('DRAFT') FOR NewsStatus;
    END;

    IF COL_LENGTH(N'dbo.NewsArticle', N'ScheduledPublishDate') IS NULL
        ALTER TABLE dbo.NewsArticle ADD ScheduledPublishDate datetime2 NULL;
    IF COL_LENGTH(N'dbo.NewsArticle', N'PublishedDate') IS NULL
        ALTER TABLE dbo.NewsArticle ADD PublishedDate datetime2 NULL;

    -- Compile after ALTER COLUMN / ADD COLUMN, including when upgrading an int column.
    EXEC sys.sp_executesql N'
        UPDATE dbo.NewsArticle
        SET NewsStatus = CASE LTRIM(RTRIM(NewsStatus))
            WHEN ''1'' THEN ''PUBLISHED''
            WHEN ''0'' THEN ''ARCHIVED''
            ELSE UPPER(LTRIM(RTRIM(NewsStatus))) END;
        UPDATE dbo.NewsArticle
        SET PublishedDate = COALESCE(CreatedDate, SYSUTCDATETIME())
        WHERE NewsStatus = ''PUBLISHED'' AND PublishedDate IS NULL;
        ALTER TABLE dbo.NewsArticle WITH CHECK ADD CONSTRAINT CK_NewsArticle_Status
            CHECK (NewsStatus IN (''DRAFT'', ''PENDING_REVIEW'', ''CHANGES_REQUESTED'',
                                 ''APPROVED'', ''SCHEDULED'', ''PUBLISHED'', ''ARCHIVED''));';
END;

COMMIT TRANSACTION;
