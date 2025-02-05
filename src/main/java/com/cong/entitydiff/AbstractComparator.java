package com.cong.entitydiff;


import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractComparator implements Comparator {
    private static final List<Class<?>> WRAPPER_TYPES =
            Arrays.asList(Boolean.class, Byte.class, Character.class, Short.class, Integer.class, Long.class, Float.class, Double.class);
    private List<String> includeFields;
    private List<String> excludeFields;

    /**
     * 默认为 true，表示只取两个对象中都存在的字段进行比较，
     * false 表示取两个对象中都存在的字段和两个对象中都不存在的字段都进行比较。
     */
    private boolean bothExistFieldOnly = true;

    protected AbstractComparator() {
        includeFields = Collections.emptyList();
        excludeFields = Collections.emptyList();
    }

    /**
     * @param bothExistFieldOnly 是否只对比两个类都包含的字段
     */
    protected AbstractComparator(boolean bothExistFieldOnly) {
        includeFields = Collections.emptyList();
        excludeFields = Collections.emptyList();
        this.bothExistFieldOnly = bothExistFieldOnly;
    }

    /**
     * 指定包含或排除某些字段
     *
     * @param includeFields 包含字段，若为 null 或空集，则不指定
     * @param excludeFields 排除字段，若为 null 或空集，则不指定
     */
    protected AbstractComparator(List<String> includeFields, List<String> excludeFields) {
        this.includeFields = includeFields;
        this.excludeFields = excludeFields;
    }

    /**
     * 指定包含或排除某些字段
     *
     * @param includeFields      包含字段，若为 null 或空集，则不指定
     * @param excludeFields      排除字段，若为 null 或空集，则不指定
     * @param bothExistFieldOnly 是否只对比两个类都包含的字段，默认为 true
     */
    protected AbstractComparator(List<String> includeFields, List<String> excludeFields, boolean bothExistFieldOnly) {
        this.includeFields = includeFields;
        this.excludeFields = excludeFields;
        this.bothExistFieldOnly = bothExistFieldOnly;
    }

    /**
     * 两个对象是否全相等
     *
     * @param first  对象1
     * @param second 对象2
     * @return 两个对象是否全相等
     */
    @Override
    public boolean isEquals(Object first, Object second) {
        List<FieldInfo> diff = getDiffFields(first, second);
        return diff == null || diff.isEmpty();
    }

    /**
     * 判断字段是否相等
     *
     * @param fieldInfo 字段信息
     * @return boolean
     */
    protected boolean isFieldEquals(FieldInfo fieldInfo) {
        // 先判断排除，如果需要排除，则无论在不在包含范围，都一律不比对，直接返回 true 相等
        if (isExclude(fieldInfo)) {
            return true;
        }
        // 如果有指定需要包含的字段而且当前字段不在需要包含的字段中则不比对，直接返回 true 相等
        if (!isInclude(fieldInfo)) {
            return true;
        }
        return nullableEquals(fieldInfo.getFirstVal(), fieldInfo.getSecondVal());
    }

    /**
     * 确定是否需要需要排除这个字段，子类可以扩展这个方法，自定义判断方式
     */
    protected boolean isExclude(FieldInfo fieldInfo) {
        // 如果有指定需要排除的字段，而且当前字段是需要排除字段，则直接返回 true
        return excludeFields != null && !excludeFields.isEmpty() && excludeFields.contains(fieldInfo.getFieldName());
    }

    /**
     * 确定是否需要比较这个字段，子类可以扩展这个方法，自定义判断方式
     */
    protected boolean isInclude(FieldInfo fieldInfo) {
        // 没有指定需要包含的字段，则全部都包含,需要比对
        if (includeFields == null || includeFields.isEmpty()) {
            return true;
        }
        return includeFields.contains(fieldInfo.getFieldName());
    }

    boolean isSimpleField(Object first, Object second) {
        Object obj = first == null ? second : first;
        Class<?> clazz = obj.getClass();
        return clazz.isPrimitive() || WRAPPER_TYPES.contains(clazz);
    }
    /**
     * 如果简单数据类型的对象则直接进行比对
     *
     * @param first  对象1
     * @param second 对象2
     * @return 不同的字段信息，相等返回空集，不等则 FieldInfo 的字段名为对象的类型名称
     */
    List<FieldInfo> compareSimpleField(Object first, Object second) {
        if (Objects.equals(first, second)) {
            return Collections.emptyList();
        } else {
            Object obj = first == null ? second : first;
            Class<?> clazz = obj.getClass();
            // 不等的字段名称使用类的名称
            return Collections.singletonList(new FieldInfo(clazz.getSimpleName(), clazz, first, second));
        }
    }

    /**
     * 根据配置的规则决定取两个对象字段的交集或并集
     */
    Set<String> getAllFieldNames(Set<String> firstFields, Set<String> secondFields) {
        Set<String> allFields;
        // 只取交集
        if (isBothExistFieldOnly()) {
            allFields = firstFields.stream().filter(secondFields::contains).collect(Collectors.toSet());
        } else {
            // 否则取并集
            allFields = new HashSet<>(firstFields);
            allFields.addAll(secondFields);
        }
        return allFields;
    }

    private boolean nullableEquals(Object first, Object second) {
        // 1. 检查两个对象是否都是集合类型（List/Set等）
        if (first instanceof Collection && second instanceof Collection) {
            // 2. 如果是集合类型，将它们转换为数组，再进行深度比较
            return Objects.deepEquals(((Collection<?>) first).toArray(), ((Collection<?>) second).toArray());
        }
        // 3. 如果不是集合类型，直接进行深度比较
        return Objects.deepEquals(first, second);
    }


    public List<String> getIncludeFields() {
        return includeFields;
    }

    public void setIncludeFields(List<String> includeFields) {
        this.includeFields = includeFields;
    }

    public List<String> getExcludeFields() {
        return excludeFields;
    }

    public void setExcludeFields(List<String> excludeFields) {
        this.excludeFields = excludeFields;
    }

    public boolean isBothExistFieldOnly() {
        return bothExistFieldOnly;
    }
}
