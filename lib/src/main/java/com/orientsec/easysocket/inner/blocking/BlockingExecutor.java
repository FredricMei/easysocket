package com.orientsec.easysocket.inner.blocking;

import com.orientsec.easysocket.Message;
import com.orientsec.easysocket.PushHandler;
import com.orientsec.easysocket.TaskType;
import com.orientsec.easysocket.exception.ConnectException;
import com.orientsec.easysocket.exception.EasyException;
import com.orientsec.easysocket.exception.TimeoutException;
import com.orientsec.easysocket.exception.WriteException;
import com.orientsec.easysocket.inner.MessageType;
import com.orientsec.easysocket.inner.SendMessage;
import com.orientsec.easysocket.inner.TaskExecutor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.inner.blocking
 * Time: 2018/01/09 15:28
 * Author: Fredric
 * coding is art not science
 */
public class BlockingExecutor implements TaskExecutor<EasyTask> {

    private Map<Integer, EasyTask> taskMap = new ConcurrentHashMap<>();

    private LinkedBlockingQueue<SendMessage> messageQueue = new LinkedBlockingQueue<>();

    private SocketConnection connection;

    private PushHandler pushHandler;

    LinkedBlockingQueue<SendMessage> getMessageQueue() {
        return messageQueue;
    }

    public BlockingExecutor(SocketConnection connection) {
        this.connection = connection;
        pushHandler = connection.options().getPushHandler();
    }

    @Override
    public void execute(EasyTask task) {
        if (connection.isShutdown()) {
            throw new IllegalStateException("connection is show down!");
        }
        connection.connect();
        if (messageQueue.offer(task.getMessage())) {
            taskMap.put(task.getMessage().getTaskId(), task);
            task.timeoutFuture = connection.executorService().schedule(() -> {
                SendMessage message = task.getMessage();
                message.invalid();
                taskMap.remove(message.getTaskId());
                if (connection.isConnect()) {
                    task.onError(new TimeoutException("request time out"));
                } else {
                    task.onError(new ConnectException("no connection"));
                }
            }, connection.options().getRequestTimeOut(), TimeUnit.SECONDS);
        } else {
            task.onError(new EasyException("task refuse to execute!"));
        }
    }

    @Override
    public void onReceive(Message message) {
        if (message.getMessageType() == MessageType.PULSE) {
            connection.pulse().feed();
        } else if (message.getMessageType() == MessageType.PUSH) {
            connection.options().getDispatchExecutor().execute(() -> pushHandler.onPush(message.getCmd(), message));
        } else {
            EasyTask easyTask = taskMap.remove(message.getTaskId());
            if (easyTask != null) {
                easyTask.onSuccess(message);
            }
        }
    }

    @Override
    public void onSend(Message message) {
        EasyTask easyTask = taskMap.get(message.getTaskId());
        if (easyTask != null) {
            if (easyTask.getTaskType() == TaskType.SEND_ONLY) {
                easyTask.onSuccess();
                taskMap.remove(message.getTaskId());
            }
        }
    }

    @Override
    public void onSendError(Message message, WriteException exception) {
        EasyTask easyTask = taskMap.remove(message.getTaskId());
        if (easyTask != null) {
            easyTask.onError(exception);
        }
    }

    @Override
    public void remove(EasyTask task) {
        if (taskMap.remove(task.getMessage().getTaskId()) != null) {
            messageQueue.remove(task.getMessage());
        }
    }

}