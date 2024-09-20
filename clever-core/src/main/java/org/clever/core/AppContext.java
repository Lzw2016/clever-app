package org.clever.core;

import org.springframework.beans.factory.*;
import org.springframework.beans.factory.config.NamedBeanHolder;
import org.springframework.beans.factory.support.BeanDefinitionOverrideException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.ResolvableType;
import org.clever.core.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 兼容IOC容器API，参考 {@link BeanFactory}、{@link ListableBeanFactory}、{@link BeanDefinitionRegistry}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/15 22:33 <br/>
 */
public class AppContext {
    /**
     * 内部锁
     */
    private final Object lock = new Object();
    /**
     * {@code Map<bean名称, bean对象>}
     */
    private final ConcurrentMap<String, BeanHolder<?>> allBeanByNames = new ConcurrentHashMap<>(256);
    /**
     * {@code Map<bean类型, Set<bean名称>>}
     */
    private final ConcurrentMap<Class<?>, CopyOnWriteArraySet<String>> allBeanNamesByType = new ConcurrentHashMap<>(128);

    // --------------------------------------------------------------------------------------------
    //  对外接口
    // --------------------------------------------------------------------------------------------

    /**
     * 获取bean对象
     *
     * @param beanName bean名称
     * @return bean不存在就返回null
     */
    public <T> T getBean(String beanName) {
        return getBean(beanName, false);
    }

    /**
     * 获取bean对象
     *
     * @param beanName bean名称
     * @param required false: bean不存在就返回null; true: bean不存在就抛出异常
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(String beanName, boolean required) {
        Assert.hasText(beanName, "Bean name must not be null");
        BeanHolder<?> beanHolder = allBeanByNames.get(beanName);
        if (beanHolder == null) {
            if (required) {
                throw new NoSuchBeanDefinitionException(beanName);
            }
            return null;
        }
        return (T) beanHolder.getBeanInstance();
    }

    /**
     * 获取bean对象
     *
     * @param beanName     bean名称
     * @param requiredType bean类型
     * @return bean不存在就返回null
     */
    public <T> T getBean(String beanName, Class<T> requiredType) {
        return getBean(beanName, requiredType, false);
    }

    /**
     * 获取bean对象
     *
     * @param beanName     bean名称
     * @param requiredType bean类型
     * @param required     false: bean不存在就返回null; true: bean不存在就抛出异常
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(String beanName, Class<T> requiredType, boolean required) {
        Assert.hasText(beanName, "Bean name must not be null");
        Assert.notNull(requiredType, "Bean type must not be null");
        BeanHolder<?> beanHolder = allBeanByNames.get(beanName);
        if (beanHolder != null) {
            if (!isTypeMatch(beanHolder.getBeanType(), requiredType)) {
                beanHolder = null;
            }
        }
        if (beanHolder == null) {
            if (required) {
                throw new NoSuchBeanDefinitionException(beanName);
            }
            return null;
        }
        return (T) beanHolder.getBeanInstance();
    }

    /**
     * 获取bean对象
     *
     * @param requiredType bean类型
     * @return bean不存在就返回null
     */
    public <T> T getBean(Class<T> requiredType) {
        return getBean(requiredType, false);
    }

    /**
     * 获取bean对象
     *
     * @param requiredType bean类型
     * @param required     false: bean不存在就返回null; true: bean不存在就抛出异常
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> requiredType, boolean required) {
        List<String> beanNames = new ArrayList<>();
        allBeanNamesByType.forEach((aClass, names) -> {
            if (isTypeMatch(aClass, requiredType)) {
                beanNames.addAll(names);
            }
        });
        if (beanNames.isEmpty()) {
            if (required) {
                throw new NoSuchBeanDefinitionException(ResolvableType.forType(requiredType));
            }
            return null;
        }
        List<BeanHolder<?>> beans = new ArrayList<>();
        for (String beanName : beanNames) {
            BeanHolder<?> beanHolder = allBeanByNames.get(beanName);
            if (beanHolder != null) {
                beans.add(beanHolder);
            }
        }
        if (beans.isEmpty()) {
            // 未找到bean
            if (required) {
                throw new NoSuchBeanDefinitionException(ResolvableType.forType(requiredType));
            }
            return null;
        } else if (beans.size() > 1) {
            // 找到多个bean
            BeanHolder<?> beanFound = null;
            List<String> beanNamesFound = new ArrayList<>();
            for (BeanHolder<?> bean : beans) {
                if (bean.isPrimary()) {
                    beanNamesFound.add(bean.getBeanName());
                    beanFound = bean;
                }
            }
            if (beanNamesFound.isEmpty()) {
                if (required) {
                    throw new NoSuchBeanDefinitionException(ResolvableType.forType(requiredType));
                }
                return null;
            } else if (beanNamesFound.size() > 1) {
                throw new NoUniqueBeanDefinitionException(ResolvableType.forType(requiredType), beanNamesFound.toArray(new String[0]));
            } else {
                return (T) beanFound.getBeanInstance();
            }
        } else {
            // 找到单个bean
            return (T) beans.get(0).getBeanInstance();
        }
    }

    /**
     * 获取bean对象
     *
     * @param requiredType bean类型
     * @return bean不存在就返回空集合
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getBeans(Class<T> requiredType) {
        List<String> beanNames = new ArrayList<>();
        allBeanNamesByType.forEach((aClass, names) -> {
            if (isTypeMatch(aClass, requiredType)) {
                beanNames.addAll(names);
            }
        });
        if (beanNames.isEmpty()) {
            return new ArrayList<>();
        }
        List<T> beans = new ArrayList<>();
        for (String beanName : beanNames) {
            BeanHolder<?> beanHolder = allBeanByNames.get(beanName);
            if (beanHolder != null) {
                beans.add((T) beanHolder.getBeanInstance());
            }
        }
        return beans;
    }

    /**
     * bean对象是否存在
     *
     * @param beanName bean类型
     */
    public boolean containsBean(String beanName) {
        return allBeanByNames.containsKey(beanName);
    }

    /**
     * 获取bean类型
     *
     * @param beanName bean名称
     * @return bean不存在就返回null
     */
    public Class<?> getBeanType(String beanName) {
        BeanHolder<?> beanHolder = allBeanByNames.get(beanName);
        return beanHolder == null ? null : beanHolder.getBeanType();
    }

    /**
     * bean数量
     */
    public int getBeanCount() {
        return allBeanByNames.size();
    }

    /**
     * 获取所有注册的bean名称
     */
    public String[] getBeanNames() {
        return allBeanByNames.keySet().toArray(new String[0]);
    }

    /**
     * 获取所有指定类型的bean名称
     *
     * @param type bean类型
     * @return bean不存在就返回空数组
     */
    public String[] getBeanNamesForType(Class<?> type) {
        List<String> beanNames = new ArrayList<>();
        allBeanNamesByType.forEach((aClass, names) -> {
            if (isTypeMatch(aClass, type)) {
                beanNames.addAll(names);
            }
        });
        return beanNames.toArray(new String[0]);
    }

    /**
     * 注册一个Bean对象
     *
     * @param beanName bean名称
     * @param bean     bean对象
     * @param primary  当前注册的bean是否是主要的
     */
    public void registerBean(String beanName, Object bean, boolean primary) {
        Assert.hasText(beanName, "Bean name must not be null");
        Assert.notNull(bean, "Bean must not be null");
        synchronized (lock) {
            BeanHolder<?> beanHolder = new BeanHolder<>(beanName, bean, primary);
            BeanHolder<?> existingBean = allBeanByNames.get(beanName);
            if (existingBean != null) {
                throw new BeanOverrideException(beanName, beanHolder, existingBean);
            }
            if (primary) {
                List<String> beanNames = new ArrayList<>();
                allBeanNamesByType.forEach((aClass, names) -> {
                    if (isTypeMatch(aClass, bean.getClass())) {
                        beanNames.addAll(names);
                    }
                });
                List<String> beanNamesFound = new ArrayList<>();
                for (String name : beanNames) {
                    beanHolder = allBeanByNames.get(name);
                    if (beanHolder != null && beanHolder.isPrimary()) {
                        beanNamesFound.add(beanHolder.getBeanName());
                    }
                }
                if (beanNamesFound.size() > 1) {
                    throw new NoUniqueBeanDefinitionException(ResolvableType.forType(bean.getClass()), beanNamesFound.toArray(new String[0]));
                }
            }
            allBeanByNames.put(beanName, beanHolder);
            CopyOnWriteArraySet<String> beanNames = allBeanNamesByType.computeIfAbsent(
                bean.getClass(),
                aClass -> new CopyOnWriteArraySet<>()
            );
            beanNames.add(beanName);
        }
    }

    /**
     * 删除一个Bean
     *
     * @param beanName bean名称
     */
    public void removeBean(String beanName) {
        Assert.hasText(beanName, "Bean name must not be null");
        synchronized (lock) {
            BeanHolder<?> beanHolder = allBeanByNames.remove(beanName);
            if (beanHolder != null) {
                CopyOnWriteArraySet<String> beanNames = allBeanNamesByType.get(beanHolder.getBeanType());
                if (beanNames != null) {
                    beanNames.remove(beanName);
                    if (beanNames.isEmpty()) {
                        allBeanNamesByType.remove(beanHolder.getBeanType());
                    }
                }
            }
        }
    }

    // --------------------------------------------------------------------------------------------
    //  内部实现
    // --------------------------------------------------------------------------------------------

    /**
     * 匹配bean类型
     *
     * @param beanType    bean类型
     * @param typeToMatch 待匹配的类型
     */
    private boolean isTypeMatch(Class<?> beanType, Class<?> typeToMatch) {
        return ResolvableType.forClass(typeToMatch).isAssignableFrom(ResolvableType.forClass(beanType));
    }

    /**
     * 一个简单的 bean 容器
     */
    protected static class BeanHolder<T> extends NamedBeanHolder<T> {
        private final boolean primary;

        public BeanHolder(String beanName, T beanInstance, boolean primary) {
            super(beanName, beanInstance);
            Assert.hasText(beanName, "Bean name must not be null");
            Assert.notNull(beanInstance, "Bean instance must not be null");
            this.primary = primary;
        }

        public boolean isPrimary() {
            return primary;
        }

        public Class<?> getBeanType() {
            T beanInstance = getBeanInstance();
            return beanInstance.getClass();
        }

        @Override
        public String toString() {
            return "BeanHolder{" +
                "primary=" + primary + ", " +
                "beanName='" + getBeanName() + "', " +
                "beanInstance=" + getBeanType().getName() +
                "}";
        }
    }

    /**
     * 参考 {@link BeanDefinitionOverrideException}
     */
    protected static class BeanOverrideException extends BeanDefinitionStoreException {
        private final BeanHolder<?> bean;
        private final BeanHolder<?> existingBean;

        public BeanOverrideException(String beanName, BeanHolder<?> bean, BeanHolder<?> existingBean) {
            super(
                bean.toString(),
                beanName,
                "Cannot register bean [" + bean + "] for bean '" + beanName + "': There is already [" + existingBean + "] bound."
            );
            this.bean = bean;
            this.existingBean = existingBean;
        }

        /**
         * 返回bean定义来自的资源的描述
         */
        @Override
        public String getResourceDescription() {
            return String.valueOf(super.getResourceDescription());
        }

        /**
         * 返回bean的名称
         */
        @Override
        public String getBeanName() {
            return String.valueOf(super.getBeanName());
        }

        /**
         * 返回新注册的bean定义
         *
         * @see #getBeanName()
         */
        public BeanHolder<?> getBeanDefinition() {
            return this.bean;
        }

        /**
         * 返回相同名称的现有bean定义
         *
         * @see #getBeanName()
         */
        public BeanHolder<?> getExistingDefinition() {
            return this.existingBean;
        }
    }
}
