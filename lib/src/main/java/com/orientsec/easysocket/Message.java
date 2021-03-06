package com.orientsec.easysocket;

import com.orientsec.easysocket.inner.MessageType;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2017/12/26 9:13
 * Author: Fredric
 * coding is art not science
 */

public class Message<T> {

    public Message(MessageType messageType) {
        this(messageType, 0);
    }

    public Message(MessageType messageType, int taskId) {
        this.taskId = taskId;
        this.messageType = messageType;
    }

    /**
     * 消息id
     */
    private int taskId;

    /**
     * 包体
     */
    private T body;

    /**
     * 消息类型
     */
    private MessageType messageType;

    /**
     * 获取任务id
     * 每一个任务的id是唯一的，通过taskId，客户端可以匹配每个请求的返回
     *
     * @return taskId
     */
    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    /**
     * 获取包体内容，类型可以自定义。一般建议使用{@code byte[]}，在业务层{@link Request#decode(Object)}
     * 出进行解码、反序列化。也可以根据自定义协议，返回其他类型的结构。
     *
     * @return 协议消息体
     */
    public T getBody() {
        return body;
    }

    public void setBody(T body) {
        this.body = body;
    }

    /**
     * 获取消息类型
     *
     * @return 消息类型
     */
    public MessageType getMessageType() {
        return messageType;
    }
}
