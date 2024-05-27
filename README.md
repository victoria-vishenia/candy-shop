**Micro service application for online shop**

Application contains separate services for Product, Order, Inventory. Serviced communicate using Kafka broker. Data is stored in PostgreSQL. All services are registered in Eureka Server and directed through Gateway server. Configuration is setted with Configuration server. Security is setted using Keycloak. The whole project is containarized in Docker.

**Tools**

The application is developed using Spring Boot(Web, Security, Data, Test), Spring Cloud (Eureka, Config, Gateway, Circuitbreaker), TestContainers, Keycloak, PostgreSQL, Kafka,  Docker.
