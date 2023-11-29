package org.clever.web.filter;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.Conv;
import org.clever.core.model.request.QueryByPage;
import org.clever.core.model.request.QueryBySort;
import org.clever.web.FilterRegistrar;
import org.clever.web.utils.GlobalExceptionHandler;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Arrays;

/**
 * 获取全局请求参数: QueryBySort、QueryByPage
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/11/29 10:42 <br/>
 */
@Slf4j
public class GlobalRequestParamsFilter implements FilterRegistrar.FilterFuc {
    public static final GlobalRequestParamsFilter INSTANCE = new GlobalRequestParamsFilter();

    @Override
    public void doFilter(FilterRegistrar.Context ctx) throws IOException, ServletException {
        try {
            QueryByPage queryByPage = resolveQueryByPage(ctx);
            QueryBySort.setCurrent(queryByPage);
            QueryByPage.setCurrent(queryByPage);
            ctx.next();
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            GlobalExceptionHandler.handle(e, ctx.req, ctx.res);
        } finally {
            QueryBySort.clearCurrent();
            QueryByPage.clearCurrent();
        }
    }

    protected QueryByPage resolveQueryByPage(FilterRegistrar.Context ctx) {
        // 排序参数
        String orderField = ctx.req.getParameter("orderField");
        String sort = ctx.req.getParameter("sort");
        String[] orderFields = ctx.req.getParameterValues("orderFields");
        String[] sorts = ctx.req.getParameterValues("sorts");
        // 分页参数
        Integer pageSize = Conv.asInteger(ctx.req.getParameter("pageSize"), 10);
        Integer pageNo = Conv.asInteger(ctx.req.getParameter("pageNo"), 1);
        Boolean countQuery = Conv.asBoolean(ctx.req.getParameter("countQuery"), true);
        // noinspection DataFlowIssue 构造 QueryByPage
        QueryByPage queryByPage = new QueryByPage(pageSize, pageNo, countQuery);
        if (StringUtils.isNotBlank(orderField) && StringUtils.isNotBlank(sort)) {
            queryByPage.setOrderField(orderField);
            queryByPage.setSort(sort);
        }
        if (orderFields != null && orderFields.length > 0 && sorts != null && sorts.length > 0) {
            queryByPage.setOrderFields(Arrays.asList(orderFields));
            queryByPage.setSorts(Arrays.asList(sorts));
        }
        return queryByPage;
    }
}

