#Duo-RISC

本代码现在已是一个较稳定的MIPS流水线状态机，支持的指令至少在测试范围内没有问题。已下板验证。
由于代码是从[`riscv-sodor`](https://github.com/ucb-bar/riscv-sodor)改来的，业务逻辑完全兼容RISCV，只需添加译码即可支持RISCV。因此该核心命名为Duo-RISC。

##Roadmap/Milestones
__添加中断__：现阶段已有`if_kill`机制，该机制会在下一个`pc`不是`pc+4`时触发，因此添加中断需做两个更改。

- 接口引入中断信号，修改触发`if_kill`的条件，使得有内部/外部中断时都触发`if_kill`。若有内/外部中断请求，则，`epc := next_pc`(`next_pc`可能是正常的`pc+4`也可能是一个跳转地址)，并且`next_pc := 中断跳转地址`。
- 第一阶段，添加硬中断。先定义好中断跳转地址（在constant.scala中)，在那块区域填满`eret`。写测试代码，分别在1,2,....,n周期时产生外部中断。调试逻辑使得核心能够正确运行。
- 第二阶段，添加软中断。添加`syscall`译码单元，或者直接判断指令寄存器是syscall即可(因为syscall指令每次都一样，硬编码即可)。程序中随意插入syscall，测试能否正常运行。
- 第三阶段，添加cause寄存器。
- 可参考[`rocket`](https://github.com/ucb-bar/rocket)是如何实现中断的。

__完善接口__：根据接口定义约定，升级各接口。一共定义了三种模块间信号互连协议。

- *Link*: 无协议连接，数据线直接相连即可。
- *Bus*: 复杂协议连接，本项目暂时参照Wishbone总线。
- *Port*: 简单协议连接。每条数据/地址线都伴随有valid和enable使能信号。

1. 工作只需将部分接口修改成*Port*协议，现阶段需修改的地方在源码中已用`TODO: MEMIFCE`标注出。修改完将其改成`NOTE: MEMIFCE`，无法修改的地方就保留原注释不改，待以后解决。具体请参考[`riscv-sodor`](https://github.com/ucb-bar/riscv-sodor)或者[`rocket`](https://github.com/ucb-bar/rocket)中的`DecoupledIO`以及`ValidWrapper`。
+ 给现阶段的DCache添加延时，验证设计是否正确。
+ 给ICache创造随机的miss情景，验证设计是否正确。

__添加指令__: 直接在译码模块后面加上新的指令即可。做好新添加指令的测试工作。

__双指令集__: 两步/种实现方式。本流水线核心与`RISCV`完全兼容，只需加入现成的译码逻辑即可。
源码涉及到的部分已经用`TODO: RISCV`标出。

1. 生成的硬件核心只支持RISCV或者MIPS中的一种：只需在constant.scala里添加一个设置变量，每次根据这个变量判断生成相应核心。代码中用的是`if`。
+ 同时支持两种指令集：
  1. 定义一条指令集切换指令，可以是直接切换，或者学ARM的`BLX`。是否需清空流水线？
  2. 还需添加一个“正在运行的指令集状态寄存器”，从流水线头传递到尾。应该用`when`。
  
__外部设备代码库__: 将外设的驱动代码迁移到另一个代码仓库，本仓库通过git-submodule调用之。
已有支持：

1. 25Mhz(50Mhz) 模拟输出
+ 数码管

添加外设支持：

1. 按钮，开关
+ PS/2
+ 50Mhz 模拟输出
+ Light Emitting Diode
+ 燧石宝剑板：
  1. 移位寄存器及其应用：数码管，Light Emitting Diode，开关
  + 4x4扫描键盘

__实用工具__: 燧石宝剑板需一些能够把数据写到SDRAM以及Flash中的小工具。

##Git branch model
请务必理解[这个链接](https://www.atlassian.com/git/tutorials/comparing-workflows)里的内容。  
还需商讨的地方：

1. `git pull --rebase origin master`或者`git config --global/local pull.rebase true`[^ref1]
+  `git merge --no-ff`[^ref2]·[^ref3]
+ 两个开发者先后push，会造成remote产生一个merge

[^ref1]: https://coderwall.com/p/tnoiug/rebase-by-default-when-doing-git-pull
[^ref2]: http://stackoverflow.com/questions/9069061/what-is-the-difference-between-git-merge-and-git-merge-no-ff
[^ref3]: https://sandofsky.com/blog/git-workflow.html