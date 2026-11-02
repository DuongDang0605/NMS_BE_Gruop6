-- Run against the application database. Challenge timestamps are UTC.
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF OBJECT_ID(N'dbo.SystemAccount', N'U') IS NOT NULL
BEGIN
    IF COL_LENGTH(N'dbo.SystemAccount', N'UnlockOtpHash') IS NULL
        ALTER TABLE dbo.SystemAccount ADD UnlockOtpHash varchar(100) NULL;
    IF COL_LENGTH(N'dbo.SystemAccount', N'UnlockOtpExpiresAt') IS NULL
        ALTER TABLE dbo.SystemAccount ADD UnlockOtpExpiresAt datetime2(6) NULL;
    IF COL_LENGTH(N'dbo.SystemAccount', N'UnlockOtpSentAt') IS NULL
        ALTER TABLE dbo.SystemAccount ADD UnlockOtpSentAt datetime2(6) NULL;
    IF COL_LENGTH(N'dbo.SystemAccount', N'UnlockOtpAttempts') IS NULL
        ALTER TABLE dbo.SystemAccount ADD UnlockOtpAttempts int NOT NULL
            CONSTRAINT DF_SystemAccount_UnlockOtpAttempts DEFAULT (0) WITH VALUES;
    IF COL_LENGTH(N'dbo.SystemAccount', N'UnlockResetTokenHash') IS NULL
        ALTER TABLE dbo.SystemAccount ADD UnlockResetTokenHash varchar(64) NULL;
    IF COL_LENGTH(N'dbo.SystemAccount', N'UnlockResetExpiresAt') IS NULL
        ALTER TABLE dbo.SystemAccount ADD UnlockResetExpiresAt datetime2(6) NULL;
END;
COMMIT TRANSACTION;
