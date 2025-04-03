# LB_Security
Language -Based Security Lab and Project

## lab1-TOCTOU

start版本是补全代码后的版本，有toctou漏洞。fixed版本是修复后的版本，让利用toctou漏洞攻击无效。

make all：编译代码
make clean：清理编译出的文件
make resetFiles：重置pocket.txt(存储已经购买的物品)和wallet.txt(存储余额)
java ShoppingCart:启动程序

start版本测试（利用toctou漏洞攻击）：
开启两个终端，终端a选择购买car，此时人为设置了5秒延迟，延迟后最终的剩余余额才会被写入wallet.txt。所以，5秒内在终端b购买另一件物品，比如book。
最终由于时间差，终端a将写入余额0，但随后终端b写入余额29900，覆盖掉终端a的写入。

fixed版本测试（测试漏洞有没有被修复）
重复上面的操作，发现终端b会直接提醒余额不足。
