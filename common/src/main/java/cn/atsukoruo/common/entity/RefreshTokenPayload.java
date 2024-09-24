package cn.atsukoruo.common.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;


@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class RefreshTokenPayload {
    int userId;
    String client;
    int version;
    int batch;
    Date expireTime;
}