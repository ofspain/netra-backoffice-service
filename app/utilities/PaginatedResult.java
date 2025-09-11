package utilities;

import java.util.List;

public class PaginatedResult<T> {
    private final List<T> items;
    private final long totalCount;
    private final int limit;
    private final int offset;

    public PaginatedResult(List<T> items, long totalCount, int limit, int offset) {
        this.items = items;
        this.totalCount = totalCount;
        this.limit = limit;
        this.offset = offset;
    }

    public List<T> getItems() { return items; }
    public long getTotalCount() { return totalCount; }
    public int getLimit() { return limit; }
    public int getOffset() { return offset; }
    public int getPage() { return offset / limit + 1; }
    public int getTotalPages() {
        return totalCount == 0 ? 1 : (int) Math.ceil((double) totalCount / limit);
    }
}
