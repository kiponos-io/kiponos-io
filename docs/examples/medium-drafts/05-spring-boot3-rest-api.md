# Your Spring Boot Timeout Was Correct — and Still Required a Restart

*A story about `@Value`, frozen judgment, airport coffee, and the night a REST service finally learned to listen while it was still running.*

---

There is a special kind of insult that only a healthy microservice can deliver.

The pods are green. The latency graph is almost polite. The on-call phone is quiet enough that someone has started a sentence that is not about queue depth. And then product asks for a smaller timeout — not next sprint, not after the release train, **now** — because a downstream is limping and your clients are stacking retries like bad luggage.

Someone opens the code.

Someone finds the line.

```text
@Value("${http.client.read-timeout-ms:4000}")
private int readTimeoutMs;
```

And the room does the thing rooms do when they already know the answer:

**“We’ll change it after we redeploy.”**

Redeploy.

To change a number.

In a service that is otherwise fine.

---

## Configuration hell wears a Spring badge

I have watched this scene in more cities than I should admit: a glass tower where the elevators judged your badge; a co-working loft that smelled like burnt espresso and ambition; once in a terminal food court where the only free outlet sat under a departure board that kept lying about delays.

Different time zones. Same freeze.

We tell ourselves Spring Boot solved configuration.

We have `application.yml`. We have profiles. We have `@ConfigurationProperties`. We have the solemn ritual of `@RefreshScope` and the hope that every dependent bean noticed. We have Actuator buttons that feel like progress until you remember they still mean **context churn under load**.

What we actually built, too often, is a beautiful way to **bake operational judgment into process startup**.

That is not engineering maturity. That is **freezing the conversation with production** at the moment the JVM said hello.

---

## What `@Value` is really promising

`@Value` is honest, in its way.

It says: *I will resolve this once, when the bean is born, and I will hold that truth like a family photo.*

That is perfect for:

- which datasource name you wire  
- which feature skeleton you compile against  
- which bootstrap secrets reference you should never put in a dashboard  

It is a terrible contract for:

- timeouts  
- greetings and copy that legal just rewrote  
- rate limits during a flash incident  
- any number a human might need to change **while the service is already the service**

Old world theater:

1. Edit YAML  
2. Open a PR  
3. Wait for CI  
4. Bake an image  
5. Roll pods  
6. Hope the right profile landed  
7. Discover half the fleet still held the old number in a long-lived bean  

By the time you are done, the incident has a name and a retrospective owner.

New world — the one this example is for:

1. Open the Kiponos hub  
2. Change `request-timeout-ms`  
3. The next request sees it  

No parade. No `@RefreshScope` prayer circle. No “we’ll take a small blip.”

<!-- medium-img: diagram-value-frozen-vs-live.png -->

---

## The example (Spring Boot 3 REST, bean + disconnect)

We published a small Spring Boot 3 service that refuses to pretend ops knobs belong only in YAML:

**`examples/java/05-spring-boot3-rest-api`** on [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

It does the unglamorous things correctly:

- `Kiponos` as a Spring `@Bean`  
- clean `disconnect()` on `@PreDestroy`  
- live reads on the request path from a hub-backed cache  

Tree under your connected profile:

```text
examples / spring-boot-rest / request-timeout-ms
examples / spring-boot-rest / greeting
```

Endpoints:

- `GET /api/ops` — shows the live timeout and greeting  
- `GET /api/hello` — hot-path style read  

First run can create safe defaults. Ops flips the dashboard. You `curl` again. **No jar rebuild. No pod restart.**

That is the whole demonstration. Not a framework religion. A process that can still hear judgment after it has started.

<!-- medium-img: diagram-bean-lifecycle.png -->

If you want the longer map of app shapes and pains — standalone kill switches, multi-env profiles, Kafka workers, libraries — it lives in `examples/CATALOG.md`. This article is the Spring Boot door: the one microservice owners walk through when they are tired of shipping releases for orthography.

---

## A traveler’s note on “we’ll just restart it”

Airports taught me that gates renumber without apology. Markets taught me that a correct price can still be wrong for the hour. On-call taught me that “we’ll just restart it” is often a confession: **we never gave the running system a way to receive a decision.**

Restarts are sometimes necessary.

They should not be the only language you have for a timeout.

The tools that matter shorten the distance between **human judgment** and **running behavior** — especially inside frameworks that make startup configuration feel complete.

Spring is excellent at bootstrap.  
Ops is not bootstrap.

---

## How to try it tonight

1. Free TeamPro at [kiponos.io](https://kiponos.io)  
2. Clone the public repo  
3. `cd examples/java/05-spring-boot3-rest-api`  
4. Export `KIPONOS_ID` / `KIPONOS_ACCESS` from Connect  
5. `./gradlew bootRun`  
6. `curl -s localhost:8080/api/ops`  
7. Change `greeting` or `request-timeout-ms` in the dashboard  
8. `curl` again — **without stopping the process**  

Watch the JSON change.

That little payload is not a demo trick. It is a rehearsal for the afternoon when product is right, the timeout is wrong, and nobody should have to schedule a deploy for a number.

---

## The moral, if you need one on a slide

**People should not have to ship a release to make a decision.**

And they should not have to recycle a Spring context to change a timeout.

Configuration that only moves at pod lifecycle speed will eventually move at incident speed.

We built Kiponos so the hub and the process share a nervous system — including after `SpringApplication.run` has already returned.

Example 05 is that handshake: a REST service that stays online while judgment arrives.

---

*Code: [kiponos-io/kiponos-io — example 05](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/05-spring-boot3-rest-api)*  
*Product: [kiponos.io](https://kiponos.io)*
