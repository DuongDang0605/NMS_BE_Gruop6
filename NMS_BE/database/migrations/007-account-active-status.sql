-- Preserve every existing account as active. Replaying this migration must not reactivate accounts.
SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF OBJECT_ID(N'dbo.SystemAccount', N'U') IS NOT NULL
BEGIN
    IF COL_LENGTH(N'dbo.SystemAccount', N'IsActive') IS NULL
        ALTER TABLE dbo.SystemAccount ADD IsActive bit NOT NULL
            CONSTRAINT DF_SystemAccount_IsActive DEFAULT (1) WITH VALUES;
END;

COMMIT TRANSACTION;
