/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.apache.ofbiz.base.util;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An observable object.
 * <p>This class is similar to <code>java.util.Observable</code> but it has some differences:</p>
 * <ul>
 *   <li>It has improved concurrency</li>
 *   <li>It cannot be subclassed</li>
 *   <li>The <code>notifyObservers</code> method does not clear the changed flag</li>
 *   <li>Protected methods have been made public</li>
 * </ul>
 *
 */
public final class Observable {

    private final AtomicBoolean changed = new AtomicBoolean();
    private final CopyOnWriteArrayList<Observer> observers = new CopyOnWriteArrayList<>();

    public Observable() {}

    public Observable(Observable observable) {
        Assert.notNull("observable", observable);
        changed.set(observable.changed.get());
        observers.addAll(observable.observers);
    }

    /**
     * Adds an observer to the set of observers for this object.
     *
     * @param observer the observer to be added.
     */
    public void addObserver(Observer observer) {
        Assert.notNull("observer", observer);
        observers.addIfAbsent(observer);
    }

    /**
     * Clears the changed flag.
     */
    public void clearChanged() {
        changed.set(false);
    }

    /**
     * Deletes an observer from the set of observers of this object.
     * Passing <code>null</code> to this method will have no effect.
     *
     * @param observer the observer to be deleted.
     */
    public void deleteObserver(Observer observer) {
        observers.remove(observer);
    }

    /**
     * Clears the observer list so that this object no longer has any observers.
     */
    public void deleteObservers() {
        observers.clear();
    }

    /**
     * Returns <code>true</code> if this object has changed.
     *
     */
    public boolean hasChanged() {
        return changed.get();
    }

    /**
     * Notify all of the observers.
     * <p>Each {@code Observer} has its {@code update} method called with two
     * arguments: this observable object and {@code null}. In other
     * words, this method is equivalent to:
     * </p>
     * <blockquote>
     *   {@code notifyObservers(null)}
     * </blockquote>
     *
     */
    public void notifyObservers() {
        notifyObservers(null);
    }

    /**
     * Notify all of the observers.
     * <p>Each observer has its <code>update</code> method called with two
     * arguments: this observable object and the <code>arg</code> argument.</p>
     *
     */
    public void notifyObservers(Object arg) {
        for (Observer observer : observers) {
            observer.update(this, arg);
        }
    }

    /**
     * Sets the changed flag to <code>true</code>.
     */
    public void setChanged() {
        changed.set(true);
    }
}
