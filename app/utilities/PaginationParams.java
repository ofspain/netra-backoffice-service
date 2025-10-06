package utilities;

import org.apache.commons.lang3.StringUtils;
import play.mvc.Http;

public class PaginationParams {
    private final int page;
    private final int limit;
    private final String sortBy;
    private final String direction;

    public PaginationParams(int page, int limit, String sortBy, String direction) {
        this.page = page;
        this.limit = limit;
        this.sortBy = sortBy;
        this.direction = direction;
    }

    public int getPage() {
        return page;
    }

    public int getLimit() {
        return limit;
    }

    public String getSortBy() {
        return sortBy;
    }

    public String getDirection() {
        return direction;
    }

    /** Factory method to build PaginationParams from a Play request */
    public static PaginationParams from(Http.Request request, String defaultSortBy, String defaultDirection) {
        String pageStr = request.getQueryString("page");
        String limitStr = request.getQueryString("limit");
        String sortBy = request.getQueryString("sort") != null ? request.getQueryString("sort") : defaultSortBy;
        String direction = request.getQueryString("sort_direction") != null ? request.getQueryString("sort_direction") : defaultDirection;

        int page = (StringUtils.isNumeric(pageStr)) ? Integer.parseInt(pageStr) : 1;
        int limit = (StringUtils.isNumeric(limitStr)) ? Integer.parseInt(limitStr) : 20;

        return new PaginationParams(page, limit, sortBy, direction);
    }
}

