package cn.atsukoruo.musicservice.service;

import cn.atsukoruo.musicservice.entity.Song;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.alibaba.otter.canal.protocol.exception.CanalClientException;
import com.aliyun.oss.OSS;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@Service
public class BinlogListener {
    private final CanalConnector canalConnector;
    private final ElasticsearchClient elasticsearchClient;
    private final OSS ossClient;

    private final Executor executor;
    public BinlogListener(@Qualifier("music-canal-connector") CanalConnector canalConnector,
                          @Qualifier("canal-thread-pool") Executor executor,
                          ElasticsearchClient elasticsearchClient,
                          OSS oss) {
        this.canalConnector = canalConnector;
        this.executor = executor;
        this.elasticsearchClient = elasticsearchClient;
        this.ossClient = oss;
    }

    private final Map<String, Consumer<List<CanalEntry.Column>>> insertionHandlerMap = Map.of(
        "song", this::handleSongInsertionEvent
    );
    private final Map<String, Consumer<List<CanalEntry.Column>>> deletionHandlerMap = Map.of(
        "sheet", this::handleSheetDeletionEvent
    );

    public void listenBinlog() {
        int batchSize = 1000;
        log.info("开始监听 song 表的 binlog");
        while (true) {
            try {
                Message message = canalConnector.getWithoutAck(batchSize);
                long batchId = message.getId();
                // 获取数据
                List<CanalEntry.Entry> entries = message.getEntries();
                if (batchId == -1 || entries.isEmpty()) {
                    try {
                        TimeUnit.SECONDS.sleep(3);
                    } catch (InterruptedException ignored) {

                    }
                    continue;
                }
                for (CanalEntry.Entry entry : entries) {
                    if (entry.getEntryType() == CanalEntry.EntryType.TRANSACTIONBEGIN || entry.getEntryType() == CanalEntry.EntryType.TRANSACTIONEND) {
                        continue;
                    }

                    CanalEntry.RowChange rowChange;
                    try {
                        rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
                    } catch (Exception e) {
                        throw new Exception("解析 binlog 数据出现异常, data: " + entry.toString(), e);
                    }

                    CanalEntry.EventType eventType = rowChange.getEventType();
                    String tableName = entry.getHeader().getTableName();
                    log.info(tableName);
                    for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                        switch (eventType) {
                            case INSERT -> handleInsertEventType(rowData.getAfterColumnsList(), tableName);
                            case DELETE -> handleDeleteEventType(rowData.getBeforeColumnsList(), tableName);
                        }
                    }
                }
                canalConnector.ack(batchId);
            } catch (Exception e) {
                log.error(e.toString());
                if (e instanceof InterruptedException ||
                    e instanceof CanalClientException) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }

    private void handleDeleteEventType(List<CanalEntry.Column> columns,
                                       String tableName) {
        var consumer =  deletionHandlerMap.get(tableName);
        if (consumer != null) {
            consumer.accept(columns);
        }
    }

    private void handleInsertEventType(List<CanalEntry.Column> columns,
                                       String tableName
    ) {
        var consumer =  insertionHandlerMap.get(tableName);
        if (consumer != null) {
            consumer.accept(columns);
        }
    }


    private void handleSongInsertionEvent(List<CanalEntry.Column> columns)  {
        HashMap<String, Object> hashMap = new HashMap<>();
        for (CanalEntry.Column column : columns) {
            hashMap.put(column.getName(), column.getValue());
        }
        Integer id =  Integer.valueOf((String)hashMap.get("id"));
        hashMap.put("id", id);
        Song song = Song.builder()
                .songUrl((String)hashMap.get("song_url"))
                .title((String)hashMap.get("title"))
                .imgUrl((String)hashMap.get("img_url"))
                .lrcUrl((String)hashMap.get("lrc_url"))
                .singer((String)hashMap.get("singer"))
                .id((Integer)hashMap.get("id"))
                .build();
        try {
            elasticsearchClient.index(i -> i.index("song").document(song));
        } catch (IOException e) {
            log.error("在 ES 中创建 song 索引 " + id + " 失败");
        }
    }

    private void handleSheetDeletionEvent(List<CanalEntry.Column> columns) {
        String imgUrl = null;
        for (CanalEntry.Column column : columns) {
            if (column.getName().equals("img_url")) {
                imgUrl = column.getValue();
                break;
            }
        }
        if (imgUrl == null)
            return;
        try {
            ossClient.deleteObject("atsukoruo-oss-image", imgUrl);
        } catch (Exception e) {
            log.error("删除图片 " + imgUrl + " 失败");
        }
    }


    @PostConstruct
    void init() {
        executor.execute(this::listenBinlog);
    }

}
