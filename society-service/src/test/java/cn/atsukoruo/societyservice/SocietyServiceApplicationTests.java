package cn.atsukoruo.societyservice;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@SpringBootTest
class SocietyServiceApplicationTests {
    @Test
    public void closure() {
        List<Integer> list = List.of(1, 2, 3, 4);
        List<CompletableFuture<Integer>> futureList = new ArrayList<>();
        for (Integer i : list) {
            CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                System.out.println(i);
                return 0;
            });
            futureList.add(future);
        }
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0])).join();
    }
}
