# zhku java高级编程期末考核-实现kv型数据库

## 前菜-使用shell实现简单的数据库
使用shell实现增加和查询的功能：
```shell
#!/bin/bash

easy_set() {
        echo "$1,$2" >> easy-db
}

easy_get() {
        grep "^$1," easy-db | sed -e "s/^$1,//" | tail -n 1
}
```
for-test:

```
~/workspace/javaspace/easy-db » source db_shell.sh                                                      urmsone@urmsonedeMacBook-Pro
-------------------------------------------------------------------------------------------------------------------------------------
~/workspace/javaspace/easy-db » easy_set zsy1 value1                                                    urmsone@urmsonedeMacBook-Pro
-------------------------------------------------------------------------------------------------------------------------------------
~/workspace/javaspace/easy-db » easy_set zsy2 value2                                                    urmsone@urmsonedeMacBook-Pro
-------------------------------------------------------------------------------------------------------------------------------------
~/workspace/javaspace/easy-db » easy_set zsy3 value3                                                    urmsone@urmsonedeMacBook-Pro
-------------------------------------------------------------------------------------------------------------------------------------
~/workspace/javaspace/easy-db » easy_get zsy3                                                           urmsone@urmsonedeMacBook-Pro
value3
-------------------------------------------------------------------------------------------------------------------------------------
~/workspace/javaspace/easy-db » cat easy-db                                                             urmsone@urmsonedeMacBook-Pro
zsy1,value1
zsy2,value2
zsy3,value3
-------------------------------------------------------------------------------------------------------------------------------------
```
## 基于内存索引的kv数据库
### 需求分析
数据库的基本功能：
- 增
- 删
- 改
- 查
- 提供client（API）供别人使用（基于C/S架构）

数据库设计：
基于append log的方式实现。
- 参考WAL或MySQL的RedoLog。
- 除了查询操作，增、删、改都可以通过在log最后append一个命令来实现；
- 将增和改合并成一个命令set，删独立一个命令rm，查询独立一个命令get。
- 由于查询操作不需要写磁盘（持久化），我们只需要记录set和rm命令

log的数据结构设计：
通过追加log的方式实现，我们可以记录命令的方式进行，而不是记录原始数据。如：
```
{"key":"zsy1","type":"SET","value":"1"}
{"key":"zsy1","type":"RM","value":"1"}
```
优点：
- 可方便的实现删除的功能（标记删除）。查询的时候，如果查到某个key的数据，type=RM时，数据该数据已被删除
- 可实现Redo的功能（回放），只需要将记录到磁盘的命令重新执行一次即可。

索引问题：
- 基于内存的索引，服务重启时，索引丢失。
- 数据库启动时，需要通过回放功能，把磁盘中的命令redo一次来刷新索引到内存。
- 缺点：数据冷启动时，磁盘数据越大，启动时间越长。
- 上述设计，存命令，仍存在问题，磁盘中没有数据长度，redo操作就无法实现。
```38{"key":"zsy1","type":"SET","value":"1"}
38{"key":"zsy2","type":"RM","value":"1"}
```


API设计：
- 基于C/S架构
- 使用Socket实现
- 也可以使用serverlet实现Restful API（加分）
- 提供命令行Client


优化：
- 日志文件压缩
- 引入内存缓存，实现数据的批量写入
- 实现lsmt

## 实现
### 项目结构
```
~/workspace/javaspace/easy-db » tree                                                                    urmsone@urmsonedeMacBook-Pro
.
├── README.md
├── data
│   └── data.table
├── db_shell.sh
├── easy-db
├── pom.xml
├── src
│   └── main
│       ├── java
│       │   ├── client
│       │   │   ├── Client.java
│       │   │   ├── CmdClient.java
│       │   │   └── SocketClient.java
│       │   ├── controller
│       │   │   ├── Controller.java
│       │   │   ├── SocketServerController.java
│       │   │   └── SocketServerHandler.java
│       │   ├── dto
│       │   │   ├── ActionDTO.java
│       │   │   ├── ActionTypeEnum.java
│       │   │   ├── RespDTO.java
│       │   │   └── RespStatusTypeEnum.java
│       │   ├── example
│       │   │   ├── SocketClientUsage.java
│       │   │   ├── SocketServerUsage.java
│       │   │   └── StoreUsage.java
│       │   ├── model
│       │   │   └── command
│       │   │       ├── AbstractCommand.java
│       │   │       ├── Command.java
│       │   │       ├── CommandPos.java
│       │   │       ├── CommandTypeEnum.java
│       │   │       ├── RmCommand.java
│       │   │       └── SetCommand.java
│       │   ├── service
│       │   │   ├── NormalStore.java
│       │   │   └── Store.java
│       │   └── utils
│       │       ├── CommandUtil.java
│       │       ├── LoggerUtil.java
│       │       └── RandomAccessFileUtil.java
│       └── resources
│           └── log4j.properties
└── target

```


### java中数据命令的定义如下
rm:
```java
@Setter
@Getter
public class RmCommand extends AbstractCommand {
    private String key;

    public RmCommand(String key) {
        super(CommandTypeEnum.RM);
        this.key = key;
    }
}
```
set:
```java
@Setter
@Getter
public class SetCommand extends AbstractCommand {
    private String key;

    private String value;

    public SetCommand(String key, String value) {
        super(CommandTypeEnum.SET);
        this.key = key;
        this.value = value;
    }
}

```

### 数据写入
```java
public class NormalStore implements Store {
    /**
     * 内存表，类似LRU缓存，有序结构
     */
    private TreeMap<String, Command> memTable;

    /**
     * hash索引，存的是数据长度和偏移量
     * */
    private HashMap<String, CommandPos> index;

    /**
     * 数据目录
     */
    private final String dataDir;

    /**
     * 读写锁，支持多线程，并发安全写入
     */
    private final ReadWriteLock indexLock;

    /**
     * 暂存数据的日志句柄
     */
    private RandomAccessFile writerReader;

    /**
     * 持久化阈值
     */
    private final int storeThreshold;
}

@Setter
@Getter
public class CommandPos {
    private int pos;
    private int len;

    public CommandPos(int pos, int len) {
        this.pos = pos;
        this.len = len;
    }
}
```
- NormalStore：进行数据操作的业务逻辑实现类
- HashMap<String, CommandPos> index：HashMap索引

```java
public class NormalStore implements Store {
    @Override
    public void set(String key, String value) {
        try {
            SetCommand command = new SetCommand(key, value);
            byte[] commandBytes = JSONObject.toJSONBytes(command);
            // 加锁
            indexLock.writeLock().lock();
            // TODO://先写内存表，内存表达到一定阀值再写进磁盘
            // 写table（wal）文件
            RandomAccessFileUtil.writeInt(this.genFilePath(), commandBytes.length);
            int pos = RandomAccessFileUtil.write(this.genFilePath(), commandBytes);
            // 保存到memTable
            // 添加索引
            CommandPos cmdPos = new CommandPos(pos, commandBytes.length);
            index.put(key, cmdPos);
            // TODO://判断是否需要将内存表中的值写回table
        } catch (Throwable t) {
            throw new RuntimeException(t);
        } finally {
            indexLock.writeLock().unlock();
        }
    }
}
```
删除操作类似：
```java
public class NormalStore implements Store {
    @Override
    public void rm(String key) {
        try {
            RmCommand command = new RmCommand(key);
            byte[] commandBytes = JSONObject.toJSONBytes(command);
            // 加锁
            indexLock.writeLock().lock();
            // TODO://先写内存表，内存表达到一定阀值再写进磁盘

            // 写table（wal）文件
            int pos = RandomAccessFileUtil.write(this.genFilePath(), commandBytes);
            // 保存到memTable

            // 添加索引
            CommandPos cmdPos = new CommandPos(pos, commandBytes.length);
            index.put(key, cmdPos);

            // TODO://判断是否需要将内存表中的值写回table

        } catch (Throwable t) {
            throw new RuntimeException(t);
        } finally {
            indexLock.writeLock().unlock();
        }
    }

}
```

数据读取
```java
public class NormalStore implements Store {
    @Override
    public String get(String key) {
        try {
            indexLock.readLock().lock();
            // 从索引中获取信息
            CommandPos cmdPos = index.get(key);
            if (cmdPos == null) {
                return null;
            }
            byte[] commandBytes = RandomAccessFileUtil.readByIndex(this.genFilePath(), cmdPos.getPos(), cmdPos.getLen());

            JSONObject value = JSONObject.parseObject(new String(commandBytes));
            Command cmd = CommandUtil.jsonToCommand(value);
            if (cmd instanceof SetCommand) {
                return ((SetCommand) cmd).getValue();
            }
            if (cmd instanceof RmCommand) {
                return null;
            }

        } catch (Throwable t) {
            throw new RuntimeException(t);
        } finally {
            indexLock.readLock().unlock();
        }
        return null;
    }
    
}
```

由于索引没有持久化，因此，当数据库启动时，都需要从磁盘中进行redo操作，刷新索引到内存（引入冷启动问题）。
```java
public class NormalStore implements Store {
    public void reloadIndex() {
        try {
            RandomAccessFile file = new RandomAccessFile(this.genFilePath(), RW_MODE);
            long len = file.length();
            long start = 0;
            file.seek(start);
            while (start < len) {
                int cmdLen = file.readInt();
                byte[] bytes = new byte[cmdLen];
                file.read(bytes);
                JSONObject value = JSON.parseObject(new String(bytes, StandardCharsets.UTF_8));
                Command command = CommandUtil.jsonToCommand(value);
                start += 4;
                if (command != null) {
                    CommandPos cmdPos = new CommandPos((int) start, cmdLen);
                    index.put(command.getKey(), cmdPos);
                }
                start += cmdLen;
            }
            file.seek(file.length());
        } catch (Exception e) {
            e.printStackTrace();
        }
        LoggerUtil.debug(LOGGER, logFormat, "reload index: "+index.toString());
    }
}
```