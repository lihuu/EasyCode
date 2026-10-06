---
name: easy-code-templates
description: 为本项目生成 EasyCode-lite 专属代码生成模板组（实体→分层代码/测试/Fenix XML）并安装到 IDE。当用户要求"为本项目定制 EasyCode 模板 / 生成代码生成模板组 / AI 生成项目专属模板"时使用。
---

# 本项目的 EasyCode-lite AI 模板定制

> 本文件由 EasyCode-lite 插件在项目打开时自动创建，可自由修改，插件不会覆盖；删除后重新打开项目会再次生成（可在插件设置页关闭该行为）。

## 背景机制（决定方案形态）

- EasyCode-lite 是实体类驱动的代码生成插件：编辑器中对 Java 实体/类/方法右键 Generate → GenerateSimpleCode / GenerateTest / GenerateFenixXml，按 Velocity 模板生成代码。
- **模板组是 IDE 全局配置，所有项目共享**，持久化于：
  `~/Library/Application Support/JetBrains/IntelliJIdea*/options/easy-code-setting.xml`（XmlSerializer 格式；存在多个版本目录时取当前使用的那个）。
- 项目级配置（`easy-code-project-setting.xml`）只记录本项目绑定的组名与保存路径，各项目互不干扰。
- 因此：**组名必须用本项目名命名**（全局唯一），避免与其他项目的定制组互相覆盖；项目内首次生成时会绑定并记住该组。

## 执行步骤

### 1. 分析本项目

- 构建工具与版本、Java 版本、Spring Boot 版本（**Boot 3 → jakarta.\*，Boot 2 → javax.\***，决定模板 import 前缀）
- 持久层：Spring Data JPA / MyBatis-Plus / MyBatis（看依赖与现有代码）
- 测试框架：JUnit 4 还是 5；是否用 Lombok
- 实际分层与子包名（entity / repository|mapper / service / impl / controller）
- 多模块结构；表/类前缀与命名约定；注释风格（是否中文 javadoc、@author 署名）

### 2. 以内置模板为基底改写（不要凭空写）

基底模板：EasyCode-lite 源码仓库 `src/main/resources/template/<组>/*.vm`（本机默认 `/Users/lihu/git/EasyCode`；路径不存在时按本项目现有代码风格直接推导）
- JPA 项目参照 `Default/`；MyBatis-Plus 参照 `MybatisPlus/`、`MybatisPlus-Mixed/`（带 mapper.xml）；MyBatis 参照 `Mybatis/`；单元测试参照 `Test/`；Fenix XML 参照 `Fenix/`
- 每个模板开头必须设置 `$!callback.setFileName(...)`，必要时加 `$!callback.setSavePath(...)`
- **Velocity 2.x：循环内用 `$foreach.hasNext`，禁止使用已移除的 `$velocityHasNext`**

模板上下文变量（实体类生成流程）：
- `$classInfo` / `$tableInfo` / `$entityClassInfo`（同一对象）：`name`、`packageName`、`modulePath`、`moduleName`、`allProperties`（List，元素含 `name/type/shortType`）、`primaryKeyProperties`、`annotationInfoList`、`savePath`、`savePackageName`
- `$tool`（GlobalTool，继承 NameUtils）：`newHashSet/newArrayList/newHashMap`、`call()`、`debug(obj)`、`serial()`、`parseJson/toJson`、`replace/replaceFirst`、`getClassName/firstLowerCase/getClsNameByFullName` 等
- `$time.currTime()`、`$author`、`$projectPath`、`$modulePath`、`$importList`
- 测试方法流程（对方法触发 GenerateTest）另有：`$methodInfo`（含 `methodName/containingClassName/classInfo/methodParameters/annotationInfos`）、`$methodAnnotationMap`、`$classAnnotationMap`、`$parameters`

### 3. 输出

写入 `<本项目>/.easycode-templates/<组名>/`：
- 一个模板一个文件，文件名 = 模板名 + `.vm`（如 `controller.java.vm`、`test.common.vm`）
- 组名 = 本项目名；测试模板名以 `test` 开头（插件据此识别测试模板，走追加写入逻辑）
- 同时生成 `README.md` 记录技术栈结论与适配点，便于人工复查

### 4. 安装到 IDE（先与用户确认，IDE 必须已完全退出）

编辑上文的 `easy-code-setting.xml`，在 `<option name="templateGroupMap"><map>` 内追加（与既有 entry 平级）：

```xml
<entry key="<组名>">
  <value>
    <TemplateGroup>
      <option name="elementList">
        <list>
          <Template>
            <option name="code" value="<模板内容,XML 属性转义>" />
            <option name="name" value="<模板名>" />
            <option name="show" value="true" />
          </Template>
        </list>
      </option>
      <option name="name" value="<组名>" />
    </TemplateGroup>
  </value>
</entry>
```

- 转义规则（`value` 属性内）：`"` → `&quot;`、`&` → `&amp;`、换行 → `&#10;`、`<` → `&lt;`、`>` → `&gt;`。推荐用 python 脚本读 `.vm` 文件做转义后拼装，不要手写。
- 同名组已存在时，先备份原 entry（改 key 为 `<组名>.bak-<日期>`）再写入。

### 5. 验收清单

- IDE 正常启动，Settings → EasyCode 确认新组出现且内容完整（中文/换行无乱码）
- 对一个真实实体跑 Generate → GenerateSimpleCode：生成文件的包名、保存路径、代码风格与本项目现有代码一致
- 生成过测试模板的：对一个 Service 方法跑 GenerateTest，确认落到正确的 test 目录且 JUnit 版本正确
- 有出入则回到 `.easycode-templates/` 修改 `.vm` 后重新执行第 4 步
