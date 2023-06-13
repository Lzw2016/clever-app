package org.clever.core.model.request;

import lombok.Getter;
import lombok.Setter;
import org.clever.core.model.request.page.Page;

import java.util.List;

/**
 * 分页查询基础类
 * <p>
 * 作者：lzw <br/>
 * 创建时间：2017-09-02 00:41 <br/>
 */
public class QueryByPage extends QueryBySort {
    private static final long serialVersionUID = 1L;
    /**
     * 每页的数据量 - 最大值 1k
     */
    public static final int PAGE_SIZE_MAX = 1000;

    /**
     * 每页的数据量(1 <= pageSize <= 100)
     */
    private int pageSize = 10;
    /**
     * 当前页面的页码数(pageNo >= 1)
     */
    private int pageNo = 1;
    /**
     * 是否进行 count 查询
     */
    @Setter
    @Getter
    private boolean isSearchCount = true;
    // /**
    //  *  是否使用缓存 count 查询值(减少 count 频率)
    //  */
    // @Setter
    // @Getter
    // private boolean useCacheCount = false;

    public QueryByPage(int pageSize, int pageNo, boolean isSearchCount) {
        this.pageSize = pageSize;
        this.pageNo = pageNo;
        this.isSearchCount = isSearchCount;
    }

    public QueryByPage(int pageSize, int pageNo) {
        this.pageSize = pageSize;
        this.pageNo = pageNo;
    }

    public QueryByPage() {
    }

    /*--------------------------------------------------------------
     * 			getter、setter
     * -------------------------------------------------------------*/

    public long offset() {
        long pageNo = getPageNo();
        return pageNo > 0 ? (pageNo - 1) * getPageSize() : 0;
    }

    public int getPageSize() {
        if (pageSize > getMaxPageSize()) {
            pageSize = getMaxPageSize();
        }
        if (pageSize < 1) {
            pageSize = 1;
        }
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        if (pageSize > getMaxPageSize()) {
            pageSize = getMaxPageSize();
        }
        if (pageSize < 1) {
            pageSize = 1;
        }
        this.pageSize = pageSize;
    }

    public int getPageNo() {
        if (pageNo < 1) {
            pageNo = 1;
        }
        return pageNo;
    }

    public void setPageNo(int pageNo) {
        if (pageNo < 1) {
            pageNo = 1;
        }
        this.pageNo = pageNo;
    }

    /**
     * 返回最大页大小
     */
    public int getMaxPageSize() {
        return PAGE_SIZE_MAX;
    }

    /**
     * 获取请求参数对应的 IPage 对象<br />
     * <strong>注意: 当前方法指定要在分页查询执行之后调用否则数据不准确</strong>
     * <pre>
     *     {@code
     *     return query.result(permissionMapper.findByPage(query));
     *     }
     * </pre>
     */
    public static <T> Page<T> result(QueryByPage queryPage, List<T> records) {
        Page<?> page = new Page<>(queryPage.getPageNo(), queryPage.getPageSize());
        Page<T> newPage = new Page<>();
        newPage.setTotal(page.getTotal());
        newPage.setSize(page.getSize());
        newPage.setCurrent(page.getCurrent());
        newPage.setSearchCount(page.isSearchCount());
        newPage.setPages(page.getPages());
        newPage.setRecords(records);
        return newPage;
    }
}
