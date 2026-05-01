[![Project Map](https://sourcespy.com/shield.svg)](https://sourcespy.com/github/funkemunkyantivpn/)

# AntiVPN
An antivpn plugin utilizing the KauriVPN API

## The Fastest Available
Just a simple plugin using an incredibly fast and accurate API.

## SpigotMC Page
Rate and support the project on SpigotMC: https://www.spigotmc.org/resources/kaurivpn-anti-proxy-tor-and-vpn-free-api.93355/

## Velocity Debugging
Run a generated local Velocity proxy with the AntiVPN Velocity loader installed:

```bash
./gradlew runVelocity
```

Run the proxy suspended for IDE debugger attach on port `5005`:

```bash
./gradlew debugVelocity
```

In IntelliJ IDEA, use the Gradle `debugVelocity` task and attach a remote JVM debugger to `localhost:5005`.
