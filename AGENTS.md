## EasyCode agent guide

### Big picture
- `EasyCode` is an IntelliJ Platform plugin for code generation from database metadata, Java PSI, and Velocity templates.
- Main flow: action classes in `src/main/java/com/sjhy/plugin/actions/` collect PSI context, open Swing dialogs in `src/main/java/com/sjhy/plugin/ui/`, then delegate generation to `CodeGenerateServiceImpl`.
- Core generation pipeline is `CodeGenerateServiceImpl` -> `TemplateUtils` / `VelocityUtils` -> `SaveFile`.
- Global plugin state lives in the application service `Settings` (`easy-code-setting.xml`); per-project defaults live in `ProjectLevelSettingsServiceImpl` (`easy-code-project-setting.xml`).

### Key files to read first
- `src/main/resources/META-INF/plugin.xml`: registered actions (`GenerateSimpleCode`, `GenerateTest`, `GenerateFenixXml`), services, and settings panel.
- `src/main/java/com/sjhy/plugin/service/impl/CodeGenerateServiceImpl.java`: save paths, template parameters, import collection, test/fenix generation.
- `src/main/java/com/sjhy/plugin/config/Settings.java`: built-in template groups, type mappers, and global config loading from resources.
- `src/main/java/com/sjhy/plugin/ui/CodeGenerateForm.java`: module/package/save-path UI and how relative paths are persisted.
- `src/main/java/com/sjhy/plugin/entity/SaveFile.java`: overwrite/compare/append behavior and when generated files are opened.

### Templates and config conventions
- Built-in templates live under `src/main/resources/template/<Group>/...`; group names must match `Settings.loadTemplateGroup()` keys (`Default`, `Mybatis`, `MybatisPlus`, `MybatisPlus-Mixed`, `Test`, `Fenix`).
- Global config snippets live under `src/main/resources/globalConfig/Default/*.vm` and are injected by `TemplateUtils.addGlobalConfig(...)` before Velocity rendering.
- Templates can steer output via `Callback`; example: `src/main/resources/template/Fenix/fenix.file.xml.vm` sets `$callback.setFileName(...)`.
- Velocity helper variables are documented in `src/main/java/velocity_implicit.vm`; keep that file in sync if you add template context variables.

### IntelliJ/plugin-specific patterns
- This repo still uses `ServiceManager.getService(...)`; keep changes compatible with the existing registration style unless you are intentionally migrating all related code.
- UI classes often have paired `.java` + `.form` files in `src/main/java/com/sjhy/plugin/ui/`; avoid changing generated form bindings from code only.
- `ModuleUtils` prefers real source roots and falls back to `src/main/java`; path handling assumes `/` separators and often stores project-relative paths starting with `.`.
- `SaveFile` treats in-project paths differently from external paths and supports append mode for generated test methods / XML fragments.

### Build, run, test
- Gradle runs on the local JDK 25 (GraalVM CE, the `java_home` default); the user standardized all personal projects and this plugin on JDK 25. Supporting versions: Gradle 9.8.0, `org.jetbrains.intellij.platform` 2.19.0, Lombok 1.18.48.
- Platform/bytecode alignment (2026-10): dev platform is the unified `intellijIdea("2026.2")` (`ideaIC`/`ideaIU` are no longer published since 2025.3), bytecode target is 25 matching the IDE's bundled JBR 25, `sinceBuild=262` with no `untilBuild`. The user's IDE is IDEA 2026.2 (JBR 25.0.4).
- commons-collections 3.x is no longer bundled with the platform since 2026.2; it is declared explicitly in `build.gradle.kts`.
- If the Gradle wrapper download stalls (`SSL peer shut down incorrectly`), download the distribution with curl and unpack it into `~/.gradle/wrapper/dists/gradle-<v>-bin/<base36-of-md5-of-url>/` with a `<zip>.ok` marker.
```bash
./gradlew tasks --all
./gradlew test
```
- Useful plugin tasks confirmed by Gradle: `runIde`, `buildPlugin`, `verifyPlugin`, `prepareSandbox`.
- Sandbox is configured at repo root `idea-sandbox/`, not under `build/`.

### Project-specific gotchas
- Do not edit generated artifacts under `build/`; change source files under `src/main/...` and let Gradle regenerate patched plugin metadata.
- `build.gradle.kts` sets Java source/target to 25 and plugin compatibility to `sinceBuild=262` with no `untilBuild`; runtime metadata is patched from Gradle, so prefer updating build config over edited copies in `build/`.
- Current unit tests use JUnit 4 (`src/test/java/...`), while generated test templates import JUnit 5 (`org.junit.jupiter.api.Test`); keep that mismatch in mind before changing test dependencies or templates.
- When adding a new template group or built-in resource, wire it through `Settings.initDefault()` / `loadTemplateGroup()` or it will never appear in the UI.

### Safe change strategy
- For generation bugs, trace both the UI/action entry point and the template/resource path it selects.
- For save-path issues, inspect `CodeGenerateForm`, `ProjectSettingModel`, `ProjectLevelSettingsServiceImpl`, and `SaveFile` together.
- For template-context changes, update both `CodeGenerateServiceImpl` parameter assembly and `velocity_implicit.vm` documentation.

