package br.com.ada.classes.meetingroom.simulation;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * ============================================================
 * Meeting Room API — Teste de Carga (Gatling Java DSL)
 * ============================================================
 *
 * Execução:  ./mvnw gatling:test
 *            ./mvnw gatling:test -DbaseUrl=http://meu-host:8080
 *
 * Usuários:
 *   - admin  (ADMIN) : cria/atualiza/exclui salas e reservas
 *   - joao   (USER)  : cria/atualiza/exclui apenas reservas
 *   - maria  (USER)  : cria/atualiza/exclui apenas reservas
 *
 * Carga:
 *   - 3 VUs simultâneos (1 por perfil de usuário)
 *   - Ramp-up: 3 req/s total (1/VU) → 15 req/s total (5/VU)
 *   - Duração total: ~4 minutos
 *
 * Cenários cobertos:
 *   - Salas     : listar, filtrar, buscar por ID, criar, atualizar, excluir
 *   - Reservas  : listar, filtrar (data/sala), buscar por ID, criar, atualizar, excluir
 *   - Erros     : payload inválido (400), não encontrado (404), proibido (403), sem auth (401)
 * ============================================================
 */
public class LoadSimulation extends Simulation {

    // ---------------------------------------------------------------
    // Configuração
    // ---------------------------------------------------------------

    private static final String BASE_URL =
            System.getProperty("baseUrl", "http://localhost:8080");

    HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .contentTypeHeader("application/json")
            .acceptHeader("application/json");

    // ---------------------------------------------------------------
    // Utilitários
    // ---------------------------------------------------------------

    /** Retorna o próximo dia útil a partir de hoje + offsetDays */
    private static String nextBusinessDay(int offsetDays) {
        LocalDate date = LocalDate.now().plusDays(offsetDays);
        while (date.getDayOfWeek().getValue() >= 6) {
            date = date.plusDays(1);
        }
        return date.toString();
    }

    private static int randInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    // ---------------------------------------------------------------
    // Autenticação
    // ---------------------------------------------------------------

    ChainBuilder login = exec(
            http("POST /auth/login")
                    .post("/auth/login")
                    .body(StringBody(session ->
                            String.format("{\"username\":\"%s\",\"password\":\"%s\"}",
                                    session.getString("username"),
                                    session.getString("password"))
                    ))
                    .check(status().is(200))
                    .check(jsonPath("$.token").saveAs("token"))
    );

    // ---------------------------------------------------------------
    // Cenário: Leitura de Salas
    // ---------------------------------------------------------------

    ChainBuilder roomsRead = exec(
            http("GET /rooms")
                    .get("/rooms?page=0&size=10")
                    .header("Authorization", session -> "Bearer " + session.getString("token"))
                    .check(status().is(200))
    )
    .exec(session -> session.set("minCap", randInt(2, 15)))
    .exec(
            http("GET /rooms?minCapacity")
                    .get(session -> "/rooms?minCapacity=" + session.getInt("minCap"))
                    .header("Authorization", session -> "Bearer " + session.getString("token"))
                    .check(status().is(200))
    )
    .exec(session -> session.set("roomReadId", randInt(1, 5)))
    .exec(
            http("GET /rooms/{id}")
                    .get(session -> "/rooms/" + session.getInt("roomReadId"))
                    .header("Authorization", session -> "Bearer " + session.getString("token"))
                    .check(status().in(200, 404))
    );

    // ---------------------------------------------------------------
    // Cenário: Escrita de Salas (ADMIN cria/atualiza/exclui; USER gera 403)
    // ---------------------------------------------------------------

    ChainBuilder roomsWrite = exec(session ->
            session
                    .set("newRoomName", "Gatling-Sala-" + System.currentTimeMillis() + "-" + randInt(100, 999))
                    .set("newRoomCapacity", randInt(4, 30))
    )
    .exec(
            http("POST /rooms")
                    .post("/rooms")
                    .header("Authorization", session -> "Bearer " + session.getString("token"))
                    .body(StringBody(session ->
                            String.format("{\"name\":\"%s\",\"capacity\":%d}",
                                    session.getString("newRoomName"),
                                    session.getInt("newRoomCapacity"))
                    ))
                    .check(status().in(201, 403))
                    // jsonPath só é aplicado quando há body JSON (status 201); 403 pode ter body vazio
                    .checkIf((response, session) -> response.status().code() == 201)
                        .then(jsonPath("$.id").saveAs("createdRoomId"))
    )
    .doIf(session -> session.contains("createdRoomId")).then(
            exec(
                    http("PUT /rooms/{id}")
                            .put(session -> "/rooms/" + session.getString("createdRoomId"))
                            .header("Authorization", session -> "Bearer " + session.getString("token"))
                            .body(StringBody(session ->
                                    String.format("{\"name\":\"%s-upd\",\"capacity\":20}",
                                            session.getString("newRoomName"))
                            ))
                            .check(status().is(200))
            )
            .exec(
                    http("DELETE /rooms/{id}")
                            .delete(session -> "/rooms/" + session.getString("createdRoomId"))
                            .header("Authorization", session -> "Bearer " + session.getString("token"))
                            .check(status().is(204))
            )
    );

    // ---------------------------------------------------------------
    // Cenário: Leitura de Reservas
    // ---------------------------------------------------------------

    ChainBuilder reservationsRead = exec(
            http("GET /reservations")
                    .get("/reservations?page=0&size=10")
                    .header("Authorization", session -> "Bearer " + session.getString("token"))
                    .check(status().is(200))
    )
    .exec(session ->
            session
                    .set("filterDate", nextBusinessDay(randInt(1, 30)))
                    .set("filterRoomId", randInt(1, 5))
    )
    .exec(
            http("GET /reservations?date")
                    .get(session -> "/reservations?date=" + session.getString("filterDate"))
                    .header("Authorization", session -> "Bearer " + session.getString("token"))
                    .check(status().is(200))
    )
    .exec(
            http("GET /reservations?roomId")
                    .get(session -> "/reservations?roomId=" + session.getInt("filterRoomId"))
                    .header("Authorization", session -> "Bearer " + session.getString("token"))
                    .check(status().is(200))
    )
    .exec(
            http("GET /reservations?roomId&date")
                    .get(session ->
                            "/reservations?roomId=" + session.getInt("filterRoomId") +
                            "&date=" + session.getString("filterDate"))
                    .header("Authorization", session -> "Bearer " + session.getString("token"))
                    .check(status().is(200))
    )
    .exec(session -> session.set("resReadId", randInt(1, 10)))
    .exec(
            http("GET /reservations/{id}")
                    .get(session -> "/reservations/" + session.getInt("resReadId"))
                    .header("Authorization", session -> "Bearer " + session.getString("token"))
                    .check(status().in(200, 404))
    );

    // ---------------------------------------------------------------
    // Cenário: Escrita de Reservas
    // ---------------------------------------------------------------

    ChainBuilder reservationsWrite = exec(
            http("GET /rooms (busca id para reserva)")
                    .get("/rooms?page=0&size=20")
                    .header("Authorization", session -> "Bearer " + session.getString("token"))
                    .check(status().is(200))
                    .check(jsonPath("$.content[0].id").optional().saveAs("targetRoomId"))
    )
    .doIf(session -> session.contains("targetRoomId")).then(
            exec(session -> {
                int startHour = randInt(8, 15);
                int endHour   = startHour + randInt(1, 3);
                return session
                        .set("resDay",       nextBusinessDay(randInt(2, 90)))
                        .set("resStartHour", startHour)
                        .set("resEndHour",   endHour)
                        .set("resGuest",     "Gatling-" + System.currentTimeMillis());
            })
            .exec(
                    http("POST /reservations")
                            .post("/reservations")
                            .header("Authorization", session -> "Bearer " + session.getString("token"))
                            .body(StringBody(session ->
                                    String.format(
                                            "{\"roomId\":%s,\"guestName\":\"%s\"," +
                                            "\"startAt\":\"%sT%02d:00:00\"," +
                                            "\"endAt\":\"%sT%02d:00:00\"}",
                                            session.getString("targetRoomId"),
                                            session.getString("resGuest"),
                                            session.getString("resDay"), session.getInt("resStartHour"),
                                            session.getString("resDay"), session.getInt("resEndHour")
                                    )
                            ))
                            .check(status().in(201, 400, 409))
                            // jsonPath só é aplicado quando há body com id (status 201)
                            .checkIf((response, session) -> response.status().code() == 201)
                                .then(jsonPath("$.id").saveAs("createdResId"))
            )
            .doIf(session -> session.contains("createdResId")).then(
                    exec(
                            http("PUT /reservations/{id}")
                                    .put(session -> "/reservations/" + session.getString("createdResId"))
                                    .header("Authorization", session -> "Bearer " + session.getString("token"))
                                    .body(StringBody(session ->
                                            String.format(
                                                    "{\"guestName\":\"%s-upd\"," +
                                                    "\"startAt\":\"%sT%02d:00:00\"," +
                                                    "\"endAt\":\"%sT%02d:00:00\"}",
                                                    session.getString("resGuest"),
                                                    session.getString("resDay"), session.getInt("resStartHour"),
                                                    session.getString("resDay"), session.getInt("resEndHour") + 1
                                            )
                                    ))
                                    .check(status().in(200, 400, 409))
                    )
                    .exec(
                            http("DELETE /reservations/{id}")
                                    .delete(session -> "/reservations/" + session.getString("createdResId"))
                                    .header("Authorization", session -> "Bearer " + session.getString("token"))
                                    .check(status().is(204))
                    )
            )
    );

    // ---------------------------------------------------------------
    // Cenário: Erros Esperados (4xx)
    // ---------------------------------------------------------------

    ChainBuilder errorScenarios = exec(
            // 400: guestName em branco + endAt < startAt
            http("POST /reservations (400 - payload inválido)")
                    .post("/reservations")
                    .header("Authorization", session -> "Bearer " + session.getString("token"))
                    .body(StringBody(
                            "{\"roomId\":1,\"guestName\":\"\"," +
                            "\"startAt\":\"2020-01-06T10:00:00\"," +
                            "\"endAt\":\"2020-01-06T09:00:00\"}"
                    ))
                    .check(status().in(400, 422))
    )
    .exec(
            // 404: sala inexistente
            http("GET /rooms/999999 (404)")
                    .get("/rooms/999999")
                    .header("Authorization", session -> "Bearer " + session.getString("token"))
                    .check(status().is(404))
    )
    .exec(
            // 404: reserva inexistente
            http("GET /reservations/999999 (404)")
                    .get("/reservations/999999")
                    .header("Authorization", session -> "Bearer " + session.getString("token"))
                    .check(status().is(404))
    )
    .exec(
            // 401: requisição sem token de autenticação
            http("GET /reservations/{id} (401 - sem token)")
                    .get("/reservations/1")
                    .check(status().in(200, 401)) // /reservations/{id} é @PermitAll
    );

    // ---------------------------------------------------------------
    // Corpo do cenário principal com distribuição ponderada
    //   35% leitura de salas
    //   15% escrita de salas        (ADMIN cria; USER gera 403)
    //   30% leitura de reservas
    //   15% escrita de reservas
    //    5% cenários de erro
    // ---------------------------------------------------------------

    ChainBuilder mainFlow = randomSwitch().on(
            percent(35.0).then(exec(roomsRead)),
            percent(15.0).then(exec(roomsWrite)),
            percent(30.0).then(exec(reservationsRead)),
            percent(15.0).then(exec(reservationsWrite)),
            percent(5.0).then(exec(errorScenarios))
    );

    // ---------------------------------------------------------------
    // Cenários por perfil de usuário
    // ---------------------------------------------------------------

    ScenarioBuilder adminScenario = scenario("admin (ADMIN)")
            .exec(session -> session.set("username", "admin").set("password", "admin123"))
            .exec(login)
            .during(Duration.ofMinutes(4)).on(
                    exec(mainFlow)
            );

    ScenarioBuilder joaoScenario = scenario("joao (USER)")
            .exec(session -> session.set("username", "joao").set("password", "user123"))
            .exec(login)
            .during(Duration.ofMinutes(4)).on(
                    exec(mainFlow)
            );

    ScenarioBuilder mariaScenario = scenario("maria (USER)")
            .exec(session -> session.set("username", "maria").set("password", "user123"))
            .exec(login)
            .during(Duration.ofMinutes(4)).on(
                    exec(mainFlow)
            );

    // ---------------------------------------------------------------
    // Setup
    //
    // 3 VUs simultâneos (1 por cenário) com throttle global:
    //   warm-up  :  3 req/s total (1 req/s × 3 VUs) por 20s
    //   hold     :  3 req/s por 20s
    //   ramp-up  :  3 → 9 req/s  (1 → 3 req/s × 3 VUs) em 1 min
    //   pico     :  9 → 15 req/s (3 → 5 req/s × 3 VUs) em 1 min
    //   sustain  : 15 req/s por 30s
    //   ramp-down: 15 → 3 req/s em 20s
    //   Total throttle: ~3min 30s | VUs: 4min | Total: ~4min
    // ---------------------------------------------------------------

    {
        setUp(
                adminScenario.injectClosed(constantConcurrentUsers(1).during(Duration.ofMinutes(4))),
                joaoScenario.injectClosed(constantConcurrentUsers(1).during(Duration.ofMinutes(4))),
                mariaScenario.injectClosed(constantConcurrentUsers(1).during(Duration.ofMinutes(4)))
        )
        .throttle(
                reachRps(3).in(Duration.ofSeconds(20)),   // warm-up:  1 req/s × 3 VUs
                holdFor(Duration.ofSeconds(20)),
                reachRps(9).in(Duration.ofMinutes(1)),    // ramp-up:  3 req/s × 3 VUs
                reachRps(15).in(Duration.ofMinutes(1)),   // pico:     5 req/s × 3 VUs
                holdFor(Duration.ofSeconds(30)),
                reachRps(3).in(Duration.ofSeconds(20))    // ramp-down
        )
        .protocols(httpProtocol)
        .assertions(
                global().responseTime().percentile(95).lt(2000),
                global().failedRequests().percent().lt(15.0)
        );
    }
}

