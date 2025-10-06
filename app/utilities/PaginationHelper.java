package utilities;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.function.TriFunction;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import play.twirl.api.Html;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.*;

public class PaginationHelper extends Results {

    public static <T> Result renderPaginatedList(
            Http.Request request,
            Function<PaginationParams, CompletionStage<PaginatedResult<T>>> findAllFunction,
            String defaultSortBy,
            String defaultDirection,
            TriFunction<List<T>, PaginationMetadata, Http.Request, play.twirl.api.Html> renderFunction
    ) {
        PaginationParams params = PaginationParams.from(request, defaultSortBy, defaultDirection);

        try {
            PaginatedResult<T> paginatedResult =
                    findAllFunction.apply(params).toCompletableFuture().get();

            List<T> listed = paginatedResult.getItems();

            PaginationMetadata metaData = new PaginationMetadata(
                    params.getPage(),
                    params.getLimit(),
                    paginatedResult.getTotalPages(),
                    params.getSortBy(),
                    params.getDirection()
            );

            return ok(renderFunction.apply(listed, metaData, request));

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }



}

