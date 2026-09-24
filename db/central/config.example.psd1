@{
    # Local named instance. Shared DB: use 'tcp:VPN_HOST,PORT' supplied by T1.
    SqlServer = 'localhost\PTITONE'
    # Separate instances can use the same DB name. Same instance: add _T2/_T5.
    DatabaseName = 'PTITONE_CENTRAL'
    # Development instance with a self-signed certificate only.
    # Set to $false when the server has a certificate trusted by the client.
    TrustServerCertificate = $true
}
