# 项目 GitFlow 分支与版本管理规范

> 适用范围：所有多品牌、多版本的系统项目
> 目标：统一分支模型和 Tag 使用方式，让版本可回溯、可维护、易协作。

---

## 一、总体目标与核心理念

1. **分支管理开发流程**

    * 分支代表“还在进行中的开发过程”，可以继续提交、继续演化。

2. **Tag 管理正式版本**

    * Tag 代表“已经确认的版本快照”，用于上线、回滚、复现环境。

3. 核心一句话：

   > **分支是过程，Tag 是结果。**
   > 开发走分支，发布认 Tag。

---

## 二、Git 仓库命名规范

统一仓库命名格式：

```text
{project name}-{layer}-{module}-{extra}
```

1. `layer`：按“领域/层级”划分

    * `core`：核心业务后端（订单、账户、投资、对账等）
    * `device`：物联网 & 设备接入相关
    * `ai`：AI / FAQ / 语音机器人相关
    * `web`：后台管理 / 官网 / H5
    * `miniapp`：小程序
    * `ops`：运维 / 部署 / 监控 / 脚本
    * `infra`：公共组件 / SDK / 工具库

2. `module`：具体功能域

    * 如：`charge-api`、`console-api`、`investor-api`、`device-gateway` 等。

3. `extra`：可选补充说明

    * 如：`v2`、`demo`、`experimental` 等。

示例：

* `evcharge-core-charge-api`
* `evcharge-device-general-gateway`
* `evcharge-miniapp-yuanqichong`
* `evcharge-ops-deploy-scripts`

---

## 三、分支策略（GitFlow）

标准分支结构：

```text
main                    ← 稳定主线（只存放可发布代码）
develop                 ← 开发主线（集成功能后的联调主线）
feature/*               ← 功能开发分支
release/*               ← 预发布分支
hotfix/*                ← 线上紧急修复分支
```

### 3.1 main 分支

1. 只存放**已上线或可随时上线**的代码。
2. 所有正式版本 Tag（`v1.0.0`、`v1.1.0` 等）都在 `main` 上打出。
3. 任意线上实例，都对应“**main 上某个 Tag + 该品牌配置**”。

### 3.2 develop 分支

1. 作为**日常开发主线**，汇总所有新功能。
2. 所有 `feature/*` 分支开发完成后，先合并到 `develop`，再联调。
3. `develop` 可以不稳定，只对内部测试负责，不直接等于线上版本。

> 初始化约定：
> 第一次落地规范时，从 `main@v1.0.0` 拉出 `develop`，以后**长期沿着 develop 演化**，不需要每个版本都从 Tag 重建 develop。

### 3.3 feature/* 分支（功能开发）

1. 用于单一需求 / 单一功能开发。
2. 命名示例：

    * `feature/charge-card-discount`
    * `feature/investor-income-summary`
    * `feature/device-metadata-registry`
3. 使用流程：

    1. 从 `develop` 拉出分支：

       ```bash
       git checkout develop
       git pull
       git checkout -b feature/xxx
       git push -u origin feature/xxx
       ```
    2. 开发 & 提交（commit）。
    3. 开 PR / merge 回 `develop`。
    4. 合并完成后删除该 `feature/*` 分支（本地 + 远程），避免分支堆积。

### 3.4 release/* 分支（预发布）

1. 使用时机：
   当 `develop` 上已经积累了一批功能，计划发布新版本（如 `v1.2.0`）时，从 `develop` 拉出：

   ```bash
   git checkout develop
   git pull
   git checkout -b release/1.2.0
   git push -u origin release/1.2.0
   ```

2. 在 `release/*` 上**只允许**：

    1. Bug 修复
    2. 文案 / 文档调整
    3. 配置微调

3. **禁止**在 `release/*` 上合入新的功能需求。

4. 当 `release/1.2.0` 测试通过、确认可上线时：

    1. 合并回 `main`：

       ```bash
       git checkout main
       git pull
       git merge --no-ff release/1.2.0
       ```
    2. 在 `main` 上打 Tag：`v1.2.0`
    3. 将 `release/1.2.0` 再合并回 `develop`，保证主线一致。
    4. 删除 `release/1.2.0` 分支（本地 + 远程）。

> 注意：
> 线上“长期跑”的是 `main@某个 Tag` 对应的构建产物（JAR），
> `release/*` 是短命分支，用完就删。

### 3.5 hotfix/* 分支（线上紧急修复）

1. 使用场景：
   已经发布的线上版本发现严重问题（计费错误、数据错误、系统崩溃等），需要快速修复且不能等下一个大版本。

2. 使用流程：

    1. 从 `main` 拉出 `hotfix` 分支：

       ```bash
       git checkout main
       git pull
       git checkout -b hotfix/fix-charge-balance-freeze
       git push -u origin hotfix/fix-charge-balance-freeze
       ```

    2. 在 `hotfix/*` 分支上修复问题并测试。

    3. 修复完成后：

        1. 合并回 `main`，并在 `main` 上打新的 Patch 版本 Tag（如 `v1.2.1`）：

           ```bash
           git checkout main
           git pull
           git merge --no-ff hotfix/fix-charge-balance-freeze
           git tag -a v1.2.1 -m "修复余额冻结异常问题"
           git push origin v1.2.1
           ```
        2. 再将 `hotfix/*` 合并回 `develop`：

           ```bash
           git checkout develop
           git pull
           git merge --no-ff hotfix/fix-charge-balance-freeze
           ```
        3. 删除 `hotfix/*` 分支（本地 + 远程）。

> 强制要求：
> **hotfix 修过的东西，必须同步回 `develop`，否则以后版本会重复踩同样的坑。**

### 3.6 GitFlow 简化流程图

```text
               +-------------------+
               |       main        |  ← 线上版本 & Tag 所在分支
               +---------+---------+
                         ^
                         |
                    +----+----+      hotfix/*
                    |  Tag   |<-----------------+
                    +----+---+                  |
                         ^                      |
                     merge                      |
                         ^                      |
+---------------+   +---+-----------------------+----+
|   feature/*   |-->|          develop               |
+---------------+   +---+-----------------------+----+
                         |
                         |  cut release
                         v
                    +----+-------------------+
                    |     release/x.y.z      |
                    +------------------------+
```

可以记一句话：

> **功能走 `feature → develop`，
> 发版走 `release → main + Tag`，
> 线上问题走 `hotfix` 并回补 `develop`。**

---

## 四、Tag 规范与版本号规则

### 4.1 Tag 的本质与作用

1. Tag 是给某个 commit 起的一个“固定名字”，用于标记一个稳定版本快照。
2. Tag 不会复制代码，不会创建新分支，只是一个静态指针。
3. 所有可用于生产环境的版本，都必须有对应的 Tag。

### 4.2 Tag 命名：语义化版本

统一使用语义化版本号：

```text
v主版本号.次版本号.修订号

示例：v1.0.0、v1.0.1、v1.1.0、v2.0.0
```

1. **主版本号（Major）**

    * 有不兼容变更、架构级调整、协议变更时 +1。
2. **次版本号（Minor）**

    * 新增较大功能、模块时 +1。
3. **修订号（Patch）**

    * Bug 修复、小功能、小改动时 +1。

### 4.3 Tag 使用规范

1. **Tag 必须打在 `main` 上**，代表“这一刻的 main 是对外承诺版本”。
2. 正式发版时：

    1. 确认 `main` 上已合入对应 `release/x.y.z`。
    2. 在 `main` 上创建 Tag：

       ```bash
       git checkout main
       git pull
       git tag -a v1.2.0 -m "版本说明：xxx"
       git push origin v1.2.0
       ```
3. 每个 Tag 对应：

    1. 一套固定的源代码
    2. 一套完整的 Flyway 数据库迁移脚本状态（目标结构）
    3. 一份可重现的构建产物（JAR / Docker 镜像）

### 4.4 基于旧版本的维护分支（可选）

1. 某些大客户长期停留在旧版本（例如 v1.0.0），但需要持续修 bug。

2. 可以基于 Tag 拉维护分支：

   ```bash
   git checkout v1.0.0          # 切到该 Tag 对应提交（detached HEAD）
   git checkout -b maint/1.0.x  # 从这里拉出维护分支
   git push -u origin maint/1.0.x
   ```

3. 后续 `v1.0.1 / v1.0.2` 等都在 `maint/1.0.x` 上演化。

---

## 五、首次落地步骤（新仓库初始化）

针对一个**刚起步 / 已经上线首版但还没规范起来**的仓库，建议步骤如下：

1. **确认 main 上的代码就是首个稳定版本**

    * 例如：当前 main 上的代码已用于上线。

2. **在 main 上打首个 Tag `v1.0.0`**

   ```bash
   git checkout main
   git pull
   git tag -a v1.0.0 -m "首个稳定版本 v1.0.0"
   git push origin v1.0.0
   ```

3. **从 main 拉出 develop 分支**

   ```bash
   git checkout -b develop
   git push -u origin develop
   ```

4. **之后所有新功能：一律从 develop 开 `feature/*` 分支开发。**

---

## 六、日常开发流程示例（从需求到合并）

以“新增投资者收益统计功能”为例，完整动作如下：

1. 从 `develop` 拉出功能分支：

   ```bash
   git checkout develop
   git pull
   git checkout -b feature/investor-income-summary
   git push -u origin feature/investor-income-summary
   ```

2. 在 `feature/investor-income-summary` 上开发、提交：

   ```bash
   # 编辑代码...
   git status
   git add .
   git commit -m "feat: 新增投资者收益汇总接口"
   git push
   ```

3. 提交 PR，将 `feature/investor-income-summary` 合并到 `develop`。

4. 合并完成后：

   ```bash
   git branch -d feature/investor-income-summary
   git push origin --delete feature/investor-income-summary
   ```

---

## 七、发版流程示例（从 develop 到 Tag）

以“准备发 `v1.2.0` 版本”为例：

1. 确认要包含的功能都已合并到 `develop`。

2. 从 `develop` 切出 `release/1.2.0`：

   ```bash
   git checkout develop
   git pull
   git checkout -b release/1.2.0
   git push -u origin release/1.2.0
   ```

3. 在 `release/1.2.0` 上只做：

    1. Bug 修复
    2. 文案 / 配置微调

4. 测试通过后：

    1. 合并到 `main`：

       ```bash
       git checkout main
       git pull
       git merge --no-ff release/1.2.0
       ```
    2. 在 `main` 上打 Tag：

       ```bash
       git tag -a v1.2.0 -m "版本 v1.2.0：说明..."
       git push origin v1.2.0
       ```
    3. 再合回 `develop`：

       ```bash
       git checkout develop
       git pull
       git merge --no-ff release/1.2.0
       ```
    4. 删除 `release/1.2.0` 分支：

       ```bash
       git branch -d release/1.2.0
       git push origin --delete release/1.2.0
       ```

---

## 八、线上 hotfix 流程示例

以“修复余额冻结异常”为例：

1. 从 `main` 拉出 hotfix 分支：

   ```bash
   git checkout main
   git pull
   git checkout -b hotfix/fix-charge-balance-freeze
   git push -u origin hotfix/fix-charge-balance-freeze
   ```

2. 修复代码、测试通过后：

    1. 合回 `main` + 打 Tag：

       ```bash
       git checkout main
       git pull
       git merge --no-ff hotfix/fix-charge-balance-freeze
       git tag -a v1.2.1 -m "修复余额冻结异常问题"
       git push origin v1.2.1
       ```

    2. 合回 `develop`：

       ```bash
       git checkout develop
       git pull
       git merge --no-ff hotfix/fix-charge-balance-freeze
       ```

    3. 删除分支：

       ```bash
       git branch -d hotfix/fix-charge-balance-freeze
       git push origin --delete hotfix/fix-charge-balance-freeze
       ```

---

## 九、注意事项与共识

1. **不要在 main 上直接开发功能**

    * main 只接收 `release/*`、`hotfix/*` 合并。

2. **develop 只初始化一次**

    * 后续版本迭代不要再从 Tag 重建 develop。

3. **feature 分支用完必须删**

    * 防止分支列表变成“垃圾场”。

4. **Tag 一旦发布，默认视为不可随便修改**

    * 如需修正版本，建议用下一个 Patch 号（例如从 `v1.2.0` 升 `v1.2.1`）。

5. **线上问题一定要走 hotfix + 回补 develop**

    * 避免同一个坑在下个版本再出现一次。

---