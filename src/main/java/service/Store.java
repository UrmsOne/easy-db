/*
 *@Type Store.java
 * @Desc
 * @Author urmsone urmsone@163.com
 * @date 2024/6/13 02:05
 * @version
 */
package service;

import java.io.Closeable;

public interface Store extends Closeable {
    void set(String key, String value);

    String get(String key);

    void rm(String key);
}
