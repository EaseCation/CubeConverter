# CubeConverter
A library for reading and converting minecraft model for Java/Bedrock edition.

### Basic example
- Not now.

### Offset to correct position
- If you put the model on item display it can be in the wrong position, you can fix this by add extra (scale * 0.5F) to the y position. That should do the trick.

Feel free to write a wiki for this project if you want (~~**PLEASE.**~~)

## Useful resources
CubeConverter would not have been possible without the following projects:
- [Blockbench](https://github.com/JannisX11/blockbench/): Used for debugging/testing models, helped with figure out converting, **DIRECT CODE IMPLEMENT FOR** [UV MAPPING](https://github.com/Oryxel/CubeConverter/blob/main/src/main/java/org/oryxel/cube/util/UVUtil.java). 

---

# EaseCation 维护说明（CubeConverter fork 如何维护）

> 本节为 EaseCation 内部维护文档。这个仓库是 **oryxel1/CubeConverter 上游的下游 fork**:以上游为基,叠加少量 EaseCation 自有特性。务必按下面的规范维护,避免重新陷入"双 fork 平行分叉"的坑。

## 1. 这是什么、以谁为准

- **上游(source of truth)= [`oryxel1/CubeConverter`](https://github.com/oryxel1/CubeConverter)**,RaphiMC 的 ViaBedrock 官方依赖、持续跟进新版本旋转。**一切以 oryxel1 规范为准**。
- 本 fork 只在上游之上携带 **极少量自有补丁**(见下),目标是让补丁趋近于零(最终尽量 PR 回上游)。
- 历史上曾与上游 **从 `63df470`(2025-04-01)平行分叉** 各跑各的,导致 gson 风味与 API 不一致、运行期 `NoSuchMethodError`。现已把这些补丁 rebase 回上游基。

## 2. 分支模型（照搬 oryxel1 的约定,按 json 库分支)

| 本 fork 分支 | 对应 oryxel1 分支 | gson 风味 | 消费方 |
|--------------|-------------------|-----------|--------|
| `master`         | `oryxel1/master`  | **plain** `com.google.gson`              | NeoForge 客户端(VBU)、BedrockMotion(NeoForge) |
| `vv-json` | `oryxel1/vv-json` | **relocated** `com.viaversion.viaversion.libs.gson` | ViaProxy / ViaBedrock(经 BedrockMotion) |

- `vv-json` = `master` + 一层薄 gson relocation(完全等价 oryxel1 "merge master into vv-json")。
- ViaProxyWorkspace 的 CubeConverter checkout 停在 **`vv-json`**;NeoForgeWorkspace 的停在 **`master`**。
- `master` 现在直接对齐上游(= oryxel1/master + 我方补丁);统一前的旧 EaseCation 状态(`da09dcc`)保留在备份分支 **`backup/master-da09dcc`**,仅作存档、不再使用。

## 3. EaseCation 自有补丁（相对 oryxel1 基的全部增量)

仅这 5 块,均为"上游缺、我方消费方需要"的能力,移植时**尽量贴合 oryxel1 代码风格**:
1. `model/element/Locator` + `Parent.getLocators()`（VBU 物品挂点/locator)
2. `model/element/PolyMesh` + `Parent.getPolyMesh()`（VBU PolyMesh 渲染)
3. `BedrockRenderController` 的 `partVisibility / ignoreLighting / lightColorMultiplier`（VBU 渲染控制)
4. attachable / entity data 解析改进
5. Bedrock locator 解析

## 4. 如何追上游(标准同步流程)

```bash
git fetch oryxel1
# 1) 把自有补丁 rebase 到上游 master 之上
git checkout master
git rebase oryxel1/master          # 冲突一律以 oryxel1 为准,仅重新落上面 5 块能力
# 2) 把 master 并入 relocated 分支(沿用上游 merge 模式)
git checkout vv-json
git merge master                   # gson import 冲突取 relocated 侧;build.gradle 保留 compileOnlyApi
# 3) 验证两风味都能编译(在各自 workspace 根目录,见第 6 节),再 push
git push origin master vv-json
```

> 关键原则:**冲突时优先采用 oryxel1 的实现/命名/API/旋转逻辑**,只把上面 5 块能力重新叠加上去。若上游已自带某块(例如未来 PR 合入),就从补丁里删掉它。

## 5. gson 风味是怎么处理的(别破坏)

- `master`(plain):`build.gradle` 用 `implementation 'com.google.code.gson:gson'`,直接给 NeoForge/MC 用。
- `vv-json`(relocated):`build.gradle` 用
  `compileOnlyApi("com.viaversion:viaversion-common") { attributes { Bundling.SHADOWED } }`。
  - **compileOnlyApi** 让 relocated gson 透传到消费方(BedrockMotion/ViaBedrock)的 compile classpath,用于解析本库解析器的 `parse(JsonObject)` 重载;**运行期由 ViaVersion 提供**,不打入产物。
  - 版本(`5.9.x-SNAPSHOT`)与 SHADOWED 属性需与 ViaBedrock 的 viaversion-common 依赖保持一致。
- 消费方注意:**不要**把跨界数据当成某一风味 gson 硬编码。典型反例已修复——BedrockMotion `Content` 曾调本库 `GsonUtil.getGson()` 当 plain 用,在 relocated 运行期下 `NoSuchMethodError`;现改为 BedrockMotion 自带 `Gson`,对本库零 gson 类型耦合。新消费方请遵循:**只用 `parse(String)` 与不暴露 gson 的数据类**,gson 自备。

## 6. 验证

两个 workspace 都用 **Java 21、在 workspace 根目录** 构建(禁止子项目目录跑 Gradle):
- ViaProxy 侧:`./gradlew :ViaBedrock:compileJava` 与 `:ViaProxy:assemble` 通过(relocated 变体 + RotationType 可解析)。
- NeoForge 侧:`copy-mods.ps1 -Build`,VBU 编译通过且在线渲染无回归(尤其 UV/旋转/几何)。
