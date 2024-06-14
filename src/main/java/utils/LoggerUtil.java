/*
 *@Type LoggerUtil.java
 * @Desc
 * @Author urmsone urmsone@163.com
 * @date 2024/6/13 01:34
 * @version
 */
package utils;

import org.slf4j.Logger;

public class LoggerUtil {

    public static void debug(Logger logger, String format, Object... arguments) {
        if (logger.isDebugEnabled()) {
            logger.debug(format, arguments);
        }
    }

    public static void info(Logger logger, String format, Object... arguments) {
        if (logger.isInfoEnabled()) {
            logger.info(format, arguments);

        }
    }

    public static void error(Logger logger, Throwable t, String format, Object... arguments) {
        if (logger.isErrorEnabled()) {
            logger.error(format, arguments, t);
        }
    }
}