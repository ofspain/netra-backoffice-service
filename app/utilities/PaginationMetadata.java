package utilities;
public class PaginationMetadata {

    private final int page;
    private final int limit;
    private final int pageCount;
    private final String sortBy;
    private final String sortDirection;

    public PaginationMetadata(int page, int limit, int pageCount, String sortBy, String sortDirection) {
        this.page = page;
        this.limit = limit;
        this.pageCount = pageCount;
        this.sortBy = sortBy;
        this.sortDirection = sortDirection;
    }

    public int getPage() {
        return page;
    }

    public int getLimit() {
        return limit;
    }

    public int getPageCount() {
        return pageCount;
    }

    public String getSortBy() {
        return sortBy;
    }

    public String getSortDirection() {
        return sortDirection;
    }
}

