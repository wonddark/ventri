# Rename Adpt → Ventri Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rename all occurrences of "Adpt/adpt/ADPT" to "Ventri/ventri/VENTRI" across the entire codebase — package names, class names, file names, resource names, and directory structure.

**Architecture:** Pure rename/refactor — no logic changes. Content substitutions happen first (sed in-place), then file renames, then directory moves, then a build verification. All changes are reversible via git.

**Tech Stack:** Kotlin Multiplatform, Android (Jetpack Compose), SQLDelight, Gradle KTS

---

## File Map

### Files with content changes only (no rename)
- `settings.gradle.kts` — root project name
- `androidApp/build.gradle.kts` — namespace, applicationId
- `shared/build.gradle.kts` — namespace, SQLDelight database name and package
- `androidApp/src/main/AndroidManifest.xml` — application name ref, theme name
- `androidApp/src/main/res/values/themes.xml` — style name
- `androidApp/src/main/res/values-night/themes.xml` — style name
- `androidApp/src/main/res/values/strings.xml` — app_name value
- All `.kt` files under `androidApp/src/` and `shared/src/` — package declarations, imports, class references

### Files renamed (content also updated)
- `AdptApplication.kt` → `VentriApplication.kt`
- `AdptColors.kt` → `VentriColors.kt`
- `AdptLocalValues.kt` → `VentriLocalValues.kt`
- `AdptShapes.kt` → `VentriShapes.kt`
- `AdptSpacing.kt` → `VentriSpacing.kt`
- `AdptTheme.kt` → `VentriTheme.kt`
- `AdptTypography.kt` → `VentriTypography.kt`
- All `Adpt*.kt` design components (19 files) → `Ventri*.kt`

### Directories moved
- `androidApp/src/main/kotlin/com/adpt/` → `androidApp/src/main/kotlin/com/ventri/`
- `shared/src/commonMain/kotlin/com/adpt/` → `shared/src/commonMain/kotlin/com/ventri/`
- `shared/src/androidMain/kotlin/com/adpt/` → `shared/src/androidMain/kotlin/com/ventri/`
- `shared/src/commonTest/kotlin/com/adpt/` → `shared/src/commonTest/kotlin/com/ventri/`
- `shared/src/commonMain/sqldelight/com/adpt/` → `shared/src/commonMain/sqldelight/com/ventri/`

---

## Task 1: Update resource files

**Files:**
- Modify: `androidApp/src/main/res/values/strings.xml`
- Modify: `androidApp/src/main/res/values/themes.xml`
- Modify: `androidApp/src/main/res/values-night/themes.xml`
- Modify: `androidApp/src/main/AndroidManifest.xml`

- [ ] **Step 1: Update app_name string**

```xml
<!-- androidApp/src/main/res/values/strings.xml -->
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Ventri</string>
</resources>
```

- [ ] **Step 2: Update light theme style name**

```xml
<!-- androidApp/src/main/res/values/themes.xml -->
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.Ventri" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

- [ ] **Step 3: Update dark theme style name**

```xml
<!-- androidApp/src/main/res/values-night/themes.xml -->
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.Ventri" parent="android:Theme.Material.NoActionBar" />
</resources>
```

- [ ] **Step 4: Update AndroidManifest theme references**

```xml
<!-- androidApp/src/main/AndroidManifest.xml -->
<application
    android:name=".AdptApplication"
    android:allowBackup="true"
    android:label="@string/app_name"
    android:supportsRtl="true"
    android:theme="@style/Theme.Ventri">

    <activity
        android:name=".MainActivity"
        android:exported="true"
        android:launchMode="singleTop"
        android:theme="@style/Theme.Ventri">
```

Note: `android:name=".AdptApplication"` stays as `.AdptApplication` for now — we'll update it after the file rename in Task 4.

- [ ] **Step 5: Commit**

```bash
git add androidApp/src/main/res/values/strings.xml \
        androidApp/src/main/res/values/themes.xml \
        androidApp/src/main/res/values-night/themes.xml \
        androidApp/src/main/AndroidManifest.xml
git commit -m "rename: update resource strings and theme names to Ventri"
```

---

## Task 2: Update Gradle build files

**Files:**
- Modify: `settings.gradle.kts`
- Modify: `androidApp/build.gradle.kts`
- Modify: `shared/build.gradle.kts`

- [ ] **Step 1: Update root project name**

In `settings.gradle.kts`, change:
```kotlin
// before
rootProject.name = "adpt"
// after
rootProject.name = "ventri"
```

- [ ] **Step 2: Update androidApp namespace and applicationId**

In `androidApp/build.gradle.kts`, change:
```kotlin
// before
namespace = "com.adpt.app"
// ...
applicationId = "com.adpt.app"

// after
namespace = "com.ventri.app"
// ...
applicationId = "com.ventri.app"
```

- [ ] **Step 3: Update shared namespace and SQLDelight config**

In `shared/build.gradle.kts`, change:
```kotlin
// before
namespace = "com.adpt.shared"
// ...
create("AdptDatabase") {
    packageName.set("com.adpt.shared.db")
}

// after
namespace = "com.ventri.shared"
// ...
create("VentriDatabase") {
    packageName.set("com.ventri.shared.db")
}
```

- [ ] **Step 4: Commit**

```bash
git add settings.gradle.kts androidApp/build.gradle.kts shared/build.gradle.kts
git commit -m "rename: update Gradle namespaces and SQLDelight database name to Ventri"
```

---

## Task 3: Bulk replace all Adpt/adpt references inside .kt and .sq files

This task updates all content in Kotlin and SQLDelight source files using sed. Directory structure and file names are unchanged at this point.

**Files:** All `.kt` files under `androidApp/src/` and `shared/src/`, and `.sq` files under `shared/src/commonMain/sqldelight/`

- [ ] **Step 1: Replace package declarations and imports (com.adpt → com.ventri)**

```bash
find /home/oz/Projects/Personal/adpt/androidApp/src \
     /home/oz/Projects/Personal/adpt/shared/src \
     -name "*.kt" \
     -exec sed -i 's/com\.adpt\./com.ventri./g' {} +
```

- [ ] **Step 2: Replace class name references (AdptXxx → VentriXxx)**

```bash
find /home/oz/Projects/Personal/adpt/androidApp/src \
     /home/oz/Projects/Personal/adpt/shared/src \
     -name "*.kt" \
     -exec sed -i 's/Adpt\([A-Z]\)/Ventri\1/g' {} +
```

Note: The pattern `Adpt([A-Z])` catches `AdptColors`, `AdptTheme`, etc. but not `adpt` (lowercase).

- [ ] **Step 3: Replace SQLDelight package reference**

```bash
find /home/oz/Projects/Personal/adpt/shared/src/commonMain/sqldelight \
     -name "*.sq" \
     -exec sed -i 's/com\.adpt\./com.ventri./g' {} +
```

- [ ] **Step 4: Replace AdptDatabase references in .kt files**

```bash
find /home/oz/Projects/Personal/adpt/androidApp/src \
     /home/oz/Projects/Personal/adpt/shared/src \
     -name "*.kt" \
     -exec sed -i 's/AdptDatabase/VentriDatabase/g' {} +
```

- [ ] **Step 5: Verify substitutions look correct (spot-check 3 files)**

```bash
grep -r "adpt\|Adpt\|ADPT" \
  /home/oz/Projects/Personal/adpt/androidApp/src \
  /home/oz/Projects/Personal/adpt/shared/src \
  --include="*.kt" | head -30
```

Expected: zero results (all references replaced).

- [ ] **Step 6: Commit**

```bash
git add androidApp/src shared/src
git commit -m "rename: replace all Adpt/adpt class names and packages in source files"
```

---

## Task 4: Rename Adpt-prefixed Kotlin files to Ventri-prefixed

All files whose names start with `Adpt` are renamed to start with `Ventri`. Content was already updated in Task 3.

**Files:** All `Adpt*.kt` in `androidApp/src/` (application class + 25 design system files)

- [ ] **Step 1: Rename the application class file**

```bash
mv /home/oz/Projects/Personal/adpt/androidApp/src/main/kotlin/com/adpt/app/AdptApplication.kt \
   /home/oz/Projects/Personal/adpt/androidApp/src/main/kotlin/com/adpt/app/VentriApplication.kt
```

- [ ] **Step 2: Update AndroidManifest to point to the renamed class**

In `androidApp/src/main/AndroidManifest.xml`, change:
```xml
android:name=".AdptApplication"
```
to:
```xml
android:name=".VentriApplication"
```

- [ ] **Step 3: Rename design system files**

```bash
cd /home/oz/Projects/Personal/adpt/androidApp/src/main/kotlin/com/adpt/app/ui/design
for f in Adpt*.kt; do
  mv "$f" "Ventri${f#Adpt}"
done
```

- [ ] **Step 4: Rename design component files**

```bash
cd /home/oz/Projects/Personal/adpt/androidApp/src/main/kotlin/com/adpt/app/ui/design/components
for f in Adpt*.kt; do
  mv "$f" "Ventri${f#Adpt}"
done
```

- [ ] **Step 5: Verify no Adpt-named .kt files remain**

```bash
find /home/oz/Projects/Personal/adpt/androidApp/src \
     /home/oz/Projects/Personal/adpt/shared/src \
     -name "Adpt*.kt"
```

Expected: no output.

- [ ] **Step 6: Commit**

```bash
git add -A androidApp/src/main/AndroidManifest.xml \
         androidApp/src/main/kotlin/com/adpt/app/
git commit -m "rename: rename Adpt-prefixed files to Ventri-prefixed"
```

---

## Task 5: Move package directories from com/adpt to com/ventri

**Directories moved:**
- `androidApp/src/main/kotlin/com/adpt/` → `androidApp/src/main/kotlin/com/ventri/`
- `shared/src/commonMain/kotlin/com/adpt/` → `shared/src/commonMain/kotlin/com/ventri/`
- `shared/src/androidMain/kotlin/com/adpt/` → `shared/src/androidMain/kotlin/com/ventri/`
- `shared/src/commonTest/kotlin/com/adpt/` → `shared/src/commonTest/kotlin/com/ventri/`

- [ ] **Step 1: Move androidApp source directory**

```bash
mkdir -p /home/oz/Projects/Personal/adpt/androidApp/src/main/kotlin/com/ventri
mv /home/oz/Projects/Personal/adpt/androidApp/src/main/kotlin/com/adpt/app \
   /home/oz/Projects/Personal/adpt/androidApp/src/main/kotlin/com/ventri/app
rmdir /home/oz/Projects/Personal/adpt/androidApp/src/main/kotlin/com/adpt
```

- [ ] **Step 2: Move shared commonMain source directory**

```bash
mkdir -p /home/oz/Projects/Personal/adpt/shared/src/commonMain/kotlin/com/ventri
mv /home/oz/Projects/Personal/adpt/shared/src/commonMain/kotlin/com/adpt/shared \
   /home/oz/Projects/Personal/adpt/shared/src/commonMain/kotlin/com/ventri/shared
rmdir /home/oz/Projects/Personal/adpt/shared/src/commonMain/kotlin/com/adpt
```

- [ ] **Step 3: Move shared androidMain source directory**

```bash
mkdir -p /home/oz/Projects/Personal/adpt/shared/src/androidMain/kotlin/com/ventri
mv /home/oz/Projects/Personal/adpt/shared/src/androidMain/kotlin/com/adpt/shared \
   /home/oz/Projects/Personal/adpt/shared/src/androidMain/kotlin/com/ventri/shared
rmdir /home/oz/Projects/Personal/adpt/shared/src/androidMain/kotlin/com/adpt
```

- [ ] **Step 4: Move shared commonTest source directory**

```bash
mkdir -p /home/oz/Projects/Personal/adpt/shared/src/commonTest/kotlin/com/ventri
mv /home/oz/Projects/Personal/adpt/shared/src/commonTest/kotlin/com/adpt/shared \
   /home/oz/Projects/Personal/adpt/shared/src/commonTest/kotlin/com/ventri/shared
rmdir /home/oz/Projects/Personal/adpt/shared/src/commonTest/kotlin/com/adpt
```

- [ ] **Step 5: Verify old directories are gone and new ones exist**

```bash
find /home/oz/Projects/Personal/adpt/androidApp/src \
     /home/oz/Projects/Personal/adpt/shared/src \
     -type d -name "adpt"
```

Expected: no output.

```bash
find /home/oz/Projects/Personal/adpt/androidApp/src \
     /home/oz/Projects/Personal/adpt/shared/src \
     -type d -name "ventri"
```

Expected: 4 directories listed.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "rename: move package directories from com/adpt to com/ventri"
```

---

## Task 6: Move SQLDelight directory

**Directory moved:**
- `shared/src/commonMain/sqldelight/com/adpt/` → `shared/src/commonMain/sqldelight/com/ventri/`

- [ ] **Step 1: Move the SQLDelight package directory**

```bash
mkdir -p /home/oz/Projects/Personal/adpt/shared/src/commonMain/sqldelight/com/ventri
mv /home/oz/Projects/Personal/adpt/shared/src/commonMain/sqldelight/com/adpt/shared \
   /home/oz/Projects/Personal/adpt/shared/src/commonMain/sqldelight/com/ventri/shared
rmdir /home/oz/Projects/Personal/adpt/shared/src/commonMain/sqldelight/com/adpt
```

- [ ] **Step 2: Verify**

```bash
ls /home/oz/Projects/Personal/adpt/shared/src/commonMain/sqldelight/com/ventri/shared/db/
```

Expected: `Item.sq  ShoppingListEntry.sq`

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "rename: move SQLDelight directory from com/adpt to com/ventri"
```

---

## Task 7: Build and verify

- [ ] **Step 1: Run a full debug build**

```bash
cd /home/oz/Projects/Personal/adpt && ./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: If build fails, check for any remaining adpt references**

```bash
grep -r "adpt\|Adpt\|ADPT" \
  /home/oz/Projects/Personal/adpt/androidApp/src \
  /home/oz/Projects/Personal/adpt/shared/src \
  /home/oz/Projects/Personal/adpt/settings.gradle.kts \
  /home/oz/Projects/Personal/adpt/androidApp/build.gradle.kts \
  /home/oz/Projects/Personal/adpt/shared/build.gradle.kts \
  --include="*.kt" --include="*.xml" --include="*.kts" --include="*.sq"
```

Fix any remaining references and rebuild.

- [ ] **Step 3: Final commit confirming success**

```bash
git commit --allow-empty -m "rename: Adpt → Ventri rename complete, build verified"
```
