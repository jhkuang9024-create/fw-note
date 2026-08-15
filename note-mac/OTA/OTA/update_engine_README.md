# Chrome OS Update Process

[TOC]

System updates in more modern operating systems like Chrome OS and Android are
called A/B updates, over-the-air ([OTA]) updates, seamless updates, or simply
auto updates. In contrast to more primitive system updates (like Windows or
macOS) where the system is booted into a special mode to override the system
partitions with newer updates and may take several minutes or hours, A/B updates
have several advantages including but not limited to:

*   Updates maintain a workable system that remains on the disk during and after
    an update. Hence, reducing the likelihood of corrupting a device into a
    non-usable state. And reducing the need for flashing devices manually or at
    repair and warranty centers, etc.
*   Updates can happen while the system is running (normally with minimum
    overhead) without interrupting the user. The only downside for users is a
    required reboot (or, in Chrome OS, a sign out which automatically causes a
    reboot if an update was performed where the reboot duration is about 10
    seconds and is no different than a normal reboot).
*   The user does not need (although they can) to request for an update. The
    update checks happen periodically in the background.
*   If the update fails to apply, the user is not affected. The user will
    continue on the old version of the system and the system will attempt to
    apply the update again at a later time.
*   If the update applies correctly but fails to boot, the system will rollback
    to the old partition and the user can still use the system as usual.
*   The user does not need to reserve enough space for the update. The system
    has already reserved enough space in terms of two copies (A and B) of a
    partition. The system doesn’t even need any cache space on the disk,
    everything happens seamlessly from network to memory to the inactive
    partitions.

## Life of an A/B Update

在支持A/B更新的系统中，每个分区（如内核或根分区（或其他工件，如[DLC]））都有两个副本。我们将这两个副本分别称为活动（A）和非活动（B）。系统会引导至活动分区（具体取决于启动时哪个副本具有更高优先级），当有新更新可用时，更新会被写入非活动分区。成功重启后，之前非活动的分区将变为活动分区，而原先的活动分区则变为非活动分区。

### Generation

但一切都要从在（谷歌）服务器上为每个新的系统镜像生成OTA包开始。这是通过调用带有源构建和目标构建的[ota_from_target_files](https://cs.android.com/android/platform/superproject/+/master:build/make/tools/releasetools/ota_from_target_files.py)脚本实现的。此脚本需要target_file.zip才能运行，仅提供镜像文件是不够的。

### Distribution/Configuration
一旦生成OTA包，就会使用特定密钥对其进行签名，并将其存储在更新服务器（GOTA）已知的位置。
然后，GOTA将通过公共URL使此OTA包可访问。运营商可以选择仅向特定设备子集提供此OTA更新。

### Installation
当设备的更新客户端启动更新（无论是定期更新还是用户主动更新）时，它会首先查阅不同的设备策略，以确定是否允许进行更新检查。例如，设备策略可能禁止在一天中的特定时间段进行更新检查，或者要求更新检查时间随机分散在一天中的不同时段，等等。

一旦策略允许更新检查，更新客户端就会向更新服务器发送请求（所有这些通信均通过HTTPS进行），并标识其参数，如应用程序ID、硬件ID、版本、板卡等。

服务器上的一些策略可能会阻止设备获取特定的OTA更新，这些服务器端策略通常由运营商设置。例如，运营商可能希望仅向部分设备推送软件的测试版。

但如果更新服务器决定提供更新载荷，它将返回执行更新所需的所有参数，如下载载荷的URL、元数据签名、载荷大小和哈希值等。更新客户端在不同的状态变化后继续与更新服务器通信，例如报告它已开始下载载荷或已完成更新，或报告更新失败并附带特定错误代码等。

然后，设备将开始实际安装OTA更新。这大致包括3个步骤。
#### Download & Install
每个有效载荷主要由两部分组成：元数据和额外数据。元数据基本上是一系列应为更新执行的操作。额外数据则包含部分或所有这些操作所需的数据块。更新客户端首先下载元数据，并使用更新服务器响应中提供的签名对其进行加密验证。一旦元数据被验证为有效，有效载荷的其余部分就可以轻松地进行加密验证（主要通过SHA256哈希）。

接下来，更新客户端将非活动分区标记为不可启动（因为它需要将新的更新写入其中）。此时，系统无法再回滚到非活动分区。

然后，更新客户端按照元数据中定义的操作顺序执行这些操作，并在这些操作需要其数据时逐步下载其余的有效载荷。一旦某个操作完成，其数据就会被丢弃。这样，在应用有效载荷之前，就不需要缓存整个有效载荷。在此过程中，更新客户端会定期检查上次执行的操作，以便在发生故障或系统关闭等情况时，可以从上次中断的地方继续执行，而无需从头开始重新执行所有操作。

在下载过程中，更新客户端会对下载的字节进行哈希运算，并在下载完成后检查有效载荷签名（位于有效载荷末尾）。如果签名无法验证，则更新将被拒绝。

#### Hash Verification & Verity Computation

在非活动分区更新后，更新客户端将为每个分区计算前向纠错（也称为FEC，即Verity）码，并将计算出的验证数据写入非活动分区。在某些更新中，验证数据包含在额外数据中，因此此步骤将被跳过。

然后，重新读取整个分区，对其进行哈希运算，并将其与元数据中传递的哈希值进行比较，以确保更新已成功写入分区。此步骤中计算的哈希值包含上一步中写入的校验码。

#### Postintall

接下来，会调用[Postinstall]脚本（如果有的话）。从OTA（Over-the-Air，无线升级）的角度来看，这些后安装脚本只是黑盒。通常，后安装脚本会优化手机上的现有应用程序并运行文件系统垃圾收集，以便设备在OTA后能够快速启动。但这些是由其他团队管理的。

#### Finishing Touches

然后，更新客户端会进入一个状态，表明更新已完成，用户需要重新启动系统。此时，在用户重新启动系统（或注销）之前，即使有新的更新可用，更新客户端也不会再进行任何系统更新。不过，它会继续执行定期更新检查，以便我们能够统计现场活动设备的数量。

更新成功后，非活动分区将被标记为具有更高优先级（在启动时，具有更高优先级的分区将首先启动）。一旦用户重新启动系统，它将引导至已更新的分区，并将该分区标记为活动分区。此时，在重新启动后，[update_verifier](https://cs.android.com/android/platform/superproject/+/master:bootable/recovery/update_verifier/)程序将运行，读取所有dm-verity设备以确保分区未损坏，然后将更新标记为成功。

至此，A/B更新被视为完成。虚拟A/B更新在此之后将进行一个额外的步骤，称为“合并”。合并通常需要几分钟时间，之后虚拟A/B更新即被视为完成。

## Update Engine Daemon

`update_engine`是一个始终运行的单线程守护进程。该进程是自动更新的核心。它在后台以较低的优先级运行，并且是系统启动后最后启动的进程之一。不同的客户端（如GMS Core或其他服务）可以向更新引擎发送更新检查请求。请求如何传递给更新引擎的细节取决于系统，但在Chrome OS中是通过D-Bus传递的。有关所有可用方法的列表，请查看[D-Bus接口]。在Android中，则是通过binder传递。

更新引擎中嵌入了许多弹性功能，使自动更新更加稳健，这些功能包括但不限于：

* 如果更新引擎崩溃，它将自动重启。
* 在主动更新过程中，它会定期检查更新的状态，如果更新失败或中途崩溃，它将从上一个检查点继续。
* 它会对失败的网络通信进行重试。
* 如果多次尝试应用增量载荷失败（由于活动分区上的位变化），它将切换到完整载荷。

更新客户端将其活动偏好设置写入 `/data/misc/update_engine/prefs`。这些偏好设置有助于在更新客户端的生命周期内跟踪更改，并允许在尝试失败或崩溃后正确继续更新过程。



### Interactive vs Non-Interactive vs. Forced Updates

非交互式更新是由更新引擎定期计划并在后台进行的更新。而交互式更新则是当用户明确请求更新检查时（例如，在Chrome操作系统的“关于”页面中点击“检查更新”按钮）进行的更新。根据更新服务器的策略，交互式更新（通过携带标记提示）的优先级高于非交互式更新。如果服务器负载繁忙等，它们可能会决定不提供更新。这两种类型的更新之间还有其他内部差异。例如，交互式更新会尝试更快地安装更新。

强制更新与交互式更新（由某种用户操作发起）类似，但它们也可以配置为非交互式。由于非交互式更新会定期进行，因此强制非交互式更新会在请求时立即执行非交互式更新，而不是在稍后时间。我们可以这样调用强制非交互式更新：

```bash
update_engine_client --interactive=false --check_for_update
```

### Network

更新客户端能够根据设备所连接的网络类型（以太网、WiFi或蜂窝网络）来下载有效载荷。由于通过蜂窝网络下载可能会消耗大量数据，因此会提示用户授予相应权限。

### Logs

在Chrome操作系统中，`update_engine`日志位于`/var/log/update_engine`目录中。每当`update_engine`启动时，它都会以当前日期时间格式在日志文件名中创建一个新的日志文件（`update_engine.log-DATE-TIME`）。在更新引擎重启几次或系统重新启动后，可以在`/var/log/update_engine`中看到许多日志文件。最新的活动日志被符号链接到`/var/log/update_engine.log`。

在Android系统中，`update_engine`的日志文件位于`/data/misc/update_engine_log`目录下。
## Update Payload Generation

更新载荷生成是将一组分区/文件转换为一种格式的过程，该格式既能让更新客户端（尤其是版本较旧时）理解，又能安全验证。此过程包括将输入分区分解为更小的组件并进行压缩，以便在下载载荷时节省网络带宽。

`delta_generator`是一个工具，具有生成不同类型更新载荷的广泛选项。其代码位于`update_engine/payload_generator`中。此目录包含与生成更新载荷机制相关的所有源代码。除了`delta_generator`之外，此目录中的任何文件都不应包含或用于任何其他库/可执行文件，这意味着此目录不会被编译到更新引擎工具的其他部分中。

然而，不建议直接使用`delta_generator`，因为它包含太多标志。应使用像[ota_from_target_files](https://cs.android.com/android/platform/superproject/+/master:build/make/tools/releasetools/ota_from_target_files.py)或[OTA Generator](https://github.com/google/ota-generator)这样的封装工具。

### Update Payload File Specification

Each update payload file has a specific structure defined in the table below:

| Field                   | Size (bytes) | Type                                 | Description                                                                                                                   |
| ----------------------- | ------------ | ------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------- |
| Magic Number            | 4            | char[4]                              | Magic string "CrAU" identifying this is an update payload.                                                                    |
| Major Version           | 8            | uint64                               | Payload major version number.                                                                                                 |
| Manifest Size           | 8            | uint64                               | Manifest size in bytes.                                                                                                       |
| Manifest Signature Size | 4            | uint32                               | Manifest signature blob size in bytes (only in major version 2).                                                              |
| Manifest                | Varies       | [DeltaArchiveManifest]               | The list of operations to be performed.                                                                                       |
| Manifest Signature      | Varies       | [Signatures]                         | The signature of the first five fields. There could be multiple signatures if the key has changed.                            |
| Payload Data            | Varies       | List of raw or compressed data blobs | The list of binary blobs used by operations in the metadata.                                                                  |
| Payload Signature Size  | Varies       | uint64                               | The size of the payload signature.                                                                                            |
| Payload Signature       | Varies       | [Signatures]                         | The signature of the entire payload except the metadata signature. There could be multiple signatures if the key has changed. |

### Delta vs. Full Update Payloads

有效载荷有两种类型：完整有效载荷和增量有效载荷。完整有效载荷仅由目标映像（我们想要更新的映像）生成，并包含更新非活动分区所需的所有数据。因此，完整有效载荷的大小可能相当大。另一方面，增量有效载荷是通过比较源映像（活动分区）和目标映像，并生成这两幅映像之间的差异而生成的差异更新。它基本上是一种类似于`diff`或`bsdiff`等应用程序的差异更新。因此，使用增量有效载荷更新系统需要系统读取活动分区的一部分，以更新非活动分区（或重建目标分区）。增量有效载荷的大小明显小于完整有效载荷。两种类型的有效载荷结构相同。

有效载荷生成需要大量资源，其工具以高并行性实现。

#### Generating Full Payloads

完整有效载荷是通过将分区划分为2MiB（可配置）大小的块来生成的，然后根据哪种算法产生的数据更小，使用bzip2或XZ算法对其进行压缩，或者保持原始数据。与增量有效载荷相比，完整有效载荷要大得多，因此在网络带宽有限的情况下需要更长的下载时间。另一方面，由于系统无需从源分区读取数据，因此应用完整有效载荷的速度会稍快一些。

#### Generating Delta Payloads

增量有效载荷是通过在文件和元数据（更准确地说，是在每个相应分区上的文件系统级别）上查看源图像和目标图像数据来生成的。我们之所以能够生成增量有效载荷，是因为Chrome OS分区是只读的。因此，我们可以高度确定地认为，客户端设备上的活动分区与图像生成/签名阶段生成的原始分区逐位相同。生成增量有效载荷的过程大致如下：

1. 找到目标分区上的所有零填充块，并为其生成“ZERO”操作。“ZERO”操作基本上会丢弃相关联的块（具体取决于实现方式）。
2. 通过直接一对一比较源分区和目标分区的块，找出源分区和目标分区之间未发生变化的块，并生成“SOURCE_COPY”操作。
3. 列出源分区和目标分区中的所有文件（及其关联的块），并删除我们在最后两个步骤中已经生成操作过的块（和文件）。将每个分区中剩余的元数据（如索引节点等）分配给一个文件。
4. 如果文件是新的，则根据哪个操作生成的数据块更小，为其数据块生成`REPLACE`、`REPLACE_XZ`或`REPLACE_BZ`操作。
5. 对于每个其他文件，比较源块和目标块，并根据哪一个生成的数据blob更小，生成`SOURCE_BSDIFF`或`PUFFDIFF`操作。这两个操作会生成源数据blob和目标数据blob之间的二进制差异。（有关此类二进制差异程序的详细信息，请参阅[bsdiff]和[puffin]！）6. 根据操作的目标分区块偏移量对其进行排序。
7. 可选择将相邻的相同或相似操作合并为更大的操作，以提高效率并可能减小有效载荷。

完整载荷只能包含`REPLACE`、`REPLACE_BZ`和`REPLACE_XZ`操作。增量载荷则可以包含任何操作。

### Major and Minor versions

主要版本和次要版本分别指定了更新载荷文件格式以及更新客户端接受特定类型更新载荷的能力。这些数字在更新客户端中是[硬编码]的。

主版本号基本上是指上述[更新载荷文件规范]中指定的更新载荷文件版本（第二个字段）。每个更新客户端支持一系列主版本号。目前，只有两个主版本号：1和2。Chrome OS和Android都使用主版本号2（主版本号1正在被弃用）。每当有新内容无法纳入[Manifest Protobuf]时，我们就需要提升主版本号。提升主版本号应极其谨慎，因为旧客户端不知道如何处理新版本。Chrome OS中的任何主版本号提升都应与GoldenEye过渡版本相关联。

次版本定义了更新客户端接受特定操作或执行特定动作的能力。每个更新客户端支持一系列次版本。例如，次版本为4（或更低）的更新客户端不知道如何处理`PUFFDIFF`操作。因此，在为具有次版本为4（或更低）的更新客户端的镜像生成增量载荷时，我们无法为其生成PUFFDIFF操作。载荷生成过程会查看源镜像的次版本，以确定其支持的操作类型，并且只生成符合这些限制的载荷。同样，如果特定次版本的客户端存在错误，则升级次版本有助于避免生成导致该错误显现的载荷。然而，升级次版本在可维护性方面也相当昂贵，并且容易出错。因此，在进行此类更改时，应谨慎行事。

在完整有效载荷中，次要版本无关紧要。完整有效载荷应始终能够应用于非常旧的客户端。原因是更新客户端可能不会发送其当前版本，因此如果我们有不同类型的完整有效载荷，我们将无法确定向客户端提供哪个版本。

### Signed vs Unsigned Payloads

更新载荷可以（使用私钥/公钥对）进行签名以用于生产环境，或者保持未签名以用于测试环境。像`delta_generator`这样的工具可以帮助生成元数据和载荷哈希，或者使用给定的私钥对载荷进行签名。

## update_payload Scripts

[update_payload]包含一组主要用于验证载荷生成和应用的Python脚本。我们通常使用实际设备（现场测试）来测试更新载荷。`brillo_update_payload`脚本可用于在主机设备上生成载荷并测试其应用。这些测试可以视为动态测试，无需实际设备。其他`update_payload`脚本（如`check_update_payload`）可用于静态检查载荷是否处于正确状态，以及其应用是否正常工作。这些脚本实际上是在静态状态下应用载荷，而不运行payload_consumer中的代码。

## Postinstall

[Postinstall] 是在更新客户端将新的镜像工件写入非活动分区后调用的一种进程。后安装的主要职责之一是在根分区末尾重新创建dm-verity树哈希。除此之外，它还负责安装新的固件更新或执行任何特定于主板的进程。后安装在新安装的分区内以单独的chroot环境运行。因此，它与正在运行的系统其他部分是相对隔离的。任何需要在更新后设备重启前完成的操作，都应在Postinstall中实现。

## Building Update Engine

你可以像构建其他平台应用程序一样构建`update_engine`：

### Setup

在构建任何内容之前，请在Android存储库的顶部运行这些命令。
每个壳体你只需要做一次。

* `source build/envsetup.sh` * `lunch aosp_cf_x86_64_only_phone-userdebug`（或者将 aosp_cf_x86_64_only_phone-userdebug 替换为您自己的目标）


### Building

`m update_engine update_engine_client delta_generator`

## Running Unit Tests

[运行与其他平台类似的单元测试]：

* `atest update_engine_unittests` 您需要一台与笔记本电脑相连且可通过ADB访问的设备来进行此操作。Cuttlefish（一种模拟器）也可以使用。
* `atest update_engine_host_unittests` 在主机上运行部分测试，无需设备。

## Initiating a Configured Update

启动更新的方法有多种：

* 点击设置中“关于”页面的“检查更新”按钮。无法配置这种更新检查方式。
* 使用[`scripts/update_device.py`]程序，并传入您的OTA压缩文件的路径。



## Note to Developers and Maintainers

在更改更新引擎源代码时，请特别注意以下事项：

### Do NOT Break Backward Compatibility

在每个发布周期，我们都应能够生成完整和差异化的有效载荷，这些载荷能够正确应用于运行旧版更新引擎客户端的旧设备。例如，在元数据原型文件中删除或不传递参数可能会破坏旧客户端。或者传递旧客户端无法理解的操作也会破坏它们。每当在有效载荷生成过程中进行任何更改时，都要问自己这样一个问题：它能在旧客户端上运行吗？如果不能，我是否需要用次要版本或其他任何方式来控制它。

特别是在企业回滚方面，较新的更新客户端应能够接受较旧的更新载荷。通常，这通过使用完整载荷来实现，但应注意不要破坏这种兼容性。

### Think About The Future

在更新引擎中做出变更时，要考虑到5年后的情形：

* 如何实施变革，以确保五年后老客户不会流失？
* 五年后它将如何维护？
* 它如何能在不破坏旧客户端或产生高额维护成本的情况下，使未来的变更更加容易？

### Prefer Not To Implement Your Feature In The Updater Client
如果一个功能可以从服务器端实现，请不要在客户端更新程序中实现。因为客户端更新程序在某些时候可能很脆弱，一个小错误就可能带来灾难性的后果。例如，如果在更新程序客户端中引入了一个错误，导致它在检查更新之前就崩溃了，而我们无法在发布过程的早期发现这个错误，那么已经迁移到新错误系统的生产设备可能就无法再接收自动更新了。因此，请始终思考，正在实现的功能是否可以从服务器端实现（可能只需对客户端更新程序进行最小的更改）？或者，是否可以将该功能转移到另一个服务中，使其与更新程序客户端的接口最小化。回答这些问题将在未来带来巨大的回报。

### Be Respectful Of Other Code Bases

~~当前的更新引擎代码库被用于许多项目，如Android。~~~

Android和ChromeOS的代码库已正式分离。

我们经常在这两个项目之间同步代码库。请尽量避免破坏Android或其他共享更新引擎代码的系统。每当提交更改时，请始终考虑Android是否需要该更改：

* 它将对Android产生怎样的影响？
* 是否可以将更改移至接口，并实现存根实现，从而不影响Android？
* Chrome OS或Android特定的代码能否通过宏来保护？

作为一项基本措施，在添加/移除/重命名代码时，请务必同时更改`build.gn`和`Android.bp`。请勿将Chrome OS特定的代码（例如`system_api`或`dlcservice`中的其他库）引入update_engine的通用代码中。请尝试使用最佳软件工程实践将这些关注点分开。

### Merging from Android (or other code bases)

Chrome OS将Android代码作为一个[上游分支]进行跟踪。要将Android代码合并到Chrome OS（反之亦然），只需将该分支通过`git merge`操作合并到Chrome OS中，使用任何方法进行测试，然后上传合并提交。

```bash
repo start merge-aosp
git merge --no-ff --strategy=recursive -X patience cros/upstream
repo upload --cbr --no-verify .
```

[Postinstall]: #postinstall
[update payload file specification]: #update-payload-file-specification
[OTA]: https://source.android.com/devices/tech/ota
[DLC]: https://chromium.googlesource.com/chromiumos/platform2/+/master/dlcservice
[`chromeos-setgoodkernel`]: https://chromium.googlesource.com/chromiumos/platform2/+/master/installer/chromeos-setgoodkernel
[D-Bus interface]: /dbus_bindings/org.chromium.UpdateEngineInterface.dbus-xml
[this repository]: /
[UpdateManager]: /update_manager/update_manager.cc
[update_manager]: /update_manager/
[P2P update related code]: https://chromium.googlesource.com/chromiumos/platform2/+/master/p2p/
[`cros_generate_update_payloads`]: https://chromium.googlesource.com/chromiumos/chromite/+/master/scripts/cros_generate_update_payload.py
[`chromite/lib/paygen`]: https://chromium.googlesource.com/chromiumos/chromite/+/master/lib/paygen/
[DeltaArchiveManifest]: /update_metadata.proto#302
[Signatures]: /update_metadata.proto#122
[hard coded]: /update_engine.conf
[Manifest protobuf]: /update_metadata.proto
[update_payload]: /scripts/
[Postinstall]: https://chromium.googlesource.com/chromiumos/platform2/+/master/installer/chromeos-postinst
[`update_engine` protobufs]: https://chromium.googlesource.com/chromiumos/platform2/+/master/system_api/dbus/update_engine/
[Running unit tests similar to other platforms]: https://chromium.googlesource.com/chromiumos/docs/+/master/testing/running_unit_tests.md
[Nebraska]: https://chromium.googlesource.com/chromiumos/platform/dev-util/+/master/nebraska/
[upstream branch]: https://chromium.googlesource.com/aosp/platform/system/update_engine/+/upstream
[`cros flash`]: https://chromium.googlesource.com/chromiumos/docs/+/master/cros_flash.md
[bsdiff]: https://android.googlesource.com/platform/external/bsdiff/+/master
[puffin]: https://android.googlesource.com/platform/external/puffin/+/master
[`update_engine_client`]: /update_engine_client.cc
[`brillo_update_payload`]: /scripts/brillo_update_payload
[`check_update_payload`]: /scripts/paycheck.py
[Dev Server]: https://chromium.googlesource.com/chromiumos/chromite/+/master/docs/devserver.md
