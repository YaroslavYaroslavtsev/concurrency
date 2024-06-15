package course.concurrency.m2_async.minPrice;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PriceAggregator {

  private PriceRetriever priceRetriever = new PriceRetriever();

  public void setPriceRetriever(PriceRetriever priceRetriever) {
    this.priceRetriever = priceRetriever;
  }

  private Collection<Long> shopIds = Set.of(10l, 45l, 66l, 345l, 234l, 333l, 67l, 123l, 768l);

  public void setShops(Collection<Long> shopIds) {
    this.shopIds = shopIds;
  }

  // задачи блокирующие применяем CachedThreadPool
  private ExecutorService executor = Executors.newCachedThreadPool();

  public double getMinPrice(long itemId) {
    // паралельно запрашиваем прайс из всех магазинов
    List<CompletableFuture<Double>> getMinPriceTasks = shopIds.stream()
        .map(shopId -> CompletableFuture.supplyAsync(() -> priceRetriever.getPrice(itemId, shopId), executor)
            .completeOnTimeout(Double.MAX_VALUE, 2950, TimeUnit.MILLISECONDS)
            .exceptionally(ex -> Double.MAX_VALUE))
        .toList();
    // ждем пока все запросы отработают или задачи прервутся по таймауту
    CompletableFuture
        .allOf(getMinPriceTasks.toArray(CompletableFuture[]::new))
        .join();
    // получаем результат
    return getMinPriceTasks.stream()
        .mapToDouble(CompletableFuture::join)
        .filter(item -> item != Double.MAX_VALUE)
        .min()
        .orElse(Double.NaN);
  }
}
