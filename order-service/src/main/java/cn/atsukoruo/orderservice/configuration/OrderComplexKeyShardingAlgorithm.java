package cn.atsukoruo.orderservice.configuration;

import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingAlgorithm;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class OrderComplexKeyShardingAlgorithm implements ComplexKeysShardingAlgorithm<Integer> {

    @Override
    public Collection<String> doSharding(Collection<String> collection, ComplexKeysShardingValue<Integer> complexKeysShardingValue) {
        if (!complexKeysShardingValue.getColumnNameAndRangeValuesMap().isEmpty()) {
            throw new RuntimeException("仅支持 = 和 in 操作");
        }
        Collection<Integer> orders = complexKeysShardingValue
                .getColumnNameAndShardingValuesMap()
                .getOrDefault("order_number", new ArrayList<>(1));

        Collection<Integer> users = complexKeysShardingValue
                .getColumnNameAndShardingValuesMap()
                .getOrDefault("user", new ArrayList<>(1));

        List<String> ids = new ArrayList<>(16);
        ids.addAll(ids2String(orders));
        ids.addAll(ids2String(users));

        return ids.stream()
                .map(id -> id.substring(id.length() - 1))
                .distinct()
                .map(Integer::valueOf)
                .map(idSuffix -> idSuffix % collection.size())
                .map(String::valueOf)
                .map(tableSuffix -> collection.stream().filter(targetName -> targetName.endsWith(tableSuffix)).findFirst().orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 转换成String
     */
    private List<String> ids2String(Collection<?> ids) {
        List<String> result = new ArrayList<>(ids.size());
        ids.forEach(id -> result.add(Objects.toString(id)));
        return result;
    }
}
