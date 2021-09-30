package com.sjhy.plugin.comm;

/**
 * @author lihu@eventslack.com
 * @since 2021/9/30
 */
public class TargetTestFileNotFoundException extends RuntimeException {
    public TargetTestFileNotFoundException() {
        super("对应的文件不存在");
    }

    public TargetTestFileNotFoundException(String message) {
        super(message);
    }
}
