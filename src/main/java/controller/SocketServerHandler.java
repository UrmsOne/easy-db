/*
 *@Type SocketServerHandler.java
 * @Desc
 * @Author urmsone urmsone@163.com
 * @date 2024/6/13 12:50
 * @version
 */
package controller;

import dto.ActionDTO;
import dto.ActionTypeEnum;
import dto.RespDTO;
import dto.RespStatusTypeEnum;
import service.NormalStore;
import service.Store;
import utils.LoggerUtil;

import java.io.*;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SocketServerHandler implements Runnable {
    private final Logger LOGGER = LoggerFactory.getLogger(SocketServerHandler.class);
    private Socket socket;
    private Store store;

    public SocketServerHandler(Socket socket, Store store) {
        this.socket = socket;
        this.store = store;
    }

    @Override
    public void run() {
        try (ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())) {

            // 接收序列化对象
            ActionDTO dto = (ActionDTO) ois.readObject();
            LoggerUtil.debug(LOGGER, "[SocketServerHandler][ActionDTO]: {}", dto.toString());
            System.out.println("" + dto.toString());

            // 处理命令逻辑(TODO://改成可动态适配的模式)
            if (dto.getType() == ActionTypeEnum.GET) {
                String value = this.store.get(dto.getKey());
                LoggerUtil.debug(LOGGER, "[SocketServerHandler][run]: {}", "get action resp" + dto.toString());
                RespDTO resp = new RespDTO(RespStatusTypeEnum.SUCCESS, value);
                oos.writeObject(resp);
                oos.flush();
            }
            if (dto.getType() == ActionTypeEnum.SET) {
                this.store.set(dto.getKey(), dto.getValue());
                LoggerUtil.debug(LOGGER, "[SocketServerHandler][run]: {}", "set action resp" + dto.toString());
                RespDTO resp = new RespDTO(RespStatusTypeEnum.SUCCESS, null);
                oos.writeObject(resp);
                oos.flush();
            }
            if (dto.getType() == ActionTypeEnum.RM) {
                this.store.rm(dto.getKey());
            }

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
