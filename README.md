# Kiponos.io
## True Real-Time Config Management.


#### Control your application variables in real-time with zero latency from your browser!

All you need is our intuitive and simple Java SDK.

- You define any variables or configurations you work with in your Kiponos.io web account.
- Any change you make online in your Kiponos.io account, is instantly dispatched directly to your SDK memory.

## How this is even possible?

Kiponos uses WebSockets to instantly dispatch changes to all subscribed SDKs and online users.

WebSockets connection is **permanent** - it's **always open!** (if dropped for any reason - our SDK auto-reconnect).

This makes dispatching changes to everyone in **light speed** - litterally!

- Want to increase that "max-queue-size" config property, but don't want to restart your server? **No Problem!**
  - Change the value online, in your account. **That's it!** The latest value is instantly in your application memory. in runtime, in real-time.
  - Now anytime your application use the SDK to get the value like: ` int maxQueueSize = kiponos.getInt("max-queue-size"); ` it's **always**, **already** the latest value!
  - So whenever your code uses the SDK to access the config values, it always have the latest data. Awesome right? :)
 
- Can my business logic react instantly when I change anything online in my Kiponos account?
  - **Sure!** Since the SDK react instantly, we provide custom hooks (listeners/events) you can use to program your logic on any change.
  - For example: ` kiponos.afterValueChanged( configValueChanged -> { if (configValueChanged.getKey().equals("max-queue-size")) { ... // resize your queue ... } } `
  - **That's It!** Anytime you change the `max-queue-size` config item in your kiponos.io account, your business logic "Queue" is resized, instantly - in real-time!

- No Redeploy on config chanhge, No Restarts, Not even a refresh! Nothing! every change is instaly and automatically applied to your runtime!

---

## Getting Started

- Sign Up at Kiponos.io
- Include the SDK in your build - Get it here: [Maven Central Repo](https://mvnrepository.com/artifact/io.kiponos/sdk-boot-3)
- Login to your Kiponos.io account and Follow the Wizard to create your first application info, environment and config.
- Create your config items. You can create config folders to conveniently group your items.

## Using the SDK - Code Samples

Suppose your application works with a database - like PostgresSql.  

You define the DB params online in your Kiponos.io account:

![image](https://github.com/user-attachments/assets/57eeca27-299b-44d0-9ff7-4fbf9206b338)

And in your code, access the values:

```java
Kiponos kiponos = Kiponos.createForCurrentTeam();

KiponosFolder postgresFolder = kiponos.path("DB", "PostgreSql");
String dbHost = postgresFolder.get("host");
int dbPort = postgresFolder.getInt("port");
```

The values are guaranteed to be the latest - always - and instantly as you change them!

Without performance costs! 
because no refresh is needed, no reloads, and obviously no restarts or redeploys!

Groundbraking? That's an **Earthquake**!

That's Kiponos revolution in Config Management, in Observability, QA, CI/CD, Maintainance - for all of us! In Any Environment!

[Join Us!](https://kiponos.io)

<!---
kiponos-io/kiponos-io is a ✨ special ✨ repository because its `README.md` (this file) appears on your GitHub profile.
You can click the Preview link to take a look at your changes.
--->
