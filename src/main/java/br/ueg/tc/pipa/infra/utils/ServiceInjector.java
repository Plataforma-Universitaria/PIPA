package br.ueg.tc.pipa.infra.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;

@Component
public class ServiceInjector {

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    public <T> T createService(Class<T> clazz, Object... constructorArgs) {
        try {
            for (Constructor<?> ctor : clazz.getConstructors()) {
                if (ctor.getParameterCount() == constructorArgs.length) {
                    T instance = (T) ctor.newInstance(constructorArgs);
                    beanFactory.autowireBean(instance);
                    return instance;
                }
            }
            throw new IllegalArgumentException("Constructor not found for class: " + clazz.getName());
        } catch (Exception e) {
            throw new RuntimeException("Erro ao criar e injetar dependências da classe: " + clazz.getName(), e);
        }
    }
}
