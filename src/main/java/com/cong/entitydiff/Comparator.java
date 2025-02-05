package com.cong.entitydiff;


import java.util.List;

/**
 * 比对器接口，用于对比两个对象是否相同
 *
 * @author cong
 * @date 2025/02/05
 */
public interface Comparator {
    /**
     * 两个对象是否全相等
     *
     * @param first  对象1
     * @param second 对象2
     * @return 两个对象是否全相等
     */
    boolean isEquals(Object first, Object second);

    /**
     * 获取不相等的属性
     *
     * @param first  对象1
     * @param second 对象2
     * @return 不相等的属性，键为属性名，值为属性类型
     */
    List<FieldInfo> getDiffFields(Object first, Object second);
}
