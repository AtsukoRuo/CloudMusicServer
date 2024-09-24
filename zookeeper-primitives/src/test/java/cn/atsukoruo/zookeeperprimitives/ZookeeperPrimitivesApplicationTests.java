package cn.atsukoruo.zookeeperprimitives;

import org.junit.jupiter.api.Test;

import java.util.List;

class ZookeeperPrimitivesApplicationTests {

    @Test
    public void testRegistry() throws Exception {
        ZkConfig zkConfig = new ZkConfig("114.116.204.34:2181,122.9.36.231:2181,116.63.9.166:2181", "/netty", 30000);
        System.out.println(zkConfig);
        ZookeeperPrimitives zk = new ZookeeperPrimitives(zkConfig);
        ZookeeperPrimitives.Metadata metadata = new ZookeeperPrimitives.Metadata("123.124", "901", "1");
        zk.register("websocket", metadata);
        Thread.sleep(1000000);
    }

    @Test
    public void testRegistry3() throws Exception {
        ZkConfig zkConfig = new ZkConfig("114.116.204.34:2181,122.9.36.231:2181,116.63.9.166:2181", "/netty", 30000);
        System.out.println(zkConfig);
        ZookeeperPrimitives zk = new ZookeeperPrimitives(zkConfig);
        ZookeeperPrimitives.Metadata metadata = new ZookeeperPrimitives.Metadata("123.124", "901", "1");
        zk.register("websocket", metadata);
        Thread.sleep(1000000);
    }

    @Test
    public void testRegistry4() throws Exception {
        ZkConfig zkConfig = new ZkConfig("114.116.204.34:2181,122.9.36.231:2181,116.63.9.166:2181", "/netty", 30000);
        System.out.println(zkConfig);
        ZookeeperPrimitives zk = new ZookeeperPrimitives(zkConfig);
        ZookeeperPrimitives.Metadata metadata = new ZookeeperPrimitives.Metadata("123.124", "901", "1");
        zk.register("websocket", metadata);
        Thread.sleep(1000000);
    }

    @Test
    public void testRegistry5() throws Exception {
        ZkConfig zkConfig = new ZkConfig("114.116.204.34:2181,122.9.36.231:2181,116.63.9.166:2181", "/netty", 30000);
        System.out.println(zkConfig);
        ZookeeperPrimitives zk = new ZookeeperPrimitives(zkConfig);
        ZookeeperPrimitives.Metadata metadata = new ZookeeperPrimitives.Metadata("123.124", "901", "1");
        zk.register("websocket", metadata);
        Thread.sleep(1000000);
    }

    @Test
    public void testDiscovery() throws Exception {
        ZkConfig zkConfig = new ZkConfig("114.116.204.34:2181,122.9.36.231:2181,116.63.9.166:2181", "/netty", 30000);
        ZookeeperPrimitives zk = new ZookeeperPrimitives((elements) -> {
            for (var element : elements) {
                System.out.println(element);
            }
        }, zkConfig);

        List<ZookeeperPrimitives.Metadata> metadataList =  zk.discovery();
        for (var metadata : metadataList) {
            System.out.println(metadata);
        }

        Thread.sleep(10000000);
    }
}
