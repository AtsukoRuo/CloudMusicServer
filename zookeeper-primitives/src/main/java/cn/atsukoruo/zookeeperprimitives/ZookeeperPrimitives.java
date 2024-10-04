package cn.atsukoruo.zookeeperprimitives;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@Slf4j
public class ZookeeperPrimitives {
    private final ZooKeeper client;
    private final ZkConfig config;
    private final Consumer<List<NodeChangeInfo>> consumer;
    private final static ObjectMapper objectMapper = new ObjectMapper();

    // 对于服务注册者，使用该构造函数即可
    public ZookeeperPrimitives(ZkConfig zkConfig) {
        this(null, zkConfig);
    }

    // 对于服务发现者，使用该构造函数
    // 当 path 下的节点发生变化后（增加，删除），会调用 consumer 回调方法
    public ZookeeperPrimitives(Consumer<List<NodeChangeInfo>> consumer, ZkConfig zkConfig) {

        ZooKeeper tempZkClient = null;
        try {
            Watcher watcher = null;
            if (consumer != null) {
                watcher = getWatcher(consumer);
            }
            tempZkClient = new ZooKeeper(zkConfig.serverUrls(), zkConfig.sessionTimeout(), watcher);
            // 保证有一个根目录
            Stat stat = tempZkClient.exists(zkConfig.path(), false);
            if (stat == null) {
                tempZkClient.create(zkConfig.path(),
                        zkConfig.path().getBytes(),
                        ZooDefs.Ids.OPEN_ACL_UNSAFE,
                        CreateMode.PERSISTENT);
            }
        } catch (Exception e) {
            log.error(e.toString());
        } finally {
            this.client = tempZkClient;
            this.serviceProviders = new HashSet<>();
            this.config = zkConfig;
            this.consumer = consumer;
        }
    }


    private Watcher getWatcher(Consumer<List<NodeChangeInfo>> consumer) {
        return watchedEvent -> {
            log.info("触发事件 " + watchedEvent);
            if (watchedEvent.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                try {
                    consumer.accept(callbackAfterChange());
                } catch (Exception e) {
                    log.error(e.toString());
                }
            }
        };
    }

    /**
     * 服务注册
     */
    public void register(String serverName, Metadata metadata) throws Exception {
        // 假设 path 为 /netty/websocket
        // 那么顺序子节点为 /netty/websocket00000001  （只有两个 / ）
        String path = this.config.path() + "/" + serverName;
        client.create(path,
                objectMapper.writeValueAsBytes(metadata),
                ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL_SEQUENTIAL);
        log.info(serverName + " 已经上线");
    }

    /**
     * 获取当前所有的服务提供者，一般用在服务发现端的初始化阶段
     */
    public List<Metadata> discovery() throws InterruptedException, KeeperException {
        List<Metadata> ret = new ArrayList<>();
        List<String> zkChildren = client.getChildren(
                this.config.path(),
                this.consumer != null);
        zkChildren.forEach(name -> {
            try {
                String path = this.config.path() + "/" + name;
                byte[] data = client.getData(path, false, null);
                String json = new String(data);
                ret.add(objectMapper.readValue(json, Metadata.class));
            } catch (Exception e) {
                log.error(e.toString());
            }
        });
        serviceProviders.addAll(zkChildren);
        return ret;
    }

    public enum NodeChangeEvent { ADD, DELETE }

    public record NodeChangeInfo(String name, Metadata data, NodeChangeEvent event) { }

    private final Set<String> serviceProviders;
    private List<NodeChangeInfo> callbackAfterChange() throws InterruptedException, KeeperException, JsonProcessingException {
        // 这里 true 就表示用 Zookeeper 句柄上的默认 Watcher 来注册监听点。
        List<String> zkChildren = client.getChildren(this.config.path(), true);
        Set<String> tempProviders = new HashSet<>(zkChildren);

        // 获取删除的 Node
        Set<String> deletedNodes = new HashSet<>(serviceProviders);
        deletedNodes.removeAll(tempProviders);

        // 获取新增的 Node
        Set<String> newNodes = new HashSet<>(tempProviders);
        newNodes.removeAll(serviceProviders);

        List<NodeChangeInfo> ret = new ArrayList<>();
        for (String name : deletedNodes) {
            ret.add(new NodeChangeInfo(name, null, NodeChangeEvent.DELETE));
            serviceProviders.remove(name);
        }

        for (String name : newNodes) {
            String path = this.config.path() + "/" + name;
            byte[] data = client.getData(path, false, null);
            String json = new String(data);
            ret.add(new NodeChangeInfo(name,
                    objectMapper.readValue(json, Metadata.class),
                    NodeChangeEvent.ADD));
            serviceProviders.add(name);
        }
        return ret;
    }

    public record Metadata(String ip, String port, String id) { }
}
