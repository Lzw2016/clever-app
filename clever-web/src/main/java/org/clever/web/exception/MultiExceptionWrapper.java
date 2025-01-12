package org.clever.web.exception;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 多个异常包装器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/22 21:50 <br/>
 */
public class MultiExceptionWrapper extends RuntimeException {
    @Getter
    private final Throwable[] errs;

    public MultiExceptionWrapper(String message, Throwable[] errs) {
        super(message, errs != null && errs.length >= 1 ? errs[0] : null);
        this.errs = getAllErrs(errs);
    }

    public MultiExceptionWrapper(Throwable[] errs) {
        super(errs != null && errs.length >= 1 ? errs[0] : null);
        this.errs = getAllErrs(errs);
    }

    @Override
    public String getMessage() {
        String msg = super.getMessage();
        if (StringUtils.isBlank(msg) && errs != null) {
            msg = Arrays.toString(Arrays.stream(errs).filter(Objects::nonNull).map(Throwable::getMessage).toArray());
        }
        return msg;
    }

    protected Throwable[] getAllErrs(Throwable[] errs) {
        List<Throwable> errList = new ArrayList<>(errs == null ? 0 : errs.length);
        if (errs != null) {
            for (Throwable err : errs) {
                if (err instanceof MultiExceptionWrapper) {
                    MultiExceptionWrapper wrapper = (MultiExceptionWrapper) err;
                    if (wrapper.errs != null) {
                        errList.addAll(Arrays.stream(wrapper.errs).collect(Collectors.toList()));
                    }
                } else {
                    errList.add(err);
                }
            }
        }
        return errList.toArray(new Throwable[0]);
    }
}
