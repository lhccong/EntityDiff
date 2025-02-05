package com.cong.entitydiff;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import java.util.*;


/**
 * 原始数据类型比对参数化测试
 *
 * @author dadiyang
 * @since 2019/3/24
 */
@RunWith(Parameterized.class)
public class ComparatorPrimitiveTest {
    private final Object first;
    private final Object second;
    private final Comparator comparator;
    private final boolean expectEq;
    private final List<FieldInfo> expectDiffField;

    public ComparatorPrimitiveTest(Object first, Object second, Comparator comparator, boolean expectEq, List<FieldInfo> expectDiffField) {
        this.first = first;
        this.second = second;
        this.comparator = comparator;
        this.expectEq = expectEq;
        this.expectDiffField = expectDiffField;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        List<Object[]> ps = new LinkedList<>();
        FieldBaseComparator fieldBaseComparator = new FieldBaseComparator();
        GetterBaseComparator getterBaseComparator = new GetterBaseComparator();
        ps.add(new Object[]{1, 2, fieldBaseComparator, false, Collections.singletonList(new FieldInfo(Integer.class.getSimpleName(), Integer.class, 1, 2))});
        ps.add(new Object[]{1, 2, getterBaseComparator, false, Collections.singletonList(new FieldInfo(Integer.class.getSimpleName(), Integer.class, 1, 2))});

        ps.add(new Object[]{1, 1, fieldBaseComparator, true, Collections.emptyList()});
        ps.add(new Object[]{1, 1, getterBaseComparator, true, Collections.emptyList()});

        ps.add(new Object[]{"1", "12", fieldBaseComparator, false, Collections.singletonList(new FieldInfo(String.class.getSimpleName(), String.class, "1", "12"))});
        ps.add(new Object[]{"1", "12", getterBaseComparator, false, Collections.singletonList(new FieldInfo(String.class.getSimpleName(), String.class, "1", "12"))});

        ps.add(new Object[]{"1", "1", fieldBaseComparator, true, Collections.emptyList()});
        ps.add(new Object[]{"1", "1", getterBaseComparator, true, Collections.emptyList()});
        return ps;
    }

    @Test
    public void testPrimitive() {
        Assert.assertEquals(expectEq, comparator.isEquals(first, second));
        Assert.assertEquals(expectDiffField.size(), comparator.getDiffFields(first, second).size());
    }
}
