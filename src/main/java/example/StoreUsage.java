/*
 *@Type Usage.java
 * @Desc
 * @Author urmsone urmsone@163.com
 * @date 2024/6/13 03:59
 * @version
 */
package example;

import service.NormalStore;

import java.io.File;

public class StoreUsage {
    public static void main(String[] args) {
        String dataDir="data"+ File.separator;
        NormalStore store = new NormalStore(dataDir);
//        store.set("zsy1","1");
//        store.set("zsy2","2");
//        store.set("zsy3","3");
//        store.set("zsy4","你好");
        System.out.println(store.get("zsy4"));
//        store.rm("zsy4");
//        System.out.println(store.get("zsy4"));
    }
}
