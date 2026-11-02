-- Report 2: role 2 becomes Contributor; existing account IDs and passwords are preserved.
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF OBJECT_ID(N'dbo.SystemAccount', N'U') IS NOT NULL
BEGIN
    IF EXISTS (SELECT 1 FROM sys.check_constraints
               WHERE parent_object_id = OBJECT_ID(N'dbo.SystemAccount') AND name = N'CK_SystemAccount_Role')
        ALTER TABLE dbo.SystemAccount DROP CONSTRAINT CK_SystemAccount_Role;
    ALTER TABLE dbo.SystemAccount WITH CHECK ADD CONSTRAINT CK_SystemAccount_Role
        CHECK (AccountRole IN (1, 2, 3, 4, 5));

    IF COL_LENGTH(N'dbo.SystemAccount', N'IsVerified') IS NULL
    BEGIN
        ALTER TABLE dbo.SystemAccount ADD IsVerified bit NOT NULL
            CONSTRAINT DF_SystemAccount_IsVerified DEFAULT (0) WITH VALUES;
        EXEC(N'UPDATE dbo.SystemAccount SET IsVerified = 1 WHERE AccountRole IN (1, 2, 3, 4)');
    END;
END;
COMMIT TRANSACTION;
