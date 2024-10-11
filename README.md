The first step is to add application.properties file to resources folder like this:
    
    spring.application.name=<application name>
    spring.datasource.url=jdbc:postgresql://localhost:5432/<database name>
    spring.datasource.username=<username>
    spring.datasource.password=<password>
    
    spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
    
    spring.sql.init.mode=always
    
    # CONST VALUES
    jwt.accessTokenExpiration = 86400000
    jwt.refreshTokenExpiration = 604800000