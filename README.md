# meeting-room

API de exemplo para a Aula 1 do projeto de reservas de salas com Quarkus.

## Escopo da Aula 1

- entidade `Room` com `id`, `name` e `capacity`
- endpoint `POST /rooms`
- endpoint `GET /rooms`
- filtro opcional `GET /rooms?minCapacity=10`
- script `rooms-demo.sh` com `curl` para demonstração em sala

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

Depois de subir a aplicação, a API ficará disponível em `http://localhost:8080`.

## Endpoints disponíveis

### Criar sala

`POST /rooms`

Exemplo de payload:

```json
{
  "name": "Sala Java",
  "capacity": 12
}
```

### Listar salas

`GET /rooms`

### Filtrar por capacidade mínima

`GET /rooms?minCapacity=15`

## Script de demonstração

Com a aplicação já em execução, rode:

```bash
chmod +x rooms-demo.sh
./rooms-demo.sh
```

Se quiser apontar para outra URL base:

```bash
BASE_URL=http://localhost:8080 ./rooms-demo.sh
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/meeting-room-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Related Guides

- Hibernate ORM ([guide](https://quarkus.io/guides/hibernate-orm)): Define your persistent model with Hibernate ORM and Jakarta Persistence
- Hibernate Validator ([guide](https://quarkus.io/guides/validation)): Validate object properties (field, getter) and method parameters for your beans (REST, CDI, Jakarta Persistence)
- Hibernate ORM with Panache ([guide](https://quarkus.io/guides/hibernate-orm-panache)): Simplify your persistence code for Hibernate ORM via the active record or the repository pattern

## Provided Code

### Hibernate ORM

Create your first JPA entity

[Related guide section...](https://quarkus.io/guides/hibernate-orm)


[Related Hibernate with Panache section...](https://quarkus.io/guides/hibernate-orm-panache)

