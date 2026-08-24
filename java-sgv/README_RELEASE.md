Release bundle for java-sgv 0.1.0

Contents:
- java-sgv-0.1.0.jar (repacked Spring Boot executable)
- db/migration/*.sql (Flyway migrations: V1..V16)
- AUDIT.md and AUDIT_DETAILED.md
- application-mysql.properties (sample/runtime configuration)

Quick install (Windows, PowerShell):

```powershell
# ensure Java 21 is available
$env:JAVA_HOME='C:\Users\DELL\.jdk\jdk-21.0.11+10'
$env:Path="$env:JAVA_HOME\bin;$env:Path"

# optional: backup DB
mysqldump -u root -p sgv > sgv_before_release.sql

# Deploy files to server and run
java -jar java-sgv-0.1.0.jar --spring.profiles.active=mysql
```

Notes:
- Flyway will run migrations automatically during startup. Ensure the DB user has ALTER privileges to apply the migrations automatically.
- If you cannot allow Flyway to modify schema at startup, apply migrations manually from `db/migration` directory.
