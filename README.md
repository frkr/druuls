# Druuls

# Executando
```bash
mvn spring-boot:run
```

[http://localhost:9090/druuls/](http://localhost:9090/druuls/)

```bash
curl -X POST "http://localhost:9090/druuls/api/execute" -H "accept: application/json" -H "Content-Type: application/json" -d "{ \"id\": 1, \"values\": { \"Cenario\": \"501\", \"Nome\": \"Davi\" }}"
```

# Tecnologias

1. Spring Boot
2. PrimeFaces JoinFaces
3. Swagger2 UI
4. Hibernate Envers
5. H2 Console
6. Drools
