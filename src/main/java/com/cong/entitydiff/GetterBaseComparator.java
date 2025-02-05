package com.cong.entitydiff;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 getter 方法比对两个对象
 * <p>
 * 所有无参的 get 和 is 方法都认为是对象的属性
 * <p>
 */
public class GetterBaseComparator extends AbstractComparator {
    private static final String GET = "get";
    private static final String IS = "is";
    private static final String GET_IS = "get|is";
    private static final String GET_CLASS = "getClass";
    private static final Map<Class<?>, Map<String, Method>> CACHE = new ConcurrentHashMap<>();


    public GetterBaseComparator() {
    }

    /**
     * @param bothExistFieldOnly 是否只对比两个类都包含的字段
     */
    public GetterBaseComparator(boolean bothExistFieldOnly) {
        super(bothExistFieldOnly);
    }

    /**
     * 指定包含或排除某些字段
     *
     * @param includeFields 包含字段，若为 null 或空集，则不指定
     * @param excludeFields 排除字段，若为 null 或空集，则不指定
     */
    public GetterBaseComparator(List<String> includeFields, List<String> excludeFields) {
        super(includeFields, excludeFields);
    }

    /**
     * 指定包含或排除某些字段
     *
     * @param includeFields      包含字段，若为 null 或空集，则不指定
     * @param excludeFields      排除字段，若为 null 或空集，则不指定
     * @param bothExistFieldOnly 是否只对比两个类都包含的字段，默认为 true
     */
    public GetterBaseComparator(List<String> includeFields, List<String> excludeFields, boolean bothExistFieldOnly) {
        super(includeFields, excludeFields, bothExistFieldOnly);
    }

    /**
     * 获取不相等的属性
     *
     * @param first  对象1
     * @param second 对象2
     * @return 不相等的属性，键为属性名，值为属性类型
     */
    @Override
    public List<FieldInfo> getDiffFields(Object first, Object second) {
        if (first == null && second == null) {
            return Collections.emptyList();
        }

        // 先尝试判断是否为普通数据类型
        if (isSimpleField(first, second)) {
            return compareSimpleField(first, second);
        }
        Set<String> allFieldNames;
        // 获取所有字段
        Map<String, Method> firstGetters = getAllGetters(first);
        Map<String, Method> secondGetters = getAllGetters(second);
        if (first == null) {
            allFieldNames = secondGetters.keySet();
        } else if (second == null) {
            allFieldNames = firstGetters.keySet();
        } else {
            allFieldNames = getAllFieldNames(firstGetters.keySet(), secondGetters.keySet());
        }
        List<FieldInfo> diffFields = new LinkedList<>();
        for (String fieldName : allFieldNames) {
            try {
                Method firstGetterMethod = firstGetters.getOrDefault(fieldName, null);
                Method secondGetterMethod = secondGetters.getOrDefault(fieldName, null);
                Object firstVal = firstGetterMethod != null ? firstGetterMethod.invoke(first) : null;
                Object secondVal = secondGetterMethod != null ? secondGetterMethod.invoke(second) : null;
                FieldInfo fieldInfo = new FieldInfo(fieldName, getReturnType(firstGetterMethod), getReturnType(secondGetterMethod));
                fieldInfo.setFirstVal(firstVal);
                fieldInfo.setSecondVal(secondVal);
                if (!isFieldEquals(fieldInfo)) {
                    diffFields.add(fieldInfo);
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException("获取属性进行比对发生异常: " + fieldName, e);
            }
        }
        return diffFields;
    }

    private Class<?> getReturnType(Method method) {
        return method == null ? null : method.getReturnType();
    }


    /**
     * 获取类中的所有 getter 方法
     *
     * @return key -> fieldName, value -> getter
     */
    private Map<String, Method> getAllGetters(Object obj) {
        if (obj == null) {
            return Collections.emptyMap();
        }
        return CACHE.computeIfAbsent(obj.getClass(), k -> {
            Class<?> clazz = obj.getClass();
            Map<String, Method> getters = new LinkedHashMap<>(8);
            while (clazz != Object.class) {
                Method[] methods = clazz.getDeclaredMethods();
                for (Method method : methods) {
                    // getter 方法必须是 public 且没有参数的
                    if (!Modifier.isPublic(method.getModifiers()) || method.getParameterTypes().length > 0) {
                        continue;
                    }
                    if ((method.getReturnType() == Boolean.class || method.getReturnType() == boolean.class) && method.getName().startsWith(IS)) {
                        //返回值是 boolean 的 getter 方法为 isXxx 的写法兼容
                        String fieldName = uncapitalize(method.getName().substring(2));
                        getters.put(fieldName, method);
                        continue;
                    }
                    //以 get 开头但排除 getClass 方法
                    if (method.getName().startsWith(GET) && !GET_CLASS.equals(method.getName())) {
                        String fieldName = uncapitalize(method.getName().replaceFirst(GET_IS, ""));
                        getters.put(fieldName, method);
                    }
                }
                clazz = clazz.getSuperclass(); //得到父类,然后赋给自己
            }
            return getters;
        });
    }

    /**
     * 来自commons-lang3包的StringUtils
     * <p>
     * 用于使首字母小写
     */
    private String uncapitalize(final String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return str;
        }
        final int firstCodepoint = str.codePointAt(0);
        final int newCodePoint = Character.toLowerCase(firstCodepoint);
        if (firstCodepoint == newCodePoint) {
            return str;
        }
        final int[] newCodePoints = new int[strLen];
        int outOffset = 0;
        newCodePoints[outOffset++] = newCodePoint;
        for (int inOffset = Character.charCount(firstCodepoint); inOffset < strLen; ) {
            final int codepoint = str.codePointAt(inOffset);
            newCodePoints[outOffset++] = codepoint;
            inOffset += Character.charCount(codepoint);
        }
        return new String(newCodePoints, 0, outOffset);
    }
}
