/*
 *@Type Controller.java
 * @Desc
 * @Author urmsone urmsone@163.com
 * @date 2024/6/13 12:17
 * @version
 */
package controller;

public interface Controller {
    void set(String key, String value);

    String get(String key);

    void rm(String key);

    void startServer();
}
