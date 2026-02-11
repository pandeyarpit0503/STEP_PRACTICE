import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class FlashSaleInventoryManager {

    private final ConcurrentHashMap<String, ProductInventory> inventory;

    public FlashSaleInventoryManager() {
        inventory = new ConcurrentHashMap<>();
    }

    static class ProductInventory {
        AtomicInteger stock;
        ConcurrentLinkedQueue<Long> waitingList;

        ProductInventory(int initialStock) {
            this.stock = new AtomicInteger(initialStock);
            this.waitingList = new ConcurrentLinkedQueue<>();
        }
    }

    public void addProduct(String productId, int stock) {
        inventory.put(productId, new ProductInventory(stock));
    }

    public int checkStock(String productId) {
        ProductInventory product = inventory.get(productId);
        return (product == null) ? 0 : product.stock.get();
    }

    public String purchaseItem(String productId, long userId) {
        ProductInventory product = inventory.get(productId);

        if (product == null) {
            return "Product not found";
        }

        while (true) {
            int currentStock = product.stock.get();

            if (currentStock <= 0) {
                int position = addToWaitingList(product, userId);
                return "Out of stock. Added to waiting list. Position #" + position;
            }

            if (product.stock.compareAndSet(currentStock, currentStock - 1)) {
                return "Success! Remaining stock: " + (currentStock - 1);
            }
        }
    }

    private int addToWaitingList(ProductInventory product, long userId) {
        product.waitingList.add(userId);
        return product.waitingList.size();
    }

    public void restock(String productId, int quantity) {
        ProductInventory product = inventory.get(productId);
        if (product == null) return;

        product.stock.addAndGet(quantity);

        while (product.stock.get() > 0 && !product.waitingList.isEmpty()) {
            Long nextUser = product.waitingList.poll();
            if (nextUser != null) {
                product.stock.decrementAndGet();
                System.out.println("Allocated to waiting user: " + nextUser);
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {

        FlashSaleInventoryManager manager = new FlashSaleInventoryManager();
        manager.addProduct("IPHONE15_256GB", 100);

        System.out.println("Initial stock: " + manager.checkStock("IPHONE15_256GB"));

        ExecutorService executor = Executors.newFixedThreadPool(200);

        for (int i = 1; i <= 50000; i++) {
            long userId = i;
            executor.submit(() -> {
                String result = manager.purchaseItem("IPHONE15_256GB", userId);

            });
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        System.out.println("Final stock: " + manager.checkStock("IPHONE15_256GB"));
    }
}
