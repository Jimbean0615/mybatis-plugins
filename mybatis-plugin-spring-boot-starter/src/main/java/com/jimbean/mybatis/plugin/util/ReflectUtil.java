package com.jimbean.mybatis.plugin.util;

import java.lang.reflect.Field;

/**
 * Created by zhangjb on 2019/6/6 17:37. <br/>
 *
 * @author: zhangjb <br/>
 * @Date: 2019/6/6 <br/>
 * @Email: <a href="mailto:zhangjb@cai-inc.com">zhangjb</a> <br/>
 * @Readme: com.jimbean.boot.mybatis.plugin.util.ReflectUtil <br/>
 */
public class ReflectUtil {

    public static Object getFieldValue0(Object obj, String name) {
        Class<?> clazz = obj.getClass();
        Field field = null;
        try {
            try {
                field = clazz.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(obj);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 根据字段的值获取该字段
     *
     * @param obj
     * @param fieldName
     * @return
     */
    public static Field getFieldByFieldName(Object obj, String fieldName) {
        for (Class<?> superClass = obj.getClass(); superClass != Object.class; superClass =
                superClass.getSuperclass()) {
            try {
                return superClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {

            }
        }
        return null;
    }

    /**
     * 获取对象某一字段的值
     *
     * @param obj
     * @param fieldName
     * @return
     * @throws SecurityException
     * @throws NoSuchFieldException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    public static Object getFieldValue(Object obj, String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = getFieldByFieldName(obj, fieldName);
        Object value = null;
        if (field != null) {
            if (field.isAccessible()) {
                value = field.get(obj);
            } else {
                field.setAccessible(true);
                value = field.get(obj);
                field.setAccessible(false);
            }
        }
        return value;
    }

    /**
     * 向对象的某一字段上设置值
     *
     * @param obj
     * @param fieldName
     * @param value
     * @throws SecurityException
     * @throws NoSuchFieldException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    public static void setFieldValue(Object obj, String fieldName, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = obj.getClass().getDeclaredField(fieldName);
        if (field.isAccessible()) {
            field.set(obj, value);
        } else {
            field.setAccessible(true);
            field.set(obj, value);
            field.setAccessible(false);
        }
    }

    static class Person {
        private String name = "cross";
    }

    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException {
        Person person = new Person();

        String name = (String) ReflectUtil.getFieldValue(person, "name");
        System.out.println(name);

        ReflectUtil.setFieldValue(person, "name", "zhangjb");

        String name2 = (String) ReflectUtil.getFieldValue(person, "name");
        System.out.println(name2);
    }
}
