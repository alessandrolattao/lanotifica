---
name: bump-version
description: Use when bumping the version number for a new release. Covers all three sync points that must stay in sync.
---

# Bump Version

Three files must always be in sync. Touch all three in the same commit.

## The three sync points

| File | What to change |
|------|---------------|
| `app/app/build.gradle.kts` | `versionName` (e.g. `"1.2.0"`) AND `versionCode` (monotone integer, always +1) |
| `packaging/rpm/lanotifica.spec` | Must use `%{pkg_version}` — NEVER hardcode. If it's hardcoded, fix it. |
| `server/cmd/lanotifica/main.go` or `main` package constant | `Version` constant |

## Steps

1. Decide the new version (semver: MAJOR.MINOR.PATCH)
2. Determine new `versionCode` = current + 1 (check `build.gradle.kts`)
3. Edit `app/app/build.gradle.kts`: update both `versionCode` and `versionName`
4. Verify `packaging/rpm/lanotifica.spec` uses `%{pkg_version}` (fix if not)
5. Update `Version` in Go main package if there's a constant
6. Commit all three files together: `chore: bump version to vX.Y.Z`

## Common mistakes to avoid

- **Never bump `versionCode` without bumping `versionName`** — Play Store rejects it
- **Never reuse a `versionCode`** — Play Store rejects it permanently for that code
- **Never hardcode version in RPM spec** — COPR build will use the wrong version
- **Never push a tag before CI is green** — fix the build first, then tag

## Checking current versions

```bash
grep versionCode app/app/build.gradle.kts
grep versionName app/app/build.gradle.kts
grep ^Version: packaging/rpm/lanotifica.spec
```
