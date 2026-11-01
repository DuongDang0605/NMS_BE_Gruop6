-- Run against the application database before deploying the login-lock feature.
-- Existing accounts start unlocked with zero failed attempts. Safe to run again.
SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF OBJECT_ID(N'dbo.SystemAccount', N'U') IS NOT NULL
BEGIN
    IF COL_LENGTH(N'dbo.SystemAccount', N'FailedLoginAttempts') IS NULL
        ALTER TABLE dbo.SystemAccount ADD FailedLoginAttempts int NOT NULL
            CONSTRAINT DF_SystemAccount_FailedLoginAttempts DEFAULT (0) WITH VALUES;

    IF COL_LENGTH(N'dbo.SystemAccount', N'AccountLocked') IS NULL
        ALTER TABLE dbo.SystemAccount ADD AccountLocked bit NOT NULL
            CONSTRAINT DF_SystemAccount_AccountLocked DEFAULT (0) WITH VALUES;
END;

COMMIT TRANSACTION;
