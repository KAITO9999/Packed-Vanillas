# Packed-Vanillas — initial static audit

## Target and provenance

- Upstream: `ItsBlackGear/VanillaBackport`, branch `1.20.1`
- Audited upstream commit: `2f62cbef963dfb0906706535474720fe07ed755d`
- Upstream mod version: `1.2.2`
- Target: Minecraft 1.20.1 / Forge 47.x
- Fork: `KAITO9999/Packed-Vanillas`
- Working branch: `packed/initial-audit`
- Local checkout: `~/src/Packed-Vanillas`
- Upstream remote: `https://github.com/ItsBlackGear/VanillaBackport.git`
- Origin remote: `https://github.com/KAITO9999/Packed-Vanillas.git`

The Forge mod id and the `minecraft` resource/data namespace are intentionally retained as `vanillabackport` for world, registry, and existing resource compatibility. The display name/artifact name can become `Packed-Vanillas` without gratuitously changing persisted identifiers.

## Static inventory

- 702 Java source files across the common and Forge source sets.
- 153 Mixin source files (count from source tree; includes common/client/compat groups).
- 2,620 resource files under the inspected common/Forge resource trees.
- 136 source files import Platform APIs, with 213 direct Platform imports.
- Platform API imports span `core`, `client`, and `common`; this is not a two-line Gradle dependency removal.

## Platform dependency findings

The upstream project has Platform dependencies in both `common/build.gradle` and `forge/build.gradle`, currently versioned through `platform_indev_version=1.4.2`. Forge `mods.toml` declares `platform` as a mandatory runtime mod.

The code uses Platform for:

- bootstrap/lifecycle and environment detection;
- config registration/loading;
- registry builders and typed registry helpers;
- Forge event pipelines for common/client/HUD/rendering;
- custom block/item/entity/particle/sound registration helpers;
- synced entity data and networking;
- biome/worldgen modifier and placement helpers;
- loot modifier and data/resource reload integration;
- creative tab and rendering integration APIs.

A simple Gradle dependency deletion would leave unresolved imports and, more importantly, silently remove event pipelines used by the mod. The preferred no-external-prerequisite route is to vendor the required Platform common + Forge source as an internal implementation, relocate its Java package to `com.blackgear.vanillabackport.internal.platform`, integrate its bootstrap into the existing Forge entrypoint, and retarget its Forge event subscribers to the existing `vanillabackport` mod id. Do not include Platform's separate `@Mod` entrypoint or `mods.toml`; Packed-Vanillas remains the only installed mod. Preserve the upstream Platform MIT notice and attribution when vendoring.

The Platform helper's `Platform.resource()` namespace and any existing `platform:` internal identifiers must be audited separately before changing them. Do not mass-rewrite persisted resource identifiers as part of package relocation.

## Resource-pack compatibility: 1.20.1 vs modern packs

Minecraft 1.20.1 uses resource-pack format 15. Modern resource packs have moved far beyond that format. Relevant documented changes include:

- 1.21.4: item model definitions under `assets/<namespace>/items/`.
- 1.21.6: strict JSON parsing for resource-pack JSON.
- 1.21.9: `min_format` / `max_format` replace `supported_formats` for modern pack metadata; `pack_format` is optional for new-format packs.
- 1.21.11: resource-pack format 75; item textures are separated into an `items` atlas without mipmaps; atlas sprite-name collisions warn; block model/state rotations expand; texture `.mcmeta` gains `mipmap_strategy` and `alpha_cutoff_bias`.
- 26.1: resource-pack format 84, with additional shader/resource changes.

Compatibility should be implemented at the resource-pack ingestion/adapter boundary, not by changing the global renderer blindly. Initial safe scope to investigate:

1. Read modern `pack.mcmeta` and normalize metadata for the 1.20.1 pack reader without modifying the user's source archive.
2. Convert the subset of modern `items/*.json` item-model definitions that can be represented by 1.20.1 item models; explicitly warn/skip unsupported node types instead of silently producing broken models.
3. Merge/remap the modern `items` atlas definitions into the legacy 1.20.1 blocks atlas where semantics permit.
4. Translate supported `.mcmeta` animation/texture fields and ignore only fields with no 1.20.1 equivalent.
5. Treat modern shader packs, equipment definitions, and expanded model rotations as separate compatibility classes; do not claim full support unless each has a faithful conversion.

The mod's existing resource reload code primarily handles its own JSON assets (waypoint styles, foliage colors, ghast equipment). It does not currently provide a general modern resource-pack conversion layer.

## Performance candidates (behavior-preserving first)

### High-confidence candidate

`ClientWaypointManager.forEachWaypoint()` currently creates a stream and sorts the entire waypoint collection by distance on every HUD render. The locator bar is rendered per frame, so this repeats sorting and stream work at display refresh rate even though waypoint state is updated on client ticks. Investigate a reusable sorted snapshot/cache invalidated on waypoint mutations and refreshed at most once per client tick. Preserve far-to-near draw order; compare behavior when two markers overlap or cross distance order within a tick.

### Low-value / do not optimize without evidence

- `DynamicBlockRenderingHandler` polls only two watched config values every 40 ticks; cost is negligible. Preserve the chunk rebuild when either value changes.
- Registry-wide item/block enumeration in `ItemLikeRendering` happens during setup/model registration, not per frame.
- `ItemWaypointProvider` scans inventory only when compass-waypoint display is enabled (default false); avoid adding a hot-path cache until profiling shows a need.
- Concurrent collections may be replaceable, but only after confirming all packet/event mutations occur on the client thread.

## Compatibility paths: preserve pending evidence

Existing compatibility gates are not arbitrary overhead and currently protect meaningful behavior:

- Better Combat: avoids duplicate spear attack/range/animation behavior.
- Caverns & Chasms: avoids duplicate weathering lightning-rod entries.
- Quark: recognizes its shiny animal texture variants.
- Backported Wolves: conversion/variant interoperability.
- Tiny Takeover: avoids replacing its baby animal models.
- Every Compat: conditional wood integration.
- JER and Mouse Tweaks mixins are `@Pseudo`; they provide optional integrations for biome listing and bundle mouse-wheel behavior.

Do not delete these solely to reduce source size. Re-evaluate only if the relevant upstream integration is obsolete for the actual target versions and the removal cannot alter gameplay, resource packs, or user interaction.

## Validation tool audit

Inspected `~/mc-test-kit/README.md`, `REQUEST.txt`, `PROVENANCE.txt`, `package.json`, and `bin/mc-test.mjs`.

- `java-jar`: ZIP integrity plus common Fabric/Quilt/Forge metadata detection.
- `java-project`: invokes Gradle `test` and `build`; it executes project Gradle scripts and is not a static source analyzer.
- It does not currently validate Mixin targets, runtime event wiring, resource-pack format conversions, or rendered output.
- Its installed Node test suite passed 6/6 tests on this audit run.

Use source inspection/search for static audit, `mc-test java-project` for Gradle validation only in an approved build route, and `mc-test java-jar` for final artifact integrity/metadata checks. Build/compile work on Termux is routed through the Termux build-plan gate; unknown/large builds should use GitHub Actions or a cloud build host.

## Initial implementation order

1. Establish a clean baseline CI path; do not treat source inspection as a build pass.
2. Internalize Platform APIs and remove the external `platform` dependency while preserving event pipelines and registry/network behavior.
3. Rename the user-facing mod/artifact to Packed-Vanillas while retaining `modId=vanillabackport` and existing data/resource namespaces.
4. Implement and test the waypoint render-order cache.
5. Implement resource-pack metadata normalization and the supported modern item-model/atlas conversion subset with fixtures.
6. Only then evaluate larger Mixin/config compatibility removals or deeper optimizations.
