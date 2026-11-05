# WitherSoundToggle

Per-player muting of the **global Wither spawning sound** on Paper servers.
Other Wither noises are intentionally unchanged.

## Install

1. Install a PacketEvents plugin build compatible with your **exact Minecraft/Paper version** (2.14.x is appropriate for the 26.2 line).
2. Put `WitherSoundToggle-1.0.0.jar` in your backend Paper server's `plugins` directory, **not** the Velocity proxy.
3. Restart that backend once.

No ProtocolLib dependency or client-side mod is required.

## Players

| Command | Effect |
| --- | --- |
| `/withersounds off` | Mute future global Wither spawn sounds |
| `/withersounds on` | Hear Wither spawn sounds again |
| `/withersounds` | Toggle on/off |
| `/withersounds status` | Show current choice |
| `/wsounds` | Short alias |

On by default; per-player settings persist across reconnects and restarts in `plugins/WitherSoundToggle/config.yml`.

## Technical notes

Uses PacketEvents to cancel the clientbound `EFFECT` packet with event ID `1023` only for opted-out player UUIDs. Does **not** mute damage/hurt/death sounds, change Wither behavior, or affect other players' audio. The sound is generated client-side in response to this level event, so cancelling regular named sound packets alone would not work. Ensure `globalSoundEvents` is true if you want your normal global Wither sound behavior restored after installing this plugin.

### Build

Run `./gradlew build` using JDK 21. Runtime dependency: PacketEvents, not bundled in the JAR.

## CI and releases

- Pull requests and commits to `main` run the Gradle build and upload the compiled JAR as a CI artifact.
- Push a version tag (for example, `v1.0.0`) to build and publish a GitHub Release with the compiled JAR and SHA-256 checksum.
- The tag's version is injected into both the JAR filename and `plugin.yml` during the release build.
- The release workflow accepts semantic versions with optional prerelease suffixes; malformed tags are rejected.

Example, after the version has been tested:

```bash
git tag v1.0.0
git push origin v1.0.0
```
