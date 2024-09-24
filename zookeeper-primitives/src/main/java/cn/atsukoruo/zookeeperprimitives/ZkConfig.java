package cn.atsukoruo.zookeeperprimitives;


import lombok.*;


public record ZkConfig(String serverUrls, String path, int sessionTimeout) { }
