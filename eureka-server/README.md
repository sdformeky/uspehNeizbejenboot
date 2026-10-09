# eureka-server

Реестр сервисов (service discovery).

Сервисы регистрируются здесь при старте, а api-gateway и Feign-клиенты находят друг друга по имени, а не по адресу.

- Порт: `8761`
- Дашборд: http://localhost:8761 — список зарегистрированных сервисов
- Сам себя не регистрирует (`register-with-eureka: false`, `fetch-registry: false`)

## Как подключить сервис

1. Зависимости в `pom.xml` сервиса (версии не нужны — их задаёт родительский pom):

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

2. `src/main/resources/application.yml` сервиса:

```yaml
server:
  port: 8081  # порт своего сервиса из таблицы в README

spring:
  application:
    name: student-service  # под этим именем сервис будет виден в Eureka и в lb:// у gateway
  config:
    import: optional:configserver:http://localhost:8888
```

Адрес Eureka прописывать не нужно — он приходит из общего `config-repo/application.yml`.
Аннотация `@EnableDiscoveryClient` тоже не нужна, достаточно зависимости.

3. Запустить config-server, eureka-server, потом сервис — через ~30 секунд он появится на дашборде в статусе `UP`.
