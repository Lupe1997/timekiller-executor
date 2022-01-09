package com.moonflying.timekiller.executor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @Author ffei
 * @Date 2021/11/20 22:38
 */
public class Test {
    public static void main(String[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class<Son> clazz = Son.class;
        Method method = clazz.getDeclaredMethod("printI", Mom.class);

        Class<?>[] paramTypes = method.getParameterTypes();
        method.invoke(new Son(),new Object[paramTypes.length]);
    }
}


interface Parent{

    void print();
}

class Mom implements Parent {
    int i = 0;
    @Override
    public void print() {
        System.out.println(123);
    }
}

class Son extends Mom implements Parent{
    @Override
    public void print() {
        super.print();
    }

    public void printI(Mom i) {
        System.out.println(i.i);
    }
}