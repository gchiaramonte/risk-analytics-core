package org.pillarone.riskanalytics.core.model.migration

import groovy.transform.CompileStatic

@CompileStatic
class ModelMigrationClassLoader extends URLClassLoader {

    ModelMigrationClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent)
    }

    ModelMigrationClassLoader(URL[] urls) {
        super(urls)
    }

    @Override
    Class<?> loadClass(String name) {
        try {
            Class clazz = findLoadedClass(name)
            if (clazz != null) {
                return clazz
            }

            return findClass(name)
        } catch (ClassNotFoundException e) {
            return super.loadClass(name)
        }
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) {
        try {
            Class clazz = findLoadedClass(name)
            if (clazz != null) {
                return clazz
            }

            return findClass(name)
        } catch (ClassNotFoundException e) {
            return super.loadClass(name, resolve)
        }
    }


}
